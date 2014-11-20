package org.opendaylight.controller.samples.differentiatedforwarding.openstack;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.opendaylight.controller.samples.differentiatedforwarding.openstack.performance.BwReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReportManager {
    private static Logger log = LoggerFactory.getLogger(ReportManager.class);

    public static void main(String[] args) {
        writeReport(new String[]{"a", "b", "c"}, "/tmp/bw-20141117-181019", false);
    }

    public static String getReportName(String prefix){
        String name;
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-HHmmss");
        Date date = new Date();
        name = prefix + "-" + dateFormat.format(date);
        return name;
    }

    public static void writeReport(List<BwReport> reports, String outputFile, boolean append){
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new BufferedWriter(new FileWriter(outputFile, append)));
            for (BwReport bwReport : reports) {
                writer.println(bwReport.toString());
            }
        } catch (IOException e) {
            log.error("writeReport", e);
        } finally {
            if (writer != null){
                writer.close();
            }
        }
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
    }
}
