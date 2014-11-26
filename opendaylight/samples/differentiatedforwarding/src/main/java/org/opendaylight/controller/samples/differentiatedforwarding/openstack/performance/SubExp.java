package org.opendaylight.controller.samples.differentiatedforwarding.openstack.performance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.opendaylight.controller.samples.differentiatedforwarding.openstack.ReportManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubExp {
    private static Logger log = LoggerFactory.getLogger(SubExp.class);

    int netNum;
    int insNum;
    int classRange[];

    boolean runClassExpConcurrently = false;
    boolean runInstanceExpConcurrently = false;

    String reportFilePrefix = "";

    public SubExp(int[] classRange, int netNum, int insNum, boolean runClassExpConcurrently, boolean runInstanceExpConcurrently) {
        this.netNum = netNum;
        this.insNum = insNum;
        this.classRange = classRange;
        this.runClassExpConcurrently = runClassExpConcurrently;
        this.runInstanceExpConcurrently = runInstanceExpConcurrently;

        this.reportFilePrefix = "/tmp/"+"classes"+Arrays.toString(classRange)+"[con="+runClassExpConcurrently+"]"
                                        +"-nets"+netNum+"-instances"+insNum+"[con="+runInstanceExpConcurrently+"]";
        System.out.println(Arrays.toString(classRange)+":"+netNum+":"+insNum);
    }

    // class -> network -> instance
    public void exec() {
        List<BwExpReport> bwExpReports;
        String outputFile = ReportManager.getReportName(reportFilePrefix);

        if (runClassExpConcurrently) {
            bwExpReports = execConcurrently();
        } else {
            bwExpReports = execSequential();
        }
        ReportManager.writeReport(bwExpReports, outputFile, true);

    }

    private List<BwExpReport> execConcurrently() {
        log.info("execConcurrent: Parallel execution of sub experiments");

        List<BwExp> bwExps = new ArrayList<>();
        List<BwExpReport> bwExpReports = new ArrayList<>();
        List<Future<BwExpReport>> futureBwExpReports = new ArrayList<>();

        ExecutorService executorService = Executors.newCachedThreadPool();

        int classNum = classRange.length;
        if (classNum > netNum){
            log.warn("Number of requested classes {} is more than requested networks {}. Reducing number of classes", Arrays.toString(classRange), netNum);
            classNum = netNum;
        }

        // Create BwExp classes
        for (int classId = 1; classId <= classNum; classId++) {
            int classNetNum = netNum / classNum;
            for (int net = 1; net <= classNetNum; net++) {
                int netInsNum = insNum / netNum;

                System.out.println("class:" + classRange[classId - 1] + " network:" + net + " netInsNum:" + netInsNum);
                BwExp bwExp = new BwExp(classId, net, netInsNum, runClassExpConcurrently, runInstanceExpConcurrently);
                bwExps.add(bwExp);

            }
        }

        // Submit BwExp classes
        for (BwExp bwExp : bwExps) {
            Future<BwExpReport> futureBwExpReport = executorService.submit(bwExp);
            futureBwExpReports.add(futureBwExpReport);
        }

        // Get BwExp reports
        for (Future<BwExpReport> futureBwExpReport : futureBwExpReports) {
            BwExpReport bwExpReport = null;
            try {
                bwExpReport = futureBwExpReport.get();
                bwExpReports.add(bwExpReport);
            } catch (Exception e) {
                log.error("execConcurrent: ", e);
            }
        }

        executorService.shutdown();
        return bwExpReports;
    }

    private List<BwExpReport> execSequential() {
        log.info("execSequential: Sequential execution of sub experiments");

        List<BwExpReport> bwExpReports = new ArrayList<>();
        int classNum = classRange.length;
        if (classNum > netNum){
            log.warn("Number of requested classes {} is more than requested networks {}. Reducing number of classes", Arrays.toString(classRange), netNum);
            classNum = netNum;
        }
        for (int classId = 1; classId <= classNum; classId++) {
            int classNetNum = netNum / classNum;
            for (int net = 1; net <= classNetNum; net++) {
                int netInsNum = insNum / netNum;

                try {
                    System.out.println("class:" + classRange[classId - 1] + " network:" + net + " netInsNum:" + netInsNum);
                    BwExpReport bwExpReport = new BwExp(classId, net, netInsNum, runClassExpConcurrently, runInstanceExpConcurrently).call();
                    bwExpReports.add(bwExpReport);
                } catch (Exception e) {
                    log.error("exec", e);
                }

            }

        }
        return bwExpReports;
    }
}
