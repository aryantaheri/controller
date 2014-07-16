package org.opendaylight.controller.samples.differentiatedforwarding.internal;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.opendaylight.controller.clustering.services.CacheConfigException;
import org.opendaylight.controller.clustering.services.CacheExistException;
import org.opendaylight.controller.clustering.services.IClusterContainerServices;
import org.opendaylight.controller.clustering.services.IClusterServices;
import org.opendaylight.controller.forwardingrulesmanager.FlowEntry;
import org.opendaylight.controller.forwardingrulesmanager.IForwardingRulesManager;
import org.opendaylight.controller.hosttracker.IfIptoHost;
import org.opendaylight.controller.hosttracker.IfNewHostNotify;
import org.opendaylight.controller.hosttracker.hostAware.HostNodeConnector;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.DataModification;
import org.opendaylight.controller.routing.yenkshortestpaths.internal.IKShortestRoutes;
import org.opendaylight.controller.routing.yenkshortestpaths.internal.YKShortestPaths;
import org.opendaylight.controller.sal.binding.api.data.DataBrokerService;
import org.opendaylight.controller.sal.compatibility.InventoryMapping;
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
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.samples.differentiatedforwarding.IForwarding;
import org.opendaylight.controller.samples.differentiatedforwarding.OpenFlowUtils;
import org.opendaylight.controller.samples.differentiatedforwarding.Tunnel;
import org.opendaylight.controller.samples.simpleforwarding.HostNodePair;
import org.opendaylight.controller.switchmanager.IInventoryListener;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.controller.topologymanager.ITopologyManager;
import org.opendaylight.ovsdb.neutron.IMDSALConsumer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private ConcurrentMap<HostNodePair, HashMap<org.opendaylight.controller.sal.core.NodeConnector, FlowEntry>> rulesDB;
    private static final String DIFF_FORWARDING_RULES_CACHE_NAME = "differentiatedforwarding.ipswitch.rules";


    /**
     * OpenFlow Tables
     */
