package org.opendaylight.controller.samples.differentiatedforwarding.openstack;

import java.util.ArrayList;
import java.util.List;

import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.compute.ServerCreate;

public class InstanceManager {

    OSClient osClient;
    int BOOT_WAIT_TIME = 3*60*1000; // in ms
    public InstanceManager(OSClient client) {
        osClient = client;
    }

    public List<Server> createInstances(String namePrefix, String imageUuid, String flavorUuid, String netUuid, String keyPairName, int number){
        List<String> networks = new ArrayList<>();
        networks.add(netUuid);

        List<Server> instances = new ArrayList<>();
        for (int i = 0; i < number; i++) {
            // Create a Server Model Object
            ServerCreate sc = Builders.server().name(namePrefix + (i+1)).flavor(flavorUuid).image(imageUuid).networks(networks).keypairName(keyPairName).build();

            // Boot the Server
            Server server = osClient.compute().servers().bootAndWaitActive(sc, BOOT_WAIT_TIME);
            instances.add(server);
        }

        return instances;


    }
}
