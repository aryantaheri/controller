package org.opendaylight.controller.routing.yenkshortestpaths.internal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import mulavito.algorithms.shortestpath.ksp.Yen;

import org.apache.commons.collections15.Transformer;
import org.opendaylight.controller.clustering.services.IClusterContainerServices;
import org.opendaylight.controller.sal.core.Bandwidth;
import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.Edge;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.Path;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.UpdateType;
import org.opendaylight.controller.sal.routing.IListenRoutingUpdates;
import org.opendaylight.controller.sal.topology.TopoEdgeUpdate;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.controller.topologymanager.ITopologyManager;
import org.opendaylight.controller.topologymanager.ITopologyManagerClusterWideAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.SparseMultigraph;
import edu.uci.ics.jung.graph.util.EdgeType;

/**
 * Currently this implementation doesn't support parallel edges between two
 * endpoints, since there is no way to distinguish them from each other. There
 * can be a property specifying some unique characteristics of the edge (ID,
 * Name, etc.) If this limitation is rectified and proper hashCode for Edge is
 * implemented, then ykspTopology can be of type
 * DirectedOrderedSparseMultigraph. For now we stick to DirectedSparseGraph.
 *
 * @author aryan
 *
 */
@SuppressWarnings("rawtypes")
public class YKShortestPaths implements ITopologyManagerClusterWideAware, IKShortestRoutes {
    private static Logger log = LoggerFactory.getLogger(YKShortestPaths.class);

    private ConcurrentMap<Short, Graph<Node, Edge>> topologyBWAware;
    private ConcurrentMap<Short, DijkstraShortestPath<Node, Edge>> sptBWAware;
    DijkstraShortestPath<Node, Edge> mtp; // Max Throughput Path

    private Graph<Node, WeightedEdge> ykspTopology;
    private Yen<Node, WeightedEdge> yenKShortestPathsAlgo;
    private static final long DEFAULT_LINK_SPEED = Bandwidth.BW1Gbps;

    private Set<IListenRoutingUpdates> routingAware;
    private ISwitchManager switchManager;
    private ITopologyManager topologyManager;
    private IClusterContainerServices clusterContainerService;

    @Override
    public synchronized List<Path> getKShortestRoutes(final Node src, final Node dst,
            final int k) {
        log.debug("getkShortestRoutes: src {} dst {} k {}", src, dst, k);
        List<Path> result = new LinkedList<>();
        try {

            if ((src == null) || (dst == null)) {
                log.error("getkShortestRoutes: missing endpoints src {} dst {} k {}", src, dst, k);
                return null;
            }

            List<List<WeightedEdge>> shortestPaths = yenKShortestPathsAlgo.getShortestPaths(src, dst, k);

            log.debug("getkShortestRoutes: src {} dst {} k {} shortestPaths {}", src, dst, k, shortestPaths);
            for (List<WeightedEdge> weightedPath : shortestPaths) {
                Path path;
                try {
                    path = new Path(new ArrayList<Edge>(weightedPath));
                    result.add(path);
                } catch (ConstructionException e) {
                    log.error("getkShortestRoutes: src {} dst {} k {} exception {}", src, dst, k, e.getStackTrace());
                    return null;
                }
            }
        } catch (Exception e) {
            log.error("getKShortestPaths: unknown src {} dst {} k {} exception:{}", src, dst, k, e.getStackTrace());
        }

        return result;

    }

    @Override
    public synchronized void clear() {
        yenKShortestPathsAlgo.resetDijkstraShortestPath();
    }