//    private static final short TABLE_TUNNEL_QOS = 100;
    private static final short TABLE_0_DEFAULT_INGRESS = 0;

    /**
     * Function called by the dependency manager when all the required
     * dependencies are satisfied
     *
     */
    void init() {
        log.debug("init()");
        allocateRulesDB();
        //FIME: This is wrong and should be fixed by introducing a new Interface for YKSP
        Object[] rList = ServiceHelper.getGlobalInstances(IRouting.class, this, null);
        for (int i = 0; i < rList.length; i++) {
            if (rList[i] instanceof YKShortestPaths){
                setRouting((IRouting) rList[i]);
                break;
            }
        }
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

    @SuppressWarnings("unchecked")
    private void allocateRulesDB() {
        if (this.clusterContainerService == null) {
            log.trace("allocateRulesDB: clusterContainerService is null, can't create cache");
            return;
        }

        try {
            rulesDB = (ConcurrentMap<HostNodePair, HashMap<org.opendaylight.controller.sal.core.NodeConnector, FlowEntry>>) clusterContainerService.createCache(DIFF_FORWARDING_RULES_CACHE_NAME, EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));
        } catch (CacheExistException cee) {
            log.error("\nCache already exists - destroy and recreate if needed", cee);
        } catch (CacheConfigException cce) {
            log.error("\nCache configuration invalid - check cache mode", cce);
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
        Path path;
        if (paths.size() >= classNum){
            path = paths.get(classNum - 1);
        } else {
            log.error("programTunnelForwarding: request classNum {} is not possible, setting largest possible value {}", classNum, paths.size());
            path = paths.get(paths.size() - 1);
        }

        log.debug("programTunnelForwarding: selected path for tunnel {} with classNum {} is {}", tunnel, classNum, path);
        List<Edge> edges = path.getEdges();



        /**
         * lastInPort-srcNode-outPort ----> inPort-dstNode
         */
        org.opendaylight.controller.sal.core.NodeConnector lastInPort = null;
        for (Edge edge : edges) {
            org.opendaylight.controller.sal.core.NodeConnector outPort = edge.getTailNodeConnector();
            org.opendaylight.controller.sal.core.NodeConnector inPort = edge.getHeadNodeConnector();

            Node outPortMDNode = constructMDNode(outPort.getNode());
            NodeConnector outPortMDNC = constructMDNodeConnector(outPort);

            Node inPortMDNode = constructMDNode(inPort.getNode());
            NodeConnector inPortMDNC = constructMDNodeConnector(inPort);

            if (lastInPort == null){
                // Tunnel src
                programEdgeSource(tunnel, null, null, outPortMDNode, outPortMDNC, write);

            } else {
                // middle/end node or err
                Node lastInPortMDNode = constructMDNode(lastInPort.getNode());
                NodeConnector lastInPortMDNC = constructMDNodeConnector(lastInPort);

                programEdgeSource(tunnel, lastInPortMDNode, lastInPortMDNC, outPortMDNode, outPortMDNC, write);
            }

            lastInPort = inPort;

        }

    }

    private void programEdgeSource(final Tunnel tunnel, final Node lastInPortMDNode, final NodeConnector lastInPortMDNC,
                                        final Node outPortMDNode, final NodeConnector outPortMDNC, boolean write) {

        log.debug("programEdgeSource: tunnel {}, lastInPortMDNode {}, lastInPortMDNC {}, outPortMDNode {}, outPortMDNC {}, write {}",
                tunnel, lastInPortMDNode, lastInPortMDNC, outPortMDNode, outPortMDNC, write);
        if (lastInPortMDNode != null && !lastInPortMDNode.equals(outPortMDNode)){
            log.error("programEdgeSource: lastInPortNode {} is not equal to outPort {}. Inconsistency found, returning", lastInPortMDNode, outPortMDNode);
            return;
        }
        MatchBuilder matchBuilder = new MatchBuilder();
        FlowBuilder flowBuilder = new FlowBuilder();
        NodeBuilder nodeBuilder = new NodeBuilder(outPortMDNode);
        nodeBuilder.setKey(new NodeKey(nodeBuilder.getId()));

        if (lastInPortMDNode != null && lastInPortMDNC != null){
            log.trace("programEdgeSource: middle/last node/edge is programming");
            // Match on lastInPort
            flowBuilder.setMatch(OpenFlowUtils.createInPortMatch(matchBuilder, lastInPortMDNode, lastInPortMDNC).build());
        } else {
            log.trace("programEdgeSource: tunnel source node/first edge is programming");
        }
        // Match on Tunnel ID
        flowBuilder.setMatch(OpenFlowUtils.createTunnelIDMatch(matchBuilder, new BigInteger(tunnel.getTunnelKey())).build());
        // Match on Destination IP
        flowBuilder.setMatch(OpenFlowUtils.createDstL3IPv4Match(matchBuilder, tunnel.getDstAddress()).build());
        // Match on Source IP
        flowBuilder.setMatch(OpenFlowUtils.createSrcL3IPv4Match(matchBuilder, tunnel.getSrcAddress()).build());


        String flowName = "QoS_Tunnel_" + tunnel.getTunnelKey() + "_"
                                        + outPortMDNode.getId() + "_"
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

        if(write){
            // ADD/UPDATE
            log.debug("programTunnelForwarding: Add/Update flow {} to node {}", flowBuilder, nodeBuilder);
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
            writeFlow(flowBuilder, nodeBuilder);


        } else {
            // DELETE
            log.debug("programTunnelForwarding: Delete flow {} from node {}", flowBuilder, nodeBuilder);
            removeFlow(flowBuilder, nodeBuilder);
        }
    }

    private void programEdgeDestination() {
        // TODO Auto-generated method stub

    }

    private void removeFlow(FlowBuilder flowBuilder, NodeBuilder nodeBuilder) {
        log.error("removeFlow: not implemented");
    }

    private void writeFlow(FlowBuilder flowBuilder, NodeBuilder nodeBuilder) {
        log.debug("writeFlow: FlowBuilder {}, NodeBuilder {}", flowBuilder, nodeBuilder);
        IMDSALConsumer mdsalConsumer = (IMDSALConsumer) ServiceHelper.getInstance(IMDSALConsumer.class, "default", this);
        if (mdsalConsumer == null) {
            log.error("writeFlow: ERROR finding MDSAL Service.");
            return;
        }

        DataBrokerService dataBrokerService = mdsalConsumer.getDataBrokerService();

        if (dataBrokerService == null) {
            log.error("writeFlow: ERROR finding reference for DataBrokerService. Please check out the MD-SAL support on the Controller.");
            return;
        }
        DataModification<InstanceIdentifier<?>, DataObject> modification = dataBrokerService.beginTransaction();
        InstanceIdentifier<Flow> path1 = InstanceIdentifier
                .builder(Nodes.class)
                .child(Node.class,
                        nodeBuilder.getKey())
                .augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(flowBuilder.getTableId()))
                .child(Flow.class, flowBuilder.getKey()).build();

        InstanceIdentifier<Node> nodeId = InstanceIdentifier
                .builder(Nodes.class)
                .child(Node.class,
                        nodeBuilder.getKey()).toInstance();


        modification.putConfigurationData(nodeId, nodeBuilder.build());
        modification.putConfigurationData(path1, flowBuilder.build());
        Future<RpcResult<TransactionStatus>> commitFuture = modification.commit();
        try {
            RpcResult<TransactionStatus> result = commitFuture.get();
            TransactionStatus status = result.getResult();
            log.debug("writeFlow: Transaction Status "+status.toString()+" for Flow "+flowBuilder.getFlowName());
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        } catch (ExecutionException e) {
            log.error(e.getMessage(), e);
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
    public ConcurrentMap<HostNodePair, HashMap<org.opendaylight.controller.sal.core.NodeConnector, FlowEntry>> getRulesDB() {
        return rulesDB;
    }

    private Node constructMDNode(final org.opendaylight.controller.sal.core.Node node) {
        log.trace("constructMDNode: ADNode: {}", node);
        final Set<org.opendaylight.controller.sal.core.NodeConnector> connectors = switchManager
                .getNodeConnectors(node);
        final ArrayList<NodeConnector> tpList = new ArrayList<NodeConnector>(
                connectors.size());
        for (final org.opendaylight.controller.sal.core.NodeConnector connector : connectors) {
            tpList.add(constructMDNodeConnector(connector));
        }

        Node mdNode =  new NodeBuilder().setKey(InventoryMapping.toNodeKey(node))
                .setNodeConnector(tpList).build();
        log.trace("constructMDNode: ADNode: {} MDNode: {}", node, mdNode);
        return mdNode;
    }

    private static NodeConnector constructMDNodeConnector(final org.opendaylight.controller.sal.core.NodeConnector connector) {
        return new NodeConnectorBuilder().setKey(
                InventoryMapping.toNodeConnectorKey(connector)).build();
    }

}
