package org.opendaylight.controller.samples.differentiatedforwarding.openstack.performance;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.openstack4j.model.compute.Server;
import org.openstack4j.model.network.Network;

public class BwReport {

    Server transmitter;
    Server receiver;
    Network network;

    String rawError;
    String rawOutput;

    long startTime;
    long endTime;
    long duration;

    String transmitterIp;
    String receiverIp;
    String transmitterHost;
    String receiverHost;
    float rate;
    float rtt;
    int retrans;
    int transmitterCpu;
    int receiverCpu;


    RateUnit rateUnit;
    enum RateUnit {
        KBPS, MBPS, GBPS;

        public static RateUnit getUnit(String unit){
            if (KBPS.name().equalsIgnoreCase(unit)) return KBPS;
            if (MBPS.name().equalsIgnoreCase(unit)) return MBPS;
            if (GBPS.name().equalsIgnoreCase(unit)) return GBPS;
            return null;
        }
    }

    Type type;
    public enum Type {
        TCP, UDP
    }
    public BwReport(Server transmitter, Server receiver, Network network, String rawOutput, String rawError, Type type, long startTime, long endTime) {
        this.transmitter = transmitter;
        this.receiver = receiver;
        this.network = network;
        this.rawError = rawError;
        this.rawOutput = rawOutput;
        this.type = type;
        this.startTime = startTime;
        this.endTime = endTime;

        transmitterIp = transmitter.getAddresses().getAddresses(network.getName()).get(0).getAddr();
        receiverIp = receiver.getAddresses().getAddresses(network.getName()).get(0).getAddr();

        transmitterHost = transmitter.getHypervisorHostname();
        receiverHost = receiver.getHypervisorHostname();

        switch (type) {
        case TCP:
            setTcpFields();
            break;
        case UDP:
            setUdpFields();
            break;
        default:
            break;
        }

    }


    public static void main(String[] args) {
        String rawOutput = "megabytes=1594.3744 real_seconds=20.00 rate_Mbps=668.8366 tx_cpu=52 rx_cpu=52 retrans=0 rtt_ms=1.03";
        String[] keyValues = rawOutput.split("\\s+");
        for (String keyValue : keyValues) {
            String[] kv = keyValue.split("=");
            if (kv.length != 2) continue;
            System.out.println(kv[0]);
            System.out.println(kv[1]);
            if (kv[0].startsWith("rate_")) {
                float rate = Float.parseFloat(kv[1]);
                RateUnit rateUnit = RateUnit.getUnit(kv[0].split("_+")[1]);
            } else if (kv[0].equalsIgnoreCase("tx_cpu")){
                int transmitterCpu = Integer.parseInt(kv[1]);
            } else if (kv[0].equalsIgnoreCase("rx_cpu")){
                int receiverCpu = Integer.parseInt(kv[1]);
            } else if (kv[0].equalsIgnoreCase("retrans")){
                int retrans = Integer.parseInt(kv[1]);
            } else if (kv[0].equalsIgnoreCase("rtt_ms")){
                float rtt = Float.parseFloat(kv[1]);
            }
        }
    }


    private void setTcpFields() {
        String[] keyValues = rawOutput.split("\\s+");
        for (String keyValue : keyValues) {
            String[] kv = keyValue.split("=");
            if (kv.length != 2) continue;
            if (kv[0].startsWith("rate_")) {
                rate = Float.parseFloat(kv[1]);
                rateUnit = RateUnit.getUnit(kv[0].split("_")[1]);
            } else if (kv[0].equalsIgnoreCase("tx_cpu")){
                transmitterCpu = Integer.parseInt(kv[1]);
            } else if (kv[0].equalsIgnoreCase("rx_cpu")){
                receiverCpu = Integer.parseInt(kv[1]);
            } else if (kv[0].equalsIgnoreCase("retrans")){
                retrans = Integer.parseInt(kv[1]);
            } else if (kv[0].equalsIgnoreCase("rtt_ms")){
                rtt = Float.parseFloat(kv[1]);
            }
        }
    }

    private void setUdpFields() {
        // TODO Auto-generated method stub

    }



    public Server getTransmitter() {
        return transmitter;
    }


    public Server getReceiver() {
        return receiver;
    }


    public Network getNetwork() {
        return network;
    }


    public String getRawError() {
        return rawError;
    }


    public String getRawOutput() {
        return rawOutput;
    }


    public String getTransmitterIp() {
        return transmitterIp;
    }


    public String getReceiverIp() {
        return receiverIp;
    }


    public String getTransmitterHost() {
        return transmitterHost;
    }


    public String getReceiverHost() {
        return receiverHost;
    }


    public float getRate() {
        return rate;
    }


    public float getRtt() {
        return rtt;
    }


    public int getRetrans() {
        return retrans;
    }


    public int getTransmitterCpu() {
        return transmitterCpu;
    }


    public int getReceiverCpu() {
        return receiverCpu;
    }


    public RateUnit getRateUnit() {
        return rateUnit;
    }


    public Type getType() {
        return type;
    }


    @Override
    public String toString() {
        String toString = "";
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-HHmmss");
        Date start = new Date(startTime);
        Date end = new Date(endTime);

        switch (type) {
        case TCP:
            toString = "\n BwReport: "
                    + " transmitterIp=" + transmitterIp
                    + " receiverIp=" + receiverIp
                    + " transmitterHost="  + transmitterHost
                    + " receiverHost=" + receiverHost
                    + " networkName=" + network.getName()
                    + " rate=" + rate + "_" + rateUnit
                    + " txCpu=" + transmitterCpu
                    + " rxCpu=" + receiverCpu
                    + " retrans=" + retrans
                    + " rtt=" + rtt
                    + " startTime=" + dateFormat.format(start)
                    + " endTime=" + dateFormat.format(end)
                    + " duration=" + (endTime - startTime);

            if (rawError != null && !rawError.trim().equalsIgnoreCase("")){
                toString = toString
                        + " rawOutput=" + rawOutput
                        + " rawError=" + rawError;

            }

            break;
//        case UDP:
//
//            break;
        default:
            toString = "BwReport: unrecognized type \n rawOutput: " + rawOutput + "\n rawError: " + rawError;
            break;
        }
        // if error != null do
//        toString = toString + "\n rawOutput: " + rawOutput + "\n rawError: " + rawError;
        return toString;
    }




}
