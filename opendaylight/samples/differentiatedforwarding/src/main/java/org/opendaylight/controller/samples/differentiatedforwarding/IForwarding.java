package org.opendaylight.controller.samples.differentiatedforwarding;

import java.util.List;

import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;

public interface IForwarding {

//    public ConcurrentMap<HostNodePair, HashMap<NodeConnector, FlowEntry>> getRulesDB();
    public Node getMdNode(String nodeDpId);
    public Long getExternalInterfaceOfPort(Node ofNode);
    public List<Long> getTenantLocalInterfaces(Node ofNode, String segmentationId);
    public void programTunnelForwarding(Tunnel tunnel, int classNum, boolean write);
}
