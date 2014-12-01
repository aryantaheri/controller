package org.opendaylight.controller.samples.differentiatedforwarding.openstack;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.opendaylight.controller.samples.differentiatedforwarding.openstack.performance.BwExpReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReportManager {
    private static Logger log = LoggerFactory.getLogger(ReportManager.class);

    public static String getReportName(String prefix){
        String name;
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-HHmmss");
        Date date = new Date();
        name = prefix + "-" + dateFormat.format(date);
        return name;
    }

    public static String createExpDir(String dirPath){
        String dirName = getReportName(dirPath+"/exp");
        if(new File(dirName).mkdirs()){
            return dirName;
        } else {
            return null;
        }
    }

    public static void writeReport(List<BwExpReport> reports, String outputFile, boolean append){
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new BufferedWriter(new FileWriter(outputFile, append)));
            for (BwExpReport bwExpReport : reports) {
                if (bwExpReport == null) continue;
                writer.println(bwExpReport.toString());
            }
        } catch (IOException e) {
            log.error("writeReport", e);
        } finally {
            if (writer != null){
                writer.close();
            }
        }
        log.info("writeReport: generated log: {}", outputFile);
    }

    public static void writeReportObjects(List<BwExpReport> reports, String outputFile) {
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(new FileOutputStream(outputFile));
            oos.writeObject(reports);
        } catch (Exception e) {
            log.error("writeReportObjects", e);
        } finally {
            try {
                if (oos != null) oos.close();
            } catch (IOException e) {
                log.error("writeReportObjects", e);
            }
        }
    }

    public static List<BwExpReport> readReportObjects(String inputFile){
        List<BwExpReport> reports = null;
        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream(new FileInputStream(inputFile));
            reports = (List<BwExpReport>) ois.readObject();
        } catch (Exception e) {
            log.error("readReportObjects", e);
        } finally {
            try {
                ois.close();
            } catch (IOException e) {
                log.error("readReportObjects", e);
            }
        }
        return reports;
    }

    public static void writeReport(String[] reports, String outputFile, boolean append){
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new BufferedWriter(new FileWriter(outputFile, append)));
            for (String bwReport : reports) {
                writer.println(bwReport.toString());
            }
        } catch (IOException e) {
            log.error("writeReport", e);
        } finally {
            if (writer != null){
                writer.close();
            }
        }
        log.info("writeReport: generated log: {}", outputFile);
    }

    public static void writeReport(Object report, String outputFile, boolean append){
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new BufferedWriter(new FileWriter(outputFile, append)));
            writer.println(report.toString());
        } catch (IOException e) {
            log.error("writeReport", e);
        } finally {
            if (writer != null){
                writer.close();
            }
        }
        log.info("writeReport: generated log: {}", outputFile);
    }

    public static void main(String[] args) {
//        writeReport(new String[]{"a", "b", "c"}, "/tmp/bw-20141117-181019", false);
//        int classValue, int networkIndex, int instanceNum,
//        int retries, int maxRetries, int acceptableFailurePercentage,
//        Date startTime, Date endTime,
//        boolean deleteNetwork, boolean deleteInstances,
//        boolean networkMayExist, boolean runClassExpConcurrently,
//        boolean runInstanceExpConcurrently, Network network,
//        List<? extends Server> instances, Set<Server> reachableInstances,
//        Set<Server> notReachableinstances, ArrayList<BwReport> nuttcpReports

//        BwExpReport bwExpReport = new BwExpReport(1, 1, 1,
//                                                    1, 1, 1,
//                                                    new Date(), new Date(),
//                                                    true, true,
//                                                    true, true,
//                                                    true, null,
//                                                    new ArrayList<Server>(), new HashSet<Server>(),
//                                                    new HashSet<Server>(), new ArrayList<BwReport>());
//        List<BwExpReport> reports = new ArrayList<>();
//        reports.add(bwExpReport);
//
//        writeReportObjects(reports, "/tmp/obj");

        List<BwExpReport> reports2 = readReportObjects("/tmp/classes[1][con=true]-nets2-instances16[con=true]-20141126-180419.obj");

        System.out.println(reports2);
    }


}
