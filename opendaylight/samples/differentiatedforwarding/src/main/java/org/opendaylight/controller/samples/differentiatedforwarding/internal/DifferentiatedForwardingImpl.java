package org.opendaylight.controller.samples.differentiatedforwarding.internal;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.clustering.services.CacheConfigException;
import org.opendaylight.controller.clustering.services.CacheExistException;
import org.opendaylight.controller.clustering.services.IClusterContainerServices;
import org.opendaylight.controller.clustering.services.IClusterServices;
import org.opendaylight.controller.forwardingrulesmanager.FlowEntry;
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
import org.opendaylight.controller.routing.yenkshortestpaths.internal.IKShortestRoutes;
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
import org.opendaylight.controller.sal.utils.HexEncode;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.samples.differentiatedforwarding.IForwarding;
import org.opendaylight.controller.samples.differentiatedforwarding.OpenFlowUtils;
import org.opendaylight.controller.samples.differentiatedforwarding.Tunnel;
import org.opendaylight.controller.samples.simpleforwarding.HostNodePair;
import org.opendaylight.controller.switchmanager.IInventoryListener;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.controller.topologymanager.ITopologyManager;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
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
    private ConcurrentMap<HostNodePair, HashMap<org.opendaylight.controller.sal.core.NodeConnector, FlowEntry>> rulesDB;
    private static final String DIFF_FORWARDING_RULES_CACHE_NAME = "differentiatedforwarding.ipswitch.rules";


    /**
     * OpenFlow Tables
     */
//    private static final short TABLE_TUNNEL_QOS = 100;
    private static final short TABLE_0_DEFAULT_INGRESS = 0;

    /**
     * OpenFlow Priorities
     */
    private static final int PRIORITY_NORMAL = 0;
    private static final int PRIORITY_TUNNEL_SRC_IN = 1001;
    private static final int PRIORITY_TUNNEL_DST_OUT = 1001;
    private static final int PRIORITY_TUNNEL_TRANSIT = 1000;


    /**
     * Function called by the dependency manager when all the required
     * dependencies are satisfied
     *
     */
    void init() {
        log.debug("init()");
        allocateRulesDB();
        //FIME: This is wrong and should be fixed by introducing a new Interface for YKSP
//        Object[] rList = ServiceHelper.getInstance(IKShortestRoutes.class, this);


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

            if (lastInPort == null){
                // Tunnel src
                programTunnelSource(tunnel, outPortMDNode, outPortMDNC, write);
                programEdgeTail(tunnel, null, null, outPortMDNode, outPortMDNC, write);
                writeNormalRule(outPortMDNode);
            } else {
                // middle/end node or err
                lastInPortMDNode = constructMDNode(lastInPort.getNode());
                lastInPortMDNC = constructMDNodeConnector(lastInPort);

                programEdgeTail(tunnel, lastInPortMDNode, lastInPortMDNC, outPortMDNode, outPortMDNC, write);
            }

            lastInPort = inPort;
//            lastInPortMDNC = inPortMDNC;
//            lastInPortMDNode = inPortMDNode;
        }

        // Here lastInPort is the nodeConnector representing the TEP.
        // 1) Use a NORMAL rule to find the host, and forward
        // 2) Be more strict, discover IP-MAC resolution, and program flow accordingly
        Node tepNode = constructMDNode(lastInPort.getNode());
        writeNormalRule(tepNode);
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
        handleIngressTrafficToTunnelSource(tunnel, outPortMDNode, outPortMDNC, write);
        // NOTE: in OVS you can not enforce tunnel egress port.
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

            writeFlow(flowBuilder, nodeBuilder);
        } else {
            removeFlow(flowBuilder, nodeBuilder);
        }
    }

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


        // FIXME: Match on Tunnel ID. This is broken!
