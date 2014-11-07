package org.opendaylight.controller.samples.differentiatedforwarding.openstack.performance;

import org.openstack4j.model.compute.Server;
import org.openstack4j.model.network.Network;

public class BwReport {

    Server transmitter;
    Server receiver;
    Network network;

    String rawError;
    String rawOutput;

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
    public BwReport(Server transmitter, Server receiver, Network network, String rawOutput, String rawError, Type type) {
        this.transmitter = transmitter;
        this.receiver = receiver;
        this.network = network;
        this.rawError = rawError;
        this.rawOutput = rawOutput;
        this.type = type;

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

    @Override
    public String toString() {
        String toString = "";
        switch (type) {
        case TCP:
            toString = " BwReport: "
                    + " transmitterIp=" + transmitterIp
                    + " receiverIp=" + receiverIp
                    + " transmitterHost="  + transmitterHost
                    + " receiverHost=" + receiverHost
                    + " Rate=" + rate + "_" + rateUnit
                    + " txCpu=" + transmitterCpu
                    + " rxCpu=" + receiverCpu
                    + " retrans=" + retrans
                    + " rtt=" + rtt;

            break;
        case UDP:

            break;
        default:
            toString = "BwReport: unrecognized type \n rawOutput: " + rawOutput + "\n rawError: " + rawError;
            break;
        }
        // if error != null do
        toString = toString + "\n rawOutput: " + rawOutput + "\n rawError: " + rawError;
        return toString;
    }




}
