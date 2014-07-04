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
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import org.opendaylight.controller.clustering.services.IClusterContainerServices;
import org.opendaylight.controller.forwardingrulesmanager.FlowEntry;
import org.opendaylight.controller.forwardingrulesmanager.IForwardingRulesManager;
import org.opendaylight.controller.hosttracker.IfIptoHost;
import org.opendaylight.controller.hosttracker.IfNewHostNotify;
import org.opendaylight.controller.hosttracker.hostAware.HostNodeConnector;
import org.opendaylight.controller.networkconfig.neutron.NeutronNetwork;
import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
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
import org.opendaylight.controller.samples.differentiatedforwarding.HostNodePair;
import org.opendaylight.controller.samples.differentiatedforwarding.Tunnel;
import org.opendaylight.controller.switchmanager.IInventoryListener;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.controller.topologymanager.ITopologyManager;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.table.Bridge;
import org.opendaylight.ovsdb.lib.table.Interface;
import org.opendaylight.ovsdb.lib.table.Port;
import org.opendaylight.ovsdb.lib.table.internal.Table;
import org.opendaylight.ovsdb.neutron.NetworkHandler;
import org.opendaylight.ovsdb.neutron.SouthboundEvent;
import org.opendaylight.ovsdb.neutron.SouthboundEvent.Action;
import org.opendaylight.ovsdb.plugin.IConnectionServiceInternal;
import org.opendaylight.ovsdb.plugin.OVSDBConfigService;
import org.opendaylight.ovsdb.plugin.OVSDBInventoryListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DifferentiatedForwardingImpl implements OVSDBInventoryListener, IfNewHostNotify,
        IListenRoutingUpdates, IInventoryListener, IListenDataPacket {
    private static Logger log = LoggerFactory.getLogger(DifferentiatedForwardingImpl.class);
    private static short DEFAULT_IPSWITCH_PRIORITY = 1;
    static final String FORWARDING_RULES_CACHE_NAME = "forwarding.ipswitch.rules";
    private IfIptoHost hostTracker;
    private IForwardingRulesManager frm;
    private ITopologyManager topologyManager;
    private IRouting routing;

    /**
     * The set of all forwarding rules: (host) -> (switch -> flowmod). Note that
     * the host includes an attachment point and that while the switch appears
     * to be a switch's port, in actuality it is a special port which just
     * represents the switch.
     */
    private ConcurrentMap<HostNodePair, HashMap<NodeConnector, FlowEntry>> rulesDB;
    private final Map<Node, List<FlowEntry>> tobePrunedPos = new HashMap<Node, List<FlowEntry>>();
    private IClusterContainerServices clusterContainerService = null;
    private ISwitchManager switchManager;
    private IDataPacketService dataPacketService;

    /**
     * Tunnel discovery using OVSDB plugin
     */
    private BlockingQueue<SouthboundInterfaceEvent> ovsdbTunnelInterfaceEvents;
    private ExecutorService tunnelEventHandler;
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
    public PacketResult receiveDataPacket(RawPacket inPkt) {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public void notifyNode(Node node, UpdateType type,
            Map<String, Property> propMap) {
        // TODO Auto-generated method stub

    }
    @Override
    public void notifyNodeConnector(NodeConnector nodeConnector,
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
    @Override
    public void nodeAdded(Node node) {
        // TODO Auto-generated method stub

    }
    @Override
    public void nodeRemoved(Node node) {
        // TODO Auto-generated method stub

    }
    @Override
    public void rowAdded(Node node, String tableName, String uuid, Table<?> row) {
        this.enqueueTunnelInterfaceEvent(new SouthboundInterfaceEvent(node, tableName, uuid, row, SouthboundEvent.Action.ADD));
    }
    @Override
    public void rowUpdated(Node node, String tableName, String uuid, Table<?> old, Table<?> row) {
        SouthboundInterfaceEvent event = new SouthboundInterfaceEvent(node, tableName, uuid, row, SouthboundEvent.Action.UPDATE);
        event.setOldRow(old);
        this.enqueueTunnelInterfaceEvent(event);
    }
    @Override
    public void rowRemoved(Node node, String tableName, String uuid, Table<?> row, Object context) {
        this.enqueueTunnelInterfaceEvent(new SouthboundInterfaceEvent(node, tableName, uuid, row, context, SouthboundEvent.Action.DELETE));
    }

    /**
     * Enqueue only Tunnel Interface Events
     * @param event
     */
    private void enqueueTunnelInterfaceEvent (SouthboundInterfaceEvent event) {
        try {
            if (event.getTableName().equalsIgnoreCase(Interface.NAME.getName())){

                Interface intf = (Interface)event.getRow();
                if (intf.getType().equalsIgnoreCase(NetworkHandler.NETWORK_TYPE_GRE) ||
                    intf.getType().equalsIgnoreCase(NetworkHandler.NETWORK_TYPE_VXLAN)){

                    if (event.getAction() == Action.UPDATE){
                        Interface oldIntf = (Interface)event.getOldRow();
                        if (oldIntf.getName() == null && oldIntf.getExternal_ids() == null && oldIntf.getMac() == null &&
                                oldIntf.getOfport() == null && oldIntf.getOptions() == null && oldIntf.getOther_config() == null &&
                                oldIntf.getType() == null){
                            // Huh, when its just stat update, all other params are null
                        } else {
                            ovsdbTunnelInterfaceEvents.put(event);
                            log.debug("enqueueTunnelInterfaceEvent: put event {}", event);
                        }
                    } else {
                        ovsdbTunnelInterfaceEvents.put(event);
                        log.debug("enqueueTunnelInterfaceEvent: put event {}", event);
                    }
                } else{
                    log.trace("enqueueTunnelInterfaceEvent: received an interface event which is not for a tunnel {} ignoring", event);
                }

            } else {
                log.trace("enqueueTunnelInterfaceEvent: received an unrelated event {} ignoring", event);
            }
        } catch (InterruptedException e) {
            log.error("enqueueEvent: Thread was interrupted while trying to enqueue event ", e);
        }
    }

    private void startTunnelEventHandler(){
        tunnelEventHandler.submit(new Runnable()  {
            @Override
            public void run() {
                while (true) {
                    SouthboundInterfaceEvent ev;
                    try {
                        ev = ovsdbTunnelInterfaceEvents.take();
                    } catch (InterruptedException e) {
                        log.info("The event handler thread was interrupted, shutting down", e);
                        return;
                    }
                    updateTunnelsFromInterfaceEvent(ev.getNode(), ev.getUuid(), (Interface)ev.getRow(), (Interface)ev.getOldRow(),
                                             ev.getContext(),ev.getAction());
                }
            }

        });
    }

    private void bulkLoadTunnelsEvents() {
        log.debug("bulkLoadTunnelsEvents: start");
        IConnectionServiceInternal connectionService = (IConnectionServiceInternal)ServiceHelper.getGlobalInstance(IConnectionServiceInternal.class, this);
        OVSDBConfigService ovsdbConfigService = (OVSDBConfigService)ServiceHelper.getGlobalInstance(OVSDBConfigService.class, this);
        List<Node> nodes = connectionService.getNodes();
        if (nodes == null) return;
        for (Node node : nodes) {
            try {
                Map<String, Table<?>> rows = ovsdbConfigService.getRows(node, Interface.NAME.getName());
                if (rows == null) continue;
                for (String uuid : rows.keySet()) {
                    Table<?> row = rows.get(uuid);
                    rowAdded(node, Interface.NAME.getName(), uuid, row);
                }
            } catch (Exception e) {
                log.error("bulkLoadTunnelsEvents: Exception during bulk load ", e);
            }
        }
    }

    private void updateTunnelsFromInterfaceEvent(Node node, String uuid, Interface intf, Interface oldIntf, Object context, Action action) {
        log.debug("updateTunnelsFromInterfaceEvent: node:{} uuid:{} intf:{} oldIntf:{} context:{} action:{}", node, uuid, intf, oldIntf, context, action);
        String remoteIP = intf.getOptions().get("remote_ip");
        String localIP = intf.getOptions().get("local_ip");
        String flowKey = intf.getOptions().get("key");
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
            }
            break;

        case UPDATE:
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
            String oldRemoteIP = oldIntf.getOptions().get("remote_ip");
            String oldLocalIP = oldIntf.getOptions().get("local_ip");
            String oldFlowKey = oldIntf.getOptions().get("key");
            Tunnel oldTunnel = findTunnel(oldLocalIP, oldRemoteIP, oldFlowKey);
            if (oldTunnel != null){
                if (!oldFlowKey.equalsIgnoreCase(flowKey)){
                    tunnelsMap.get(oldFlowKey).remove(oldTunnel);
                    // FIXME: find the reverse of oldTunnel, and set dstNodeConnector to null
                    // Tunnel oldReverseTunnel = findTunnel(oldRemote, IPoldLocalIP, oldFlowKey);
                    // oldReverseTunnel.setDstNodeConnector(null);
                    tunnelsMap.get(flowKey).add(tunnel);
                    log.debug("updateTunnelsFromInterfaceEvent: the old tunnel {} has changed its TunnelKey new tunnel {}. Removed from {} list, added to {} (UPDATE ACTION)", oldTunnel, tunnel, oldFlowKey, flowKey);
                } else {
                    oldTunnel.setDstAddress(tunnel.getDstAddress());
                    oldTunnel.setDstNodeConnector(tunnel.getDstNodeConnector());
                    oldTunnel.setSrcAddress(tunnel.getSrcAddress());
                    oldTunnel.setSrcNodeConnector(tunnel.getSrcNodeConnector());
                    log.debug("updateTunnelsFromInterfaceEvent: updating tunnel from {} to {} (UPDATE ACTION)", oldTunnel, tunnel);
                }
            } else if (!tunnels.contains(tunnel)){
                log.debug("updateTunnelsFromInterfaceEvent: a new tunnel is discovered (UPDATE ACTION): {}", tunnel);
                tunnels.add(tunnel);
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
     // FIXME: This can be terribly bad, NodeConnector only accepts Short values, while OVSDB stores them in BigInteger
        Short ofPortShort = new Short(((BigInteger)intf.getOfport().toArray()[0]).shortValue());
        if(ofPortShort <= 0){
            log.debug("updateTunnelsFromInterfaceEvent: received OpenFlowPort {} is not valid. srcNodeConnctor is not available. Returning", ofPortShort);
            return null;
        }
        Node ofNode = getOpenFlowNode(node, uuid, intf);
        log.debug("updateTunnelsFromInterfaceEvent: creating srcNodeConnector from: {}, {}, {}", NodeConnector.NodeConnectorIDType.OPENFLOW, Short.parseShort(ofPortShort.toString()), ofNode);
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
            log.error("updateTunnelsFromInterfaceEvent: exception in parsing local/remote IP addresses. Returning", e);
            return null;
        }

        Tunnel tunnel = new Tunnel(srcNodeConnector, null, srcAddress, dstAddress, flowKey);
        if (srcAddress == null || dstAddress == null || srcNodeConnector == null){
            log.error("updateTunnelsFromInterfaceEvent: srcNodeConnector {}, or srcAddress {}, or dstAddress {} are null in tunnel {}. Can not properly determine the tunnel. Returning", srcNodeConnector, srcAddress, dstAddress, tunnel);
            return null;
        }

        return tunnel;
    }



    private Node getOpenFlowNode(Node ovsNode, String intfUuid, Interface intf){
        Node ofNode = null;
        OVSDBConfigService ovsdbTable = (OVSDBConfigService)ServiceHelper.getGlobalInstance(OVSDBConfigService.class, this);
        Map<String, Table<?>> bridges = null;
        Map<String, Table<?>> ports = null;
        try {
            bridges = ovsdbTable.getRows(ovsNode, Bridge.NAME.getName());
            ports = ovsdbTable.getRows(ovsNode, Port.NAME.getName());
        } catch (Exception e1) {
            log.error("getOpenFlowNode: exception ", e1);
        }
        if (bridges == null || ports == null) {
            log.debug("getOpenFlowNode: bridges {} or ports {} are null. Returning", bridges, ports);
            return null;
        }

        for (String brUuid : bridges.keySet()) {
            Bridge bridge = (Bridge) bridges.get(brUuid);
            for (UUID portUUID : bridge.getPorts()) {
                Port port = (Port) ports.get(portUUID.toString());
                if (port.getInterfaces().contains(new UUID(intfUuid))){
                    log.debug("getOpenFlowNode: OpenFlow Bridge is found: bridge {}, port {}, interface {}", bridge, port, intf);
                    Set<String> dpids = bridge.getDatapath_id();
                    if (dpids == null || dpids.size() ==  0) return null;
                    Long dpidLong = Long.valueOf(HexEncode.stringToLong((String)dpids.toArray()[0]));
                    try {
                        ofNode = new Node(Node.NodeIDType.OPENFLOW, dpidLong);
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
        log.debug("Setting routing");
        this.routing = routing;
    }

    public void unsetRouting(IRouting routing) {
        if (this.routing == routing) {
            this.routing = null;
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

    public void setForwardingRulesManager(IForwardingRulesManager forwardingRulesManager) {
        log.debug("Setting ForwardingRulesManager");
        this.frm = forwardingRulesManager;
    }

    public void unsetForwardingRulesManager(
            IForwardingRulesManager forwardingRulesManager) {
        if (this.frm == forwardingRulesManager) {
            this.frm = null;
        }
    }

    public void setHostTracker(IfIptoHost hostTracker) {
        log.debug("Setting HostTracker");
        this.hostTracker = hostTracker;
    }

    public void unsetHostTracker(IfIptoHost hostTracker) {
        if (this.hostTracker == hostTracker) {
            this.hostTracker = null;
        }
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

    public void setSwitchManager(ISwitchManager switchManager) {
        this.switchManager = switchManager;
    }

    public void unsetSwitchManager(ISwitchManager switchManager) {
        if (this.switchManager == switchManager) {
            this.switchManager = null;
        }
    }


}
