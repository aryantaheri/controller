package org.opendaylight.controller.samples.differentiatedforwarding;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

public interface ITunnelObserver {
    public HashMap<String,List<Tunnel>> getTunnelsMap();
    public HashMap<String,Set<TunnelEndPoint>> getTunnelEndPoints();
    public void loadTunnelEndPoints();
}
