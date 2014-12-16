package org.opendaylight.controller.samples.differentiatedforwarding;

import java.util.List;

import org.opendaylight.controller.sal.core.Path;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.meters.MeterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;

public interface IForwarding {

//    public ConcurrentMap<HostNodePair, HashMap<NodeConnector, FlowEntry>> getRulesDB();
    public Node getMdNode(String nodeDpId);
    public Long getExternalInterfaceOfPort(Node ofNode);
    public List<Long> getTenantLocalInterfaces(Node ofNode, String segmentationId);
    public Path getProgrammedPath(Tunnel tunnel);
    public void programTunnelForwarding(Tunnel tunnel, int classNum, boolean write);
    public String reportNetwork(String segmentationId);
    public MeterBuilder prepareMeter(Node node, short dscp);
}
