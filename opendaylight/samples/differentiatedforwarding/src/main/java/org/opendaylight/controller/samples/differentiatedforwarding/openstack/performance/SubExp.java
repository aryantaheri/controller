package org.opendaylight.controller.samples.differentiatedforwarding.openstack.performance;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubExp {
    private static Logger log = LoggerFactory.getLogger(SubExp.class);

    int netNum;
    int insNum;
    int classRange[];

    public SubExp(int[] classRange, int netNum, int insNum) {
        this.netNum = netNum;
        this.insNum = insNum;
        this.classRange = classRange;
        System.out.println(Arrays.toString(classRange)+":"+netNum+":"+insNum);
    }

    // class -> network -> instance
    public void exec() {
        int classNum = classRange.length;
        if (classNum > netNum){
            log.warn("Number of requested classes {} is more than requested networks {}. Reducing number of classes", Arrays.toString(classRange), netNum);
            classNum = netNum;
        }
        for (int classId = 1; classId <= classNum; classId++) {
            int classNetNum = netNum / classNum;
            for (int net = 1; net <= classNetNum; net++) {
                int netInsNum = insNum / netNum;
                new BwExp(classId, net, netInsNum).run();
                System.out.println("class:" + classRange[classId - 1] + " network:" + net + " netInsNum:" + netInsNum);

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
}
