package org.opendaylight.controller.samples.differentiatedforwarding.internal;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import org.apache.felix.service.command.Descriptor;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.samples.differentiatedforwarding.IForwarding;
import org.opendaylight.controller.samples.differentiatedforwarding.ITunnelObserver;
import org.opendaylight.controller.samples.differentiatedforwarding.Tunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.osgi.framework.ServiceRegistration;

public class DifferentiatedForwardingCLI {
    @SuppressWarnings("rawtypes")
    private ServiceRegistration sr = null;

    public void init() {
    }

    public void destroy() {
    }

    public void start() {
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("osgi.command.scope", "odpcontroller");
        props.put("osgi.command.function", new String[] { "programTunnel", "getMdNode", "getExternalInterface" });
        this.sr = ServiceHelper.registerGlobalServiceWReg(DifferentiatedForwardingCLI.class, this, props);
    }

    public void stop() {
        if (this.sr != null) {
            this.sr.unregister();
            this.sr = null;
        }
    }

    public void programTunnel(
          @Descriptor("Container on the context of which the routing service need to be looked up") String container,
          @Descriptor("Tunnel key from getTunnelsMap") String tunnelKey,
          @Descriptor("Tunnel index from getTunnelsMap") int index,
          @Descriptor("Tunnel class (k in KShortestRoutes)") int classNum){
        System.out.println("TenantTunnelObserver.getTunnelsMap");

        ITunnelObserver tunnelObserver = (ITunnelObserver) ServiceHelper.getInstance(ITunnelObserver.class, container, this);
        if(tunnelObserver == null){
            System.err.println("TenantTunnelObserverCLI - getTunnelsMap: TenantTunnelObserver is not available");
            return;
        }
        IForwarding forwarding = (IForwarding) ServiceHelper.getInstance(IForwarding.class, container, this);
        if(forwarding == null){
            System.err.println("DifferentiatedForwardingCLI - programTunnel: IForwarding is not available");
            return;
        }
        HashMap<String, List<Tunnel>> tunnelsMap = tunnelObserver.getTunnelsMap();
        System.out.println(tunnelsMap);
        Tunnel tunnel = tunnelsMap.get(tunnelKey).get(index);
        System.out.println("Chosen Tunnel:" + tunnel);
        forwarding.programTunnelForwarding(tunnel, classNum, true);


    }

    public void getMdNode(
            @Descriptor("Container on the context of which the routing service need to be looked up") String container,
            @Descriptor("Node name") String nodeName){

        IForwarding forwarding = (IForwarding) ServiceHelper.getInstance(IForwarding.class, container, this);
        if(forwarding == null){
            System.err.println("DifferentiatedForwardingCLI - programTunnel: IForwarding is not available");
            return;
        }
        Node node = forwarding.getMdNode(nodeName);
        System.out.println("Node: "+ node);
    }

    public void getExternalInterface(
            @Descriptor("Container on the context of which the routing service need to be looked up") String container,
            @Descriptor("Node name") String nodeName){

        IForwarding forwarding = (IForwarding) ServiceHelper.getInstance(IForwarding.class, container, this);
        if(forwarding == null){
            System.err.println("DifferentiatedForwardingCLI - programTunnel: IForwarding is not available");
            return;
        }
        Node node = forwarding.getMdNode(nodeName);
        Long ofPort = forwarding.getExternalInterfaceOfPort(node);
        System.out.println("Node: "+ node + "");
        System.out.println("OfPort: " + ofPort);

    }
//    @Descriptor("Retrieves K shortest routes between two Nodes in the discovered Topology DB")
//    public void getkShortestRoutes(
//            @Descriptor("Container on the context of which the routing service need to be looked up") String container,
//            @Descriptor("String representation of the Source Node, this need to be consumable from Node.fromString()") String srcNode,
//            @Descriptor("String representation of the Destination Node") String dstNode,
//            @Descriptor("Number of shortest paths (K)") int k) {
//        System.out.println("YKShortestPathsCLI.getkShortestRoutes src:" + srcNode + " dst: " + dstNode + " K: " + k);
//        IRouting r = null;
//        Object[] rList = ServiceHelper.getInstances(IRouting.class, container, this, null);
//        for (int i = 0; i < rList.length; i++) {
//            if (rList[i] instanceof YKShortestPaths)
//                r = (IRouting) rList[i];
//        }
//
//        if (r == null) {
//            System.out.println("Cannot find the YKSP routing instance on container:" + container);
//            return;
//        }
//        System.out.println("YKShortestPathsCLI.getkShortestRoutes IRouting is retrieved: " + r.getClass());
//        final Node src = Node.fromString(srcNode);
//        final Node dst = Node.fromString(dstNode);
//        final List<Path> paths = r.getKShortestRoutes(src, dst, k);
//        if (paths != null) {
//            System.out.println("K Shortest Routes between srcNode:" + src + " and dstNode:" + dst + " = " + paths);
//            int i = 1;
//            for (Path path : paths) {
//                System.out.println("Route "+ i + ": " + path);
//                i++;
//            }
//        } else {
//            System.out.println("There is no route between srcNode:" + src + " and dstNode:" + dst);
//        }
//    }
}