    /**
     *
     * @param edge
     * @param weight
     *            : aka cost can be 1 or REFERENCE_BW/EDGE_BW. One of these two
     *            should consistently be chosen. Additional consideration should
     *            be taken for missing EDGE_BW to avoid DBZ
     * @param type
     * @return
     */
    private synchronized boolean updateTopo(Edge edge, double weight,
            UpdateType type) {

        if (ykspTopology == null) {
            this.ykspTopology = new DirectedSparseGraph<Node, WeightedEdge>();
            this.yenKShortestPathsAlgo = new Yen<Node, WeightedEdge>(
                    ykspTopology, weightTransformer);
        }

        if (ykspTopology != null && yenKShortestPathsAlgo == null) {
            this.yenKShortestPathsAlgo = new Yen<Node, WeightedEdge>(
                    ykspTopology, weightTransformer);
        }

        NodeConnector src = edge.getTailNodeConnector();
        NodeConnector dst = edge.getHeadNodeConnector();
        WeightedEdge weightedEdge = null;
        try {
            weightedEdge = new WeightedEdge(src, dst, weight);
        } catch (ConstructionException e) {
            log.error(
                    "updateTopo exception for edge {} weight {} type {} exception {} ",
                    edge, weight, type, e.getStackTrace());
        }
        boolean vertexPresentInGraph = false;
        boolean edgePresentInGraph = false;
        boolean topologyChanged = false;

        switch (type) {
        case ADDED:
            log.debug("updateTopo ADDED: edge={} weight={} updateType={}",
                    edge, weight, type);
            // Add/Check vertex presence
            vertexPresentInGraph = ykspTopology.addVertex(src.getNode());
            log.debug("updateTopo ADDED: vertex={} vertexPresentInGraph={}",
                    src, vertexPresentInGraph);
            vertexPresentInGraph = ykspTopology.addVertex(dst.getNode());
            log.debug("updateTopo ADDED: vertex={} vertexPresentInGraph={}",
                    dst, vertexPresentInGraph);

            // Add/Check edge presence
            edgePresentInGraph = ykspTopology.containsEdge(weightedEdge);
            if (!edgePresentInGraph) {
                topologyChanged = ykspTopology.addEdge(weightedEdge,
                        src.getNode(), dst.getNode(), EdgeType.DIRECTED);
            } else {
                log.error(
                        "updateTopo ADDED: WeightedEdge {} already exists but we received an ADDED event. Topology not changed. Skipping",
                        weightedEdge);
                topologyChanged = false;
            }
            break;

        case CHANGED:
            log.debug("updateTopo CHANGED: edge={} weight={} updateType={}",
                    edge, weight, type);
            WeightedEdge existingEdge = ykspTopology.findEdge(src.getNode(),
                    dst.getNode());
            if (existingEdge == null) {
                log.error(
                        "updateTopo CHANGED: There is no edge between src {} and dst {}. The non-existing edge can not be changed. The new edge will be added",
                        src, dst);
            }
            topologyChanged = ykspTopology.addEdge(weightedEdge, src.getNode(),
                    dst.getNode(), EdgeType.DIRECTED);
            break;

        case REMOVED:
            log.debug("updateTopo REMOVED: edge={} weight={} updateType={}",
                    edge, weight, type);
            topologyChanged = ykspTopology.removeEdge(weightedEdge);

            // Remove vertices without any edges
            if (ykspTopology.containsVertex(src.getNode())
                    && (ykspTopology.inDegree(src.getNode()) == 0)
                    && (ykspTopology.outDegree(src.getNode()) == 0)) {
                log.debug("Removing vertex {}", src);
                ykspTopology.removeVertex(src.getNode());
            }

            if (ykspTopology.containsVertex(dst.getNode())
                    && (ykspTopology.inDegree(dst.getNode()) == 0)
                    && (ykspTopology.outDegree(dst.getNode()) == 0)) {
                log.debug("Removing vertex {}", dst);
                ykspTopology.removeVertex(dst.getNode());
            }
            break;
        }

        yenKShortestPathsAlgo.resetDijkstraShortestPath();
        // or better yenKShortestPathsAlgo.resetDijkstraShortestPath(src);
        // yenKShortestPathsAlgo.resetDijkstraShortestPath(dst);

        // FIXME: handle MaxThroughput
        // if (bw.equals(baseBW)) {
        // clearMaxThroughput();
        // }
        return topologyChanged;
    }

