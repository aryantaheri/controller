package org.opendaylight.controller.samples.differentiatedforwarding.openstack;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.samples.differentiatedforwarding.openstack.performance.BwReport;
import org.opendaylight.controller.samples.differentiatedforwarding.openstack.ssh.CommandOutPut;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NuttcpManager {
    private static Logger log = LoggerFactory.getLogger(NuttcpManager.class);

    static int PERIOD = 60; // Transmission period in second

    /**
     * Start nuttcp server on all instance with -S, and iterate over client instances
     * @param servers
     * @param vmNameSpace
     */
    public static void measureBwPersistentServer(List<? extends Server> servers, Network vmNetwork, BwReport.Type type){
        List<BwReport> reports = new ArrayList<>();
        for (Server server : servers) {
            startNuttcpServer(server, vmNetwork);
        }

        for (Server transmitter : servers) {
            for (Server receiver : servers) {
                if (transmitter.equals(receiver)) continue;
                BwReport report = runNuttcpClient(transmitter, receiver, vmNetwork, type);
                reports.add(report);
            }
        }

        for (Server server : servers) {
            stopNuttcpServer(server, vmNetwork);
        }

        log.info("measureBwPersistentServer: Measurement reports: \n {}", reports);
    }



    public static void startNuttcpServer(Server server, Network vmNetwork){
        String vmNameSpace = getNameSpace(vmNetwork);
        String address = server.getAddresses().getAddresses(vmNetwork.getName()).get(0).getAddr();
        String vmCmd = "sudo /usr/bin/nuttcp -S";
        String vmKeyLocation = OpenStackUtil.vmKeyPair.get(server.getKeyName());
        String vmUser = OpenStackUtil.defaultVmUser;
        try {
            CommandOutPut output = SshUtil.execVmCmd(vmNameSpace, vmKeyLocation, vmUser, address, vmCmd);
            log.debug("startNuttcpServer: output {}", output);
        } catch (IOException e) {
            log.error("startNuttcpServer", e);
        }
    }

    public static BwReport runNuttcpClient(Server transmitter, Server receiver, Network vmNetwork, BwReport.Type type) {
        String vmNameSpace = getNameSpace(vmNetwork);
        String txAddress = transmitter.getAddresses().getAddresses(vmNetwork.getName()).get(0).getAddr();
        String rxAddress = receiver.getAddresses().getAddresses(vmNetwork.getName()).get(0).getAddr();
        String vmKeyLocation = OpenStackUtil.vmKeyPair.get(transmitter.getKeyName());
        String vmUser = OpenStackUtil.defaultVmUser;
        String vmTxCmd = null;

        switch (type) {
        case TCP:
            vmTxCmd = "sudo /usr/bin/nuttcp -t " + " -T" + PERIOD + " -fparse " + rxAddress;
            break;
        case UDP:
            vmTxCmd = "sudo /usr/bin/nuttcp -t " + " -T" + PERIOD + " -fparse -R10g -u " + rxAddress;
            break;
        }

        long startTime = System.currentTimeMillis();
        long endTime;
        try {
            CommandOutPut txOutput = SshUtil.execVmCmd(vmNameSpace, vmKeyLocation, vmUser, txAddress, vmTxCmd);
            endTime = System.currentTimeMillis();

            BwReport bwReport = new BwReport(transmitter, receiver, vmNetwork, txOutput.getOutput(), txOutput.getError(), type, startTime, endTime);
//            log.info("runNuttcpClient: txOutput {}", txOutput);
            return bwReport;
        } catch (IOException e) {
            log.error("runNuttcpClient", e);
            endTime = System.currentTimeMillis();
            return new BwReport(transmitter, receiver, vmNetwork, null, e.getMessage() + "\n" + e.getStackTrace(), type, startTime, endTime);
        }
    }

    public static void stopNuttcpServer(Server server, Network vmNetwork) {
        String vmNameSpace = getNameSpace(vmNetwork);
        String address = server.getAddresses().getAddresses(vmNetwork.getName()).get(0).getAddr();
        String vmCmd = "sudo killall -9 nuttcp";
        String vmKeyLocation = OpenStackUtil.vmKeyPair.get(server.getKeyName());
        String vmUser = OpenStackUtil.defaultVmUser;
        try {
            CommandOutPut output = SshUtil.execVmCmd(vmNameSpace, vmKeyLocation, vmUser, address, vmCmd);
            log.debug("stopNuttcpServer: output {}", output);
        } catch (IOException e) {
            log.error("stopNuttcpServer", e);
        }
    }

    /**
     * Start one-shoot server each time, so it will terminate after a client finishes the measurement.
     * @param servers
     * @param vmNameSpace
     * @param useIntermediate
     */
    public static void measureBw(List<? extends Server> servers, Network vmNetwork, BwReport.Type type){
        List<BwReport> reports = new ArrayList<>();
        for (Server src : servers) {
            for (Server dst : servers) {
                if (src.equals(dst)) continue;
                BwReport report = measureBw(src, dst, vmNetwork, type);
                reports.add(report);
            }
        }
        log.info("measureBw: Measurement reports: \n {}", reports);
    }

    private static BwReport measureBw(Server src, Server dst, Network vmNetwork, BwReport.Type type) {
        String vmNameSpace = getNameSpace(vmNetwork);
        String srcAddress = src.getAddresses().getAddresses(vmNetwork.getName()).get(0).getAddr();
        String dstAddress = dst.getAddresses().getAddresses(vmNetwork.getName()).get(0).getAddr();
        String vmKeyLocation = OpenStackUtil.vmKeyPair.get(src.getKeyName());
        String vmUser = OpenStackUtil.defaultVmUser;
        String vmDstCmd = "sudo /usr/bin/nuttcp -1";
        String vmSrcCmd = null;

        switch (type) {
        case TCP:
            vmSrcCmd = "sudo /usr/bin/nuttcp -t " + " -T" + PERIOD + " -fparse " + dstAddress;
            break;
        case UDP:
            vmSrcCmd = "sudo /usr/bin/nuttcp -t " + " -T" + PERIOD + " -fparse -R10g -u " + dstAddress ;
            break;
        }
        long startTime = System.currentTimeMillis();
        long endTime;
        try {
            CommandOutPut dstOutput = SshUtil.execVmCmd(vmNameSpace, vmKeyLocation, vmUser, dstAddress, vmDstCmd);
            log.debug("measureBw: dstOutput {}", dstOutput);
            CommandOutPut srcOutput = SshUtil.execVmCmd(vmNameSpace, vmKeyLocation, vmUser, srcAddress, vmSrcCmd);
            log.info("measureBw: srcOutput {}", srcOutput);
            endTime = System.currentTimeMillis();
            BwReport bwReport = new BwReport(src, dst, vmNetwork, srcOutput.getOutput(), srcOutput.getError(), type, startTime, endTime);
            return bwReport;
        } catch (IOException e) {
            log.error("measureBw", e);
            endTime = System.currentTimeMillis();
            return new BwReport(src, dst, vmNetwork, null, e.getMessage() + "\n" + e.getStackTrace(), type, startTime, endTime);
        }
    }

    private static String getNameSpace(Network network){
        return "qdhcp-"+network.getId();
    }
}
