package org.opendaylight.controller.samples.differentiatedforwarding;

import java.util.HashMap;
import java.util.concurrent.ConcurrentMap;

import org.opendaylight.controller.forwardingrulesmanager.FlowEntry;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.samples.simpleforwarding.HostNodePair;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;

public interface IForwarding {

    public ConcurrentMap<HostNodePair, HashMap<NodeConnector, FlowEntry>> getRulesDB();
    public Node getMdNode(String nodeName);
    public void programTunnelForwarding(Tunnel tunnel, int classNum, boolean write);
}
