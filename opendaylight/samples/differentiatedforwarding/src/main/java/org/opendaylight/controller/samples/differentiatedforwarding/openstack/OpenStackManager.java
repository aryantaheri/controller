package org.opendaylight.controller.samples.differentiatedforwarding.openstack;

import java.util.ArrayList;
import java.util.List;

import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.Flavor;
import org.openstack4j.model.compute.Keypair;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.compute.ServerCreate;
import org.openstack4j.model.image.Image;
import org.openstack4j.model.network.IPVersionType;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.Subnet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenStackManager {

    private static Logger log = LoggerFactory.getLogger(OpenStackManager.class);
    static int BOOT_WAIT_TIME = 3*60*1000; // in ms


    public static List<Server> createInstances(OSClient osClient, String namePrefix, String imageUuid, String flavorUuid, String netUuid, String keyPairName, int number){
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


    public static void deleteAllInstances(OSClient os){
        List<? extends Server> servers = os.compute().servers().list();
        for (Server server : servers) {
            os.compute().servers().delete(server.getId());
        }
    }

    public static void deleteInstances(OSClient os, List<? extends Server> servers){
        for (Server server : servers) {
            os.compute().servers().delete(server.getId());
        }
    }

    public static void createKeyPair(OSClient osClient, String keyName, String pubKey){
        List<? extends Keypair> keyPairs = osClient.compute().keypairs().list();
        boolean exist = false;
        for (Keypair keypair : keyPairs) {
            if (keypair.getName().equals(keyName) && keypair.getPublicKey().equals(pubKey)) {
                exist = true;
                break;
            }
        }
        if (!exist){
            osClient.compute().keypairs().create(keyName, pubKey);
        }
    }

    public static Network createNetwork(OSClient os, String tenantName, String networkName, String cidr){
        String tenantUuid = os.identity().tenants().getByName(tenantName).getId();
        Network network = Builders.network().name(networkName).tenantId(tenantUuid).adminStateUp(true).build();
        network = os.networking().network().create(network);
        Subnet subnet = Builders.subnet()
                .name("sub"+networkName)
                .networkId(network.getId())
                .tenantId(tenantUuid)
                .ipVersion(IPVersionType.V4)
                .enableDHCP(true)
                .cidr(cidr)
                .build();
        subnet = os.networking().subnet().create(subnet);
        return network;
    }

    public static void deleteNetworkById(OSClient os, String networkUuid){
        os.networking().network().delete(networkUuid);
    }

    public static void deleteNetwork(OSClient os, String networkName){
        Network network = getNetwork(os, networkName);
        if (network != null){
            os.networking().network().delete(network.getId());
        }
    }


    public static Network getNetwork(OSClient os, String networkName){
        List<? extends Network> networks = os.networking().network().list();
        for (Network network : networks) {
            if (network.getName().equalsIgnoreCase(networkName)) return network;
        }
        return null;
    }

    public static Image getImage(OSClient os, String imageName){
        List<? extends Image> images = os.images().list();
        for (Image image : images) {
            if (image.getName().equalsIgnoreCase(imageName)) return image;
        }
        return null;
    }

    public static Flavor getFlavor(OSClient os, String flavorName){
        List<? extends Flavor> flavors = os.compute().flavors().list();
        for (Flavor flavor : flavors) {
            if (flavor.getName().equalsIgnoreCase(flavorName)) return flavor;
        }
        return null;
    }
}
