package org.opendaylight.controller.samples.differentiatedforwarding.openstack.performance;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
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

public class BwExp implements Runnable{

    private static Logger log = LoggerFactory.getLogger(BwExp.class);
    private static final int ACCEPTABLE_FAILURE_PERCENTAGE = 20;

    int classValue;
    int networkIndex;
    int instanceNum;
    boolean deleteNetwork = true;
    boolean deleteInstances = true;
    boolean networkMayExist = true;
    private ExecutorService executorService = null;

    Network network;
    List<? extends Server> instances;
    Set<Server> reachableInstances;
    Set<Server> notReachableinstances;

    public BwExp(int classValue, int networkIndex, int instanceNum) {
        this.classValue = classValue;
        this.networkIndex = networkIndex;
        this.instanceNum = instanceNum;

        init();
    }


    private void init() {
        String networkName = "class" + classValue + "net" + networkIndex;
        String cidr = "10." + classValue + "." + networkIndex + ".0/24";
        String vmNamePrefix = "vm-" + networkName;

        executorService = Executors.newCachedThreadPool();
        network = OpenStackUtil.createNetwork(networkName, cidr, true);
        instances = OpenStackUtil.createInstances(vmNamePrefix, networkName, instanceNum);
        notReachableinstances = new HashSet<>(instances);
        reachableInstances = new HashSet<>();
        programNetwork();
    }


    private void programNetwork() {
        // TODO Auto-generated method stub

    }

    private boolean isReady(){
        log.info("isReady: {}", instances);
        if (instances == null || instances.size() == 0) return false;
        for (Server server : instances) {
            if (!server.getStatus().equals(Status.ACTIVE)) return false;
        }
        checkReachables();
        if (reachableInstances.size() == instances.size()) {
            return true;
        } else if ((100 * reachableInstances.size() / instances.size()) >= (100 - ACCEPTABLE_FAILURE_PERCENTAGE)){
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
    public void run() {
        // We may need to wait and check if all instances are created, and reachable
        while(!isReady()){
            // wait
            try {
                log.warn("run: Not ready for performing the experiment yet");
                Thread.sleep(1000*6);
            } catch (InterruptedException e) {
                log.error("run: Something went wrong buddy!", e);
            }
        }

        ArrayList<AsyncNuttcpClient> nuttcpClients = new ArrayList<>();
        ArrayList<Future<BwReport>> nuttcpFutures = new ArrayList<>();
        ArrayList<BwReport> nuttcpReports = new ArrayList<>();

        for (Server server : reachableInstances) {
            NuttcpManager.startNuttcpServer(server, network);
        }

        Server[] instanceArray = reachableInstances.toArray(new Server[0]);
        for (int i = 0; i < instanceArray.length; i++) {
            AsyncNuttcpClient nuttcpClient = new AsyncNuttcpClient(instanceArray[i], instanceArray[(i + 1) % instanceArray.length], network, Type.TCP);
            Future<BwReport> report = executorService.submit(nuttcpClient);
            nuttcpFutures.add(report);
            nuttcpClients.add(nuttcpClient);
        }

        for (Future<BwReport> futureReport : nuttcpFutures) {
            try {
                BwReport report = futureReport.get();
                nuttcpReports.add(report);
            } catch (InterruptedException e) {
                log.error("run: ", e);
            } catch (ExecutionException e) {
                log.error("run: ", e);
            }
        }


        for (Server server : reachableInstances) {
            NuttcpManager.stopNuttcpServer(server, network);
        }
        executorService.shutdown();

        log.info("Got everythings: {}", nuttcpReports);
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

