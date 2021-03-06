package org.opendaylight.controller.samples.differentiatedforwarding.internal;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import org.opendaylight.controller.clustering.services.IClusterContainerServices;
import org.opendaylight.controller.networkconfig.neutron.INeutronNetworkCRUD;
import org.opendaylight.controller.networkconfig.neutron.NeutronNetwork;
import org.opendaylight.controller.networkconfig.neutron.NeutronPort;
import org.opendaylight.controller.routing.yenkshortestpaths.internal.IKShortestRoutes;
import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.Edge;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.utils.HexEncode;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.samples.differentiatedforwarding.AbstractEvent.Action;
import org.opendaylight.controller.samples.differentiatedforwarding.ITunnelObserver;
import org.opendaylight.controller.samples.differentiatedforwarding.SouthboundEvent;
import org.opendaylight.controller.samples.differentiatedforwarding.Tunnel;
import org.opendaylight.controller.samples.differentiatedforwarding.TunnelEndPoint;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.openstack.netvirt.api.Constants;
import org.opendaylight.ovsdb.plugin.IConnectionServiceInternal;
import org.opendaylight.ovsdb.plugin.OvsdbConfigService;
import org.opendaylight.ovsdb.plugin.OvsdbInventoryListener;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.schema.openvswitch.Interface;
import org.opendaylight.ovsdb.schema.openvswitch.OpenVSwitch;
import org.opendaylight.ovsdb.schema.openvswitch.Port;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TenantTunnelObserver implements OvsdbInventoryListener,
        ITunnelObserver {
    private static Logger log = LoggerFactory
            .getLogger(TenantTunnelObserver.class);
    public static String REMOVED_BY_TENANT_TUNNEL_OBSERVER = "REMOVED_BY_TENANT_TUNNEL_OBSERVER";
    static final String FORWARDING_RULES_CACHE_NAME = "forwarding.ipswitch.rules";

    private IClusterContainerServices clusterContainerService = null;
    private IKShortestRoutes shortestRoutes;
    OvsdbConfigService ovsdbConfigService;
    /**
     * Tunnel discovery using OVSDB plugin
     */
    private BlockingQueue<SouthboundInterfaceEvent> ovsdbTunnelInterfaceEvents;
    private ExecutorService tunnelEventHandler;
    // TODO: Use clusterContainerService.createCache for creating the object and
    // storage
    private HashMap<String, List<Tunnel>> tunnelsMap;
    private HashMap<String, Set<TunnelEndPoint>> tunnelEndPointsMap;

    /**
     * Function called by the dependency manager when all the required
     * dependencies are satisfied
     *
     */
    void init() {
        log.debug("init()");
        ovsdbTunnelInterfaceEvents = new LinkedBlockingQueue<SouthboundInterfaceEvent>();
        tunnelEventHandler = Executors.newSingleThreadExecutor();
        tunnelsMap = new HashMap<String, List<Tunnel>>();
        tunnelEndPointsMap = new HashMap<String, Set<TunnelEndPoint>>();
        // FIXME: This is wrong
        OvsdbConfigService ovsdbConfigService = (OvsdbConfigService) ServiceHelper
                .getGlobalInstance(OvsdbConfigService.class, this);
        setOVSDBConfigService(ovsdbConfigService);
        IKShortestRoutes shortestRoutes = (IKShortestRoutes) ServiceHelper
                .getGlobalInstance(IKShortestRoutes.class, this);
        setKShortestRoutes(shortestRoutes);
    }

    /**
     * Function called by the dependency manager when at least one dependency
     * become unsatisfied or when the component is shutting down because for
     * example bundle is being stopped.
     *
     */
    void destroy() {
        log.debug("destroy()");
    }

    /**
     * Function called by dependency manager after "init ()" is called and after
     * the services provided by the class are registered in the service registry
     *
     */
    void start() {
        log.debug("start()");
        startTunnelEventHandler();
        bulkLoadTunnelsEvents();
    }

    /**
     * Function called by the dependency manager before the services exported by
     * the component are unregistered, this will be followed by a "destroy ()"
     * calls
     *
     */
    void stop() {
        log.debug("stop()");
        tunnelEventHandler.shutdownNow();
    }

    @Override
    public void nodeAdded(Node node) {
        // TODO Auto-generated method stub

    }

    @Override
    public void nodeRemoved(Node node) {
        // TODO Auto-generated method stub

    }

    @Override
    public void rowAdded(Node node, String tableName, String uuid, Row row) {
        this.enqueueTunnelInterfaceEvent(new SouthboundInterfaceEvent(node,
                tableName, uuid, row, SouthboundEvent.Action.ADD));
    }

    @Override
    public void rowUpdated(Node node, String tableName, String uuid, Row old,
            Row row) {
        SouthboundInterfaceEvent event = new SouthboundInterfaceEvent(node,
                tableName, uuid, row, SouthboundEvent.Action.UPDATE);
        event.setOldRow(old);
        this.enqueueTunnelInterfaceEvent(event);
    }

    @Override
    public void rowRemoved(Node node, String tableName, String uuid, Row row,
            Object context) {
        this.enqueueTunnelInterfaceEvent(new SouthboundInterfaceEvent(node,
                tableName, uuid, row, context, SouthboundEvent.Action.DELETE));
    }

    /**
     * Enqueue only Tunnel Interface Events
     *
     * @param event
     */
    private void enqueueTunnelInterfaceEvent(SouthboundInterfaceEvent event) {
        try {
            // Interface oldIntf = ovsdbConfigService.getTypedRow(node,
            // Interface.class, oldRow);
            // newRow.getTableSchema().getName().equals(ovsdbConfigService.getTableName(node,
            // Interface.class))
            // tableName.equalsIgnoreCase(ovsdbConfigService.getTableName(node,
            // Interface.class))
            // Interface intf = this.ovsdbConfigService.getTypedRow(node,
            // Interface.class, row);
            String tableName = ovsdbConfigService.getTableName(event.getNode(),
                    Interface.class);
            if (event.getTableName().equalsIgnoreCase(tableName)) {

                Interface intf = this.ovsdbConfigService.getTypedRow(
                        event.getNode(), Interface.class, event.getRow());
                if (intf.getTypeColumn()
                        .getData()
                        .equalsIgnoreCase(
                                org.opendaylight.controller.samples.differentiatedforwarding.Constants.NETWORK_TYPE_GRE)
                        || intf.getTypeColumn()
                                .getData()
                                .equalsIgnoreCase(
                                        org.opendaylight.controller.samples.differentiatedforwarding.Constants.NETWORK_TYPE_VXLAN)) {

                    if (event.getAction() == Action.UPDATE) {
                        Interface oldIntf = this.ovsdbConfigService
                                .getTypedRow(event.getNode(), Interface.class,
                                        event.getOldRow());
                        if (oldIntf.getName() == null
                                && oldIntf.getExternalIdsColumn() == null
                                && oldIntf.getMacColumn() == null
                                && oldIntf.getOpenFlowPortColumn() == null
                                && oldIntf.getOptionsColumn() == null
                                && oldIntf.getOtherConfigColumn() == null
                                && oldIntf.getTypeColumn() == null) {
                            // Huh, when its just stat update, all other params
                            // are null
                        } else {
                            ovsdbTunnelInterfaceEvents.put(event);
                            log.trace(
                                    "enqueueTunnelInterfaceEvent: put event {}",
                                    event);
                        }
                    } else {
                        ovsdbTunnelInterfaceEvents.put(event);
                        log.trace("enqueueTunnelInterfaceEvent: put event {}",
                                event);
                    }
                } else {
                    // log.trace("enqueueTunnelInterfaceEvent: received an interface event which is not for a tunnel {} ignoring",
                    // event);
                }

            } else {
                // log.trace("enqueueTunnelInterfaceEvent: received an unrelated event {} ignoring",
                // event);
            }
        } catch (InterruptedException e) {
            log.error(
                    "enqueueEvent: Thread was interrupted while trying to enqueue event ",
                    e);
        }
    }

    private void startTunnelEventHandler() {
        log.info("startTunnelEventHandler: is started");
        tunnelEventHandler.submit(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    SouthboundInterfaceEvent ev;
                    try {
                        ev = ovsdbTunnelInterfaceEvents.take();
                        log.trace("startTunnelEventHandler: Queue size {}",
                                ovsdbTunnelInterfaceEvents.size());
                        log.trace("startTunnelEventHandler: ");
                        log.trace("startTunnelEventHandler: take event {}", ev);
                        updateTunnelsFromInterfaceEvent(ev.getNode(),
                                ev.getUuid(), ev.getRow(), ev.getOldRow(),
                                ev.getContext(), ev.getAction());
                        log.trace(
                                "startTunnelEventHandler: processed event {}",
                                ev);
                    } catch (InterruptedException e) {
                        log.info(
                                "The event handler thread was interrupted, shutting down",
                                e);
                        return;
                    }
                }
            }

        });
    }

    private void bulkLoadTunnelsEvents() {
        log.debug("bulkLoadTunnelsEvents: start");
        IConnectionServiceInternal connectionService = (IConnectionServiceInternal) ServiceHelper
                .getGlobalInstance(IConnectionServiceInternal.class, this);
        OvsdbConfigService ovsdbConfigService = (OvsdbConfigService) ServiceHelper
                .getGlobalInstance(OvsdbConfigService.class, this);
        List<Node> nodes = connectionService.getNodes();
        if (nodes == null)
            return;
        for (Node node : nodes) {
            try {
                Map<String, Row> rows = ovsdbConfigService.getRows(node,
                        ovsdbConfigService.getTableName(node, Interface.class));
                if (rows == null)
                    continue;
                for (String uuid : rows.keySet()) {
                    rowAdded(node, ovsdbConfigService.getTableName(node,
                            Interface.class), uuid, rows.get(uuid));
                }
            } catch (Exception e) {
                log.error("bulkLoadTunnelsEvents: Exception during bulk load ",
                        e);
            }
        }
    }

    @Override
    public HashMap<String, Set<TunnelEndPoint>> getTunnelEndPoints() {
        return tunnelEndPointsMap;
    }

    /**
     * NOTE: This is specifically designed for the cases where "key=flow",
     * otherwise getTepIp and getTepNc should be rewritten.
     */
    @Override
    public void loadTunnelEndPoints() {
        log.debug("loadTunnelEndPoints");
        INeutronNetworkCRUD neutronNetworkService = (INeutronNetworkCRUD) ServiceHelper
                .getGlobalInstance(INeutronNetworkCRUD.class, this);
        if (neutronNetworkService == null) {
            log.error("loadTunnelEndPoints: INeutronNetworkCRUD is not available");
            return;
        }
        List<NeutronNetwork> neutronNetworks = neutronNetworkService
                .getAllNetworks();
        log.debug("loadTunnelEndPoints: neutronNetworks {}", neutronNetworks);
        if (neutronNetworks == null)
            return;
        for (NeutronNetwork neutronNetwork : neutronNetworks) {
            String segmentationId = neutronNetwork.getProviderSegmentationID();
            if (segmentationId == null)
                continue;

            Set<TunnelEndPoint> teps = tunnelEndPointsMap.get(segmentationId);
            if (teps == null) {
                teps = new HashSet<TunnelEndPoint>();
                tunnelEndPointsMap.put(segmentationId, teps);
            }

            List<NeutronPort> neutronPorts = neutronNetwork.getPortsOnNetwork();
            log.debug(
                    "loadTunnelEndPoints: segmentationId {}, neutronPorts {}",
                    segmentationId, neutronPorts);
            if (neutronPorts == null)
                continue;

            List<Node> ovsdbNodes = findOvsdbNodes(neutronPorts);
            log.debug("loadTunnelEndPoints: segmentationId {}, ovsdbNodes {}",
                    segmentationId, ovsdbNodes);
            if (ovsdbNodes == null)
                continue;

            List<TunnelEndPoint> extractedTeps = extractTunnelEndPoints(
                    ovsdbNodes, segmentationId);
            log.debug(
                    "loadTunnelEndPoints: segmentationId {}, extractedTeps {}",
                    segmentationId, extractedTeps);
            if (extractedTeps == null)
                continue;
            teps.addAll(extractedTeps);

            tunnelEndPointsMap.put(segmentationId, teps);
        }
        removeTunnelEdgesUsingTeps();
        log.debug("loadTunnelEndPoints: tunnelEndPointsMap {}",
                tunnelEndPointsMap);
    }

    @Override
    public void loadTunnelEndPoints(String segmentationId) {
        log.debug("loadTunnelEndPoints");
        INeutronNetworkCRUD neutronNetworkService = (INeutronNetworkCRUD) ServiceHelper
                .getGlobalInstance(INeutronNetworkCRUD.class, this);
        if (neutronNetworkService == null) {
            log.error("loadTunnelEndPoints: INeutronNetworkCRUD is not available");
            return;
        }
        List<NeutronNetwork> neutronNetworks = neutronNetworkService
                .getAllNetworks();
        log.debug("loadTunnelEndPoints: neutronNetworks {}", neutronNetworks);
        if (neutronNetworks == null)
            return;
        for (NeutronNetwork neutronNetwork : neutronNetworks) {
            String nnSegmentationId = neutronNetwork.getProviderSegmentationID();
            if (segmentationId != nnSegmentationId)
                continue;

            Set<TunnelEndPoint> teps = tunnelEndPointsMap.get(segmentationId);
            if (teps == null) {
                teps = new HashSet<TunnelEndPoint>();
                tunnelEndPointsMap.put(segmentationId, teps);
            }

            List<NeutronPort> neutronPorts = neutronNetwork.getPortsOnNetwork();
            log.debug(
                    "loadTunnelEndPoints: segmentationId {}, neutronPorts {}",
                    segmentationId, neutronPorts);
            if (neutronPorts == null)
                continue;

            List<Node> ovsdbNodes = findOvsdbNodes(neutronPorts);
            log.debug("loadTunnelEndPoints: segmentationId {}, ovsdbNodes {}",
                    segmentationId, ovsdbNodes);
            if (ovsdbNodes == null)
                continue;

            List<TunnelEndPoint> extractedTeps = extractTunnelEndPoints(
                    ovsdbNodes, segmentationId);
            log.debug(
                    "loadTunnelEndPoints: segmentationId {}, extractedTeps {}",
                    segmentationId, extractedTeps);
            if (extractedTeps == null)
                continue;
            teps.addAll(extractedTeps);

            tunnelEndPointsMap.put(segmentationId, teps);
        }
        removeTunnelEdgesUsingTeps();
        log.debug("loadTunnelEndPoints: tunnelEndPointsMap {}",
                tunnelEndPointsMap);
    }

    private List<TunnelEndPoint> extractTunnelEndPoints(List<Node> ovsdbNodes,
            String segmentationId) {
        List<TunnelEndPoint> teps = new ArrayList<TunnelEndPoint>();
        for (Node ovsNode : ovsdbNodes) {
//            InetAddress tepIp = getTepIp(ovsNode);
//            List<NodeConnector> tepNcs = getTepNcs(ovsNode);

//            if (tepIp == null || tepNcs == null) {
//                log.error(
//                        "loadTunnels: cannot determine TEP NCs {} or IP {}. skipping to next OVSDB Node",
//                        tepNcs, tepIp);
//                continue;
//            }
//            for (NodeConnector tepNc : tepNcs) {
//                // FIXME
//                TunnelEndPoint tep = new TunnelEndPoint(tepNc, tepIp, null,
//                        segmentationId);
//                if (!teps.contains(tep))
//                    teps.add(tep);
//            }
            List<TunnelEndPoint> nodeTeps = getTeps(ovsNode, segmentationId);
            if (nodeTeps == null){
                log.error("extractTunnelEndPoints: can't find TEPs for ovsNode {}", ovsNode);
                continue;
            }
            teps.addAll(nodeTeps);
        }
        return teps;
    }

    private InetAddress getTepIp(Node ovsNode) {
        Map<String, Row> rows = ovsdbConfigService.getRows(ovsNode,
                ovsdbConfigService.getTableName(ovsNode, OpenVSwitch.class));
        if (rows == null)
            return null;
        OpenVSwitch openVSwitch = ovsdbConfigService.getTypedRow(ovsNode,
                OpenVSwitch.class, rows.values().iterator().next());
        Map<String, String> otherConfigs = openVSwitch.getOtherConfigColumn()
                .getData();
        if (otherConfigs == null) {
            log.error(
                    "getTepIp: ovsNode {} has no otherConfigs in Open_vSwitch table",
                    ovsNode);
            return null;
        }
        String localIp = otherConfigs.get("local_ip");
        if (localIp == null) {
            log.error("getTepIp: ovsNode {} has no TEP IP configuration",
                    ovsNode);
            return null;
        }
        InetAddress address = null;
        try {
            address = InetAddress.getByName(localIp);
        } catch (UnknownHostException e) {
            log.error("getTepIp: ", e);
        }
        return address;
    }

    private List<NodeConnector> getTepNcs(Node ovsNode) {
        List<NodeConnector> tepNcs = new ArrayList<NodeConnector>();
        Map<String, Row> rows = ovsdbConfigService.getRows(ovsNode,
                ovsdbConfigService.getTableName(ovsNode, Interface.class));
        if (rows == null)
            return null;
        for (String rowUuid : rows.keySet()) {
            NodeConnector tepNc = null;
            Interface intf = ovsdbConfigService.getTypedRow(ovsNode,
                    Interface.class, rows.get(rowUuid));
            if (intf != null
                    && intf.getTypeColumn().getData() != null
                    && (intf.getTypeColumn()
                            .getData()
                            .equalsIgnoreCase(
                                    org.opendaylight.controller.samples.differentiatedforwarding.Constants.NETWORK_TYPE_GRE) || intf
                            .getTypeColumn()
                            .getData()
                            .equalsIgnoreCase(
                                    org.opendaylight.controller.samples.differentiatedforwarding.Constants.NETWORK_TYPE_VXLAN))) {
                // Found TEP Interface
                Node ofNode = getOpenFlowNode(ovsNode, rowUuid, intf);

                if (intf.getOpenFlowPortColumn().getData() == null
                        || intf.getOpenFlowPortColumn().getData().size() == 0) {
                    log.error("getTepNc: interface OFPort is not present.");
                    continue;
                }
                // FIXME: This can be terribly bad, NodeConnector only accepts
                // Short values, while OVSDB stores them in Long
                Short ofPortShort = new Short(
                        ((Long) intf.getOpenFlowPortColumn().getData()
                                .toArray()[0]).shortValue());
                if (ofPortShort <= 0) {
                    log.error(
                            "getTepNc: received OpenFlowPort {} is not valid. srcNodeConnctor is not available. Continueing",
                            ofPortShort);
                    continue;
                }
                tepNc = NodeConnector.fromStringNoNode(
                        NodeConnector.NodeConnectorIDType.OPENFLOW,
                        ofPortShort.toString(), ofNode);
                tepNcs.add(tepNc);
            }
        }

        if (tepNcs.size() == 0){
            log.error("getTepNc: can not find TEP NodeConnector(s) for ovsNode {}",
                    ovsNode);
            return null;
        } else {
            return tepNcs;
        }
    }

    /**
     * All tunnel ports will be used by resident tenants. Retrieving all tunnel ports and creating a TEPs set should be fine.
     * @param ovsNode
     * @param segmentationId
     * @return All TEPs on this OVS node, which can be used by the resident tenant with segmentation Id <code>segmentationId</code>
     */
    private List<TunnelEndPoint> getTeps(Node ovsNode, String segmentationId) {
        List<TunnelEndPoint> teps = new ArrayList<TunnelEndPoint>();

        Map<String, Row> rows = ovsdbConfigService.getRows(ovsNode, ovsdbConfigService.getTableName(ovsNode, Interface.class));
        if (rows == null)
            return null;
        for (String rowUuid : rows.keySet()) {
            NodeConnector tepNc = null;
            Interface intf = ovsdbConfigService.getTypedRow(ovsNode, Interface.class, rows.get(rowUuid));
            if (intf != null
                    && intf.getTypeColumn().getData() != null
                    && (intf.getTypeColumn()
                            .getData()
                            .equalsIgnoreCase(
                                    org.opendaylight.controller.samples.differentiatedforwarding.Constants.NETWORK_TYPE_GRE) || intf
                            .getTypeColumn()
                            .getData()
                            .equalsIgnoreCase(
                                    org.opendaylight.controller.samples.differentiatedforwarding.Constants.NETWORK_TYPE_VXLAN))) {
                // Found TEP Interface
                Node ofNode = getOpenFlowNode(ovsNode, rowUuid, intf);

                // Find OF_Port number
                if (intf.getOpenFlowPortColumn().getData() == null
                        || intf.getOpenFlowPortColumn().getData().size() == 0) {
                    log.error("getTeps: interface OFPort is not present.");
                    continue;
                }
                // FIXME: This can be terribly bad, NodeConnector only accepts
                // Short values, while OVSDB stores them in Long
                Short ofPortShort = new Short(
                        ((Long) intf.getOpenFlowPortColumn().getData()
                                .toArray()[0]).shortValue());
                if (ofPortShort <= 0) {
                    log.error(
                            "getTepNc: received OpenFlowPort {} is not valid. srcNodeConnctor is not available. Continueing",
                            ofPortShort);
                    continue;
                }

                if (intf.getOptionsColumn().getData() == null || intf.getOptionsColumn().getData().size() == 0){
                    log.error("getTeps: interface doesn't have required Options.");
                    continue;
                }
                String localIp = intf.getOptionsColumn().getData().get("local_ip");
                String remoteIp = intf.getOptionsColumn().getData().get("remote_ip");
                if (localIp == null || remoteIp == null){
                    log.error("getTeps: interface doesn't have local_ip {} or remote_ip {}.", localIp, remoteIp);
                    continue;
                }

                InetAddress localAddress = null, remoteAddress = null;
                try {
                    localAddress = InetAddress.getByName(localIp);
                    remoteAddress = InetAddress.getByName(remoteIp);
                } catch (UnknownHostException e) {
                    log.error("Can't create local {} or remote {} addresses", localIp, remoteIp, e);
                    continue;
                }


                tepNc = NodeConnector.fromStringNoNode(
                        NodeConnector.NodeConnectorIDType.OPENFLOW,
                        ofPortShort.toString(), ofNode);

                TunnelEndPoint tep = new TunnelEndPoint(tepNc, localAddress, remoteAddress, segmentationId);
                teps.add(tep);
            }
        }

        if (teps.size() == 0){
            log.error("getTeps: can not find TEP(s) for ovsNode {}", ovsNode);
            return null;
        } else {
            return teps;
        }
    }

    private List<Node> findOvsdbNodes(List<NeutronPort> neutronPorts) {
        List<Node> ovsdbNodes = new ArrayList<Node>();
        IConnectionServiceInternal connectionService = (IConnectionServiceInternal) ServiceHelper
                .getGlobalInstance(IConnectionServiceInternal.class, this);
        List<Node> nodes = connectionService.getNodes();
        boolean nodeAdded = false;
        for (Node node : nodes) {
            if (node.getType().equalsIgnoreCase("OVS")) {
                nodeAdded = false;
                Map<String, Row> rows = ovsdbConfigService.getRows(node,
                        ovsdbConfigService.getTableName(node, Interface.class));
                if (rows == null)
                    continue;
                for (String rowUuid : rows.keySet()) {
                    Interface intf = ovsdbConfigService.getTypedRow(node,
                            Interface.class, rows.get(rowUuid));
                    for (NeutronPort neutronPort : neutronPorts) {
                        if (neutronPort
                                .getPortUUID()
                                .equalsIgnoreCase(
                                        intf.getExternalIdsColumn()
                                                .getData()
                                                .get(Constants.EXTERNAL_ID_INTERFACE_ID))) {
                            if (!ovsdbNodes.contains(node)) {
                                ovsdbNodes.add(node);
                                nodeAdded = true;
                                break;
                            }
                        }
                    }
                    if (nodeAdded)
                        break;
                }
            }
        }
        return ovsdbNodes;
    }

    private synchronized void updateTunnelsFromInterfaceEvent(Node node,
            String uuid, Row intfRow, Row oldIntfRow, Object context,
            Action action) {
        log.trace(
                "updateTunnelsFromInterfaceEvent: node:{} uuid:{} intf:{} oldIntf:{} context:{} action:{}",
                node, uuid, intfRow, oldIntfRow, context, action);
        final Interface intf = this.ovsdbConfigService.getTypedRow(node,
                Interface.class, intfRow);
        final Interface oldIntf = this.ovsdbConfigService.getTypedRow(node,
                Interface.class, oldIntfRow);

        log.debug("updateTunnelsFromInterfaceEvent: intf {} ", intf.getName());
        String remoteIP = intf.getOptionsColumn().getData().get("remote_ip");
        String localIP = intf.getOptionsColumn().getData().get("local_ip");
        String flowKey = intf.getOptionsColumn().getData().get("key");
        String segmentationID = null;
        Tunnel tunnel;
        if (context != null && context instanceof NeutronNetwork) {
            segmentationID = ((NeutronNetwork) context)
                    .getProviderSegmentationID();
        }
        log.debug(
                "updateTunnelsFromInterfaceEvent: remoteIP:{} localIP:{} flowKey:{} segmentationID:{}",
                remoteIP, localIP, flowKey, segmentationID);
        if (segmentationID != null && flowKey != null
                && !flowKey.equalsIgnoreCase(segmentationID)) {
            log.error(
                    "updateTunnelsFromInterfaceEvent: flowKey {} and segmentationID {} are not equal",
                    flowKey, segmentationID);
        }
        if (segmentationID == null && flowKey == null) {
            log.error(
                    "updateTunnelsFromInterfaceEvent: flowKey {} and segmentationID {} are null. Cannot determine the tunnel properly",
                    flowKey, segmentationID);
            return;
        }

        List<Tunnel> tunnels = tunnelsMap.get(flowKey);
        if (tunnels == null) {
            tunnels = new ArrayList<Tunnel>();
            tunnelsMap.put(flowKey, tunnels);
        }

        switch (action) {
        case ADD:
            log.debug("updateTunnelsFromInterfaceEvent: intf {} ",
                    intf.getName());
            tunnel = createDummyTunnel(node, intf, uuid, localIP, remoteIP,
                    flowKey);
            if (tunnel == null)
                break;
            // Fill the dummy tunnel if its reverse exists
            for (Tunnel existingTunnel : tunnels) {
                if (tunnel.isReverse(existingTunnel)) {
                    log.debug(
                            "updateTunnelsFromInterfaceEvent: {} is reverse of {} updating both references",
                            tunnel, existingTunnel);
                    tunnel.fillFromReverse(existingTunnel);
                    existingTunnel.fillFromReverse(tunnel);
                    break;
                }
            }
            if (!tunnels.contains(tunnel)) {
                log.debug(
                        "updateTunnelsFromInterfaceEvent: a new tunnel is discovered (ADD ACTION): {}",
                        tunnel);
                tunnels.add(tunnel);
                removeFromKShortestRoutesTopology(tunnel);
            } else {
                log.debug(
                        "updateTunnelsFromInterfaceEvent: tunnel {} exists (ADD ACTION): {}",
                        tunnel);
            }
            break;

        case UPDATE:
            log.debug("updateTunnelsFromInterfaceEvent: intf {} ",
                    intf.getName());
            tunnel = createDummyTunnel(node, intf, uuid, localIP, remoteIP,
                    flowKey);
            if (tunnel == null)
                break;
            // Fill the dummy tunnel if its reverse exists
            for (Tunnel existingTunnel : tunnels) {
                if (tunnel.isReverse(existingTunnel)) {
                    log.debug(
                            "updateTunnelsFromInterfaceEvent: {} is reverse of {} updating both references",
                            tunnel, existingTunnel);
                    tunnel.fillFromReverse(existingTunnel);
                    existingTunnel.fillFromReverse(tunnel);
                    break;
                }
            }
            Tunnel oldTunnel = null;
            String oldRemoteIP;
            String oldLocalIP;
            String oldFlowKey = null;
            if (oldIntf.getOptionsColumn() != null) {
                oldRemoteIP = oldIntf.getOptionsColumn().getData()
                        .get("remote_ip");
                oldLocalIP = oldIntf.getOptionsColumn().getData()
                        .get("local_ip");
                oldFlowKey = oldIntf.getOptionsColumn().getData().get("key");
                oldTunnel = findTunnel(oldLocalIP, oldRemoteIP, oldFlowKey);
            }
            if (oldTunnel != null) {
                if (!oldFlowKey.equalsIgnoreCase(flowKey)) {
                    tunnelsMap.get(oldFlowKey).remove(oldTunnel);
                    // FIXME: find the reverse of oldTunnel, and set
                    // dstNodeConnector to null
                    // Tunnel oldReverseTunnel = findTunnel(oldRemote,
                    // IPoldLocalIP, oldFlowKey);
                    // oldReverseTunnel.setDstNodeConnector(null);
                    tunnelsMap.get(flowKey).add(tunnel);
                    removeFromKShortestRoutesTopology(tunnel);
                    log.debug(
                            "updateTunnelsFromInterfaceEvent: the old tunnel {} has changed its TunnelKey new tunnel {}. Removed from {} list, added to {} (UPDATE ACTION)",
                            oldTunnel, tunnel, oldFlowKey, flowKey);
                } else {
                    oldTunnel.setDstAddress(tunnel.getDstAddress());
                    oldTunnel.setDstNodeConnector(tunnel.getDstNodeConnector());
                    oldTunnel.setSrcAddress(tunnel.getSrcAddress());
                    oldTunnel.setSrcNodeConnector(tunnel.getSrcNodeConnector());
                    removeFromKShortestRoutesTopology(oldTunnel);
                    log.debug(
                            "updateTunnelsFromInterfaceEvent: updating tunnel from {} to {} (UPDATE ACTION)",
                            oldTunnel, tunnel);
                }
            } else if (!tunnels.contains(tunnel)) {
                log.debug(
                        "updateTunnelsFromInterfaceEvent: a new tunnel is discovered (UPDATE ACTION): {}",
                        tunnel);
                tunnels.add(tunnel);
                removeFromKShortestRoutesTopology(tunnel);
            }
            break;

        case DELETE:
            Tunnel tobeDeletedTunnel = findTunnel(localIP, remoteIP, flowKey);
            boolean deleted = false;
            if (tobeDeletedTunnel != null) {
                deleted = tunnelsMap.get(flowKey).remove(tobeDeletedTunnel);
            }
            if (deleted) {
                // FIXME: find the reverse of oldTunnel, and set
                // dstNodeConnector to null
                // Tunnel oldReverseTunnel = findTunnel(oldRemote, IPoldLocalIP,
                // oldFlowKey);
                // oldReverseTunnel.setDstNodeConnector(null);
                log.debug(
                        "updateTunnelsFromInterfaceEvent: tunnel is removed (DELETE ACTION): tunnel {}",
                        tobeDeletedTunnel);
            } else {
                log.error(
                        "updateTunnelsFromInterfaceEvent: tunnel is NOT removed (DELETE ACTION): tunnel {}",
                        tobeDeletedTunnel);
            }

            break;
        }
        log.debug("updateTunnelsFromInterfaceEvent: current tunnels: {}",
                tunnelsMap);
    }

    private Tunnel createDummyTunnel(Node node, Interface intf, String uuid,
            String localIP, String remoteIP, String flowKey) {
        try {
            log.debug(
                    "createDummyTunnel: node {} intf {} UUID {} localIP {} remoteIP {} flowKey {}",
                    node, intf.getName(), uuid, localIP, remoteIP, flowKey);
            if (intf.getOpenFlowPortColumn().getData() == null
                    || intf.getOpenFlowPortColumn().getData().size() == 0) {
                log.trace("createDummyTunnel: interface OFPort is not present.");
                return null;
            }
            // FIXME: This can be terribly bad, NodeConnector only accepts Short
            // values, while OVSDB stores them in Long
            Short ofPortShort = new Short(((Long) intf.getOpenFlowPortColumn()
                    .getData().toArray()[0]).shortValue());
            if (ofPortShort <= 0) {
                log.trace(
                        "createDummyTunnel: received OpenFlowPort {} is not valid. srcNodeConnctor is not available. Returning",
                        ofPortShort);
                return null;
            }
            Node ofNode = getOpenFlowNode(node, uuid, intf);
            log.debug(
                    "createDummyTunnel: creating srcNodeConnector from: {}, {}, {}",
                    NodeConnector.NodeConnectorIDType.OPENFLOW,
                    Short.parseShort(ofPortShort.toString()), ofNode);
            NodeConnector srcNodeConnector = NodeConnector.fromStringNoNode(
                    NodeConnector.NodeConnectorIDType.OPENFLOW,
                    ofPortShort.toString(), ofNode);
            InetAddress srcAddress = null;
            InetAddress dstAddress = null;
            try {
                if (remoteIP != null && remoteIP.length() != 0) {
                    dstAddress = InetAddress.getByName(remoteIP);
                }
                if (localIP != null && localIP.length() != 0) {
                    srcAddress = InetAddress.getByName(localIP);
                }
            } catch (UnknownHostException e) {
                log.error(
                        "createDummyTunnel: exception in parsing local/remote IP addresses. Returning",
                        e);
                return null;
            }

            Tunnel tunnel = new Tunnel(srcNodeConnector, null, srcAddress,
                    dstAddress, flowKey);
            if (srcAddress == null || dstAddress == null
                    || srcNodeConnector == null) {
                log.error(
                        "createDummyTunnel: srcNodeConnector {}, or srcAddress {}, or dstAddress {} are null in tunnel {}. Can not properly determine the tunnel. Returning",
                        srcNodeConnector, srcAddress, dstAddress, tunnel);
                return null;
            }
            log.trace("createDummyTunnel: returning tunnel {}", tunnel);
            return tunnel;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    private Node getOpenFlowNode(Node ovsNode, String intfUuid, Interface intf) {
        log.trace("getOpenFlowNode: node {} intfUuid {}, intf {}", ovsNode,
                intfUuid, intf.getName());
        Node ofNode = null;

        OvsdbConfigService ovsdbTable = (OvsdbConfigService) ServiceHelper
                .getGlobalInstance(OvsdbConfigService.class, this);
        Map<String, Row> bridges = null;
        Map<String, Row> ports = null;
        try {
            bridges = ovsdbTable.getRows(ovsNode,
                    ovsdbTable.getTableName(ovsNode, Bridge.class));
            ports = ovsdbTable.getRows(ovsNode,
                    ovsdbTable.getTableName(ovsNode, Port.class));
        } catch (Exception e1) {
            log.error("getOpenFlowNode: exception ", e1);
        }
        if (bridges == null || ports == null) {
            log.debug(
                    "getOpenFlowNode: bridges {} or ports {} are null. Returning",
                    bridges, ports);
            return null;
        }
        log.trace("getOpenFlowNode: ports {}", ports);
        log.trace("getOpenFlowNode: bridges {}", bridges);

        for (String brUuid : bridges.keySet()) {
            Bridge bridge = ovsdbTable.getTypedRow(ovsNode, Bridge.class,
                    bridges.get(brUuid));

            for (UUID portUUID : bridge.getPortsColumn().getData()) {

                log.trace("getOpenFlowNode: portUuid {} portRow {}", portUUID,
                        ports.get(portUUID.toString()));
                log.trace("getOpenFlowNode: bridgeUuid {} bridgeRow {}",
                        brUuid, bridges.get(brUuid));
                Port port = ovsdbTable.getTypedRow(ovsNode, Port.class,
                        ports.get(portUUID.toString()));
                if (port == null || port.getInterfacesColumn() == null
                        || port.getInterfacesColumn().getData() == null) {
                    log.trace("getOpenFlowNode: port is not complete");
                    continue;
                }
                if (port.getInterfacesColumn().getData()
                        .contains(new UUID(intfUuid))) {
                    log.debug(
                            "getOpenFlowNode: OpenFlow Bridge is found: bridge {}, port {}, interface {}",
                            bridge.getUuid(), port.getName(), intf.getName());
                    Set<String> dpids = bridge.getDatapathIdColumn().getData();
                    if (dpids == null || dpids.size() == 0)
                        return null;
                    Long dpidLong = Long.valueOf(HexEncode
                            .stringToLong((String) dpids.toArray()[0]));
                    // BigInteger dpid =
                    // OpenFlowUtils.getDpId((String)dpids.toArray()[0]);

                    try {
                        ofNode = new Node(Node.NodeIDType.OPENFLOW, dpidLong);
                        log.trace(
                                "getOpenFlowNode: found ofNode {} for intfUuid {}, intf {}",
                                ofNode, intfUuid, intf.getName());
                        return ofNode;
                    } catch (ConstructionException e) {
                        log.error("getOpenFlowNode: exception ", e);
                        return ofNode;
                    }
                }
            }
        }
        log.error(
                "getOpenFlowNode: can not find OpenFlow node for ovsNode {} intfUuid {} intf {}",
                ovsNode, intfUuid, intf);
        return null;
    }

    private Tunnel findTunnel(String localIP, String remoteIP, String tunnelKey) {

        InetAddress srcAddress = null;
        InetAddress dstAddress = null;
        try {
            if (remoteIP != null && remoteIP.length() != 0) {
                dstAddress = InetAddress.getByName(remoteIP);
            }
            if (localIP != null && localIP.length() != 0) {
                srcAddress = InetAddress.getByName(localIP);
            }
        } catch (UnknownHostException e) {
            log.error(
                    "findTunnel: exception in parsing local/remote IP addresses. Returning",
                    e);
            return null;
        }

        List<Tunnel> tunnels = tunnelsMap.get(tunnelKey);
        for (Tunnel tunnel : tunnels) {
            if (tunnel.getSrcAddress().equals(srcAddress)
                    && tunnel.getDstAddress().equals(dstAddress)) {
                return tunnel;
            }
        }
        return null;
    }

    /**
     * Build Tunnels from TEPs, and invoke removeFromKShortestRoutesTopology
     */
    private void removeTunnelEdgesUsingTeps() {
        List<Tunnel> tunnels = createTunnels(tunnelEndPointsMap);
        for (Tunnel tunnel : tunnels) {
            // TODO: This will remove each edge twice, unless removeFromKSRT
            // accepts a boolean ignoreReverseAsWell=false.
            // Generally this should be harmless. However, better to optimize it
            // later
            removeFromKShortestRoutesTopology(tunnel);
        }
    }

    private List<Tunnel> createTunnels(
            HashMap<String, Set<TunnelEndPoint>> tepMap) {
        List<Tunnel> tunnels = new ArrayList<>();
        for (String key : tepMap.keySet()) {
            Set<TunnelEndPoint> teps = tepMap.get(key);

            try {
                tunnels.addAll(Tunnel.createTunnels(teps));
            } catch (Exception e1) {
                log.error(
                        "Can't create Tunnels from TEPs {}",
                        teps, e1);
            }
        }
        return tunnels;
    }

    private void removeFromKShortestRoutesTopology(final Tunnel tunnel) {
        log.debug("removeFromKShortestRoutesTopology: tunnel {}", tunnel);
        if (tunnel.getSrcNodeConnector() == null
                || tunnel.getDstNodeConnector() == null) {
            log.trace(
                    "removeFromKShortestRoutesTopology: src or dst is null, returning {}",
                    tunnel);
            return;
        }
        Edge edge;
        try {
            edge = new Edge(tunnel.getSrcNodeConnector(),
                    tunnel.getDstNodeConnector());
            shortestRoutes.ignoreEdge(edge, true);
        } catch (ConstructionException e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Applies the filter to the topology. e.g. Tunnels which are used in the
     * infrastructure, should be visible in the topology and used for finding
     * shortest paths. While tenant tunnels and other overlay tunnels should be
     * avoided. Different schemes can be used, such as infrastructure tunnel
     * key=[0-100], tenant tunnel key=[5000-6000] We start by a simple case, if
     * tunnel doesn't have a key that's infrastructure tunnel, otherwise tenant
     * tunnel.
     */
    public boolean shouldUseInTopology(Edge edge) {
        log.debug("shouldUseInTopology: edge {}", edge);
        NodeConnector srcNc = edge.getTailNodeConnector();
        NodeConnector dstNc = edge.getHeadNodeConnector();

        return true;
    }

    @Override
    public HashMap<String, List<Tunnel>> getTunnelsMap() {
        return tunnelsMap;
    }

    void setClusterContainerService(IClusterContainerServices s) {
        log.debug("Cluster Service set");
        this.clusterContainerService = s;
    }

    void unsetClusterContainerService(IClusterContainerServices s) {
        if (this.clusterContainerService == s) {
            log.debug("Cluster Service removed!");
            this.clusterContainerService = null;
        }
    }

    public OvsdbConfigService getOVSDBConfigService() {
        return ovsdbConfigService;
    }

    public void unsetOVSDBConfigService(OvsdbConfigService s) {
        if (s == this.ovsdbConfigService) {
            this.ovsdbConfigService = null;
        }
    }

    public void setOVSDBConfigService(OvsdbConfigService s) {
        log.debug("setOVSDBConfigService: {}", s);
        this.ovsdbConfigService = s;
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

}
