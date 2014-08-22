package org.opendaylight.controller.samples.differentiatedforwarding.internal;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import org.opendaylight.controller.clustering.services.IClusterContainerServices;
import org.opendaylight.controller.networkconfig.neutron.NeutronNetwork;
import org.opendaylight.controller.routing.yenkshortestpaths.internal.IKShortestRoutes;
import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.Edge;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.samples.differentiatedforwarding.ITunnelObserver;
import org.opendaylight.controller.samples.differentiatedforwarding.OpenFlowUtils;
import org.opendaylight.controller.samples.differentiatedforwarding.Tunnel;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.neutron.NetworkHandler;
import org.opendaylight.ovsdb.neutron.SouthboundEvent;
import org.opendaylight.ovsdb.neutron.SouthboundEvent.Action;
import org.opendaylight.ovsdb.plugin.IConnectionServiceInternal;
import org.opendaylight.ovsdb.plugin.OvsdbConfigService;
import org.opendaylight.ovsdb.plugin.OvsdbInventoryListener;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.schema.openvswitch.Interface;
import org.opendaylight.ovsdb.schema.openvswitch.Port;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TenantTunnelObserver implements OvsdbInventoryListener, ITunnelObserver {
    private static Logger log = LoggerFactory.getLogger(TenantTunnelObserver.class);
    private static short DEFAULT_IPSWITCH_PRIORITY = 1;
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
    //TODO: Use clusterContainerService.createCache for creating the object and storage
    private HashMap<String, List<Tunnel>> tunnelsMap;


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
        //FIXME: This is wrong
        OvsdbConfigService ovsdbConfigService = (OvsdbConfigService)ServiceHelper.getGlobalInstance(OvsdbConfigService.class, this);
        setOVSDBConfigService(ovsdbConfigService);
        IKShortestRoutes shortestRoutes = (IKShortestRoutes)ServiceHelper.getGlobalInstance(IKShortestRoutes.class, this);
        setKShortestRoutes(shortestRoutes);
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
        startTunnelEventHandler();
        bulkLoadTunnelsEvents();
    }

    /**
     * Function called by the dependency manager before the services
     * exported by the component are unregistered, this will be
     * followed by a "destroy ()" calls
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
        this.enqueueTunnelInterfaceEvent(new SouthboundInterfaceEvent(node, tableName, uuid, row, SouthboundEvent.Action.ADD));
    }

    @Override
    public void rowUpdated(Node node, String tableName, String uuid, Row old, Row row) {
        SouthboundInterfaceEvent event = new SouthboundInterfaceEvent(node, tableName, uuid, row, SouthboundEvent.Action.UPDATE);
        event.setOldRow(old);
        this.enqueueTunnelInterfaceEvent(event);
    }

    @Override
    public void rowRemoved(Node node, String tableName, String uuid, Row row,
            Object context) {
        this.enqueueTunnelInterfaceEvent(new SouthboundInterfaceEvent(node, tableName, uuid, row, context, SouthboundEvent.Action.DELETE));
    }
    /**
     * Enqueue only Tunnel Interface Events
     * @param event
     */
    private void enqueueTunnelInterfaceEvent (SouthboundInterfaceEvent event) {
        try {
            //Interface oldIntf = ovsdbConfigService.getTypedRow(node, Interface.class, oldRow);
            //newRow.getTableSchema().getName().equals(ovsdbConfigService.getTableName(node, Interface.class))
            //tableName.equalsIgnoreCase(ovsdbConfigService.getTableName(node, Interface.class))
            //Interface intf = this.ovsdbConfigService.getTypedRow(node, Interface.class, row);
            String tableName = ovsdbConfigService.getTableName(event.getNode(), Interface.class);
            if (event.getTableName().equalsIgnoreCase(tableName)){

                Interface intf = this.ovsdbConfigService.getTypedRow(event.getNode(), Interface.class, event.getRow());
                if (intf.getTypeColumn().getData().equalsIgnoreCase(NetworkHandler.NETWORK_TYPE_GRE) ||
                    intf.getTypeColumn().getData().equalsIgnoreCase(NetworkHandler.NETWORK_TYPE_VXLAN)){

                    if (event.getAction() == Action.UPDATE){
                        Interface oldIntf = this.ovsdbConfigService.getTypedRow(event.getNode(), Interface.class, event.getOldRow());
                        if (oldIntf.getName() == null && oldIntf.getExternalIdsColumn() == null && oldIntf.getMacColumn() == null &&
                                oldIntf.getOpenFlowPortColumn() == null && oldIntf.getOptionsColumn() == null && oldIntf.getOtherConfigColumn() == null &&
                                oldIntf.getTypeColumn() == null){
                            // Huh, when its just stat update, all other params are null
                        } else {
                            ovsdbTunnelInterfaceEvents.put(event);
                            log.trace("enqueueTunnelInterfaceEvent: put event {}", event);
                        }
                    } else {
                        ovsdbTunnelInterfaceEvents.put(event);
                        log.trace("enqueueTunnelInterfaceEvent: put event {}", event);
                    }
                } else{
//                    log.trace("enqueueTunnelInterfaceEvent: received an interface event which is not for a tunnel {} ignoring", event);
                }

            } else {
//                log.trace("enqueueTunnelInterfaceEvent: received an unrelated event {} ignoring", event);
            }
        } catch (InterruptedException e) {
            log.error("enqueueEvent: Thread was interrupted while trying to enqueue event ", e);
        }
    }

    private void startTunnelEventHandler(){
        log.info("startTunnelEventHandler: is started");
        tunnelEventHandler.submit(new Runnable()  {
            @Override
            public void run() {
                while (true) {
                    SouthboundInterfaceEvent ev;
                    try {
                        ev = ovsdbTunnelInterfaceEvents.take();
                        log.trace("startTunnelEventHandler: Queue size {}", ovsdbTunnelInterfaceEvents.size());
                        log.trace("startTunnelEventHandler: ");
                        log.trace("startTunnelEventHandler: take event {}", ev);
                        updateTunnelsFromInterfaceEvent(ev.getNode(), ev.getUuid(), ev.getRow(), ev.getOldRow(),
                                ev.getContext(),ev.getAction());
                        log.trace("startTunnelEventHandler: processed event {}", ev);
                    } catch (InterruptedException e) {
                        log.info("The event handler thread was interrupted, shutting down", e);
                        return;
                    }
                }
            }

        });
    }

    private void bulkLoadTunnelsEvents() {
        log.debug("bulkLoadTunnelsEvents: start");
        IConnectionServiceInternal connectionService = (IConnectionServiceInternal)ServiceHelper.getGlobalInstance(IConnectionServiceInternal.class, this);
        OvsdbConfigService ovsdbConfigService = (OvsdbConfigService)ServiceHelper.getGlobalInstance(OvsdbConfigService.class, this);
        List<Node> nodes = connectionService.getNodes();
        if (nodes == null) return;
        /**
         *             OvsdbConfigService ovsdbTable = (OvsdbConfigService) ServiceHelper.getGlobalInstance(OvsdbConfigService.class, this);
            Map<String, Row> bridgeTable = ovsdbTable.getRows(node, ovsdbTable.getTableName(node, Bridge.class));
            if (bridgeTable != null) {
                for (String key : bridgeTable.keySet()) {
                    Bridge bridge = ovsdbTable.getTypedRow(node, Bridge.class, bridgeTable.get(key));
                    if (bridge.getName().equals(bridgeName)) {
                        return bridge;
                    }
                }
            }
         */
        for (Node node : nodes) {
            try {
                Map<String, Row> rows = ovsdbConfigService.getRows(node, ovsdbConfigService.getTableName(node, Interface.class));
                if (rows == null) continue;
                for (String uuid : rows.keySet()) {
//                    Interface row = ovsdbConfigService.getTypedRow(node, Interface.class, rows.get(uuid));
                    rowAdded(node, ovsdbConfigService.getTableName(node, Interface.class), uuid, rows.get(uuid));
                }
            } catch (Exception e) {
                log.error("bulkLoadTunnelsEvents: Exception during bulk load ", e);
            }
        }
    }

    private synchronized void updateTunnelsFromInterfaceEvent(Node node, String uuid, Row intfRow, Row oldIntfRow, Object context, Action action) {
        log.trace("updateTunnelsFromInterfaceEvent: node:{} uuid:{} intf:{} oldIntf:{} context:{} action:{}", node, uuid, intfRow, oldIntfRow, context, action);
        final Interface intf = this.ovsdbConfigService.getTypedRow(node, Interface.class, intfRow);
        final Interface oldIntf = this.ovsdbConfigService.getTypedRow(node, Interface.class, oldIntfRow);

        log.debug("updateTunnelsFromInterfaceEvent: intf {} ", intf.getName());
        String remoteIP = intf.getOptionsColumn().getData().get("remote_ip");
        String localIP = intf.getOptionsColumn().getData().get("local_ip");
        String flowKey = intf.getOptionsColumn().getData().get("key");
        String segmentationID = null;
        Tunnel tunnel;
        if (context != null && context instanceof NeutronNetwork){
            segmentationID = ((NeutronNetwork) context).getProviderSegmentationID();
        }
        log.debug("updateTunnelsFromInterfaceEvent: remoteIP:{} localIP:{} flowKey:{} segmentationID:{}", remoteIP, localIP, flowKey, segmentationID);
        if(segmentationID != null && flowKey != null && !flowKey.equalsIgnoreCase(segmentationID)){
            log.error("updateTunnelsFromInterfaceEvent: flowKey {} and segmentationID {} are not equal", flowKey, segmentationID);
        }
        if(segmentationID == null && flowKey == null){
            log.error("updateTunnelsFromInterfaceEvent: flowKey {} and segmentationID {} are null. Cannot determine the tunnel properly", flowKey, segmentationID);
            return;
        }


        List<Tunnel> tunnels = tunnelsMap.get(flowKey);
        if (tunnels == null){
            tunnels = new ArrayList<Tunnel>();
            tunnelsMap.put(flowKey, tunnels);
        }



        switch (action) {
        case ADD:
            log.debug("updateTunnelsFromInterfaceEvent: intf {} ", intf.getName());
            tunnel = createDummyTunnel(node, intf, uuid, localIP, remoteIP, flowKey);
            if (tunnel == null) break;
            // Fill the dummy tunnel if its reverse exists
            for (Tunnel existingTunnel : tunnels) {
                if (tunnel.isReverse(existingTunnel)){
                    log.debug("updateTunnelsFromInterfaceEvent: {} is reverse of {} updating both references", tunnel, existingTunnel);
                    tunnel.fillFromReverse(existingTunnel);
                    existingTunnel.fillFromReverse(tunnel);
                    break;
                }
            }
            if (!tunnels.contains(tunnel)){
                log.debug("updateTunnelsFromInterfaceEvent: a new tunnel is discovered (ADD ACTION): {}", tunnel);
                tunnels.add(tunnel);
                removeFromKShortestRoutesTopology(tunnel);
            } else {
                log.debug("updateTunnelsFromInterfaceEvent: tunnel {} exists (ADD ACTION): {}", tunnel);
            }
            break;

        case UPDATE:
            log.debug("updateTunnelsFromInterfaceEvent: intf {} ", intf.getName());
            tunnel = createDummyTunnel(node, intf, uuid, localIP, remoteIP, flowKey);
            if (tunnel == null) break;
            // Fill the dummy tunnel if its reverse exists
            for (Tunnel existingTunnel : tunnels) {
                if (tunnel.isReverse(existingTunnel)){
                    log.debug("updateTunnelsFromInterfaceEvent: {} is reverse of {} updating both references", tunnel, existingTunnel);
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
                oldRemoteIP = oldIntf.getOptionsColumn().getData().get("remote_ip");
                oldLocalIP = oldIntf.getOptionsColumn().getData().get("local_ip");
                oldFlowKey = oldIntf.getOptionsColumn().getData().get("key");
                oldTunnel = findTunnel(oldLocalIP, oldRemoteIP, oldFlowKey);
            }
            if (oldTunnel != null){
                if (!oldFlowKey.equalsIgnoreCase(flowKey)){
                    tunnelsMap.get(oldFlowKey).remove(oldTunnel);
                    // FIXME: find the reverse of oldTunnel, and set dstNodeConnector to null
                    // Tunnel oldReverseTunnel = findTunnel(oldRemote, IPoldLocalIP, oldFlowKey);
                    // oldReverseTunnel.setDstNodeConnector(null);
                    tunnelsMap.get(flowKey).add(tunnel);
                    removeFromKShortestRoutesTopology(tunnel);
                    log.debug("updateTunnelsFromInterfaceEvent: the old tunnel {} has changed its TunnelKey new tunnel {}. Removed from {} list, added to {} (UPDATE ACTION)", oldTunnel, tunnel, oldFlowKey, flowKey);
                } else {
                    oldTunnel.setDstAddress(tunnel.getDstAddress());
                    oldTunnel.setDstNodeConnector(tunnel.getDstNodeConnector());
                    oldTunnel.setSrcAddress(tunnel.getSrcAddress());
                    oldTunnel.setSrcNodeConnector(tunnel.getSrcNodeConnector());
                    removeFromKShortestRoutesTopology(oldTunnel);
                    log.debug("updateTunnelsFromInterfaceEvent: updating tunnel from {} to {} (UPDATE ACTION)", oldTunnel, tunnel);
                }
            } else if (!tunnels.contains(tunnel)){
                log.debug("updateTunnelsFromInterfaceEvent: a new tunnel is discovered (UPDATE ACTION): {}", tunnel);
                tunnels.add(tunnel);
                removeFromKShortestRoutesTopology(tunnel);
            }
            break;

        case DELETE:
            Tunnel tobeDeletedTunnel = findTunnel(localIP, remoteIP, flowKey);
            boolean deleted = false;
            if (tobeDeletedTunnel != null){
                deleted = tunnelsMap.get(flowKey).remove(tobeDeletedTunnel);
            }
            if (deleted){
                // FIXME: find the reverse of oldTunnel, and set dstNodeConnector to null
                // Tunnel oldReverseTunnel = findTunnel(oldRemote, IPoldLocalIP, oldFlowKey);
                // oldReverseTunnel.setDstNodeConnector(null);
                log.debug("updateTunnelsFromInterfaceEvent: tunnel is removed (DELETE ACTION): tunnel {}", tobeDeletedTunnel);
            } else {
                log.error("updateTunnelsFromInterfaceEvent: tunnel is NOT removed (DELETE ACTION): tunnel {}", tobeDeletedTunnel);
            }

            break;
        }
        log.debug("updateTunnelsFromInterfaceEvent: current tunnels: {}", tunnelsMap);
    }

    private Tunnel createDummyTunnel(Node node, Interface intf, String uuid, String localIP, String remoteIP, String flowKey){
        try {
            log.debug("createDummyTunnel: node {} intf {} UUID {} localIP {} remoteIP {} flowKey {}", node, intf.getName(), uuid, localIP, remoteIP, flowKey);
            if (intf.getOpenFlowPortColumn().getData() == null || intf.getOpenFlowPortColumn().getData().size() == 0){
                log.trace("createDummyTunnel: interface OFPort is not present.");
                return null;
            }
            // FIXME: This can be terribly bad, NodeConnector only accepts Short values, while OVSDB stores them in Long
            Short ofPortShort = new Short(((Long)intf.getOpenFlowPortColumn().getData().toArray()[0]).shortValue());
            if(ofPortShort <= 0){
                log.trace("createDummyTunnel: received OpenFlowPort {} is not valid. srcNodeConnctor is not available. Returning", ofPortShort);
                return null;
            }
            Node ofNode = getOpenFlowNode(node, uuid, intf);
            log.debug("createDummyTunnel: creating srcNodeConnector from: {}, {}, {}", NodeConnector.NodeConnectorIDType.OPENFLOW, Short.parseShort(ofPortShort.toString()), ofNode);
            NodeConnector srcNodeConnector = NodeConnector.fromStringNoNode(NodeConnector.NodeConnectorIDType.OPENFLOW, ofPortShort.toString(), ofNode);
            InetAddress srcAddress = null;
            InetAddress dstAddress = null;
            try {
                if (remoteIP != null && remoteIP.length() != 0){
                    dstAddress = InetAddress.getByName(remoteIP);
                }
                if (localIP != null && localIP.length() != 0){
                    srcAddress = InetAddress.getByName(localIP);
                }
            } catch (UnknownHostException e) {
                log.error("createDummyTunnel: exception in parsing local/remote IP addresses. Returning", e);
                return null;
            }

            Tunnel tunnel = new Tunnel(srcNodeConnector, null, srcAddress, dstAddress, flowKey);
            if (srcAddress == null || dstAddress == null || srcNodeConnector == null){
                log.error("createDummyTunnel: srcNodeConnector {}, or srcAddress {}, or dstAddress {} are null in tunnel {}. Can not properly determine the tunnel. Returning", srcNodeConnector, srcAddress, dstAddress, tunnel);
                return null;
            }
            log.trace("createDummyTunnel: returning tunnel {}", tunnel);
            return tunnel;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }



    private Node getOpenFlowNode(Node ovsNode, String intfUuid, Interface intf){
        log.trace("getOpenFlowNode: node {} intfUuid {}, intf {}", ovsNode, intfUuid, intf.getName());
        Node ofNode = null;

        OvsdbConfigService ovsdbTable = (OvsdbConfigService)ServiceHelper.getGlobalInstance(OvsdbConfigService.class, this);
        Map<String, Row> bridges = null;
        Map<String, Row> ports = null;
        try {
            bridges = ovsdbTable.getRows(ovsNode, ovsdbTable.getTableName(ovsNode, Bridge.class));
            ports = ovsdbTable.getRows(ovsNode, ovsdbTable.getTableName(ovsNode, Port.class));
        } catch (Exception e1) {
            log.error("getOpenFlowNode: exception ", e1);
        }
        if (bridges == null || ports == null) {
            log.debug("getOpenFlowNode: bridges {} or ports {} are null. Returning", bridges, ports);
            return null;
        }
        log.trace("getOpenFlowNode: ports {}", ports);
        log.trace("getOpenFlowNode: bridges {}", bridges);

        for (String brUuid : bridges.keySet()) {
            Bridge bridge = ovsdbTable.getTypedRow(ovsNode, Bridge.class, bridges.get(brUuid));

            for (UUID portUUID : bridge.getPortsColumn().getData()) {

                log.trace("getOpenFlowNode: portUuid {} portRow {}", portUUID, ports.get(portUUID.toString()));
                log.trace("getOpenFlowNode: bridgeUuid {} bridgeRow {}", brUuid, bridges.get(brUuid));
                Port port = ovsdbTable.getTypedRow(ovsNode, Port.class, ports.get(portUUID.toString()));
                if (port == null || port.getInterfacesColumn() == null || port.getInterfacesColumn().getData() == null){
                    log.trace("getOpenFlowNode: port is not complete");
                    continue;
                }
                if (port.getInterfacesColumn().getData().contains(new UUID(intfUuid))){
                    log.debug("getOpenFlowNode: OpenFlow Bridge is found: bridge {}, port {}, interface {}", bridge.getUuid(), port.getName(), intf.getName());
                    Set<String> dpids = bridge.getDatapathIdColumn().getData();
                    if (dpids == null || dpids.size() ==  0) return null;
//                    Long dpidLong = Long.valueOf(HexEncode.stringToLong((String)dpids.toArray()[0]));
                    BigInteger dpid = OpenFlowUtils.getDpId((String)dpids.toArray()[0]);

                    try {
                        ofNode = new Node(Node.NodeIDType.OPENFLOW, dpid);
                        log.trace("getOpenFlowNode: found ofNode {} for intfUuid {}, intf {}", ofNode, intfUuid, intf.getName());
                        return ofNode;
                    } catch (ConstructionException e) {
                        log.error("getOpenFlowNode: exception ", e);
                        return ofNode;
                    }
                }
            }
        }
        log.error("getOpenFlowNode: can not find OpenFlow node for ovsNode {} intfUuid {} intf {}", ovsNode, intfUuid, intf);
        return null;
    }

    private Tunnel findTunnel(String localIP, String remoteIP, String tunnelKey) {

        InetAddress srcAddress = null;
        InetAddress dstAddress = null;
        try {
            if (remoteIP != null && remoteIP.length() != 0){
                dstAddress = InetAddress.getByName(remoteIP);
            }
            if (localIP != null && localIP.length() != 0){
                srcAddress = InetAddress.getByName(localIP);
            }
        } catch (UnknownHostException e) {
            log.error("findTunnel: exception in parsing local/remote IP addresses. Returning", e);
            return null;
        }

        List<Tunnel> tunnels = tunnelsMap.get(tunnelKey);
        for (Tunnel tunnel : tunnels) {
            if (tunnel.getSrcAddress().equals(srcAddress) && tunnel.getDstAddress().equals(dstAddress)){
                return tunnel;
            }
        }
        return null;
    }

    private void removeFromKShortestRoutesTopology(final Tunnel tunnel){
        log.debug("removeFromKShortestRoutesTopology: tunnel {}", tunnel);
        if (tunnel.getSrcNodeConnector() == null || tunnel.getDstNodeConnector() == null) {
            log.trace("removeFromKShortestRoutesTopology: src or dst is null, returning {}", tunnel);
            return;
        }
        Edge edge;
        try {
            edge = new Edge(tunnel.getSrcNodeConnector(), tunnel.getDstNodeConnector());
            shortestRoutes.ignoreEdge(edge, true);
        } catch (ConstructionException e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Applies the filter to the topology. e.g. Tunnels which are used in the infrastructure,
     *  should be visible in the topology and used for finding shortest paths. While tenant
     *  tunnels and other overlay tunnels should be avoided.
     * Different schemes can be used, such as infrastructure tunnel key=[0-100], tenant tunnel key=[5000-6000]
     * We start by a simple case, if tunnel doesn't have a key that's infrastructure tunnel, otherwise tenant tunnel.
     */
    public boolean shouldUseInTopology(Edge edge) {
        log.debug("shouldUseInTopology: edge {}", edge);
        NodeConnector srcNc = edge.getTailNodeConnector();
        NodeConnector dstNc = edge.getHeadNodeConnector();

        return true;
    }

    @Override
    public HashMap<String,List<Tunnel>> getTunnelsMap(){
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
