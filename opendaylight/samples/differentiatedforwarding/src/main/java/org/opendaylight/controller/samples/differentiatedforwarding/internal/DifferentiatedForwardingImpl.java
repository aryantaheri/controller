package org.opendaylight.controller.samples.differentiatedforwarding.internal;

import java.math.BigInteger;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.clustering.services.IClusterContainerServices;
import org.opendaylight.controller.forwardingrulesmanager.IForwardingRulesManager;
import org.opendaylight.controller.hosttracker.IfIptoHost;
import org.opendaylight.controller.hosttracker.IfNewHostNotify;
import org.opendaylight.controller.hosttracker.hostAware.HostNodeConnector;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.OptimisticLockFailedException;
import org.opendaylight.controller.networkconfig.neutron.INeutronNetworkCRUD;
import org.opendaylight.controller.networkconfig.neutron.NeutronNetwork;
import org.opendaylight.controller.networkconfig.neutron.NeutronPort;
import org.opendaylight.controller.routing.yenkshortestpaths.internal.IKShortestRoutes;
import org.opendaylight.controller.routing.yenkshortestpaths.internal.WeightedEdge;
import org.opendaylight.controller.routing.yenkshortestpaths.internal.YKShortestPaths;
import org.opendaylight.controller.sal.core.Edge;
import org.opendaylight.controller.sal.core.Path;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.UpdateType;
import org.opendaylight.controller.sal.packet.IDataPacketService;
import org.opendaylight.controller.sal.packet.IListenDataPacket;
import org.opendaylight.controller.sal.packet.PacketResult;
import org.opendaylight.controller.sal.packet.RawPacket;
import org.opendaylight.controller.sal.routing.IListenRoutingUpdates;
import org.opendaylight.controller.sal.routing.IRouting;
import org.opendaylight.controller.sal.utils.EtherTypes;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.samples.differentiatedforwarding.IForwarding;
import org.opendaylight.controller.samples.differentiatedforwarding.ITunnelObserver;
import org.opendaylight.controller.samples.differentiatedforwarding.OpenFlowUtils;
import org.opendaylight.controller.samples.differentiatedforwarding.Tunnel;
import org.opendaylight.controller.samples.differentiatedforwarding.TunnelEndPoint;
import org.opendaylight.controller.switchmanager.IInventoryListener;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.controller.topologymanager.ITopologyManager;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.openstack.netvirt.api.Constants;
import org.opendaylight.ovsdb.plugin.IConnectionServiceInternal;
import org.opendaylight.ovsdb.plugin.OvsdbConfigService;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.schema.openvswitch.Interface;
import org.opendaylight.ovsdb.schema.openvswitch.Port;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.EtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg7;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class DifferentiatedForwardingImpl implements IfNewHostNotify, IListenRoutingUpdates, IInventoryListener, IListenDataPacket, IForwarding{

    private static Logger log = LoggerFactory.getLogger(DifferentiatedForwardingImpl.class);

    private IfIptoHost hostTracker;
    private IForwardingRulesManager frm;
    private ITopologyManager topologyManager;
    private IRouting routing;
    private IKShortestRoutes shortestRoutes;
    private IClusterContainerServices clusterContainerService;
    private ISwitchManager switchManager;
    private IDataPacketService dataPacketService;

    /**
     * The set of all forwarding rules: (host) -> (switch -> flowmod). Note that
     * the host includes an attachment point and that while the switch appears
     * to be a switch's port, in actuality it is a special port which just
     * represents the switch.
     */
//    private ConcurrentMap<HostNodePair, HashMap<org.opendaylight.controller.sal.core.NodeConnector, FlowEntry>> rulesDB;
    private static final String DIFF_FORWARDING_RULES_CACHE_NAME = "differentiatedforwarding.ipswitch.rules";
    private Map<Tunnel, Short> tunnelsDscp;
    private Map<Tunnel, Path> tunnelsPath;

    /**
     * OpenFlow Tables
     */
//    private static final short TABLE_TUNNEL_QOS = 100;
    private static final short TABLE_0_DEFAULT_INGRESS = 0;

    /**
     * OpenFlow Priorities
     */
    private static final int PRIORITY_MAX = 65535;
    private static final int PRIORITY_NORMAL = 0;
    private static final int PRIORITY_LLDP = 4000; // Put it high, so it will reach controller
    private static final int PRIORITY_TUNNEL_EXTLOCAL = 500;
    private static final int PRIORITY_TUNNEL_LOCALEXT = 500;
    private static final int PRIORITY_TUNNEL_TRANSIT = 1000;
    private static final int PRIORITY_TUNNEL_SRC_IN = 1001;
    private static final int PRIORITY_TUNNEL_IP_SRC_IN = 1002; // Higher priority compared to PRIORITY_TUNNEL_SRC_IN, so it captures IP packets for DSCP marking
    private static final int PRIORITY_TUNNEL_DST_OUT = 1001;
    private static final int PRIORITY_TUNNEL_INGRESS_DSCP_MARKING = PRIORITY_MAX;

    private static final int RETRIES = 3;
    /**
     * Function called by the dependency manager when all the required
     * dependencies are satisfied
     *
     */
    void init() {
        log.debug("init()");
        allocateRulesDB();
        tunnelsDscp = new HashMap<>();
        tunnelsPath = new HashMap<>();
    }

    /**
     * Function called by the dependency manager when at least one
     * dependency become unsatisfied or when the component is shutting
     * down because for example bundle is being stopped.
     *
     */
    void destroy() {
        log.debug("destroy()");
    }

    /**
     * Function called by dependency manager after "init ()" is called
     * and after the services provided by the class are registered in
     * the service registry
     *
     */
    void start() {
        log.debug("start()");
    }

    /**
     * Function called by the dependency manager before the services
     * exported by the component are unregistered, this will be
     * followed by a "destroy ()" calls
     *
     */
    void stop() {
        log.debug("stop()");
    }

    private void allocateRulesDB() {
        if (this.clusterContainerService == null) {
            log.trace("allocateRulesDB: clusterContainerService is null, can't create cache");
            return;
        }

//        try {
//            rulesDB = (ConcurrentMap<HostNodePair, HashMap<org.opendaylight.controller.sal.core.NodeConnector, FlowEntry>>) clusterContainerService.createCache(DIFF_FORWARDING_RULES_CACHE_NAME, EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));
//        } catch (CacheExistException cee) {
//            log.error("\nCache already exists - destroy and recreate if needed", cee);
//        } catch (CacheConfigException cce) {
//            log.error("\nCache configuration invalid - check cache mode", cce);
//        }
    }

    public void programTunnels(List<Tunnel> tunnels, int classNum, boolean write){
        for (Tunnel tunnel : tunnels) {
            programTunnelForwarding(tunnel, classNum, write);
        }
    }

    /**
     * TODO: first we should forward the interesting traffic to another table, and apply these policies there.
     * The table(s) can be categorized as Tenant specific tables, or Forwarding class specific tables. A comparative study is required here.
     * In the initial phase, we should just handle them all in a single table (e.g.100) not to overlap with OVSDB tables. Nevertheless, such an overlap should
     * cause issues, since these forwarding decisions are made in the core network, not on edges.
     * @param tunnel
     * @param classNum the Kth shortest path. K > 0
     */
    @Override
    public void programTunnelForwarding(Tunnel tunnel, int classNum, boolean write){
        log.debug("programTunnelForwarding: Tunnel {} classNum {} write {}", tunnel, classNum, write);
        org.opendaylight.controller.sal.core.Node srcNode = tunnel.getSrcNodeConnector().getNode();
        org.opendaylight.controller.sal.core.Node dstNode = tunnel.getDstNodeConnector().getNode();
        List<Path> paths = shortestRoutes.getKShortestRoutes(srcNode, dstNode, classNum);
        log.trace("programTunnelForwarding: tunnel {} paths {}", tunnel, paths);
        if (paths == null || paths.size() == 0){
            log.error("programTunnelForwarding: No path available between {} and {}. Returning", srcNode, dstNode);
            return;
        }
        Path path;
        if (paths.size() >= classNum){
            path = paths.get(classNum - 1);
        } else {
            log.error("programTunnelForwarding: request classNum {} is not possible, setting largest possible value {}", classNum, paths.size());
            path = paths.get(paths.size() - 1);
        }

        log.debug("programTunnelForwarding: selected path for tunnel {} with classNum {} is {}", tunnel, classNum, path);

        if(write){
            tunnelsPath.put(tunnel, path);
            tunnelsDscp.put(tunnel, (short) classNum);
        } else {
            tunnelsPath.remove(tunnel);
            tunnelsDscp.remove(tunnel);
        }

        List<Edge> edges = path.getEdges();

        /**
         * lastInPort-srcNode-outPort ----> inPort-dstNode
         */
        org.opendaylight.controller.sal.core.NodeConnector lastInPort = null;
        Node lastInPortMDNode = null;
        NodeConnector lastInPortMDNC = null;
        for (Edge edge : edges) {
            org.opendaylight.controller.sal.core.NodeConnector outPort = edge.getTailNodeConnector();
            org.opendaylight.controller.sal.core.NodeConnector inPort = edge.getHeadNodeConnector();

            Node outPortMDNode = constructMDNode(outPort.getNode());
            NodeConnector outPortMDNC = constructMDNodeConnector(outPort);

            Node inPortMDNode = constructMDNode(inPort.getNode());
            NodeConnector inPortMDNC = constructMDNodeConnector(inPort);

            if (outPortMDNode == null || inPortMDNode == null){
                log.error("programTunnelForwarding: outPortMDNode or inPortMDNode is null for edge {} tunnel {}. Aborting", edge, tunnel);
                return;
            }
            if (lastInPort == null){
                // Tunnel source
                programTunnelSource(tunnel, outPortMDNode, outPortMDNC, write);

                writeLLDPRule(outPortMDNode);
//                writeNormalRule(outPortMDNode);
            } else {
                // Transit node or err
                lastInPortMDNode = constructMDNode(lastInPort.getNode());
                lastInPortMDNC = constructMDNodeConnector(lastInPort);

                programEdgeTail(tunnel, lastInPortMDNode, lastInPortMDNC, outPortMDNode, outPortMDNC, write);
            }

            lastInPort = inPort;
//            lastInPortMDNC = inPortMDNC;
//            lastInPortMDNode = inPortMDNode;
        }

        // Tunnel destination
        // Here lastInPort is the nodeConnector representing the TEP.
        // 1) Use a NORMAL rule to find the host, and forward
        // 2) Be more strict, discover IP-MAC resolution, and program flow accordingly
        Node tepDstNode = constructMDNode(lastInPort.getNode());
        writeLLDPRule(tepDstNode);
//        writeNormalRule(tepDstNode);
    }

    /**
     * 1) Forward designated ingress traffic to the tunnel interface
     * 2) Forward egress tunnel traffic to outPort
     * @param tunnel
     * @param outPortMDNode
     * @param outPortMDNC
     * @param write
     */
    private void programTunnelSource(Tunnel tunnel, Node outPortMDNode, NodeConnector outPortMDNC, boolean write) {
        log.debug("programTunnelSource: tunnel {} outPortMDNode {} outPortMDNC {} write {}", tunnel, outPortMDNode.getId(), outPortMDNC.getId(), write);

        // This is required when using OpenStack
        handleTenantPortsDscpMarking(outPortMDNode, tunnel, write);

        // These two are required when we're using mininet.
        handleIngressTrafficToTunnelSource(tunnel, outPortMDNode, outPortMDNC, write);
        handleIngressIpTrafficToTunnelSource(tunnel, outPortMDNode, outPortMDNC, write);

        // NOTE: in OVS you can not enforce tunnel egress port.
        handleExternalInterfaceForwarding(tunnel, outPortMDNode);
    }

    /**
     * TEP Nodes have IP addresses on their default internal interface (e.g. SW s1, Interface s1).
     * These IPs are used as remote_ip and local_ip. The physical Ethernet is added to the switch for
     * connectivity.
     * In mininet case, since Linux bridge doesn't forward LLDP multicasts, we added a set of L2TP
     * tunnels to provide L2 connectivity. L2TP ports behave as physical Ethernet, and are added to
     * the switch in mininet.
     * For now we just need simple forwarding between LOCAL and L2TP port
     * The packet flow is:
     * vhost <--> tunnel interface <--> linux IP stack <--> switch internal interface <--> physical ethernet (l2tp)
     *
     * When adding the l2tp port to the switch, make sure type=system is present. Since that's the
     * only discovery mechanism here
     * ovs-vsctl add-port s1 l2tpeth0 -- set Interface l2tpeth0 type=system
     *
     * @param Node
     */
    private void handleExternalInterfaceForwarding(Tunnel tunnel, Node node) {
        log.debug("handleExternalInterfaceForwarding: node {}", node.getId());
        List<NodeConnector> nodeConnectors = node.getNodeConnector();

        Long externalInterfaceOfPort = getExternalInterfaceOfPort(node);
        if (externalInterfaceOfPort == null){
            log.error("handleExternalInterfaceForwarding: Can not find the ExternalInterface on {}. Returning", node);
            return;
        }
        NodeConnectorId exInfId = new NodeConnectorId(node.getId().getValue() + ":" + externalInterfaceOfPort);
        NodeConnector exNodeConnector = new NodeConnectorBuilder().setId(exInfId).build();

        NodeConnectorId localInfId = new NodeConnectorId(node.getId().getValue() + ":" + "LOCAL");
        NodeConnector localNodeConnector = new NodeConnectorBuilder().setId(localInfId).build();

        // Handle External -> LOCAL
        String flowNameExtLocal = "TE_TUNNEL_EXT_LOCAL";
        writeSimpleInOutRule(node, exNodeConnector, localNodeConnector, flowNameExtLocal, PRIORITY_TUNNEL_EXTLOCAL, TABLE_0_DEFAULT_INGRESS, true);

        // Handle LOCAL -> External
        String flowNameLocalExt = "TE_TUNNEL_LOCAL_EXT";
        writeSimpleInOutRule(node, localNodeConnector, exNodeConnector, flowNameLocalExt, PRIORITY_TUNNEL_LOCALEXT, TABLE_0_DEFAULT_INGRESS, true);
        handleLocalInterfaceForwarding(node, localNodeConnector, exNodeConnector, flowNameLocalExt, tunnel, true);
    }

    /**
     * Mark traffic with DSCP=0 to the biggest programmed DSCP value in the network.
     * This should cover non-IP traffic from end points.
     * It can be further optimized by querying the SW for available flow rules, and avoid rewriting with the same value.
     * Match in_port=LOCAL,ip,nw_tos=0
     * @param node
     * @param localNodeConnector
     * @param exNodeConnector
     * @param flowNamePrefix
     * @param tunnel
     * @param write
     */
    private void handleLocalInterfaceForwarding(Node node, NodeConnector localNodeConnector, NodeConnector exNodeConnector, String flowNamePrefix, Tunnel tunnel, boolean write){
        short biggestDscp = findBiggestDscp(tunnel.getSrcAddress(), tunnel.getDstAddress());
        if (biggestDscp == Short.MIN_VALUE){
            log.error("handleLocalInterfaceForwarding: Unable to find the biggest programmed DSCP value. Traffic with DSCP=0 are dropped.");
            return;
        }

        MatchBuilder matchBuilder = new MatchBuilder();
        FlowBuilder flowBuilder = new FlowBuilder();
        NodeBuilder nodeBuilder = getFlowCapableNodeBuilder(node, TABLE_0_DEFAULT_INGRESS);

        // Match on in_port,ip,tos=0
        OpenFlowUtils.createInPortMatch(matchBuilder, node, localNodeConnector);
        OpenFlowUtils.createEtherTypeMatch(matchBuilder, new EtherType((long) EtherTypes.IPv4.intValue()));
        OpenFlowUtils.createNwDscpMatch(matchBuilder, (short) 0);
        flowBuilder.setMatch(matchBuilder.build());

        String flowName = flowNamePrefix + "_"
                + tunnel.getTunnelKey() + "_"
                + tunnel.getSrcAddress().getHostAddress() + "_"
                + tunnel.getDstAddress().getHostAddress()+ "_ZToS";


        flowBuilder.setId(new FlowId(flowName));
        FlowKey flowKey = new FlowKey(flowBuilder.getId());
        flowBuilder.setStrict(true);
        flowBuilder.setBarrier(false);
        flowBuilder.setTableId(TABLE_0_DEFAULT_INGRESS);
        flowBuilder.setKey(flowKey);
        flowBuilder.setFlowName(flowName);
        flowBuilder.setHardTimeout(0);
        flowBuilder.setIdleTimeout(0);
        flowBuilder.setPriority((PRIORITY_TUNNEL_LOCALEXT + 1)); // Intentionally left relative, to make sure this rule is matched first

        if(write){
            log.debug("handleLocalInterfaceForwarding: Add/Update flow {} to node {}", flowBuilder, nodeBuilder);
            InstructionBuilder ib = new InstructionBuilder();
            InstructionsBuilder isb = new InstructionsBuilder();
            List<Instruction> instructions = new ArrayList<Instruction>();

            // Set the Output Port to the external interface
            // Set the biggest DSCP value available between src and dst and the output port to the external interface.
            ib = OpenFlowUtils.createMarkDscpAndOutputInstructions(ib, biggestDscp, exNodeConnector);
            ib.setOrder(0);
            ib.setKey(new InstructionKey(0));

            instructions.add(ib.build());
            isb.setInstruction(instructions);
            flowBuilder.setInstructions(isb.build());

            writeFlow(flowBuilder, nodeBuilder, RETRIES);
        } else {
            removeFlow(flowBuilder, nodeBuilder);
        }


    }

    private short findBiggestDscp(InetAddress srcAddress, InetAddress dstAddress) {
        short dscp = Short.MIN_VALUE;
        for (Tunnel tunnel : tunnelsDscp.keySet()) {
            if (tunnel.getSrcAddress().equals(srcAddress) && tunnel.getDstAddress().equals(dstAddress) && tunnelsDscp.get(tunnel) > dscp){
                dscp = tunnelsDscp.get(tunnel);
            }
        }

        return dscp;
    }

    private void handleTenantPortsDscpMarking(Node ofNode, Tunnel tunnel, boolean write){
        List<Long> inPortOfPorts = getTenantLocalInterfaces(ofNode, tunnel.getTunnelKey());
        String flowNamePrefix = "TE_TUNNEL_INGRESS_" + tunnel.getTunnelKey() + ofNode.getId().getValue() + "_OFPort";
        String flowName;
        for (Long inPortOfPort : inPortOfPorts) {
            NodeConnectorId ncId = new NodeConnectorId(ofNode.getId().getValue() + ":" + inPortOfPort);
            NodeConnector nc = new NodeConnectorBuilder().setId(ncId).build();
            flowName = new String(flowNamePrefix + inPortOfPort);
            markIngressDscpAndResubmit(tunnel, ofNode, nc, flowName, PRIORITY_TUNNEL_INGRESS_DSCP_MARKING, TABLE_0_DEFAULT_INGRESS, TABLE_0_DEFAULT_INGRESS, write);
        }
    }
    /**
     * 0- Check reg7 to see if it has been marked before. Discard DSCP marks not specified by us.
     * 1- Mark internal ingress traffic (from connected VMs) with DSCP
     * 2- Set reg7
     * 3- Resubmit to make it compatible
     * @param tunnel
     * @param node
     * @param inPort
     * @param outPort
     * @param flowName
     * @param priority
     * @param table
     * @param write
     */
    private void markIngressDscpAndResubmit(Tunnel tunnel, Node node, NodeConnector inPort, String flowName, int priority, short table, short resubmitTable, boolean write) {
        MatchBuilder matchBuilder = new MatchBuilder();
        FlowBuilder flowBuilder = new FlowBuilder();
        NodeBuilder nodeBuilder = getFlowCapableNodeBuilder(node, table);

        //match:ip,in_port=$A_TENANT_PORT,reg7=0
        OpenFlowUtils.createEtherTypeMatch(matchBuilder, new EtherType((long) EtherTypes.IPv4.intValue()));
        OpenFlowUtils.createInPortMatch(matchBuilder, node, inPort);
        OpenFlowUtils.createRegMatch(matchBuilder, NxmNxReg7.class, 0L);
        flowBuilder.setMatch(matchBuilder.build());

        flowBuilder.setId(new FlowId(flowName));
        FlowKey flowKey = new FlowKey(flowBuilder.getId());
        flowBuilder.setStrict(true);
        flowBuilder.setBarrier(false);
        flowBuilder.setTableId(table);
        flowBuilder.setKey(flowKey);
        flowBuilder.setFlowName(flowName);
        flowBuilder.setHardTimeout(0);
        flowBuilder.setIdleTimeout(0);
        flowBuilder.setPriority(priority);

        if(write){
            log.debug("markIngressDscpAndResubmit: Add/Update flow {} to node {}", flowBuilder, nodeBuilder);
            InstructionBuilder ib = new InstructionBuilder();
            InstructionsBuilder isb = new InstructionsBuilder();
            List<Instruction> instructions = new ArrayList<Instruction>();

            //actions=set_field:X->ip_dscp,set_field:1->reg7,resubmit(,0)
            OpenFlowUtils.createIngressDscpMarkResubmitInstructions(ib, NxmNxReg7.class, 1L, tunnelsDscp.get(tunnel), null, resubmitTable);
            ib.setOrder(0);
            ib.setKey(new InstructionKey(0));
            instructions.add(ib.build());
            isb.setInstruction(instructions);

            flowBuilder.setInstructions(isb.build());

            writeFlow(flowBuilder, nodeBuilder, RETRIES);
        } else {
            removeFlow(flowBuilder, nodeBuilder);
        }

    }



    @Override
    public List<Long> getTenantLocalInterfaces(Node ofNode, String segmentationId) {
        log.debug("getTenantLocalInterfaces: ofNode {} SegmentationId {}", ofNode.getId(), segmentationId);
        org.opendaylight.controller.sal.core.Node ovsNode = getOvsNode(ofNode);
        List<NeutronPort> neutronPorts = getNeutronPorts(segmentationId);
        log.trace("getTenantLocalInterfaces: neutronPorts {}", neutronPorts);
        if (neutronPorts == null) return null;
        List<Long> ofPorts = getOfPorts(ovsNode, neutronPorts);
        log.trace("getTenantLocalInterfaces: ovsNode={} segmentationId={}, ofPorts={}", ovsNode.getID(), segmentationId, ofPorts);
        return ofPorts;
    }

    private List<Long> getOfPorts(org.opendaylight.controller.sal.core.Node ovsNode, List<NeutronPort> neutronPorts) {
        OvsdbConfigService ovsdbConfigService = (OvsdbConfigService)ServiceHelper.getGlobalInstance(OvsdbConfigService.class, this);
        Map<String, Row> ports = ovsdbConfigService.getRows(ovsNode, ovsdbConfigService.getTableName(ovsNode, Port.class));
        if (ports == null || ports.size() == 0) {
            log.error("getOfPort: didn't find Ports in ovsNode {} ", ovsNode.getNodeIDString());
            return null;
        }

        List<Long> ofPorts = new ArrayList<>();
        for (String portUuid : ports.keySet()) {
            Port port = ovsdbConfigService.getTypedRow(ovsNode, Port.class, ports.get(portUuid));
            if (port == null || port.getInterfacesColumn() == null || port.getInterfacesColumn().getData() == null) continue;
            UUID interfaceUuid = (UUID) port.getInterfacesColumn().getData().toArray()[0];
            Row interfaceRow = ovsdbConfigService.getRow(ovsNode, ovsdbConfigService.getTableName(ovsNode, Interface.class), interfaceUuid.toString());
            Interface intf = ovsdbConfigService.getTypedRow(ovsNode, Interface.class, interfaceRow);
            for (NeutronPort neutronPort : neutronPorts) {
                if (intf.getExternalIdsColumn().getData() != null &&
                        intf.getExternalIdsColumn().getData().get(Constants.EXTERNAL_ID_INTERFACE_ID) != null &&
                        intf.getExternalIdsColumn().getData().get(Constants.EXTERNAL_ID_INTERFACE_ID).equalsIgnoreCase(neutronPort.getPortUUID())){
                    // This neutronPort exists on this ovsNode and belongs to the given neutronNetwork/segmentationId
                    log.trace("getOfPorts: found interface {}={}, ovsNode {}", intf.getName(), intf.getOpenFlowPortColumn().getData(), ovsNode);
                    ofPorts.add((Long) intf.getOpenFlowPortColumn().getData().toArray()[0]);
                }
            }
        }
        log.trace("getOfPorts: ovsNode {} ofPorts {}", ovsNode.getNodeIDString(), ofPorts);
        return ofPorts;
    }

    private List<NeutronPort> getNeutronPorts(String segmentationId) {
        INeutronNetworkCRUD neutronNetworkService = (INeutronNetworkCRUD)ServiceHelper.getGlobalInstance(INeutronNetworkCRUD.class, this);
        if (neutronNetworkService == null){
            log.error("getNeutronPorts: INeutronNetworkCRUD is not available");
            return null;
        }
        List <NeutronNetwork> neutronNetworks = neutronNetworkService.getAllNetworks();
        log.trace("getNeutronPorts: neutronNetworks {}", neutronNetworks);
        if (neutronNetworks == null) return null;
        NeutronNetwork neutronNetwork = null;
        for (NeutronNetwork network : neutronNetworks) {
            if (network.getProviderSegmentationID().equalsIgnoreCase(segmentationId)) {
                neutronNetwork = network;
            }
        }
        log.trace("getNeutronPorts: chosen neutronNetwork {}", neutronNetwork);
        if (neutronNetwork == null) return null;
        List<NeutronPort> neutronPorts = neutronNetwork.getPortsOnNetwork();
        return neutronPorts;
    }

    /**
     * 1) Select designated ingress traffic to node
     * 1.1) Traffic from hosts
     * 1.2) Traffic from other nodes (e.g. patch ports)
     * 1.3) Traffic from 1.1 and 1.2
     * 1.4) Other policies
     * Chosen designated ports: !outPort && !tunnelSrcPort && !LOCAL
     * @param tunnel
     * @param write
     */
    private void handleIngressTrafficToTunnelSource(Tunnel tunnel, Node outPortMDNode, NodeConnector outPortMDNC, boolean write) {
        log.debug("handleIngressTrafficToTunnelSource: tunnel {} outPortMDNode {} outPortMDNC {} write {}", tunnel, outPortMDNode.getId(), outPortMDNC.getId(), write);
        NodeConnector tunnelSrcNodeConnector = constructMDNodeConnector(tunnel.getSrcNodeConnector());
        Node srcNode = constructMDNode(tunnel.getSrcNodeConnector().getNode());

        List<NodeConnector> ingressPorts = new ArrayList<>();
        List<NodeConnector> connectors = srcNode.getNodeConnector();
        for (NodeConnector nodeConnector : connectors) {
            if(!nodeConnector.getId().getValue().equalsIgnoreCase(outPortMDNC.getId().getValue()) &&
                    !nodeConnector.getId().getValue().equalsIgnoreCase(tunnelSrcNodeConnector.getId().getValue()) &&
                    !nodeConnector.getId().getValue().contains(":LOCAL")){
                ingressPorts.add(nodeConnector);
            }
        }
        log.trace("handleIngressTrafficToTunnelSource: designated ingress ports: {}", ingressPorts);

        MatchBuilder matchBuilder = new MatchBuilder();
        FlowBuilder flowBuilder = new FlowBuilder();
        NodeBuilder nodeBuilder = getFlowCapableNodeBuilder(outPortMDNode, TABLE_0_DEFAULT_INGRESS);

        //FIXME: This is broken, and only the last port is set. We should augment it with other MatchBuilders probably.
        for (NodeConnector ingressNodeConnector : ingressPorts) {
            flowBuilder.setMatch(OpenFlowUtils.createInPortMatch(matchBuilder, srcNode, ingressNodeConnector).build());
        }
        String flowName = "TE_TUNNEL_IN_SRC" + tunnel.getTunnelKey() + "_"
                + outPortMDNode.getId().getValue() + "_"
                + tunnel.getSrcAddress().getHostAddress() + "_"
                + tunnel.getDstAddress().getHostAddress();

        flowBuilder.setId(new FlowId(flowName));
        FlowKey flowKey = new FlowKey(flowBuilder.getId());
        flowBuilder.setStrict(true);
        flowBuilder.setBarrier(false);
        flowBuilder.setTableId(TABLE_0_DEFAULT_INGRESS);
        flowBuilder.setKey(flowKey);
        flowBuilder.setFlowName(flowName);
        flowBuilder.setHardTimeout(0);
        flowBuilder.setIdleTimeout(0);
        flowBuilder.setPriority(PRIORITY_TUNNEL_SRC_IN);

        if(write){
            log.debug("programEdgeTail: Add/Update flow {} to node {}", flowBuilder, nodeBuilder);
            // Instantiate the Builders for the OF Actions and Instructions
            InstructionBuilder ib = new InstructionBuilder();
            InstructionsBuilder isb = new InstructionsBuilder();

            // Instructions List Stores Individual Instructions
            List<Instruction> instructions = new ArrayList<Instruction>();

            // Set the Output Port/Iface to the tunnel interface
            ib = OpenFlowUtils.createOutputPortInstructions(ib, outPortMDNode, tunnelSrcNodeConnector);
            // FIXME: Ultimately we should specify the tunnel key, src, dst as well.
            ib.setOrder(0);
            ib.setKey(new InstructionKey(0));
            instructions.add(ib.build());

            // Add InstructionBuilder to the Instruction(s)Builder List
            isb.setInstruction(instructions);

            // Add InstructionsBuilder to FlowBuilder
            flowBuilder.setInstructions(isb.build());

            writeFlow(flowBuilder, nodeBuilder, RETRIES);
        } else {
            removeFlow(flowBuilder, nodeBuilder);
        }
    }

    private void handleIngressIpTrafficToTunnelSource(Tunnel tunnel, Node outPortMDNode, NodeConnector outPortMDNC, boolean write) {
        log.debug("handleIngressIpTrafficToTunnelSource: tunnel {} outPortMDNode {} outPortMDNC {} write {}", tunnel, outPortMDNode.getId(), outPortMDNC.getId(), write);
        NodeConnector tunnelSrcNodeConnector = constructMDNodeConnector(tunnel.getSrcNodeConnector());
        Node srcNode = constructMDNode(tunnel.getSrcNodeConnector().getNode());

        List<NodeConnector> ingressPorts = new ArrayList<>();
        List<NodeConnector> connectors = srcNode.getNodeConnector();
        for (NodeConnector nodeConnector : connectors) {
            if(!nodeConnector.getId().getValue().equalsIgnoreCase(outPortMDNC.getId().getValue()) &&
                    !nodeConnector.getId().getValue().equalsIgnoreCase(tunnelSrcNodeConnector.getId().getValue()) &&
                    !nodeConnector.getId().getValue().contains(":LOCAL")){
                ingressPorts.add(nodeConnector);
            }
        }
        log.trace("handleIngressIpTrafficToTunnelSource: designated ingress ports: {}", ingressPorts);

        MatchBuilder matchBuilder = new MatchBuilder();
        FlowBuilder flowBuilder = new FlowBuilder();
        NodeBuilder nodeBuilder = getFlowCapableNodeBuilder(outPortMDNode, TABLE_0_DEFAULT_INGRESS);

        //FIXME: This is broken, and only the last port is set. We should augment it with other MatchBuilders probably.
        for (NodeConnector ingressNodeConnector : ingressPorts) {
            flowBuilder.setMatch(OpenFlowUtils.createInPortMatch(matchBuilder, srcNode, ingressNodeConnector).build());
        }
        // Make sure this is IP, for DSCP
        flowBuilder.setMatch(OpenFlowUtils.createEtherTypeMatch(matchBuilder, new EtherType((long) EtherTypes.IPv4.intValue())).build());

        String flowName = "TE_TUNNEL_IP_IN_SRC" + tunnel.getTunnelKey() + "_"
                + outPortMDNode.getId().getValue() + "_"
                + tunnel.getSrcAddress().getHostAddress() + "_"
                + tunnel.getDstAddress().getHostAddress();

        flowBuilder.setId(new FlowId(flowName));
        FlowKey flowKey = new FlowKey(flowBuilder.getId());
        flowBuilder.setStrict(true);
        flowBuilder.setBarrier(false);
        flowBuilder.setTableId(TABLE_0_DEFAULT_INGRESS);
        flowBuilder.setKey(flowKey);
        flowBuilder.setFlowName(flowName);
        flowBuilder.setHardTimeout(0);
        flowBuilder.setIdleTimeout(0);
        flowBuilder.setPriority(PRIORITY_TUNNEL_IP_SRC_IN);

        if(write){
            log.debug("handleIngressIpTrafficToTunnelSource: Add/Update flow {} to node {}", flowBuilder, nodeBuilder);
            // Instantiate the Builders for the OF Actions and Instructions
            InstructionBuilder ib = new InstructionBuilder();
            InstructionsBuilder isb = new InstructionsBuilder();

            // Instructions List Stores Individual Instructions
            List<Instruction> instructions = new ArrayList<Instruction>();

            // Set the Output Port/Iface to the tunnel interface
            // ib = OpenFlowUtils.createOutputPortInstructions(ib, outPortMDNode, tunnelSrcNodeConnector);
            // Set the DSCP value on the inner header and the output port to the tunnel interface. options:tos=inherit will set the inner ToS to outer ToS
            ib = OpenFlowUtils.createMarkDscpAndOutputInstructions(ib, tunnelsDscp.get(tunnel), tunnelSrcNodeConnector);

            // FIXME: Ultimately we should specify the tunnel key, src, dst as well.
            ib.setOrder(0);
            ib.setKey(new InstructionKey(0));
            instructions.add(ib.build());

            // Add InstructionBuilder to the Instruction(s)Builder List
            isb.setInstruction(instructions);

            // Add InstructionsBuilder to FlowBuilder
            flowBuilder.setInstructions(isb.build());

            writeFlow(flowBuilder, nodeBuilder, RETRIES);
        } else {
            removeFlow(flowBuilder, nodeBuilder);
        }

    }
    /**
     * This essentially program all transit nodes.
     * @param tunnel
     * @param lastInPortMDNode
     * @param lastInPortMDNC
     * @param outPortMDNode
     * @param outPortMDNC
     * @param write
     */
    private void programEdgeTail(final Tunnel tunnel, final Node lastInPortMDNode, final NodeConnector lastInPortMDNC,
                                        final Node outPortMDNode, final NodeConnector outPortMDNC, boolean write) {

        if (lastInPortMDNode != null && !lastInPortMDNode.equals(outPortMDNode)){
            log.error("programEdgeTail: lastInPortNode {} is not equal to outPort {}. Inconsistency found, returning", lastInPortMDNode, outPortMDNode);
            return;
        }

        MatchBuilder matchBuilder = new MatchBuilder();
        FlowBuilder flowBuilder = new FlowBuilder();
        NodeBuilder nodeBuilder = getFlowCapableNodeBuilder(outPortMDNode, TABLE_0_DEFAULT_INGRESS);

        if (lastInPortMDNode != null && lastInPortMDNC != null){
            log.debug("programEdgeTail: tunnel {}, lastInPortMDNode {}, lastInPortMDNC {}, outPortMDNode {}, outPortMDNC {}, write {}",
                    tunnel, lastInPortMDNode.getId(), lastInPortMDNC, outPortMDNode.getId(), outPortMDNC, write);
            log.trace("programEdgeTail: middle/last node/edge is programming");
            // Match on lastInPort
            flowBuilder.setMatch(OpenFlowUtils.createInPortMatch(matchBuilder, lastInPortMDNode, lastInPortMDNC).build());

        } else {
            log.debug("programEdgeTail: tunnel {}, lastInPortMDNode {}, lastInPortMDNC {}, outPortMDNode {}, outPortMDNC {}, write {}",
                    tunnel, lastInPortMDNode, lastInPortMDNC, outPortMDNode.getId(), outPortMDNC, write);

            log.trace("programEdgeTail: tunnel source node/first edge is programming");
        }


        // NOTE: Match on Tunnel ID. This is broken! tun_id is a metadata and won't be accessible in transit.
        // flowBuilder.setMatch(OpenFlowUtils.createTunnelIDMatch(matchBuilder, new BigInteger(tunnel.getTunnelKey())).build());

        // FIXME: Alternatively use destination tunnel port to cover the tunnel ID. tp_dst=tun_id,
        // this should be taken care of while creating tunnels
        // flowBuilder.setMatch(OpenFlowUtils.createDstPortUdpMatch(matchBuilder, new PortNumber(Integer.parseInt(tunnel.getTunnelKey()))).build());

        // Match on DSCP
        flowBuilder.setMatch(OpenFlowUtils.createNwDscpMatch(matchBuilder, tunnelsDscp.get(tunnel)).build());

        // Match on Source & Destination IP
        flowBuilder.setMatch(OpenFlowUtils.createSrcDstL3IPv4Match(matchBuilder, tunnel.getSrcAddress(), tunnel.getDstAddress()).build());


        String flowName = "QoS_Tunnel_" + tunnel.getTunnelKey() + "_"
                                        + outPortMDNode.getId().getValue() + "_"
                                        + tunnel.getSrcAddress().getHostAddress() + "_"
                                        + tunnel.getDstAddress().getHostAddress();

        flowBuilder.setId(new FlowId(flowName));
        FlowKey flowKey = new FlowKey(flowBuilder.getId());
        flowBuilder.setStrict(true);
        flowBuilder.setBarrier(false);
        flowBuilder.setTableId(TABLE_0_DEFAULT_INGRESS);
        flowBuilder.setKey(flowKey);
        flowBuilder.setFlowName(flowName);
        flowBuilder.setHardTimeout(0);
        flowBuilder.setIdleTimeout(0);
        flowBuilder.setPriority(PRIORITY_TUNNEL_TRANSIT);

        if(write){
            // ADD/UPDATE
            log.debug("programEdgeTail: Add/Update flow {} to node {}", flowBuilder, nodeBuilder);
            // Instantiate the Builders for the OF Actions and Instructions
            InstructionBuilder ib = new InstructionBuilder();
            InstructionsBuilder isb = new InstructionsBuilder();

            // Instructions List Stores Individual Instructions
            List<Instruction> instructions = new ArrayList<Instruction>();

            // Set the Output Port/Iface
            ib = OpenFlowUtils.createOutputPortInstructions(ib, outPortMDNode, outPortMDNC);
            ib.setOrder(0);
            ib.setKey(new InstructionKey(0));
            instructions.add(ib.build());

            // Add InstructionBuilder to the Instruction(s)Builder List
            isb.setInstruction(instructions);

            // Add InstructionsBuilder to FlowBuilder
            flowBuilder.setInstructions(isb.build());
            writeFlow(flowBuilder, nodeBuilder, RETRIES);


        } else {
            // DELETE
            log.debug("programEdgeTail: Delete flow {} from node {}", flowBuilder, nodeBuilder);
            removeFlow(flowBuilder, nodeBuilder);
        }
    }


    private void removeFlow(FlowBuilder flowBuilder, NodeBuilder nodeBuilder) {
        log.error("removeFlow: not implemented");
    }

    private void writeFlow(final FlowBuilder flowBuilder, final NodeBuilder nodeBuilder, final int retries) {
        log.debug("writeFlow: Entry");
        IMDSALConsumer mdsalConsumer = (IMDSALConsumer) ServiceHelper.getInstance(IMDSALConsumer.class, "default", this);
        if (mdsalConsumer == null) {
            log.error("writeFlow: ERROR finding MDSAL Service.");
            return;
        }

        DataBroker dataBroker = mdsalConsumer.getDataBroker();
        if (dataBroker == null) {
            log.error("writeFlow: ERROR finding reference for DataBroker. Please check out the MD-SAL support on the Controller.");
            return;
        }

        ReadWriteTransaction readWriteTransaction = dataBroker.newReadWriteTransaction();
        InstanceIdentifier<Flow> flowPath = InstanceIdentifier
                .builder(Nodes.class)
                .child(Node.class,
                        nodeBuilder.getKey())
                .augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(flowBuilder.getTableId()))
                .child(Flow.class, flowBuilder.getKey()).build();

        InstanceIdentifier<Node> nodePath = InstanceIdentifier
                .builder(Nodes.class)
                .child(Node.class,
                        nodeBuilder.getKey()).toInstance();


        log.trace("writeFlow: nodePath {}", nodePath);
        log.trace("writeFlow: flowPath {}", flowPath);

        readWriteTransaction.merge(LogicalDatastoreType.CONFIGURATION, nodePath, nodeBuilder.build());
        readWriteTransaction.put(LogicalDatastoreType.CONFIGURATION, flowPath, flowBuilder.build());

        ListenableFuture<RpcResult<TransactionStatus>> commitFuture = readWriteTransaction.commit();
        Futures.addCallback(commitFuture, new FutureCallback<RpcResult<TransactionStatus>>() {

            @Override
            public void onSuccess(final RpcResult<TransactionStatus> result) {
                log.debug("writeFlow: Transaction Status {} for Result {} ", result.getResult(), result);
            }

            @Override
            public void onFailure(final Throwable t) {
                log.error("writeFlow: onFailure flowBuilder {} nodeBuilder {}", flowBuilder.getFlowName(), nodeBuilder.getId(), t);
                if(t instanceof OptimisticLockFailedException) {
                    // Failed because of concurrent transaction modifying same data
                    log.error("writeFlow: onFailure: Failed because of concurrent transaction modifying same data, we should retry");
                    if ((retries - 1) > 0){
                        log.debug("writeFlow: retrying for flowBuilder {} nodeBuilder {}", flowBuilder.getFlowName(), nodeBuilder.getId());
                        writeFlow(flowBuilder, nodeBuilder, retries - 1);
                    } else {
                        log.error("writeFlow: no more retries for flowBuilder {} nodeBuilder {}. Aborting", flowBuilder.getFlowName(), nodeBuilder.getId());
                    }
                } else {
//                    log.error("writeFlow: onFailure", t);

                }

            }

        });
    }



    /*
    * Create a NORMAL Table Miss Flow Rule
    * Match: any
    * Action: forward to NORMAL pipeline
    * Borrowed from OVSDB.OF13Provider
    */

    private void writeNormalRule(Node node) {
        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = getFlowCapableNodeBuilder(node, TABLE_0_DEFAULT_INGRESS);
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create the OF Actions and Instructions
        InstructionBuilder ib = new InstructionBuilder();
        InstructionsBuilder isb = new InstructionsBuilder();

        // Instructions List Stores Individual Instructions
        List<Instruction> instructions = new ArrayList<Instruction>();

        // Call the InstructionBuilder Methods Containing Actions
        OpenFlowUtils.createNormalInstructions(ib);
        ib.setOrder(0);
        ib.setKey(new InstructionKey(0));
        instructions.add(ib.build());

        // Add InstructionBuilder to the Instruction(s)Builder List
        isb.setInstruction(instructions);

        // Add InstructionsBuilder to FlowBuilder
        flowBuilder.setInstructions(isb.build());

        String flowId = "NORMAL";
        flowBuilder.setId(new FlowId(flowId));
        FlowKey key = new FlowKey(new FlowId(flowId));
        flowBuilder.setMatch(matchBuilder.build());
        flowBuilder.setPriority(PRIORITY_NORMAL);
        flowBuilder.setBarrier(true);
        flowBuilder.setTableId(TABLE_0_DEFAULT_INGRESS);
        flowBuilder.setKey(key);
        flowBuilder.setFlowName(flowId);
        flowBuilder.setHardTimeout(0);
        flowBuilder.setIdleTimeout(0);
        writeFlow(flowBuilder, nodeBuilder, RETRIES);
    }

    /*
    * Create an LLDP Flow Rule to encapsulate into
    * a packet_in that is sent to the controller
    * for topology handling.
    * Match: Ethertype 0x88CCL
    * Action: Punt to Controller in a Packet_In msg
    * Borrowed from OVSDB.OF13Provider
    */

    private void writeLLDPRule(Node node) {
        log.debug("writeLLDPRule: {}", node.getId());
        EtherType etherType = new EtherType(0x88CCL);

        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = getFlowCapableNodeBuilder(node, TABLE_0_DEFAULT_INGRESS);
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create Match(es) and Set them in the FlowBuilder Object
        flowBuilder.setMatch(OpenFlowUtils.createEtherTypeMatch(matchBuilder, etherType).build());

        // Create the OF Actions and Instructions
        InstructionBuilder ib = new InstructionBuilder();
        InstructionsBuilder isb = new InstructionsBuilder();

        // Instructions List Stores Individual Instructions
        List<Instruction> instructions = new ArrayList<Instruction>();

        // Call the InstructionBuilder Methods Containing Actions
        OpenFlowUtils.createSendToControllerInstructions(ib);
        ib.setOrder(0);
        ib.setKey(new InstructionKey(0));
        instructions.add(ib.build());

        // Add InstructionBuilder to the Instruction(s)Builder List
        isb.setInstruction(instructions);

        // Add InstructionsBuilder to FlowBuilder
        flowBuilder.setInstructions(isb.build());

        String flowId = "LLDP";
        flowBuilder.setId(new FlowId(flowId));
        FlowKey key = new FlowKey(new FlowId(flowId));
        flowBuilder.setBarrier(true);
        flowBuilder.setTableId(TABLE_0_DEFAULT_INGRESS);
        flowBuilder.setPriority(PRIORITY_LLDP);
        flowBuilder.setKey(key);
        flowBuilder.setFlowName(flowId);
        flowBuilder.setHardTimeout(0);
        flowBuilder.setIdleTimeout(0);
        writeFlow(flowBuilder, nodeBuilder, RETRIES);
    }


    private void writeSimpleInOutRule(Node node, NodeConnector inPort, NodeConnector outPort, String flowName, int priority, short table, boolean write){
        MatchBuilder matchBuilder = new MatchBuilder();
        FlowBuilder flowBuilder = new FlowBuilder();
        NodeBuilder nodeBuilder = getFlowCapableNodeBuilder(node, table);

        flowBuilder.setMatch(OpenFlowUtils.createInPortMatch(matchBuilder, node, inPort).build());

        flowBuilder.setId(new FlowId(flowName));
        FlowKey flowKey = new FlowKey(flowBuilder.getId());
        flowBuilder.setStrict(true);
        flowBuilder.setBarrier(false);
        flowBuilder.setTableId(table);
        flowBuilder.setKey(flowKey);
        flowBuilder.setFlowName(flowName);
        flowBuilder.setHardTimeout(0);
        flowBuilder.setIdleTimeout(0);
        flowBuilder.setPriority(priority);

        if(write){
            log.debug("writeSimpleInOutRule: Add/Update flow {} to node {}", flowBuilder, nodeBuilder);
            InstructionBuilder ib = new InstructionBuilder();
            InstructionsBuilder isb = new InstructionsBuilder();
            List<Instruction> instructions = new ArrayList<Instruction>();
            ib = OpenFlowUtils.createOutputPortInstructions(ib, node, outPort);
            ib.setOrder(0);
            ib.setKey(new InstructionKey(0));
            instructions.add(ib.build());

            isb.setInstruction(instructions);

            flowBuilder.setInstructions(isb.build());

            writeFlow(flowBuilder, nodeBuilder, RETRIES);
        } else {
            removeFlow(flowBuilder, nodeBuilder);
        }
    }
    @Override
    public PacketResult receiveDataPacket(RawPacket inPkt) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void notifyNode(org.opendaylight.controller.sal.core.Node node, UpdateType type,
            Map<String, Property> propMap) {
        // TODO Auto-generated method stub

    }

    @Override
    public void notifyNodeConnector(org.opendaylight.controller.sal.core.NodeConnector nodeConnector,
            UpdateType type, Map<String, Property> propMap) {
        // TODO Auto-generated method stub

    }

    @Override
    public void recalculateDone() {
        // TODO Auto-generated method stub

    }

    @Override
    public void notifyHTClient(HostNodeConnector host) {
        // TODO Auto-generated method stub

    }

    @Override
    public void notifyHTClientHostRemoved(HostNodeConnector host) {
        // TODO Auto-generated method stub

    }

    public void setDataPacketService(IDataPacketService s) {
        log.debug("Setting dataPacketService");
        this.dataPacketService = s;
    }

    public void unsetDataPacketService(IDataPacketService s) {
        if (this.dataPacketService == s) {
            this.dataPacketService = null;
        }
    }

    public void setRouting(IRouting routing) {
        if(routing instanceof YKShortestPaths){
            log.debug("Setting routing {}", routing);
            this.routing = routing;
        }
    }

    public void unsetRouting(IRouting routing) {
        if (this.routing == routing) {
            this.routing = null;
        }
    }

    public void setKShortestRoutes(IKShortestRoutes shortestRoutes) {
        log.debug("Setting KShortestRoutes {}", shortestRoutes);
        this.shortestRoutes = shortestRoutes;
    }

    public void unsetKShortestRoutes(IKShortestRoutes shortestRoutes) {
        if (this.shortestRoutes == shortestRoutes) {
            this.shortestRoutes = null;
        }
    }

    public void setTopologyManager(ITopologyManager topologyManager) {
        log.debug("Setting topologyManager");
        this.topologyManager = topologyManager;
    }

    public void unsetTopologyManager(ITopologyManager topologyManager) {
        if (this.topologyManager == topologyManager) {
            this.topologyManager = null;
        }
    }

    public void setHostTracker(IfIptoHost hostTracker) {
        log.debug("Setting HostTracker");
        this.hostTracker = hostTracker;
    }

    public void setForwardingRulesManager(
            IForwardingRulesManager forwardingRulesManager) {
        log.debug("Setting ForwardingRulesManager");
        this.frm = forwardingRulesManager;
    }

    public void unsetHostTracker(IfIptoHost hostTracker) {
        if (this.hostTracker == hostTracker) {
            this.hostTracker = null;
        }
    }

    public void unsetForwardingRulesManager(
            IForwardingRulesManager forwardingRulesManager) {
        if (this.frm == forwardingRulesManager) {
            this.frm = null;
        }
    }

    public void setSwitchManager(ISwitchManager switchManager) {
        this.switchManager = switchManager;
    }

    public void unsetSwitchManager(ISwitchManager switchManager) {
        if (this.switchManager == switchManager) {
            this.switchManager = null;
        }
    }

    @Override
    public Node getMdNode(String nodeDpId) {
//        Long dpid = Long.valueOf(HexEncode.stringToLong(nodeDpId));
        BigInteger dpid = OpenFlowUtils.getDpId(nodeDpId);
        NodeId nodeId = new NodeId("openflow:"+dpid.toString());
        NodeKey nodeKey = new NodeKey(nodeId);
        InstanceIdentifier<Node> nodeIdentifier = InstanceIdentifier.builder(Nodes.class).
                                                        child(Node.class,nodeKey).toInstance();
//        log.trace("getMdNode: MDNodeIdentifier {}", nodeIdentifier);

        IMDSALConsumer mdsalConsumer = (IMDSALConsumer) ServiceHelper.getInstance(IMDSALConsumer.class, "default", this);
        if (mdsalConsumer == null) {
            log.error("getMdNode: ERROR finding MDSAL Service.");
            return null;
        }

        DataBroker dataBroker = mdsalConsumer.getDataBroker();
        if (dataBroker == null) {
            log.error("getMdNode: ERROR finding reference for DataBroker. Please check out the MD-SAL support on the Controller.");
            return null;
        }

        ReadOnlyTransaction readTx = dataBroker.newReadOnlyTransaction();
        Optional<Node> data;
        try {
            data = readTx.read(LogicalDatastoreType.OPERATIONAL, nodeIdentifier).get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("getMdNode: {}", e.getMessage(), e);
            return null;
        }

        if(data.isPresent()) {
           // data are present in data store.
//            log.debug("getMdNode: data {}", data);
            Node node = data.get();
//            log.trace("getMdNode: Node {}", node.getId());
            return node;
        } else {
            log.error("getMdNode: data is not present for identifier {}", nodeIdentifier);
            return null;
        }

    }

    public NodeConnector getMdNodeConnector(Node node, String portId) {
        return null;
    }

    private Node constructMDNode(final org.opendaylight.controller.sal.core.Node node) {
//        log.trace("constructMDNode: ADNode: {}", node);
//        final Set<org.opendaylight.controller.sal.core.NodeConnector> connectors = switchManager
//                .getNodeConnectors(node);
//        final ArrayList<NodeConnector> tpList = new ArrayList<NodeConnector>(
//                connectors.size());
//        for (final org.opendaylight.controller.sal.core.NodeConnector connector : connectors) {
//            tpList.add(constructMDNodeConnector(connector));
//        }
//
//        Node mdNode =  new NodeBuilder().setKey(InventoryMapping.toNodeKey(node))
//                .setNodeConnector(tpList).build();
//        log.trace("constructMDNode: ADNode: {} MDNode: {}", node, mdNode);

        return getMdNode(node.getNodeIDString());
    }

    private static NodeConnector constructMDNodeConnector(final org.opendaylight.controller.sal.core.NodeConnector connector) {
        String nodeName = connector.getNode().getNodeIDString();
        BigInteger dpid = OpenFlowUtils.getDpId(nodeName);
        NodeId nodeId = new NodeId("openflow:"+dpid.toString());
        NodeKey nodeKey = new NodeKey(nodeId);
        InstanceIdentifier<Node> nodeIdentifier = InstanceIdentifier.builder(Nodes.class).
                                                      child(Node.class,nodeKey).toInstance();
        String portId = connector.getNodeConnectorIDString();
        NodeConnectorId ncId = new NodeConnectorId(nodeId.getValue() + ":" + portId);
        NodeConnector nodeConnector = new NodeConnectorBuilder().setId(ncId).build();
        log.trace("constructMDNodeConnector: ADNC: {}, MDNC: {}", connector, nodeConnector);
        return nodeConnector;
    }

    private static NodeBuilder getFlowCapableNodeBuilder(Node node, short tableNumber){
        List<Table> tables = new ArrayList<>();
        TableBuilder tableBuilder = new TableBuilder();
        tableBuilder.setKey(new TableKey(tableNumber));
        tables.add(tableBuilder.build());

        FlowCapableNodeBuilder flowCapableNodeBuilder = new FlowCapableNodeBuilder();
        flowCapableNodeBuilder.setTable(tables);

        NodeBuilder nodeBuilder = new NodeBuilder(node);
        nodeBuilder.setKey(new NodeKey(nodeBuilder.getId()));
        nodeBuilder.addAugmentation(FlowCapableNode.class, flowCapableNodeBuilder.build());

        return nodeBuilder;

    }

    @Override
    public Long getExternalInterfaceOfPort(final Node ofNode){
        OvsdbConfigService ovsdbConfigService = (OvsdbConfigService)ServiceHelper.getGlobalInstance(OvsdbConfigService.class, this);
        IConnectionServiceInternal connectionService = (IConnectionServiceInternal)ServiceHelper.getGlobalInstance(IConnectionServiceInternal.class, this);
        List<org.opendaylight.controller.sal.core.Node> ovsNodes = connectionService.getNodes();

        for (org.opendaylight.controller.sal.core.Node ovsNode : ovsNodes) {
            Map<String, Row> bridges = ovsdbConfigService.getRows(ovsNode, ovsdbConfigService.getTableName(ovsNode, Bridge.class));
            if (bridges == null) continue;
            for (String brUuid : bridges.keySet()) {
                Bridge bridge = ovsdbConfigService.getTypedRow(ovsNode, Bridge.class, bridges.get(brUuid));

//                long bridgeDpid = sEncode.stringToLong((String)bridge.getDatapathIdColumn().getData().toArray()[0]);
                BigInteger bridgeDpid = OpenFlowUtils.getDpId((String)bridge.getDatapathIdColumn().getData().toArray()[0]);
//                long ofNodeDpid = Long.parseLong(ofNode.getId().getValue().split(":")[1]);
                BigInteger ofNodeDpid = new BigInteger(ofNode.getId().getValue().split(":")[1]);

                if (ofNodeDpid.equals(bridgeDpid)){
                    // Found the bridge
                    log.trace("getExternalInterfaceOfPort: found ovsNode {} bridge {} for ofNode {}", ovsNode.getNodeIDString(), bridge.getName(), ofNode.getId());
                    return getExternalInterfaceOfPort(ovsNode, bridge);

                }
            }
        }
        log.error("getExternalInterfaceOfPort: didn't find ExternalInterface in ofNode {} make sure the type is system", ofNode.getId());
        return null;
    }

    private Long getExternalInterfaceOfPort(org.opendaylight.controller.sal.core.Node ovsNode, Bridge bridge){
        OvsdbConfigService ovsdbConfigService = (OvsdbConfigService)ServiceHelper.getGlobalInstance(OvsdbConfigService.class, this);
        Map<String, Row> ports = ovsdbConfigService.getRows(ovsNode, ovsdbConfigService.getTableName(ovsNode, Port.class));
        Set<UUID> portUuids = bridge.getPortsColumn().getData();
        if (portUuids == null || ports == null) {
            log.error("getExternalInterfaceOfPort: didn't find ExternalInterface in ovsNode {} bridge {} ports are not available", ovsNode.getNodeIDString(), bridge.getName());
            return null;
        }

        for (UUID portUuid : portUuids) {
            Port port = ovsdbConfigService.getTypedRow(ovsNode, Port.class, ports.get(portUuid.toString()));
            if (port == null || port.getInterfacesColumn() == null || port.getInterfacesColumn().getData() == null) continue;
            UUID interfaceUuid = (UUID) port.getInterfacesColumn().getData().toArray()[0];
            Row interfaceRow = ovsdbConfigService.getRow(ovsNode, ovsdbConfigService.getTableName(ovsNode, Interface.class), interfaceUuid.toString());
            Interface intf = ovsdbConfigService.getTypedRow(ovsNode, Interface.class, interfaceRow);
            if (intf.getTypeColumn().getData().equalsIgnoreCase("system")){
                log.trace("getExternalInterfaceOfPort: found externalInterface {}={}, ovsNode {}, bridge {}", intf.getName(), intf.getOpenFlowPortColumn().getData(), ovsNode, bridge.getName());
                return (Long) intf.getOpenFlowPortColumn().getData().toArray()[0];
            }
        }
        log.error("getExternalInterfaceOfPort: didn't find ExternalInterface in ovsNode {} bridge {} make sure the type is system", ovsNode.getNodeIDString(), bridge.getName());
        return null;
    }

    private org.opendaylight.controller.sal.core.Node getOvsNode(Node ofNode){
        OvsdbConfigService ovsdbConfigService = (OvsdbConfigService)ServiceHelper.getGlobalInstance(OvsdbConfigService.class, this);
        IConnectionServiceInternal connectionService = (IConnectionServiceInternal)ServiceHelper.getGlobalInstance(IConnectionServiceInternal.class, this);
        List<org.opendaylight.controller.sal.core.Node> ovsNodes = connectionService.getNodes();

        for (org.opendaylight.controller.sal.core.Node ovsNode : ovsNodes) {
            Map<String, Row> bridges = ovsdbConfigService.getRows(ovsNode, ovsdbConfigService.getTableName(ovsNode, Bridge.class));
            if (bridges == null) continue;
            for (String brUuid : bridges.keySet()) {
                Bridge bridge = ovsdbConfigService.getTypedRow(ovsNode, Bridge.class, bridges.get(brUuid));

                BigInteger bridgeDpid = OpenFlowUtils.getDpId((String)bridge.getDatapathIdColumn().getData().toArray()[0]);
                BigInteger ofNodeDpid = new BigInteger(ofNode.getId().getValue().split(":")[1]);

                if (ofNodeDpid.equals(bridgeDpid)){
                    // Found the bridge
                    log.trace("getOvsNode: found ovsNode {} bridge {} for ofNode {}", ovsNode.getNodeIDString(), bridge.getName(), ofNode.getId());
                    return ovsNode;

                }
            }
        }
        return null;
    }

    @Override
    public Path getProgrammedPath(Tunnel tunnel) {
        Path path = tunnelsPath.get(tunnel);
        log.info("getProgrammedPath: Tunnel {}, Path {}, DSCP {}", tunnel, path, tunnelsDscp.get(tunnel));
        return path;
    }

    @Override
    public String reportNetwork(String segmentationId){
        StringBuilder reportBuilder = new StringBuilder();
        StringBuilder detailsBuilder = new StringBuilder();

        ITunnelObserver tunnelObserver = (ITunnelObserver) ServiceHelper.getGlobalInstance(ITunnelObserver.class, this);
        if(tunnelObserver == null){
            log.error("reportNetwork: TenantTunnelObserver is not available");
            return "TenantTunnelObserver is not available";
        }
        tunnelObserver.loadTunnelEndPoints(segmentationId);
        Set<TunnelEndPoint> teps = tunnelObserver.getTunnelEndPoints().get(segmentationId);

        List<Tunnel> tunnels = null;
        List<Short> dscps = new ArrayList<>();
        try {
            tunnels = Tunnel.createTunnels(teps);
        } catch (Exception e) {
            log.error("reportNetwork: Can't create Tunnels from TEPs", e);
            return "Can't create Tunnels from TEPs";
        }

        int sumHops = 0, sumWeight = 0;
        for (Tunnel tunnel : tunnels) {
            Path tunnelPath = getProgrammedPath(tunnel);

            double pathWeight = 0;
            for (Edge edge : tunnelPath.getEdges()) {
                pathWeight += ((WeightedEdge) edge).getWeight();
            }

            sumHops += tunnelPath.getEdges().size();
            sumWeight += pathWeight;
            dscps.add(tunnelsDscp.get(tunnel));
            detailsBuilder.append(tunnel)
                            .append('\n')
                            .append(" #Hops:" + tunnelPath.getEdges().size())
                            .append('\n')
                            .append(" Weight: " + pathWeight)
                            .append('\n')
                            .append(" DSCP: " + tunnelsDscp.get(tunnel))
                            .append('\n');
        }
        double avgHops = sumHops / tunnels.size();
        double avgWeight = sumWeight / tunnels.size();

        reportBuilder.append("Details: ")
                        .append('\n')
                        .append(detailsBuilder.toString())
                        .append('\n');

        reportBuilder.append("Summary: ")
                        .append('\n')
                        .append(" Number of Tunnels: " + tunnels.size())
                        .append('\n')
                        .append(" Avg #Hops: " + avgHops)
                        .append('\n')
                        .append(" Avg Weight: " + avgWeight)
                        .append('\n')
                        .append(" DSCP(s): " + dscps)
                        .append('\n');



        return reportBuilder.toString();
    }
}
