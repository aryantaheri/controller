package org.opendaylight.controller.samples.differentiatedforwarding.openstack;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opendaylight.controller.samples.differentiatedforwarding.openstack.performance.BwReport;
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
import ch.qos.logback.core.subst.Token.Type;

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
    public static final String vmPubKey = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQDBKfIhlJ1NkcRJdbyqtaE07UGBTuH5L71WCZ406jRLM5itEaMdM+HuTimetTwBN7W5HO4msEFllAdZCansEXmmY6jEfFMHtpbXOwKso1fKPmcjE/oj+cVBbF0ORgtnf4Rr8/b/QbIHm3sdStl0nO5IE/PKsSxXU1I+OWTkeUMVbcbqOZc/dPW9uoEBYlA+3O4p7tCAy/HlO0TuNUpghLyyaVNdv+KOnNPzMO4x8BfIV+oi6D3Z0WB0/9aci6UN8RsIoopG6HKh5aMM1a4wXZko5nW7Zv8JAUurj2kpu3Ia+9BGrlhB1GESBGOxFm7Yv9QuQAvTlNVLuNhcvrW+MS1T fedora@f-control-1";
    public static Map<String, String> vmKeyPair;
    static {
        vmKeyPair = new HashMap<String, String>();
        vmKeyPair.put("cloud-keypair", "/home/fedora/devstack/cloud-keypair.pem");
//        myMap = Collections.unmodifiableMap(aMap);

    }

    public static void main(String[] args) {
        OSClient os = OSFactory.builder()
                .endpoint("http://127.0.0.1:5000/v2.0")
                .credentials("admin","admin")
                .tenantName("demo")
                .authenticate();
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
//        createInstances(os, "vm", "cirros-disk-iperf-nuttcp", "m1.nano", "net1", "cloud-keypair", 3);
//        testNuttcp();
        testNuttcpPersistentServer(os, "net1");
//        list();
    }

    private static void list(){
        OSClient os = OSFactory.builder()
                .endpoint("http://127.0.0.1:5000/v2.0")
                .credentials("admin", "admin").tenantName("demo")
                .authenticate();

        List<? extends Tenant> tenants = os.identity().tenants().list();
        System.out.println(tenants);
        List<? extends Image> images = os.images().list();
        System.out.println(images);
        List<? extends Flavor> flavors = os.compute().flavors().list();
        System.out.println(flavors);
        List<? extends Server> servers = os.compute().servers().list(true);
        for (Server server : servers) {
            System.out.println(server);

        }
        List<? extends Network> networks = os.networking().network().list();
        System.out.println(networks);
        List<? extends Subnet> subnets = os.networking().subnet().list();
        System.out.println(subnets);

    }

    private static Network getNetwork(OSClient os, String networkName){
        List<? extends Network> networks = os.networking().network().list();
        for (Network network : networks) {
            if (network.getName().equalsIgnoreCase(networkName)) return network;
        }
        return null;
    }

    private static Image getImage(OSClient os, String imageName){
        List<? extends Image> images = os.images().list();
        for (Image image : images) {
            if (image.getName().equalsIgnoreCase(imageName)) return image;
        }
        return null;
    }

    private static Flavor getFlavor(OSClient os, String flavorName){
        List<? extends Flavor> flavors = os.compute().flavors().list();
        for (Flavor flavor : flavors) {
            if (flavor.getName().equalsIgnoreCase(flavorName)) return flavor;
        }
        return null;
    }

    private static void createInstances(OSClient os, String namePrefix, String imageName, String flavorName, String networkName, String keyPairName, int number){
        String imageUuid = getImage(os, imageName).getId();
        String flavorUuid = getFlavor(os, flavorName).getId();
        String networkUuid = getNetwork(os, networkName).getId();
        List<Server> instances = InstanceManager.createInstances(os, namePrefix, imageUuid, flavorUuid, networkUuid, keyPairName, number);
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

    private static void testNuttcp(OSClient os, String networkName){
        List<? extends Server> servers = os.compute().servers().list(true);
        Network network = getNetwork(os, networkName);
        NuttcpManager.measureBw(servers, network,  BwReport.Type.TCP, true);
    }

    private static void testNuttcpPersistentServer(OSClient os, String networkName){
        List<? extends Server> servers = os.compute().servers().list(true);
        Network network = getNetwork(os, networkName);
        NuttcpManager.measureBwPersistentServer(servers, network, BwReport.Type.TCP, true);
    }
}
