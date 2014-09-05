package org.opendaylight.controller.samples.differentiatedforwarding.internal;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import org.apache.felix.service.command.Descriptor;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.samples.differentiatedforwarding.IForwarding;
import org.opendaylight.controller.samples.differentiatedforwarding.ITunnelObserver;
import org.opendaylight.controller.samples.differentiatedforwarding.Tunnel;
import org.opendaylight.controller.samples.differentiatedforwarding.TunnelEndPoint;
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
        System.out.println("DifferentiatedForwardingCLI - programTunnel");

        ITunnelObserver tunnelObserver = (ITunnelObserver) ServiceHelper.getInstance(ITunnelObserver.class, container, this);
        if(tunnelObserver == null){
            System.err.println("DifferentiatedForwardingCLI - programTunnel: TenantTunnelObserver is not available");
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

    public void programTEPs(
            @Descriptor("Container on the context of which the routing service need to be looked up") String container,
            @Descriptor("Tunnel key from getTunnelEndPoints") String tunnelKey,
            @Descriptor("Src TEP index from getTunnelEndPoints().get") int srcTepIndex,
            @Descriptor("Dst TEP index from getTunnelEndPoints().get") int dstTepIndex,
            @Descriptor("Tunnel class (k in KShortestRoutes)") int classNum){

        System.out.println("DifferentiatedForwardingCLI - programTEPs");

        ITunnelObserver tunnelObserver = (ITunnelObserver) ServiceHelper.getInstance(ITunnelObserver.class, container, this);
        if(tunnelObserver == null){
            System.err.println("DifferentiatedForwardingCLI - programTEPs: TenantTunnelObserver is not available");
            return;
        }
        IForwarding forwarding = (IForwarding) ServiceHelper.getInstance(IForwarding.class, container, this);
        if(forwarding == null){
            System.err.println("DifferentiatedForwardingCLI - programTEPs: IForwarding is not available");
            return;
        }
        HashMap<String, Set<TunnelEndPoint>> tunnelEndPointsMap = tunnelObserver.getTunnelEndPoints();
        System.out.println(tunnelEndPointsMap);
        TunnelEndPoint srcTep = tunnelEndPointsMap.get(tunnelKey).toArray(new TunnelEndPoint[0])[srcTepIndex];
        TunnelEndPoint dstTep = tunnelEndPointsMap.get(tunnelKey).toArray(new TunnelEndPoint[0])[dstTepIndex];
        System.out.println("Src TEP:" + srcTep);
        System.out.println("Dst TEP:" + dstTep);
        try {
            Tunnel tunnel = null;
            tunnel = new Tunnel(srcTep, dstTep);
            System.out.println("Tunnel: " + tunnel);
            forwarding.programTunnelForwarding(tunnel, classNum, true);
        } catch (Exception e) {
            e.printStackTrace();
        }

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

}
