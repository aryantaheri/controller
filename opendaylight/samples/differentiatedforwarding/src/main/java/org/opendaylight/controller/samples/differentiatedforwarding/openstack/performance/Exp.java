package org.opendaylight.controller.samples.differentiatedforwarding.openstack.performance;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.samples.differentiatedforwarding.openstack.ReportManager;
import org.opendaylight.controller.samples.differentiatedforwarding.openstack.ssh.OvsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Exp {

    private static Logger log = LoggerFactory.getLogger(Exp.class);

    public static final boolean DELETE_INSTANCES = true;
    public static final boolean DELETE_NETWORKS = true;
    public static final boolean FIX_OVS = false;

    // This is more like a wrong name. It should be ClassNetwork concurrency. It runs the networks concurrently with the given classes.
    public static final boolean RUN_CLASSES_CONCURRENTLY = true;
    public static final boolean RUN_INSTANCES_CONCURRENTLY = false;

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

    String expDir = "";

    public Exp(int[] classRange, int minNetworks, int maxNetworks, int minInstances, int maxInstances, boolean runClassExpConcurrently, boolean runInstanceExpConcurrently) {

        this.minInstances = minInstances;
        this.maxInstances = maxInstances;
        this.minNetworks = minNetworks;
        this.maxNetworks = maxNetworks;

        this.classRange = classRange;
        //(n & (n - 1)) != 0 return error checking for 2^x conformity

        this.runClassExpConcurrently = runClassExpConcurrently;
        this.runInstanceExpConcurrently = runInstanceExpConcurrently;
        this.expDir = ReportManager.createExpDir("/home/aryan/data");
    }

    public void exec() {
        List<String> errors = new ArrayList<>();
        for (int netNum = minNetworks; netNum <= maxNetworks; netNum = netNum * 2) {
            // Each network requires at least two instances to run an exp.
            for (int insNum = Math.max(netNum * 2, minInstances); insNum <= maxInstances; insNum = insNum * 2) {

                SubExp subExp = null;
                try {
                    subExp = new SubExp(classRange, netNum, insNum, runClassExpConcurrently, runInstanceExpConcurrently, expDir);
                    subExp.exec();
                    log.info("SubExp {} is executed completely. Dir {}", subExp.getSubExpName(), expDir);
                    Thread.sleep(1000*insNum);
                    boolean readyForNext = true;
                    if (FIX_OVS) readyForNext = OvsManager.fixNucs();
                    if (!readyForNext){
                        log.error("OVS is not ready for the next experiment.");
                        return;
                    }
                    log.info("Sleeping for {}ms before next SubExp.", (1000*10*insNum));
                    Thread.sleep(1000*10*insNum);
                } catch (Exception e) {
                    log.error("exec(): Skipping to the next SubExp", e);
                    errors.add("Error at " + subExp.getSubExpName() + ": " + e.getMessage());
                }
                log.info("Going for next SubExp----------------------------");
            }
        }

        for (String string : errors) {
            log.error(string);
        }
    }

    public static Exp getSimpleExp(int[] classRange) {
        int computeSize = 3;
        int networkSize = classRange.length;
        int instanceSize = computeSize * networkSize;
        Exp exp = new Exp( classRange, networkSize, networkSize, instanceSize, instanceSize, RUN_CLASSES_CONCURRENTLY, RUN_INSTANCES_CONCURRENTLY);
        return exp;
    }

    public static void main(String[] args) {
        // NOTE: Keep the size of network and class range identical to make the plots meaningful.
//        int[] classRange = {1};
//        new Exp( classRange, 1, 1, 2, 2, RUN_CLASSES_CONCURRENTLY, RUN_INSTANCES_CONCURRENTLY).exec();

        int[] classRange = {1,2,3,4};
        getSimpleExp(classRange).exec();

//        new Exp( classRange, 1, 8, 1, 32, RUN_CLASSES_CONCURRENTLY, RUN_INSTANCES_CONCURRENTLY).exec();
//        new Exp( classRange, 1, 8, 64, 128, RUN_CLASSES_CONCURRENTLY, RUN_INSTANCES_CONCURRENTLY).exec();
//        new Exp( classRange, 1, 1, 1, 2, RUN_CLASSES_CONCURRENTLY, RUN_INSTANCES_CONCURRENTLY).exec();
//        new Exp( classRange, 3, 3, 24, 24, RUN_CLASSES_CONCURRENTLY, RUN_INSTANCES_CONCURRENTLY).exec();
//        new Exp( classRange, 3, 3, 96, 256, RUN_CLASSES_CONCURRENTLY, RUN_INSTANCES_CONCURRENTLY).exec();


    }
}
