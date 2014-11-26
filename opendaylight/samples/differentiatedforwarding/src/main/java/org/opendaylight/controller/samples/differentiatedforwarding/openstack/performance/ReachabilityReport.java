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

    boolean pingReachable = false;
    boolean sshReachable = false;

    float rtt = -1;
    Type type;

    public enum Type {
        PHYSICAL_VM, VM_VM
    }

    public ReachabilityReport(Server transmitter, Server receiver, Network network, String rawOutput, String rawError, int exitStatus, boolean sshReachable, Type type) {
        this.transmitter = transmitter;
        this.receiver = receiver;
        this.network = network;
        this.rawError = rawError;
        this.rawOutput = rawOutput;
        this.exitStatus = exitStatus;
        this.sshReachable = sshReachable;
        this.type = type;

        setReceiverFields();
        setTransmitterFields();
        setPingFields();

    }

    private void setPingFields() {
        if (exitStatus != 0){
            pingReachable = false;
        } else {
            pingReachable = true;
        }
        if (rawOutput == null || rawOutput == ""){
            pingReachable = false;
            return;
        }

        String[] splits = rawOutput.split("rtt");
        if (splits.length > 1){
            rtt = Float.parseFloat(splits[1].split("/")[4]);
            pingReachable = true;
        } else {
            pingReachable = false;
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
        return pingReachable && sshReachable;
    }

    public boolean isPingReachable() {
        return pingReachable ;
    }

    public void setSshReachable(boolean sshReachable) {
        this.sshReachable = sshReachable;
    }

    public boolean isSshReachable() {
        return sshReachable;
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
                    + " pingReachable=" + pingReachable
                    + " sshReachable=" + sshReachable
                    + " reachable=" + isReachable();

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
