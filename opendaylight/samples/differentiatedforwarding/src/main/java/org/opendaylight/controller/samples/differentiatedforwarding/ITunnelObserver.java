package org.opendaylight.controller.samples.differentiatedforwarding;

import java.util.HashMap;
import java.util.List;

public interface ITunnelObserver {
    public HashMap<String,List<Tunnel>> getTunnelsMap();
    public void loadTunnelEndPoints();
}