    private boolean edgeUpdate(final Edge e, final UpdateType type, final Set<Property> props,
            final boolean local) {
        String srcType = null;
        String dstType = null;

        log.debug("Got an edgeUpdate: {} props: {} update type: {} local: {}",
                e, props, type, local);

        if ((e == null) || (type == null)) {
            log.error("Edge or Update type are null!");
            return false;
        } else {
            srcType = e.getTailNodeConnector().getType();
            dstType = e.getHeadNodeConnector().getType();

            // What is type PRODUCTION?
            if (srcType.equals(NodeConnector.NodeConnectorIDType.PRODUCTION)) {
                log.debug("Skip updates for {}", e);
                return false;
            }

            if (dstType.equals(NodeConnector.NodeConnectorIDType.PRODUCTION)) {
                log.debug("Skip updates for {}", e);
                return false;
            }
        }

        long bw = 0;
        double weight = 1;

        if (props != null) {
            for (Property prop : props) {
                if (prop.getName().equals(Bandwidth.BandwidthPropName)) {
                    bw = ((Bandwidth) prop).getValue();
                    // FIXME: for now ignoring bw completely
                    // if (bw != 0) weight = DEFAULT_LINK_SPEED / bw;
                }
            }
        }

        boolean topologyChanged = updateTopo(e, weight, type);
        return topologyChanged;
    }

    @Override
    public void edgeUpdate(final List<TopoEdgeUpdate> topoedgeupdateList) {
        log.debug("Start of a Bulk EdgeUpdate with "
                + topoedgeupdateList.size() + " elements");
        boolean callListeners = false;
        boolean topologyChanged = false;
        for (int i = 0; i < topoedgeupdateList.size(); i++) {
            Edge e = topoedgeupdateList.get(i).getEdge();
            Set<Property> p = topoedgeupdateList.get(i).getProperty();
            UpdateType type = topoedgeupdateList.get(i).getUpdateType();
            boolean isLocal = topoedgeupdateList.get(i).isLocal();
            topologyChanged = edgeUpdate(e, type, p, isLocal);
            if (topologyChanged && (!callListeners)) {
                callListeners = true;
            }
        }

        // The routing listeners should only be called on the coordinator, to
        // avoid multiple controller cluster nodes to actually do the
        // recalculation when only one need to react
        boolean amICoordinator = true;
        if (this.clusterContainerService != null) {
            amICoordinator = this.clusterContainerService.amICoordinator();
        }
        if ((callListeners) && (this.routingAware != null) && amICoordinator) {
            log.trace("Calling the routing listeners");
            for (IListenRoutingUpdates ra : this.routingAware) {
                try {
                    ra.recalculateDone();
                } catch (Exception ex) {
                    log.error("Exception on routingAware listener call", ex);
                }
            }
        }
        log.trace("End of a Bulk EdgeUpdate");
    }

    /**
     * Function called by the dependency manager when all the required
     * dependencies are satisfied
     *
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void init() {
        log.debug("Yen's K Shortest Paths init() is called");
        this.topologyBWAware = new ConcurrentHashMap<Short, Graph<Node, Edge>>();
        this.sptBWAware = new ConcurrentHashMap<Short, DijkstraShortestPath<Node, Edge>>();
        // Now create the default topology, which doesn't consider the
        // BW, also create the corresponding Dijkstra calculation
        Graph<Node, Edge> g = new SparseMultigraph();
        Short sZero = Short.valueOf((short) 0);
        this.topologyBWAware.put(sZero, g);
        this.sptBWAware.put(sZero, new DijkstraShortestPath(g));
        // Topologies for other BW will be added on a needed base

        this.ykspTopology = new DirectedSparseGraph<Node, WeightedEdge>();
        this.yenKShortestPathsAlgo = new Yen<Node, WeightedEdge>(ykspTopology,
                weightTransformer);
    }

    /**
     * Function called by the dependency manager when at least one dependency
     * become unsatisfied or when the component is shutting down because for
     * example bundle is being stopped.
     *
     */
    void destroy() {
        log.info("Yen's K Shortest Paths destroy() is called. Reason: Stopped or unmet dependencies");
    }

