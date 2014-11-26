package org.opendaylight.controller.samples.differentiatedforwarding.openstack.performance;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.network.Network;

import com.google.common.math.DoubleMath;

public class BwExpReport {

    private int classValue;
    private int networkIndex;
    private int instanceNum;
    private int retries;
    private int maxRetries;
    private int acceptableFailurePercentage;
    private Date startTime;
    private Date endTime;

    private boolean deleteNetwork;
    private boolean deleteInstances = true;
    private boolean networkMayExist = true;

    private boolean runClassExpConcurrently = false;
    private boolean runInstanceExpConcurrently = false;

    private Network network;
    private List<? extends Server> instances;
    private Set<Server> reachableInstances;
    private Set<Server> notReachableinstances;

    ArrayList<BwReport> nuttcpReports;


    public BwExpReport(int classValue, int networkIndex, int instanceNum,
            int retries, int maxRetries, int acceptableFailurePercentage,
            Date startTime, Date endTime,
            boolean deleteNetwork, boolean deleteInstances,
            boolean networkMayExist, boolean runClassExpConcurrently,
            boolean runInstanceExpConcurrently, Network network,
            List<? extends Server> instances, Set<Server> reachableInstances,
            Set<Server> notReachableinstances, ArrayList<BwReport> nuttcpReports) {

        this.classValue = classValue;
        this.networkIndex = networkIndex;
        this.instanceNum = instanceNum;
        this.retries = retries;
        this.maxRetries = maxRetries;
        this.acceptableFailurePercentage = acceptableFailurePercentage;
        this.startTime = startTime;
        this.endTime = endTime;
        this.deleteNetwork = deleteNetwork;
        this.deleteInstances = deleteInstances;
        this.networkMayExist = networkMayExist;
        this.runClassExpConcurrently = runClassExpConcurrently;
        this.runInstanceExpConcurrently = runInstanceExpConcurrently;
        this.network = network;
        this.instances = instances;
        this.reachableInstances = reachableInstances;
        this.notReachableinstances = notReachableinstances;
        this.nuttcpReports = nuttcpReports;

        prepareStats();
    }

    DescriptiveStatistics rateStats = new DescriptiveStatistics();
    DescriptiveStatistics rateStatsDiffHyper = new DescriptiveStatistics();
    DescriptiveStatistics rateStatsSameHyper = new DescriptiveStatistics();

    DescriptiveStatistics cpuRxStats = new DescriptiveStatistics();
    DescriptiveStatistics cpuRxStatsDiffHyper = new DescriptiveStatistics();
    DescriptiveStatistics cpuRxStatsSameHyper = new DescriptiveStatistics();

    DescriptiveStatistics cpuTxStats = new DescriptiveStatistics();
    DescriptiveStatistics cpuTxStatsDiffHyper = new DescriptiveStatistics();
    DescriptiveStatistics cpuTxStatsSameHyper = new DescriptiveStatistics();

    DescriptiveStatistics rttStats = new DescriptiveStatistics();
    DescriptiveStatistics rttStatsDiffHyper = new DescriptiveStatistics();
    DescriptiveStatistics rttStatsSameHyper = new DescriptiveStatistics();

    DescriptiveStatistics retransStats = new DescriptiveStatistics();
    DescriptiveStatistics retransStatsDiffHyper = new DescriptiveStatistics();
    DescriptiveStatistics retransStatsSameHyper = new DescriptiveStatistics();

    private void prepareStats() {
        for (BwReport report : nuttcpReports) {
            rateStats.addValue(report.getRate());
            cpuRxStats.addValue(report.getReceiverCpu());
            cpuTxStats.addValue(report.getTransmitterCpu());
            rttStats.addValue(report.getRtt());
            retransStats.addValue(report.getRetrans());

            if (report.getReceiverHost().equalsIgnoreCase(report.getTransmitterHost())){
                rateStatsSameHyper.addValue(report.getRate());
                cpuRxStatsSameHyper.addValue(report.getReceiverCpu());
                cpuTxStatsSameHyper.addValue(report.getTransmitterCpu());
                rttStatsSameHyper.addValue(report.getRtt());
                retransStatsSameHyper.addValue(report.getRetrans());

            } else {
                rateStatsDiffHyper.addValue(report.getRate());
                cpuRxStatsDiffHyper.addValue(report.getReceiverCpu());
                cpuTxStatsDiffHyper.addValue(report.getTransmitterCpu());
                rttStatsDiffHyper.addValue(report.getRtt());
                retransStatsDiffHyper.addValue(report.getRetrans());
            }
        }
    }

    public String getSummary() {
        // TODO Auto-generated method stub
        return ToStringBuilder.reflectionToString(this, new BwExpReportToStringStyle(false));
    }

    public String getDetailedReport() {
        return ToStringBuilder.reflectionToString(this, new BwExpReportToStringStyle(true));
    }

    @Override
    public String toString() {
        return getSummary();
    }
}

class BwExpReportToStringStyle extends ToStringStyle{

    boolean detailed = true;
    public BwExpReportToStringStyle(boolean detailed) {
        super();
        this.detailed = detailed;
        this.setUseShortClassName(true);
        this.setUseIdentityHashCode(false);
        this.setFieldSeparator("\n");
        this.setArraySeparator("\n");
    }
    @Override
    protected void appendDetail(StringBuffer buffer, String fieldName, Object value) {
        if (value instanceof DescriptiveStatistics) {
            DescriptiveStatistics stats = (DescriptiveStatistics) value;
            buffer.append("{");
            buffer.append(fieldName + "Mean=" + stats.getMean());
            buffer.append(",");
            buffer.append(fieldName + "Std=" + stats.getStandardDeviation());
            buffer.append(",");
            buffer.append(fieldName + "Min=" + stats.getMin());
            buffer.append(",");
            buffer.append(fieldName + "Max=" + stats.getMax());
            buffer.append("}");
        } else if (value instanceof Network) {
            buffer.append(((Network) value).getName());
        } else if (value instanceof Date) {
            DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-HHmmss");
            buffer.append(dateFormat.format(value));
        } else {
            buffer.append(value);

        }
    }

    @Override
    protected void appendDetail(StringBuffer buffer, String fieldName, Collection coll) {
        if (detailed){
            super.appendDetail(buffer, fieldName, coll);
            return;
        }

        if (fieldName.equals("instances")){
            buffer.append(coll.size());
        } else if (fieldName.equals("reachableInstances")) {
            buffer.append(coll.size());
        } else if (fieldName.equals("notReachableinstances")) {
            buffer.append(coll.size());
        } else {
            super.appendDetail(buffer, fieldName, coll);
        }
    }
}
