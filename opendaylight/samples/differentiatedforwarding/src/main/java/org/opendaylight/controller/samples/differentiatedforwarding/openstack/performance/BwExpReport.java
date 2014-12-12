package org.opendaylight.controller.samples.differentiatedforwarding.openstack.performance;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.network.Network;


public class BwExpReport implements Serializable{

    private static final long serialVersionUID = 1L;

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

    private int missingValueCount = 0;
    private int reportErrorCount = 0;


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
            if (report.hasError()){
                reportErrorCount++;
                System.err.println("BwReport has errors: " + report);
            }

            addValue(rateStats, report.getRate(), true);
            addValue(cpuRxStats, report.getReceiverCpu(), true);
            addValue(cpuTxStats, report.getTransmitterCpu(), true);
            addValue(rttStats, report.getRtt(), true);
            addValue(retransStats, report.getRetrans(), true);

            if (report.getReceiverHost().equalsIgnoreCase(report.getTransmitterHost())){

                addValue(rateStatsSameHyper, report.getRate());
                addValue(cpuRxStatsSameHyper, report.getReceiverCpu());
                addValue(cpuTxStatsSameHyper, report.getTransmitterCpu());
                addValue(rttStatsSameHyper, report.getRtt());
                addValue(retransStatsSameHyper, report.getRetrans());

            } else {

                addValue(rateStatsDiffHyper, report.getRate());
                addValue(cpuRxStatsDiffHyper, report.getReceiverCpu());
                addValue(cpuTxStatsDiffHyper, report.getTransmitterCpu());
                addValue(rttStatsDiffHyper, report.getRtt());
                addValue(retransStatsDiffHyper, report.getRetrans());
            }
        }
    }

    private void addValue(DescriptiveStatistics stats, Float value){
        addValue(stats, value, false);
    }

    private void addValue(DescriptiveStatistics stats, Float value, boolean countMissing){
        if (value == null) {
            // Report somewhere?
            if (countMissing) missingValueCount++;
            return;
        } else {
            stats.addValue(value);
        }
    }

    public String getSummary() {
        return ToStringBuilder.reflectionToString(this, new BwExpReportToStringStyle(false));
    }

    public String getDetailedReport() {
        return ToStringBuilder.reflectionToString(this, new BwExpReportToStringStyle(true));
    }



    public int getClassValue() {
        return classValue;
    }

    public int getNetworkIndex() {
        return networkIndex;
    }

    public int getInstanceNum() {
        return instanceNum;
    }

    public int getRetries() {
        return retries;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public int getAcceptableFailurePercentage() {
        return acceptableFailurePercentage;
    }

    public Date getStartTime() {
        return startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public boolean isDeleteNetwork() {
        return deleteNetwork;
    }

    public boolean isDeleteInstances() {
        return deleteInstances;
    }

    public boolean isNetworkMayExist() {
        return networkMayExist;
    }

    public boolean isRunClassExpConcurrently() {
        return runClassExpConcurrently;
    }

    public boolean isRunInstanceExpConcurrently() {
        return runInstanceExpConcurrently;
    }

    public Network getNetwork() {
        return network;
    }

    public List<? extends Server> getInstances() {
        return instances;
    }

    public Set<Server> getReachableInstances() {
        return reachableInstances;
    }

    public Set<Server> getNotReachableinstances() {
        return notReachableinstances;
    }

    public ArrayList<BwReport> getBwReports() {
        return nuttcpReports;
    }

    public int getMissingValueCount(){
        return missingValueCount;
    }

    public int getReportErrorCount(){
        return reportErrorCount;
    }

    public DescriptiveStatistics getRateStats() {
        return rateStats;
    }

    public DescriptiveStatistics getRateStatsDiffHyper() {
        return rateStatsDiffHyper;
    }

    public DescriptiveStatistics getRateStatsSameHyper() {
        return rateStatsSameHyper;
    }

    public DescriptiveStatistics getCpuRxStats() {
        return cpuRxStats;
    }

    public DescriptiveStatistics getCpuRxStatsDiffHyper() {
        return cpuRxStatsDiffHyper;
    }

    public DescriptiveStatistics getCpuRxStatsSameHyper() {
        return cpuRxStatsSameHyper;
    }

    public DescriptiveStatistics getCpuTxStats() {
        return cpuTxStats;
    }

    public DescriptiveStatistics getCpuTxStatsDiffHyper() {
        return cpuTxStatsDiffHyper;
    }

    public DescriptiveStatistics getCpuTxStatsSameHyper() {
        return cpuTxStatsSameHyper;
    }

    public DescriptiveStatistics getRttStats() {
        return rttStats;
    }

    public DescriptiveStatistics getRttStatsDiffHyper() {
        return rttStatsDiffHyper;
    }

    public DescriptiveStatistics getRttStatsSameHyper() {
        return rttStatsSameHyper;
    }

    public DescriptiveStatistics getRetransStats() {
        return retransStats;
    }

    public DescriptiveStatistics getRetransStatsDiffHyper() {
        return retransStatsDiffHyper;
    }

    public DescriptiveStatistics getRetransStatsSameHyper() {
        return retransStatsSameHyper;
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
