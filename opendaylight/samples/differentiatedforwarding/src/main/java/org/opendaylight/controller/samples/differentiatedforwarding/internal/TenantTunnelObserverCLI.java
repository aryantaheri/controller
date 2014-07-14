package org.opendaylight.controller.samples.differentiatedforwarding.internal;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import org.apache.felix.service.command.Descriptor;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.samples.differentiatedforwarding.ITunnelObserver;
import org.opendaylight.controller.samples.differentiatedforwarding.Tunnel;
import org.osgi.framework.ServiceRegistration;

public class TenantTunnelObserverCLI {
    @SuppressWarnings("rawtypes")
    private ServiceRegistration sr = null;

    public void init() {
    }

    public void destroy() {
    }

    public void start() {
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("osgi.command.scope", "odpcontroller");
        props.put("osgi.command.function", new String[] { "getTunnelsMap" });
        this.sr = ServiceHelper.registerGlobalServiceWReg(TenantTunnelObserverCLI.class, this, props);
    }

    public void stop() {
        if (this.sr != null) {
            this.sr.unregister();
            this.sr = null;
        }
    }

    @Descriptor("Retrieves Tunnels Map. This returns just those tunnels with a tunnel key (e.g. Tenants Tunnels)")
    public void getTunnelsMap(
            @Descriptor("Container on the context of which the routing service need to be looked up") String containerName) {
        System.out.println("TenantTunnelObserver.getTunnelsMap");

        ITunnelObserver tunnelObserver = (ITunnelObserver) ServiceHelper.getInstance(ITunnelObserver.class, containerName, this);
        if(tunnelObserver == null){
            System.err.println("TenantTunnelObserverCLI - getTunnelsMap: TenantTunnelObserver is not available");
            return;
        }
        HashMap<String, List<Tunnel>> tunnelsMap = tunnelObserver.getTunnelsMap();
        System.out.println(tunnelsMap);
    }

}
