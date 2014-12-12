package org.opendaylight.controller.samples.differentiatedforwarding.openstack;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.opendaylight.controller.samples.differentiatedforwarding.openstack.performance.ReachabilityReport;
import org.opendaylight.controller.samples.differentiatedforwarding.openstack.ssh.CommandOutPut;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReachabilityManager {
    private static Logger log = LoggerFactory.getLogger(ReachabilityManager.class);

    private static final int pingCount = 2;

    public static ReachabilityReport runReachability(Server receiver, Network vmNetwork) {
        String vmNameSpace = getNameSpace(vmNetwork);
        if (receiver.getAddresses() == null ||
                receiver.getAddresses().getAddresses(vmNetwork.getName()) == null ||
                receiver.getAddresses().getAddresses(vmNetwork.getName()).get(0) == null) {
            return null;
        }
        String rxAddress = receiver.getAddresses().getAddresses(vmNetwork.getName()).get(0).getAddr();
        String controllerCmd = "sudo /sbin/ip netns exec " + vmNameSpace + " ping -c " + pingCount + " " + rxAddress;


        try {
            CommandOutPut output = SshUtil.execControllerCmd(controllerCmd);
            boolean sshReachable = false;
            ReachabilityReport report = new ReachabilityReport(null, receiver, vmNetwork, output.getOutput(), output.getError(), output.getExitStatus(), sshReachable, ReachabilityReport.Type.PHYSICAL_VM);

            if (report.isPingReachable()){
                CommandOutPut sshOutPut = sshReachability(receiver, vmNetwork);
                if (sshOutPut.getExitStatus() == 0){
                    sshReachable = true;
                    report.setSshReachable(sshReachable);
                } else {
                    sshReachable = false;
                    report.setSshReachable(sshReachable);
                }
            }
            log.info("runReachability: {}", report);
            return report;
        } catch (IOException e) {
            log.error("runReachability", e);
            return new ReachabilityReport(null, receiver, vmNetwork, null, e.getMessage() + "\n" + e.getStackTrace(), -1, false, ReachabilityReport.Type.PHYSICAL_VM );
        }
    }

    private static CommandOutPut sshReachability(Server receiver, Network vmNetwork){
        String vmNameSpace = getNameSpace(vmNetwork);
        String address = receiver.getAddresses().getAddresses(vmNetwork.getName()).get(0).getAddr();
        String vmCmd = "uptime";
        String vmKeyLocation = OpenStackUtil.vmKeyPair.get(receiver.getKeyName());
        String vmUser = OpenStackUtil.defaultVmUser;
        try {
            CommandOutPut output = SshUtil.execVmCmd(vmNameSpace, vmKeyLocation, vmUser, address, vmCmd);
            log.debug("sshReachability: output {}", output);
            return output;
        } catch (IOException e) {
            log.error("sshReachability", e);
            return null;
        }
    }

    public static synchronized Set<Server> getReachables(Set<? extends Server> instances, Network network){
        ExecutorService executor = Executors.newCachedThreadPool();
        ArrayList<Future<ReachabilityReport>> futureReports = new ArrayList<>();
        Set<Server> reachableInstances = new HashSet<Server>();

        for (Server receiver : instances) {
            AsyncPingClient pingClient = new AsyncPingClient(receiver, network);
            Future<ReachabilityReport> futureReport = executor.submit(pingClient);
            futureReports.add(futureReport);
        }

        for (Future<ReachabilityReport> futureReport : futureReports) {
            try {
                ReachabilityReport reachabilityReport = futureReport.get();
                if (reachabilityReport != null && reachabilityReport.isReachable()){
                    reachableInstances.add(reachabilityReport.getReceiver());
                }
            } catch (Exception e) {
                log.error("getReachables: ", e);
            }

        }
        executor.shutdown();
        return reachableInstances;
    }

    private static String getNameSpace(Network network){
        return "qdhcp-"+network.getId();
    }


}


class AsyncPingClient implements Callable<ReachabilityReport>{

    Server receiver;
    Network network;

    public AsyncPingClient(Server receiver, Network network) {
        this.receiver = receiver;
        this.network = network;
    }

    @Override
    public ReachabilityReport call() throws Exception {
        ReachabilityReport report = ReachabilityManager.runReachability(receiver, network);
        return report;
    }

}