    /**
     * Function called by dependency manager after "init ()" is called and after
     * the services provided by the class are registered in the service registry
     *
     */
    void start() {
        log.info("Yen's K Shortest Paths start() is called");
        // build the routing database from the topology if it exists.
        Map<Edge, Set<Property>> edges = topologyManager.getEdges();
        if (edges.isEmpty()) {
            return;
        }
        List<TopoEdgeUpdate> topoedgeupdateList = new ArrayList<TopoEdgeUpdate>();
        log.debug("Creating YKSP routing database from the topology");
        for (Iterator<Map.Entry<Edge, Set<Property>>> i = edges.entrySet()
                .iterator(); i.hasNext();) {
            Map.Entry<Edge, Set<Property>> entry = i.next();
            Edge e = entry.getKey();
            Set<Property> props = entry.getValue();
            TopoEdgeUpdate topoedgeupdate = new TopoEdgeUpdate(e, props,
                    UpdateType.ADDED);
            topoedgeupdateList.add(topoedgeupdate);
        }
        edgeUpdate(topoedgeupdateList);
    }

    /**
     * Function called by the dependency manager before the services exported by
     * the component are unregistered, this will be followed by a "destroy ()"
     * calls
     *
     */
    public void stop() {
        log.debug("Yen's K Shortest Paths stop() is called");
    }

    public void setSwitchManager(ISwitchManager switchManager) {
        this.switchManager = switchManager;
    }

    public void unsetSwitchManager(ISwitchManager switchManager) {
        if (this.switchManager == switchManager) {
            this.switchManager = null;
        }
    }

    public void setTopologyManager(ITopologyManager tm) {
        this.topologyManager = tm;
    }

    public void unsetTopologyManager(ITopologyManager tm) {
        if (this.topologyManager == tm) {
            this.topologyManager = null;
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

    public void setListenRoutingUpdates(final IListenRoutingUpdates i) {
        if (this.routingAware == null) {
            this.routingAware = new HashSet<IListenRoutingUpdates>();
        }
        if (this.routingAware != null) {
            log.debug("Adding routingAware listener: {}", i);
            this.routingAware.add(i);
        }
    }

    public void unsetListenRoutingUpdates(final IListenRoutingUpdates i) {
        if (this.routingAware == null) {
            return;
        }
        log.debug("Removing routingAware listener");
        this.routingAware.remove(i);
        if (this.routingAware.isEmpty()) {
            // We don't have any listener lets dereference
            this.routingAware = null;
        }
    }

    @Override
    public void edgeOverUtilized(Edge edge) {
        // TODO Auto-generated method stub

    }

    @Override
    public void edgeUtilBackToNormal(Edge edge) {
        // TODO Auto-generated method stub

    }

    private class WeightedEdge extends Edge {

        private static final long serialVersionUID = -1775245183703241873L;
        private double weight = 1;

        public WeightedEdge(Edge src, double weight)
                throws ConstructionException {
            super(src);
            this.setWeight(weight);
        }

        public WeightedEdge(NodeConnector tailNodeConnector,
                NodeConnector headNodeConnector, double weight)
                throws ConstructionException {
            super(tailNodeConnector, headNodeConnector);
            this.setWeight(weight);
        }

        public double getWeight() {
            return weight;
        }

        private void setWeight(double weight) {
            this.weight = weight;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = super.hashCode();
            // Casting here can be dangerous
            // FIXME: two edges with the same weight between similar endpoints
            // will result in the same hashCode and this is wrong
            result = (int) (prime * result + weight);

            return result;

        }

        @Override
        public String toString() {
            return "(" + this.getTailNodeConnector() + "-" + weight + "->"
                    + this.getHeadNodeConnector() + ")";
        }
    }

    static Transformer<WeightedEdge, Number> weightTransformer = new Transformer<WeightedEdge, Number>() {
        @Override
        public Number transform(WeightedEdge edge) {
            return edge.getWeight();
        }
    };


}
