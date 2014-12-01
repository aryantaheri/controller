package org.opendaylight.controller.samples.differentiatedforwarding.openstack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opendaylight.controller.samples.differentiatedforwarding.openstack.performance.BwReport;
import org.opendaylight.controller.samples.differentiatedforwarding.openstack.performance.ReachabilityReport;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.Flavor;
import org.openstack4j.model.compute.Keypair;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.compute.ext.Hypervisor;
import org.openstack4j.model.identity.Access;
import org.openstack4j.model.identity.Tenant;
import org.openstack4j.model.image.Image;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.Subnet;
import org.openstack4j.openstack.OSFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public static final String defaultVmUser = "cirros";
    public static final String defaultKeyPair = "cloud-keypair";
    public static final String defaultVmPubKey = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQDBKfIhlJ1NkcRJdbyqtaE07UGBTuH5L71WCZ406jRLM5itEaMdM+HuTimetTwBN7W5HO4msEFllAdZCansEXmmY6jEfFMHtpbXOwKso1fKPmcjE/oj+cVBbF0ORgtnf4Rr8/b/QbIHm3sdStl0nO5IE/PKsSxXU1I+OWTkeUMVbcbqOZc/dPW9uoEBYlA+3O4p7tCAy/HlO0TuNUpghLyyaVNdv+KOnNPzMO4x8BfIV+oi6D3Z0WB0/9aci6UN8RsIoopG6HKh5aMM1a4wXZko5nW7Zv8JAUurj2kpu3Ia+9BGrlhB1GESBGOxFm7Yv9QuQAvTlNVLuNhcvrW+MS1T fedora@f-control-1";
    public static final String defaultTenantName = "admin";
    public static final String defaultImage = "cirros-iperf-nuttcp";
    public static final String defaultFlavor = "m1.nano";

    public static Map<String, String> vmKeyPair;
    private static Access access;
    private static OSClient os;
    static {
        vmKeyPair = new HashMap<String, String>();
        vmKeyPair.put(defaultKeyPair, "/tmp/cloud-keypair.pem");

        os = OSFactory.builder()
                .endpoint("http://nuc2:5000/v2.0")
                .credentials("admin","adminpass")
                .tenantName("admin")
                .authenticate();
        access = os.getAccess();

    }

    public static void main(String[] args) {
//        OSClient os = OSFactory.builder()
//                .endpoint("http://127.0.0.1:5000/v2.0")
//                .credentials("admin","admin")
//                .tenantName(tenantName)
//                .authenticate();
//        OSClient os2 = OSFactory.builder()
//                .endpoint("http://nuc2:5000/v2.0")
//                .credentials("admin","adminpass")
//                .tenantName("admin")
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
//        createInstances(os, tenantName, "vm", "cirros-disk-iperf-nuttcp", "m1.nano", "net2", "cloud-keypair", 3);
//        OpenStackManager.createNetwork(os, defaultTenantName, "net20", "10.20.0.0/24");
//        reachability();
//        deleteNetwork("class1net1");
        deleteAllInstances();
//        testNuttcp();
//        testNuttcpPersistentServer(os, "anet2", false);
//        list(os);
    }

    private static void reachability() {
        List<? extends Server> instances = os.compute().servers().list();
        Network network = OpenStackManager.getNetwork(os, "net100");
        for (Server server : instances) {
            ReachabilityReport report = ReachabilityManager.runReachability(server, network);
            log.info("ReachabilityReport: {}", report);
        }
    }
    private static void list(OSClient os){


        List<? extends Tenant> tenants = os.identity().tenants().list();
        System.out.println(tenants);
        List<? extends Image> images = os.images().list();
        System.out.println(images);
        List<? extends Flavor> flavors = os.compute().flavors().list();
        System.out.println(flavors);
        List<? extends Hypervisor> hosts = os.compute().hypervisors().list();
        System.out.println(hosts);
        List<? extends Server> servers = os.compute().servers().list(true);
        for (Server server : servers) {
            System.out.println(server);

        }
        List<? extends Network> networks = os.networking().network().list();
        System.out.println(networks);
        List<? extends Subnet> subnets = os.networking().subnet().list();
        System.out.println(subnets);

    }



    private static void createInstances(OSClient os, String tenantName, String namePrefix, String imageName, String flavorName, String networkName, String keyPairName, int number){


        Image image = OpenStackManager.getImage(os, imageName);
        Flavor flavor = OpenStackManager.getFlavor(os, flavorName);
        Network network = OpenStackManager.getNetwork(os, networkName);
        if (network == null) OpenStackManager.createNetwork(os, tenantName, networkName, "10.2.0.0/24");

        OpenStackManager.createKeyPair(os, keyPairName, defaultVmPubKey);

        List<Server> instances = OpenStackManager.createInstances(os, namePrefix, image.getId(), flavor.getId(), network.getId(), keyPairName, number);
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
        Network network = OpenStackManager.getNetwork(os, networkName);
        NuttcpManager.measureBw(servers, network,  BwReport.Type.TCP);
    }

    private static void testNuttcpPersistentServer(OSClient os, String networkName){
        List<? extends Server> servers = os.compute().servers().list(true);
        Network network = OpenStackManager.getNetwork(os, networkName);
        NuttcpManager.measureBwPersistentServer(servers, network, BwReport.Type.TCP);
    }

    public static List<Server> createInstances(String namePrefix, String networkName, int number){
        OSClient osClient = OSFactory.clientFromAccess(access);
        Image image = OpenStackManager.getImage(osClient, defaultImage);
        Flavor flavor = OpenStackManager.getFlavor(osClient, defaultFlavor);
        Network network = OpenStackManager.getNetwork(osClient, networkName);
        Keypair keypair = osClient.compute().keypairs().get(defaultKeyPair);
        if (network == null || image == null || flavor == null || keypair == null){
            log.error("createInstances: missing params image {} flavor {} network {} keypair {}", image, flavor, network, keypair);
            return null;
        }
        List<Server> instances = OpenStackManager.createInstances(osClient, namePrefix, image.getId(), flavor.getId(), network.getId(), keypair.getName(), number);
        return instances;
    }

    public static void deleteAllInstances() {
        OSClient osClient = OSFactory.clientFromAccess(access);
        OpenStackManager.deleteAllInstances(osClient);
    }

    public static void deleteInstances(List<? extends Server> instances) {
        OSClient osClient = OSFactory.clientFromAccess(access);
        OpenStackManager.deleteInstances(osClient, instances);
    }

    public static Network createNetwork(String networkName, String cidr, boolean mayExist){
        OSClient osClient = OSFactory.clientFromAccess(access);
        if (mayExist){
            Network network = OpenStackManager.getNetwork(osClient, networkName);
            if (network != null){
                log.warn("createNetwork: Network {} exists", network);
                return network;
            }
        }
        return OpenStackManager.createNetwork(osClient, defaultTenantName, networkName, cidr);
    }

    public static void deleteNetworkById(String networkUuid){
        OSClient osClient = OSFactory.clientFromAccess(access);
        OpenStackManager.deleteNetworkById(osClient, networkUuid);
    }

    public static void deleteNetwork(String networkName){
        OSClient osClient = OSFactory.clientFromAccess(access);
        OpenStackManager.deleteNetwork(osClient, networkName);
    }

//    private void areUp(List<? extends Server> servers) {
//        List<? extends Server> loadedServers = os.compute().servers().list(true);
//        for (Server server : servers) {
//            loadedServers.get
//        }
//    }
}
