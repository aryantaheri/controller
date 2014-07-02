package org.opendaylight.controller.samples.differentiatedforwarding;

import java.io.Serializable;
import java.net.InetAddress;

import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.NodeConnector;

public class Tunnel implements Serializable {


    private static final long serialVersionUID = 1L;
    private NodeConnector srcNodeConnector = null;
    private NodeConnector dstNodeConnector = null;
    private InetAddress srcAddress;
    private InetAddress dstAddress;
    private String tunnelKey;



    public Tunnel(NodeConnector srcNodeConnector,
            NodeConnector dstNodeConnector, InetAddress srcAddress,
            InetAddress dstAddress, String tunnelKey) {
        this.srcNodeConnector = srcNodeConnector;
        this.dstNodeConnector = dstNodeConnector;
        this.srcAddress = srcAddress;
        this.dstAddress = dstAddress;
        this.tunnelKey = tunnelKey;
    }

    public NodeConnector getSrcNodeConnector() {
        return srcNodeConnector;
    }

    public void setSrcNodeConnector(NodeConnector srcNodeConnector) {
        this.srcNodeConnector = srcNodeConnector;
    }

    public NodeConnector getDstNodeConnector() {
        return dstNodeConnector;
    }

    public void setDstNodeConnector(NodeConnector dstNodeConnector) {
        this.dstNodeConnector = dstNodeConnector;
    }

    public InetAddress getSrcAddress() {
        return srcAddress;
    }

    public void setSrcAddress(InetAddress srcAddress) {
        this.srcAddress = srcAddress;
    }

    public InetAddress getDstAddress() {
        return dstAddress;
    }

    public void setDstAddress(InetAddress dstAddress) {
        this.dstAddress = dstAddress;
    }

    public String getTunnelKey() {
        return tunnelKey;
    }

    public void setTunnelKey(String tunnelKey) {
        this.tunnelKey = tunnelKey;
    }

    public boolean isReverse(Tunnel tunnel){
        if (this.srcNodeConnector != null && this.srcNodeConnector == tunnel.getDstNodeConnector() &&
                this.tunnelKey != null && this.tunnelKey.equalsIgnoreCase(tunnel.getTunnelKey())){
            return true;
        } else if (this.dstNodeConnector != null && this.dstNodeConnector == tunnel.getSrcNodeConnector() &&
                this.tunnelKey != null && this.tunnelKey.equalsIgnoreCase(tunnel.getTunnelKey())){
            return true;
        }
        return false;
    }
    @Override
    public String toString() {
        return "Tunnel: " +
                srcNodeConnector + "(" + srcAddress + ")" +
                "->" +
                dstNodeConnector + "(" + dstAddress + ")";
    }
}
