package org.opendaylight.controller.samples.differentiatedforwarding.openstack.performance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

    String subExpName = "";
    String expDir = "";

    public SubExp(int[] classRange, int netNum, int insNum, boolean runClassExpConcurrently, boolean runInstanceExpConcurrently, String expDir) {
        this.netNum = netNum;
        this.insNum = insNum;
        this.classRange = classRange;
        this.runClassExpConcurrently = runClassExpConcurrently;
        this.runInstanceExpConcurrently = runInstanceExpConcurrently;
        this.expDir = expDir;

        this.subExpName = "classes"+Arrays.toString(classRange)+"[con="+runClassExpConcurrently+"]"
                          +"-nets"+netNum+"-instances"+insNum+"[con="+runInstanceExpConcurrently+"]";
        log.info("executing SubExp {}", subExpName);
    }

    // class -> network -> instance
    public void exec() {
        List<BwExpReport> bwExpReports;
        String outputFile = ReportManager.getReportName(expDir + "/" + subExpName);

        if (runClassExpConcurrently) {
            bwExpReports = execConcurrently();
        } else {
            bwExpReports = execSequential();
        }
        ReportManager.writeReport(bwExpReports, outputFile, true);
        ReportManager.writeReportObjects(bwExpReports, outputFile+".obj");
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

                log.debug("class:" + classRange[classId - 1] + " network:" + net + " netInsNum:" + netInsNum);
                BwExp bwExp = new BwExp(classRange[classId - 1], net, netInsNum, runClassExpConcurrently, runInstanceExpConcurrently);
                bwExps.add(bwExp);
                log.info("{} added for concurrent exec.", bwExp.getBwExpName());

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

        // Clean up
        for (BwExp bwExp : bwExps) {
            bwExp.cleanUp();
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

                BwExp bwExp = null;
                try {
                    log.debug("class:" + classRange[classId - 1] + " network:" + net + " netInsNum:" + netInsNum);
                    bwExp = new BwExp(classRange[classId - 1], net, netInsNum, runClassExpConcurrently, runInstanceExpConcurrently);
                    log.info("{} created for sequential exec.", bwExp.getBwExpName());
                    BwExpReport bwExpReport = bwExp.call();
                    bwExpReports.add(bwExpReport);
                } catch (Exception e) {
                    log.error("exec", e);
                } finally {
                    if (bwExp != null) bwExp.cleanUp();
                    try {
                        Thread.sleep(1000*10*netInsNum);
                    } catch (InterruptedException e) {
                        log.error("exec", e);
                    }
                }

            }

        }
        return bwExpReports;
    }

    public String getSubExpName() {
        return subExpName;
    }
}
