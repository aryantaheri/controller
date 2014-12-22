package org.opendaylight.controller.samples.differentiatedforwarding.openstack.ssh;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opendaylight.controller.samples.differentiatedforwarding.openstack.SshUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This will try to fix the unstability of ODL OVSDB after instance delete.
 * @author aryan
 *
 */
public class OvsManager {

    private static Logger log = LoggerFactory.getLogger(OvsManager.class);
    private static String[] nodes = {"nuc2", "nuc3", "nuc4", "nuc5"};
    private static HashMap<String, String> nodeTunnelIp = new HashMap<String, String>();

    private static final int SLEEP = 10*1000;
    private static final int INIT_SLEEP = 20*1000;
    private static final int FLOW_RELOAD_SLEEP = 60*1000;
    private static final int RETRIES = 10;
    private static Map<String, List<String>> fixedTunnels = new HashMap<>();

    static {
        nodeTunnelIp.put("nuc2", "172.16.10.2");
        nodeTunnelIp.put("nuc3", "172.16.10.3");
        nodeTunnelIp.put("nuc4", "172.16.10.4");
        nodeTunnelIp.put("nuc5", "172.16.10.5");
    }

    public static void main(String[] args) {
        try {
//            boolean readyForNext = fixNucs();
//            log.info("ReadyForNext: {}", readyForNext);
            fixTunnelTos();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean fixNucs() throws Exception {
        log.warn("Going to reset all OVS configurations on {}", Arrays.toString(nodes));
        for (String node : nodes) {
            clean(node);
        }

        for (String node : nodes) {
            boolean ready = init(node);
            if (!ready) {
                return false;
            } else {
                continue;
            }
        }
        return true;
    }

    public static void fixPicOs(){

    }

    public static void fixTunnelTos(){
        for (String node : nodeTunnelIp.keySet()) {
            for (String ip : nodeTunnelIp.values()) {
                if (nodeTunnelIp.get(node).equalsIgnoreCase(ip)) continue;
                String tunnelName = "gre-"+ip;
                if (fixedTunnels.get(node) != null && fixedTunnels.get(node).contains(tunnelName)) {
                    continue;
                }
                try {
                    CommandOutPut output;
                    output = setTunnelTos(node, tunnelName);
                    if (output.getExitStatus() == 0){
                        List<String> tunnels = fixedTunnels.get(node);
                        if (tunnels == null) {
                            tunnels = new ArrayList<>();
                            fixedTunnels.put(node, tunnels);
                        }
                        tunnels.add(tunnelName);
                    } else {
                        log.warn("Can't fix ToS on Node {} for Tunnel {} Error {}", node, tunnelName, output.getError());
                    }
                } catch (Exception e) {
                    log.error("fixTunnelTos Exception", e);
                }
            }
        }
    }

    private static void clean(String hostName) throws Exception {
//        delFlows(hostName);
//        Thread.sleep(SLEEP);

        delManager(hostName);
        Thread.sleep(SLEEP);

        delBrInt(hostName);
        Thread.sleep(SLEEP);

        restartOvs(hostName);
    }

    private static boolean init(String hostName) throws Exception {
        setManager(hostName);
        Thread.sleep(INIT_SLEEP);
        boolean ready = verifyInit(hostName);
        if (ready){
            addPhyPort(hostName, "em1");
            setBrIntIp(hostName, nodeTunnelIp.get(hostName));
        }
        return ready;
    }

    private static void delFlows(String hostName) throws Exception {
        SshUtil.execCmd(hostName, "ovs-ofctl del-flows br-int -O OpenFlow13");
    }

    private static void delFlows(String hostName, String bridge) throws Exception {
        SshUtil.execCmd(hostName, "ovs-ofctl del-flows " + bridge + " -O OpenFlow13");
    }

    private static void delManager(String hostName) throws Exception {
        SshUtil.execCmd(hostName, "ovs-vsctl del-manager");
    }

    private static void delBrInt(String hostName) throws Exception {
        SshUtil.execCmd(hostName, "ovs-vsctl del-br br-int");
    }

    private static void restartOvs(String hostName) throws Exception {
        SshUtil.execCmd(hostName, "service openvswitch-switch restart");
    }

    private static void setManager(String hostName) throws Exception {
        SshUtil.execCmd(hostName, "ovs-vsctl set-manager tcp:192.168.10.1:6640");
    }

    private static void addPhyPort(String hostName, String portName) throws Exception {
        SshUtil.execCmd(hostName, "ovs-vsctl add-port br-int " + portName + " -- set Interface " + portName + " type=system");
    }

    private static void setBrIntIp(String hostName, String ip) throws Exception {
        SshUtil.execCmd(hostName, "ifconfig br-int " + ip + " netmask 255.255.255.0");
    }

    private static CommandOutPut setTunnelTos(String hostName, String tunnelName) throws Exception {
        return SshUtil.execCmd(hostName, "ovs-vsctl set interface " + tunnelName + " options:tos=inherit");
    }

    private static boolean verifyInit(String hostName) throws Exception {
        int retries = 0;
        boolean ready = false;
        while(retries < RETRIES){
            ready = checkSkeleton(hostName);
            if (ready){
                log.info("Host {} is ready now. Retries {}", hostName, retries);
                return true;
            } else {
                log.error("Host {} is not ready now. Retries {}", hostName, retries);
            }
            retries++;
            Thread.sleep(FLOW_RELOAD_SLEEP);
        }
        log.error("Host {} is not initialized after {} retries", hostName, retries);
        return ready;
    }

    private static boolean checkSkeleton(String hostName) throws Exception {
        CommandOutPut output = SshUtil.execCmd(hostName, "ovs-ofctl dump-flows br-int -O OpenFlow13 | grep table | cut -d','  -f3-3 | sort -u | wc -l");
        if (output.getExitStatus() == 0 && Integer.parseInt(output.getOutput()) >= 11) {
            return true;
        } else {
            return false;
        }
    }
}