//        flowBuilder.setMatch(OpenFlowUtils.createTunnelIDMatch(matchBuilder, new BigInteger(tunnel.getTunnelKey())).build());
        // FIXME: Alternatively use destination tunnel port to cover the tunnel ID. tp_dst=tun_id,
        // this should be taken care of while creating tunnels
        flowBuilder.setMatch(OpenFlowUtils.createDstPortUdpMatch(matchBuilder, new PortNumber(Integer.parseInt(tunnel.getTunnelKey()))).build());
        // Match on Destination IP
        flowBuilder.setMatch(OpenFlowUtils.createDstL3IPv4Match(matchBuilder, tunnel.getDstAddress()).build());
        // Match on Source IP
        flowBuilder.setMatch(OpenFlowUtils.createSrcL3IPv4Match(matchBuilder, tunnel.getSrcAddress()).build());


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
            writeFlow(flowBuilder, nodeBuilder);


        } else {
            // DELETE
            log.debug("programEdgeTail: Delete flow {} from node {}", flowBuilder, nodeBuilder);
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



//        Iterator<PathArgument> nodePathArgs = nodePath.getPathArguments().iterator();
//        InstanceIdentifier nodeCurrent = InstanceIdentifier.builder(Nodes.class).toInstance();
//        Iterator<PathArgument> flowPathArgs = flowPath.getPathArguments().iterator();
//        InstanceIdentifier flowCurrent = InstanceIdentifier.builder(Nodes.class).build();
//        try {
//            while(nodePathArgs.hasNext()) {
//                nodeCurrent = nodeCurrent.child(nodePathArgs.next().getClass());
//                log.trace("writeFlow: fixing node parents current: {}", nodeCurrent);
//                    if(readWriteTransaction.read(LogicalDatastoreType.CONFIGURATION, nodeCurrent).get() == null){
//                        log.trace("writeFlow: current: {} is null. merging", nodeCurrent);
//                        readWriteTransaction.merge(LogicalDatastoreType.CONFIGURATION, nodeCurrent, null);
//                        log.trace("writeFlow: current: {} is null. merged", nodeCurrent);
//                    }
//            }
//            while(flowPathArgs.hasNext()) {
//                flowCurrent = flowCurrent.child(flowPathArgs.next().getClass());
//                log.trace("writeFlow: fixing flow parents current: {}", flowCurrent);
//                if(readWriteTransaction.read(LogicalDatastoreType.CONFIGURATION, flowCurrent).get() == null){
//                    log.trace("writeFlow: flowcurrent: {} is null. merging", flowCurrent);
//                    readWriteTransaction.merge(LogicalDatastoreType.CONFIGURATION, flowCurrent, null);
//                    log.trace("writeFlow: flowcurrent: {} is null. merged", flowCurrent);
//                }
//            }
//        } catch (InterruptedException e) {
//            log.error("writeFlow: {}", e.getMessage(), e);
//            return;
//        } catch (ExecutionException e) {
//            log.error("writeFlow: {}", e.getMessage(), e);
//            return;
//        }


//        log.trace("writeFlow: nodeBuilder {}", nodeBuilder.build());
//        log.trace("writeFlow: flowBuilder {}", flowBuilder.build());
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
                log.error("writeFlow: onFailure", t);
                if(t instanceof OptimisticLockFailedException) {
                    // Failed because of concurrent transaction modifying same data
                    log.error("writeFlow: Failed because of concurrent transaction modifying same data, we should retry", t);
                }
            }

        });
//        modification.putConfigurationData(nodePath, nodeBuilder.build());
//        modification.putConfigurationData(flowPath, flowBuilder.build());
//        Future<RpcResult<TransactionStatus>> commitFuture = modification.commit();
//        try {
//            RpcResult<TransactionStatus> result = commitFuture.get();
//            TransactionStatus status = result.getResult();
//            log.debug("writeFlow: Transaction Status "+status.toString()+" for Flow "+flowBuilder.getFlowName());
//        } catch (InterruptedException e) {
//            log.error(e.getMessage(), e);
//        } catch (ExecutionException e) {
//            log.error(e.getMessage(), e);
//        }

    }

//    private final void ensureParentsByMerge(
//            final LogicalDatastoreType store,
//            final org.opendaylight.yangtools.yang.data.api.InstanceIdentifier normalizedPath,
//            final InstanceIdentifier<?> path,
//            ReadWriteTransaction readWriteTransaction) {
//        List<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument> currentArguments = new ArrayList<>();
//        DataNormalizationOperation<?> currentOp = getCodec().getDataNormalizer().getRootOperation();
//        Iterator<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument> iterator = normalizedPath.getPathArguments().iterator();
//        while (iterator.hasNext()) {
//            org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument currentArg = iterator.next();
//            try {
//                currentOp = currentOp.getChild(currentArg);
//            } catch (DataNormalizationException e) {
//                throw new IllegalArgumentException(String.format(
//                        "Invalid child encountered in path %s", path), e);
//            }
//            currentArguments.add(currentArg);
//            org.opendaylight.yangtools.yang.data.api.InstanceIdentifier currentPath = org.opendaylight.yangtools.yang.data.api.InstanceIdentifier
//                    .create(currentArguments);
//
//            final Optional<NormalizedNode<?, ?>> d;
//            try {
//                d = getDelegate().read(store, currentPath).get();
//            } catch (InterruptedException | ExecutionException e) {
//                log.error(
//                        "Failed to read pre-existing data from store {} path {}",
//                        store, currentPath, e);
//                throw new IllegalStateException(
//                        "Failed to read pre-existing data", e);
//            }
//
//            if (!d.isPresent() && iterator.hasNext()) {
//                getDelegate().merge(store, currentPath,
//                        currentOp.createDefault(currentArg));
//            }
//        }
//    }

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
        writeFlow(flowBuilder, nodeBuilder);
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

    @Override
    public Node getMdNode(String nodeName) {
        Long dpid = Long.valueOf(HexEncode.stringToLong(nodeName));
        NodeId nodeId = new NodeId("openflow:"+dpid.toString());
        NodeKey nodeKey = new NodeKey(nodeId);
        InstanceIdentifier<Node> nodeIdentifier = InstanceIdentifier.builder(Nodes.class).
                                                        child(Node.class,nodeKey).toInstance();
        log.trace("getMdNode: MDNodeIdentifier {}", nodeIdentifier);

        IMDSALConsumer mdsalConsumer = (IMDSALConsumer) ServiceHelper.getInstance(IMDSALConsumer.class, "default", this);
        if (mdsalConsumer == null) {
            log.error("writeFlow: ERROR finding MDSAL Service.");
            return null;
        }

        DataBroker dataBroker = mdsalConsumer.getDataBroker();
        if (dataBroker == null) {
            log.error("writeFlow: ERROR finding reference for DataBroker. Please check out the MD-SAL support on the Controller.");
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
            log.trace("getMdNode: Node {}", node.getId());
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
        Long dpid = Long.valueOf(HexEncode.stringToLong(nodeName));
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


}
