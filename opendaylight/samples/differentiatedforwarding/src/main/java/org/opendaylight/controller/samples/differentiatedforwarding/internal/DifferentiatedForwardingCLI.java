package org.opendaylight.controller.samples.differentiatedforwarding.internal;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import org.apache.felix.service.command.Descriptor;
import org.opendaylight.controller.sal.core.Path;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.samples.differentiatedforwarding.IForwarding;
import org.opendaylight.controller.samples.differentiatedforwarding.ITunnelObserver;
import org.opendaylight.controller.samples.differentiatedforwarding.Tunnel;
import org.opendaylight.controller.samples.differentiatedforwarding.TunnelEndPoint;
import org.opendaylight.controller.samples.differentiatedforwarding.internal.DifferentiatedForwardingImpl.CoreQosStrategy;
import org.opendaylight.controller.samples.differentiatedforwarding.openstack.SshUtil;
import org.opendaylight.controller.samples.differentiatedforwarding.openstack.performance.Exp;
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
        props.put("osgi.command.function", new String[] { "programTunnel", "programTEPs", "programNetwork", "getMdNode", "getExternalInterface", "getTenantLocalInterfaces", "getProgrammedPath", "reportNetwork",
                "runSimpleExp", "runExp", "execCmd", "prepareMeter"});
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
          @Descriptor("Tunnel class (k in KShortestRoutes)") int classNum,
          @Descriptor("Core QoS Strategy (NONE(0), METER(1), QUEUE(2), METER_QUEUE(3))") int qosStrategy){
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
        forwarding.programTunnelForwarding(tunnel, classNum, CoreQosStrategy.getStrategy(qosStrategy), true);
    }

    public void programTEPs(
            @Descriptor("Container on the context of which the routing service need to be looked up") String container,
            @Descriptor("Tunnel key from getTunnelEndPoints") String tunnelKey,
            @Descriptor("Src TEP index from getTunnelEndPoints().get") int srcTepIndex,
            @Descriptor("Dst TEP index from getTunnelEndPoints().get") int dstTepIndex,
            @Descriptor("Tunnel class (k in KShortestRoutes)") int classNum,
            @Descriptor("Core QoS Strategy (NONE(0), METER(1), QUEUE(2), METER_QUEUE(3))") int qosStrategy){

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
            forwarding.programTunnelForwarding(tunnel, classNum, CoreQosStrategy.getStrategy(qosStrategy), true);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void programNetwork(
            @Descriptor("Container on the context of which the routing service need to be looked up") String container,
            @Descriptor("Tunnel key/SegmentationId from getTunnelEndPoints") String segmentationId,
            @Descriptor("Tunnel class (k in KShortestRoutes)") int classNum,
            @Descriptor("Core QoS Strategy (NONE(0), METER(1), QUEUE(2), METER_QUEUE(3))") int qosStrategy){

        System.out.println("DifferentiatedForwardingCLI - programNetwork");

        ITunnelObserver tunnelObserver = (ITunnelObserver) ServiceHelper.getInstance(ITunnelObserver.class, container, this);
        if(tunnelObserver == null){
            System.err.println("DifferentiatedForwardingCLI - programNetwork: TenantTunnelObserver is not available");
            return;
        }
        IForwarding forwarding = (IForwarding) ServiceHelper.getInstance(IForwarding.class, container, this);
        if(forwarding == null){
            System.err.println("DifferentiatedForwardingCLI - programNetwork: IForwarding is not available");
            return;
        }
        tunnelObserver.loadTunnelEndPoints(segmentationId);
        Set<TunnelEndPoint> teps = tunnelObserver.getTunnelEndPoints().get(segmentationId);

        List<Tunnel> tunnels = new ArrayList<>();
        try {
            tunnels = Tunnel.createTunnels(teps);
        } catch (Exception e1) {
            System.err.println("Can't create Tunnels from TEPs: " + teps);
            System.err.println(e1);
        }

        for (Tunnel tunnel : tunnels) {
            System.out.println("programTunnelForwarding: " + tunnel + ", classNum: " + classNum);
            forwarding.programTunnelForwarding(tunnel, classNum, CoreQosStrategy.getStrategy(qosStrategy), true);
        }
    }

    public Path getProgrammedPath(
            @Descriptor("Container on the context of which the routing service need to be looked up") String container,
            @Descriptor("Tunnel key from getTunnelEndPoints") String tunnelKey,
            @Descriptor("Src TEP index from getTunnelEndPoints().get") int srcTepIndex,
            @Descriptor("Dst TEP index from getTunnelEndPoints().get") int dstTepIndex){
        System.out.println("DifferentiatedForwardingCLI - getProgrammedPath");

        ITunnelObserver tunnelObserver = (ITunnelObserver) ServiceHelper.getInstance(ITunnelObserver.class, container, this);
        if(tunnelObserver == null){
            System.err.println("DifferentiatedForwardingCLI - getProgrammedPath: TenantTunnelObserver is not available");
            return null;
        }
        IForwarding forwarding = (IForwarding) ServiceHelper.getInstance(IForwarding.class, container, this);
        if(forwarding == null){
            System.err.println("DifferentiatedForwardingCLI - getProgrammedPath: IForwarding is not available");
            return null;
        }
        TunnelEndPoint srcTep = tunnelObserver.getTunnelEndPoints().get(tunnelKey).toArray(new TunnelEndPoint[0])[srcTepIndex];
        TunnelEndPoint dstTep = tunnelObserver.getTunnelEndPoints().get(tunnelKey).toArray(new TunnelEndPoint[0])[dstTepIndex];
        System.out.println("Src TEP:" + srcTep);
        System.out.println("Dst TEP:" + dstTep);
        Path path = null;
        try {
            Tunnel tunnel = new Tunnel(srcTep, dstTep);
            System.out.println("Tunnel: " + tunnel);
            path = forwarding.getProgrammedPath(tunnel);
        } catch (Exception e) {
            System.err.println("DifferentiatedForwardingCLI - getProgrammedPath: ERROR");
            e.printStackTrace();
        }

        return path;
    }

    public void reportNetwork(
            @Descriptor("Container on the context of which the routing service need to be looked up") String container,
            @Descriptor("Tunnel key/SegmentationId from getTunnelEndPoints") String segmentationId){
        System.out.println("DifferentiatedForwardingCLI - reportNetwork");

        IForwarding forwarding = (IForwarding) ServiceHelper.getInstance(IForwarding.class, container, this);
        if(forwarding == null){
            System.err.println("DifferentiatedForwardingCLI - reportNetwork: IForwarding is not available");
            return;
        }
        String report = forwarding.reportNetwork(segmentationId);
        System.out.println(report);
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
            System.err.println("DifferentiatedForwardingCLI - getExternalInterface: IForwarding is not available");
            return;
        }
        Node node = forwarding.getMdNode(nodeName);
        Long ofPort = forwarding.getExternalInterfaceOfPort(node);
        System.out.println("Node: "+ node + "");
        System.out.println("OfPort: " + ofPort);

    }

    public void getTenantLocalInterfaces(@Descriptor("Container on the context of which the routing service need to be looked up") String container,
            @Descriptor("Node DP ID") String nodeDpId,
            @Descriptor("Tenant Segmentation ID") String segmentationId){

        IForwarding forwarding = (IForwarding) ServiceHelper.getInstance(IForwarding.class, container, this);
        if(forwarding == null){
            System.err.println("DifferentiatedForwardingCLI - getTenantLocalInterfaces: IForwarding is not available");
            return;
        }
        Node ofNode = forwarding.getMdNode(nodeDpId);
        List<Long> ofPorts = forwarding.getTenantLocalInterfaces(ofNode, segmentationId);
        System.out.println("Node: "+ ofNode.getId() + "");
        System.out.println("OfPorts: " + ofPorts);
    }

    public void runSimpleExp(
            @Descriptor("Container on the context of which the routing service need to be looked up") String container,
            @Descriptor("ClassRange x,y,z") String classesString) {
        String[] classes = classesString.split(",");
        int[] classRange = new int[classes.length];
        for (int i = 0; i < classes.length; i++) {
            classRange[i] = Integer.parseInt(classes[i]);
        }

        Exp exp = Exp.getSimpleExp(classRange);
        exp.exec();
    }

    public void runExp(
            @Descriptor("Container on the context of which the routing service need to be looked up") String container,
            @Descriptor("ClassRange x,y,z") String classesString,
            @Descriptor("Min Network") int minNetworks,
            @Descriptor("Max Network") int maxNetworks,
            @Descriptor("Min Instances") int minInstances,
            @Descriptor("Max Instances") int maxInstances) {
        String[] classes = classesString.split(",");
        int[] classRange = new int[classes.length];
        for (int i = 0; i < classes.length; i++) {
            classRange[i] = Integer.parseInt(classes[i]);
        }

        Exp exp = new Exp(classRange, minNetworks, maxNetworks, minInstances, maxInstances, Exp.RUN_CLASSES_CONCURRENTLY, Exp.RUN_INSTANCES_CONCURRENTLY);
        exp.exec();
    }

    public void execCmd(
            @Descriptor("HostName") String hostName,
            @Descriptor("Cmd") String cmd) {

        try {
            SshUtil.execCmd(hostName, cmd);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void prepareMeter(@Descriptor("Node name") String nodeName,
                           @Descriptor("DSCP") int dscp){
        IForwarding forwarding = (IForwarding) ServiceHelper.getInstance(IForwarding.class, "default", this);
        if(forwarding == null){
            System.err.println("DifferentiatedForwardingCLI - getExternalInterface: IForwarding is not available");
            return;
        }
        Node node = forwarding.getMdNode(nodeName);
        forwarding.prepareMeter(node, (short) dscp);
    }
}
