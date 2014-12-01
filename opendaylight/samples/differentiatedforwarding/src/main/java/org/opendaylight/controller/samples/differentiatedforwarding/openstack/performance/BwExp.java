package org.opendaylight.controller.samples.differentiatedforwarding.openstack.performance;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.opendaylight.controller.samples.differentiatedforwarding.openstack.NuttcpManager;
import org.opendaylight.controller.samples.differentiatedforwarding.openstack.OpenStackUtil;
import org.opendaylight.controller.samples.differentiatedforwarding.openstack.ReachabilityManager;
import org.opendaylight.controller.samples.differentiatedforwarding.openstack.performance.BwReport.Type;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.compute.Server.Status;
import org.openstack4j.model.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BwExp implements Callable<BwExpReport>{

    private static Logger log = LoggerFactory.getLogger(BwExp.class);
    private static final int ACCEPTABLE_FAILURE_PERCENTAGE = 50;
    private static final int REACHABILITY_RETRIES = 20;

    private int classValue;
    private int networkIndex;
    private int instanceNum;
    private int retries = 0;
    private long startTime;
    private long endTime;

    private boolean deleteNetwork = true;
    private boolean deleteInstances = true;
    private boolean networkMayExist = true;
    private boolean runClassExpConcurrently = false;
    private boolean runInstanceExpConcurrently = false;

    private ExecutorService executorService = null;

    private Network network;
    private List<? extends Server> instances;
    private Set<Server> reachableInstances;
    private Set<Server> notReachableinstances;

    public BwExp(int classValue, int networkIndex, int instanceNum, boolean runClassExpConcurrently, boolean runInstanceExpConcurrently) {
        startTime = System.currentTimeMillis();

        this.classValue = classValue;
        this.networkIndex = networkIndex;
        this.instanceNum = instanceNum;

        this.runClassExpConcurrently = runClassExpConcurrently;
        this.runInstanceExpConcurrently = runInstanceExpConcurrently;

        init();
    }


    private void init() {
        String networkName = "class" + classValue + "net" + networkIndex;
        String cidr = "10." + classValue + "." + networkIndex + ".0/24";
        String vmNamePrefix = "vm-" + networkName + "-";

        executorService = Executors.newCachedThreadPool();
        network = OpenStackUtil.createNetwork(networkName, cidr, networkMayExist);
        instances = OpenStackUtil.createInstances(vmNamePrefix, networkName, instanceNum);
        notReachableinstances = new HashSet<>(instances);
        reachableInstances = new HashSet<>();
        programNetwork();
    }


    private void programNetwork() {
        // TODO Auto-generated method stub

    }

    private boolean isReady(){
        log.trace("isReady: {}", instances);
        if (instances == null || instances.size() == 0) return false;
        for (Server server : instances) {
            if (!server.getStatus().equals(Status.ACTIVE)) return false;
        }

        checkReachables();
        if (reachableInstances.size() == instances.size()) {
            return true;
        } else {
            return false;
        }
    }

    private void checkReachables() {
        //FIXME: check for server.hashCode, this may be problematic.

        Set<Server> currentlyReachable = ReachabilityManager.getReachables(notReachableinstances, network);
        if (currentlyReachable == null) return;
        reachableInstances.addAll(currentlyReachable);
        notReachableinstances.removeAll(reachableInstances);
    }


    @Override
    public BwExpReport call() throws Exception {
        // We may need to wait and check if all instances are created, and reachable
        boolean ready = false;
        while(!(ready = isReady()) && retries <= REACHABILITY_RETRIES){
            // wait
            retries++;
            try {
                log.warn("call: Not ready for performing the experiment yet. "
                        + "#RequestedInstances={} #ReachableInstances={} #NotReachableInstances={} #retries={}"
                        , instances.size(), reachableInstances.size(), notReachableinstances.size(), retries);
                Thread.sleep(1000*30*retries); //.5 min * #retries
            } catch (InterruptedException e) {
                log.error("run: Something went wrong buddy!", e);
            }
        }

        if (!ready && retries > REACHABILITY_RETRIES){
            if ((100 * reachableInstances.size() / instances.size()) >= (100 - ACCEPTABLE_FAILURE_PERCENTAGE)){
                // Acceptable failure percentage is not reached yet. So we can proceed with some unreachable instances
                log.warn("call: #RequestedInstances={} #ReachableInstances={} #NotReachableInstances={} ACCEPTABLE_FAILURE_PERCENTAGE={}. "
                        + "Continueing with the reachables"
                        , instances.size()
                        , reachableInstances.size()
                        , notReachableinstances.size()
                        , ACCEPTABLE_FAILURE_PERCENTAGE);
            } else {
                // screwed, return
                log.error("call: Aborting #RequestedInstances={} #ReachableInstances={} #NotReachableInstances={} ACCEPTABLE_FAILURE_PERCENTAGE={}. "
                        , instances.size()
                        , reachableInstances.size()
                        , notReachableinstances.size()
                        , ACCEPTABLE_FAILURE_PERCENTAGE);
                endTime = System.currentTimeMillis();
                BwExpReport errorReport = new BwExpReport(classValue, networkIndex, instanceNum, retries,
                        REACHABILITY_RETRIES, ACCEPTABLE_FAILURE_PERCENTAGE,
                        new Date(startTime), new Date(endTime),
                        deleteNetwork, deleteInstances, networkMayExist,
                        runClassExpConcurrently, runInstanceExpConcurrently,
                        network, instances, reachableInstances, notReachableinstances,
                        null);
                return errorReport;
            }
        }

        log.info("call: #RequestedInstances={} #ReachableInstances={} #NotReachableInstances={}"
                , instances.size()
                , reachableInstances.size()
                , notReachableinstances.size());


        ArrayList<BwReport> nuttcpReports;

        // Start servers
        for (Server server : reachableInstances) {
            NuttcpManager.startNuttcpServer(server, network);
        }

        // Run clients
        if (runInstanceExpConcurrently){
            nuttcpReports = runInstanceExpConcurrently();
        } else {
            nuttcpReports = runInstanceExpSequentially();
        }

        // Stop servers! Not really needed if we're deleting instances right after

//        for (Server server : reachableInstances) {
//            NuttcpManager.stopNuttcpServer(server, network);
//        }

        executorService.shutdown();

        log.info("Got everythings: \n{}", nuttcpReports);

        endTime = System.currentTimeMillis();
        BwExpReport report = new BwExpReport(classValue, networkIndex, instanceNum, retries,
                                            REACHABILITY_RETRIES, ACCEPTABLE_FAILURE_PERCENTAGE,
                                            new Date(startTime), new Date(endTime),
                                            deleteNetwork, deleteInstances, networkMayExist,
                                            runClassExpConcurrently, runInstanceExpConcurrently,
                                            network, instances, reachableInstances, notReachableinstances,
                                            nuttcpReports);
        log.info("Call: {}", report.getSummary());
//        log.info("Call: {}", report.getDetailedReport());
//        cleanUp();

        return report;
    }

    private ArrayList<BwReport> runInstanceExpConcurrently(){
        log.info("runInstanceExpConcurrently: Parallel execution of the experiment across instances");
        List<AsyncNuttcpClient> nuttcpClients = new ArrayList<>();
        List<Future<BwReport>> nuttcpFutures = new ArrayList<>();
        ArrayList<BwReport> bwReports = new ArrayList<>();

        // Run clients
        Server[] instanceArray = reachableInstances.toArray(new Server[0]);
        for (int i = 0; i < instanceArray.length; i++) {
            AsyncNuttcpClient nuttcpClient = new AsyncNuttcpClient(instanceArray[i], instanceArray[(i + 1) % instanceArray.length], network, Type.TCP);
            nuttcpClients.add(nuttcpClient);

            Future<BwReport> report = executorService.submit(nuttcpClient);
            nuttcpFutures.add(report);
        }

        // Get reports
        for (Future<BwReport> futureReport : nuttcpFutures) {
            try {
                BwReport report = futureReport.get();
                bwReports.add(report);
            } catch (Exception e) {
                log.error("call: ", e);
            }
        }
        return bwReports;
    }

    private ArrayList<BwReport> runInstanceExpSequentially(){
        log.info("runInstanceExpSequentially: Sequential execution of the experiment across instances");
        ArrayList<BwReport> bwReports = new ArrayList<>();

        for (Server transmitter : reachableInstances) {
            for (Server receiver : reachableInstances) {
                if (transmitter.equals(receiver)) continue;

                BwReport bwReport = NuttcpManager.runNuttcpClient(transmitter, receiver, network, Type.TCP);
                bwReports.add(bwReport);
            }
        }
        return bwReports;
    }

    public void cleanUp() {
        try {
            if (deleteInstances)
                OpenStackUtil.deleteInstances(instances);
            if (deleteInstances && deleteNetwork)
                OpenStackUtil.deleteNetworkById(network.getId());
        } catch (Exception e) {
            log.error("cleanUp: ", e);
        }
    }

}


class AsyncNuttcpClient implements Callable<BwReport>{

    Server transmitter;
    Server receiver;
    Network network;
    BwReport.Type type;

    public AsyncNuttcpClient(Server transmitter, Server receiver, Network network, Type type) {
        this.transmitter = transmitter;
        this.receiver = receiver;
        this.network = network;
        this.type = type;
    }

    @Override
    public BwReport call() throws Exception {
        BwReport report = NuttcpManager.runNuttcpClient(transmitter, receiver, network, type);
        return report;
    }

}

