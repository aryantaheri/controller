package org.opendaylight.controller.samples.differentiatedforwarding.openstack.performance;

import org.opendaylight.controller.samples.differentiatedforwarding.openstack.OpenStackUtil;
import org.openstack4j.model.network.Network;

public class Exp {

    public static final boolean DELETE_INSTANCES = true;
    public static final boolean DELETE_NETWORKS = true;

    public static final boolean RUN_CLASSES_CONCURRENTLY = true;
    public static final boolean RUN_INSTANCES_CONCURRENTLY = true;

    int[] instanceRange;
    int[] networkRange;

    // Total number of instances
    int maxInstances = 128;
    int maxNetworks = 32;
    int minInstances = 1;
    int minNetworks = 1;

    // max DSCP 64, 6bit
    int[] classRange = {1, 2, 3, 4};

    // Sequential vs Concurrent runs of the experiments.
    boolean runClassExpConcurrently = false;
    boolean runInstanceExpConcurrently = false;

    public Exp(int[] classRange, int minNetworks, int maxNetworks, int minInstances, int maxInstances, boolean runClassExpConcurrently, boolean runInstanceExpConcurrently) {
        this.minInstances = minInstances;
        this.maxInstances = maxInstances;
        this.minNetworks = minNetworks;
        this.maxNetworks = maxNetworks;

        this.classRange = classRange;
        //(n & (n - 1)) != 0 return error checking for 2^x conformity

        this.runClassExpConcurrently = runClassExpConcurrently;
        this.runInstanceExpConcurrently = runInstanceExpConcurrently;
    }

    public void exec() {
        for (int netNum = minNetworks; netNum <= maxNetworks; netNum = netNum * 2) {
            for (int insNum = Math.max(netNum, minInstances); insNum <= maxInstances; insNum = insNum * 2) {
                SubExp subExp = new SubExp(classRange, netNum, insNum, runClassExpConcurrently, runInstanceExpConcurrently);
                subExp.exec();
                System.out.println("----------------");
            }
        }
            // create network
            // create instances
            // program network
            // run subexp
            // write results
            // delete instances
            // delete network
    }

    public static void main(String[] args) {
        int[] classRange = {1};
        new Exp( classRange, 1, 8, 1, 32, RUN_CLASSES_CONCURRENTLY, RUN_INSTANCES_CONCURRENTLY).exec();
    }
}
