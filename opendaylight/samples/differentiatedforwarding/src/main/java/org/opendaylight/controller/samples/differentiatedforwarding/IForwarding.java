package org.opendaylight.controller.samples.differentiatedforwarding;

import java.util.HashMap;
import java.util.concurrent.ConcurrentMap;

import org.opendaylight.controller.forwardingrulesmanager.FlowEntry;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.samples.simpleforwarding.HostNodePair;

public interface IForwarding {

    public ConcurrentMap<HostNodePair, HashMap<NodeConnector, FlowEntry>> getRulesDB();
    public void programTunnelForwarding(Tunnel tunnel, int classNum, boolean write);
}
