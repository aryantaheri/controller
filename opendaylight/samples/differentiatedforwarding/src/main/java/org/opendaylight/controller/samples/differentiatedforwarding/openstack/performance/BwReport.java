package org.opendaylight.controller.samples.differentiatedforwarding.openstack.performance;

import org.openstack4j.model.compute.Server;

public class BwReport {

    Server transmitter;
    Server receiver;
    String rawError;
    String rawOutput;

    String trasmitterIp;
    String receiverIp;
    String trasmitterHost;
    String receiverHost;
    int rate;
    RateUnit rateUnit;

    enum RateUnit {
        Kbps, Mbps, Gbps
    }
    public BwReport(Server transmitter, Server receiver, String rawOutput, String rawError) {
        this.transmitter = transmitter;
        this.receiver = receiver;
        this.rawError = rawError;
        this.rawOutput = rawOutput;
    }

    @Override
    public String toString() {
        // TODO Auto-generated method stub
        return super.toString();
    }


}
