package org.opendaylight.controller.samples.differentiatedforwarding.openstack;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
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
        String rxAddress = receiver.getAddresses().getAddresses(vmNetwork.getName()).get(0).getAddr();
        String controllerCmd = "sudo /sbin/ip netns exec " + vmNameSpace + " ping -c " + pingCount + " " + rxAddress;


        try {
            CommandOutPut output = SshUtil.execControllerCmd(controllerCmd);
            ReachabilityReport report = new ReachabilityReport(null, receiver, vmNetwork, output.getOutput(), output.getError(), output.getExitStatus(), ReachabilityReport.Type.PHYSICAL_VM);
            return report;
        } catch (IOException e) {
            log.error("runReachability", e);
            return new ReachabilityReport(null, receiver, vmNetwork, null, e.getMessage() + "\n" + e.getStackTrace(), -1, ReachabilityReport.Type.PHYSICAL_VM );
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
                if (reachabilityReport.isReachable()){
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