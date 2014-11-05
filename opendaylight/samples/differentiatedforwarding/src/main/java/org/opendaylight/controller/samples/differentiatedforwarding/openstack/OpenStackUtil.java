package org.opendaylight.controller.samples.differentiatedforwarding.openstack;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.Flavor;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.identity.Tenant;
import org.openstack4j.model.image.Image;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.Subnet;
import org.openstack4j.openstack.OSFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.Log4jLoggerAdapter;

import ch.qos.logback.classic.Level;

/**
ssh -L 5000:192.168.10.250:5000 \
-L 8774:192.168.10.250:8774 \
-L 8773:192.168.10.250:8773 \
-L 8775:192.168.10.250:8775 \
-L 9292:192.168.10.250:9292 \
-L 9191:192.168.10.250:9191 \
-L 9696:192.168.10.250:9696 \
-L 35357:192.168.10.250:35357 \
root@ha10
**/
public class OpenStackUtil {

    private static Logger log = LoggerFactory.getLogger(OpenStackUtil.class);

    public static final String vmUser = "cirros";
    public static Map<String, String> vmKeyPair;
    static {
        vmKeyPair = new HashMap<String, String>();
        vmKeyPair.put("cloud-keypair", "/home/fedora/devstack/cloud-keypair.pem");
//        myMap = Collections.unmodifiableMap(aMap);

    }

    public static void main(String[] args) {
//        OSClient os = OSFactory.builder()
//                .endpoint("http://127.0.0.1:5000/v2.0")
//                .credentials("admin","admin")
//                .tenantName("demo")
//                .authenticate();
//
//        List<? extends Tenant> tenants = os.identity().tenants().list();
//        System.out.println(tenants);
//        List<? extends Image> images = os.images().list();
//        System.out.println(images);
//        List<? extends Flavor> flavors = os.compute().flavors().list();
//        System.out.println(flavors);
//        List<? extends Server> servers = os.compute().servers().list(true);
//        for (Server server : servers) {
//            System.out.println(server);
//
//        }
//        List<? extends Network> networks = os.networking().network().list();
//        System.out.println(networks);
//        List<? extends Subnet> subnets = os.networking().subnet().list();
//        System.out.println(subnets);
//
//
//        String imageUuid = images.get(0).getId();
//        String flavorUuid = flavors.get(0).getId();
//        String networkUuid = networks.get(1).getId();
//        String keyPairName = "cloud-keypair";
//        int number = 3;
//        createInstances(os, imageUuid, flavorUuid, networkUuid, keyPairName, number);
//        testNuttcp();
        testNuttcpPersistentServer();
    }

    private static void createInstances(OSClient os, String imageUuid, String flavorUuid, String networkUuid, String keyPairName, int number){
        InstanceManager instanceManager = new InstanceManager(os);
        List<Server> instances = instanceManager.createInstances("vm", imageUuid, flavorUuid, networkUuid, keyPairName, number);
        for (Server server : instances) {
            System.out.println(server);
        }
        try {
            Thread.sleep(60*1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("----------------------");
        for (Server server : instances) {
            System.out.println(server);
        }
    }

    private static void testNuttcp(){
        OSClient os = OSFactory.builder()
                .endpoint("http://127.0.0.1:5000/v2.0")
                .credentials("admin","admin")
                .tenantName("demo")
                .authenticate();
        List<? extends Server> servers = os.compute().servers().list(true);
        List<? extends Network> networks = os.networking().network().list();
        String vmNameSpace = "qdhcp-" + networks.get(1).getId();
        NuttcpManager.measureBw(servers, vmNameSpace, true);
    }

    private static void testNuttcpPersistentServer(){
        OSClient os = OSFactory.builder()
                .endpoint("http://127.0.0.1:5000/v2.0")
                .credentials("admin","admin")
                .tenantName("demo")
                .authenticate();
        List<? extends Server> servers = os.compute().servers().list(true);
        List<? extends Network> networks = os.networking().network().list();
        String vmNameSpace = "qdhcp-" + networks.get(1).getId();
        NuttcpManager.measureBwPersistentServer(servers, vmNameSpace, true);
    }
}
