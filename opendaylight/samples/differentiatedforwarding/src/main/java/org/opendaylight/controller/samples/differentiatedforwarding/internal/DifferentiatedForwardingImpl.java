package org.opendaylight.controller.samples.differentiatedforwarding.internal;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.samples.differentiatedforwarding.HostNodePair;
import org.opendaylight.controller.samples.differentiatedforwarding.Tunnel;
import org.opendaylight.controller.switchmanager.IInventoryListener;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.controller.topologymanager.ITopologyManager;
import org.opendaylight.ovsdb.lib.table.Interface;
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
    private BlockingQueue<SouthboundEvent> ovsdbTunnelInterfaceEvents;
    private ExecutorService tunnelEventHandler;

    /**
     * Function called by the dependency manager when all the required
     * dependencies are satisfied
     *
     */
    void init() {
        log.debug("init()");
        ovsdbTunnelInterfaceEvents = new LinkedBlockingQueue<SouthboundEvent>();
        tunnelEventHandler = Executors.newSingleThreadExecutor();
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
        this.enqueueTunnelInterfaceEvent(new SouthboundEvent(node, tableName, uuid, row, SouthboundEvent.Action.ADD));
    }
    @Override
    public void rowUpdated(Node node, String tableName, String uuid, Table<?> old, Table<?> row) {
        this.enqueueTunnelInterfaceEvent(new SouthboundEvent(node, tableName, uuid, row, SouthboundEvent.Action.UPDATE));
    }
    @Override
    public void rowRemoved(Node node, String tableName, String uuid, Table<?> row, Object context) {
        this.enqueueTunnelInterfaceEvent(new SouthboundEvent(node, tableName, uuid, row, context, SouthboundEvent.Action.DELETE));
    }

    /**
     * Enqueue only Tunnel Interface Events
     * @param event
     */
    private void enqueueTunnelInterfaceEvent (SouthboundEvent event) {
        try {
            if (event.getTableName().equalsIgnoreCase(Interface.NAME.getName())){

                Interface intf = (Interface)event.getRow();
                if (intf.getType().equalsIgnoreCase(NetworkHandler.NETWORK_TYPE_GRE) ||
                    intf.getType().equalsIgnoreCase(NetworkHandler.NETWORK_TYPE_VXLAN)){

                    ovsdbTunnelInterfaceEvents.put(event);
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
                    SouthboundEvent ev;
                    try {
                        ev = ovsdbTunnelInterfaceEvents.take();
                    } catch (InterruptedException e) {
                        log.info("The event handler thread was interrupted, shutting down", e);
                        return;
                    }
                    discoverTunnelsFromInterfaceEvent(ev.getNode(), ev.getUuid(), (Interface)ev.getRow(),
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

    private void discoverTunnelsFromInterfaceEvent(Node node, String uuid, Interface intf, Object context, Action action) {
        // TODO Auto-generated method stub
        log.debug("discoverTunnelsFromInterfaceEvent: node:{} uuid:{} intf:{} context:{} action:{}", node, uuid, intf, context, action);
        String remoteIP = intf.getOptions().get("remote_ip");
        String localIP = intf.getOptions().get("local_ip");
        String flowKey = intf.getOptions().get("key");
        String segmentationID = null;
        if (context != null && context instanceof NeutronNetwork){
            segmentationID = ((NeutronNetwork) context).getProviderSegmentationID();
        }
        log.debug("discoverTunnelsFromInterfaceEvent: remoteIP:{} localIP:{} flowKey:{} segmentationID:{}", remoteIP, localIP, flowKey, segmentationID);
        if(segmentationID != null && flowKey != null && !flowKey.equalsIgnoreCase(segmentationID)){
            log.error("discoverTunnelsFromInterfaceEvent: flowKey {} and segmentationID {} are not equal", flowKey, segmentationID);
        }
        if(segmentationID == null && flowKey == null){
            log.error("discoverTunnelsFromInterfaceEvent: flowKey {} and segmentationID {} are null. Cannot determine the tunnel properly", flowKey, segmentationID);
            return;
        }


        // FIXME: This can be terribly bad, NodeConnector only accepts Short values, while OVSDB stores them in BigInteger
        Short ofPortShort = new Short(((BigInteger)intf.getOfport().toArray()[0]).shortValue());
        NodeConnector srcNodeConnector = NodeConnector.fromStringNoNode(NodeConnector.NodeConnectorIDType.OPENFLOW, ofPortShort.toString(), node);
        InetAddress srcAddress = null;
        InetAddress dstAddress = null;
        try {
            if (remoteIP != null){
                dstAddress = InetAddress.getByName(remoteIP);
            }
            if (localIP != null){
                srcAddress = InetAddress.getByName(localIP);
            }
        } catch (UnknownHostException e) {
            log.error("discoverTunnelsFromInterfaceEvent: exception ", e);
        }

        Tunnel tunnel = new Tunnel(srcNodeConnector, null, srcAddress, dstAddress, flowKey);
        HashMap<String, List<Tunnel>> tunnels = new HashMap<String, List<Tunnel>>();
//        List<Tunnel>
        for (Tunnel existingTunnel : tunnels.get(flowKey)) {
            if (tunnel.isReverse(existingTunnel)){

            }
        }


//                InetAddress.getByName(

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
