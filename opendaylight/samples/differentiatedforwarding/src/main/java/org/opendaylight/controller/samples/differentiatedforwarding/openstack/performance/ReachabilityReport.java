package org.opendaylight.controller.samples.differentiatedforwarding.openstack.performance;

import org.openstack4j.model.compute.Server;
import org.openstack4j.model.network.Network;

public class ReachabilityReport {

    Server transmitter;
    Server receiver;
    Network network;

    String rawError;
    String rawOutput;
    int exitStatus;

    String transmitterIp;
    String receiverIp;
    String transmitterHost;
    String receiverHost;

    boolean reachable = false;
    float rtt = -1;
    Type type;

    public enum Type {
        PHYSICAL_VM, VM_VM
    }

    public ReachabilityReport(Server transmitter, Server receiver, Network network, String rawOutput, String rawError, int exitStatus, Type type) {
        this.transmitter = transmitter;
        this.receiver = receiver;
        this.network = network;
        this.rawError = rawError;
        this.rawOutput = rawOutput;
        this.exitStatus = exitStatus;
        this.type = type;

        setReceiverFields();
        setTransmitterFields();
        setPingFields();

    }

    private void setPingFields() {
        if (exitStatus != 0){
            reachable = false;
        } else {
            reachable = true;
        }
        if (rawOutput == null || rawOutput == ""){
            reachable = false;
            return;
        }

        String[] splits = rawOutput.split("rtt");
        if (splits.length > 1){
            rtt = Float.parseFloat(splits[1].split("/")[4]);
            reachable = true;
        } else {
            reachable = false;
        }
    }

    private void setReceiverFields() {
        receiverIp = receiver.getAddresses().getAddresses(network.getName()).get(0).getAddr();
        receiverHost = receiver.getHypervisorHostname();
    }

    private void setTransmitterFields() {
        if (type.equals(Type.PHYSICAL_VM)) return;
        transmitterIp = transmitter.getAddresses().getAddresses(network.getName()).get(0).getAddr();
        transmitterHost = transmitter.getHypervisorHostname();
    }

    public boolean isReachable() {
        return reachable;
    }

    public Server getReceiver() {
        return receiver;
    }

    @Override
    public String toString() {
        String toString = "";
        switch (type) {
        case PHYSICAL_VM:
            toString = " ReachabilityReport: "
                    + " receiverIp=" + receiverIp
                    + " receiverHost=" + receiverHost
                    + " rtt=" + rtt
                    + " reachable=" + reachable;

            break;
        case VM_VM:

            break;
        default:
            toString = "ReachabilityReport: unrecognized type \n rawOutput: " + rawOutput + "\n rawError: " + rawError;
            break;
        }
        // if error != null do
//        toString = toString + "\n rawOutput: " + rawOutput + "\n rawError: " + rawError;
        return toString;

    }
}
