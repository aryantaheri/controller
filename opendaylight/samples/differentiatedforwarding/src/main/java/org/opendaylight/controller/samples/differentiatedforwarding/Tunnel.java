package org.opendaylight.controller.samples.differentiatedforwarding;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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

    public Tunnel(TunnelEndPoint srcTep, TunnelEndPoint dstTep) throws Exception {
        this.srcNodeConnector = srcTep.getTepNodeConnector();
        this.dstNodeConnector = dstTep.getTepNodeConnector();
        this.srcAddress = srcTep.getTepAddress();
        this.dstAddress = dstTep.getTepAddress();
        if (!srcTep.getTunnelKey().equalsIgnoreCase(dstTep.getTunnelKey())) throw new Exception("Src TEP and Dst TEP doesn't have a same Tunnel Key");
        this.tunnelKey = srcTep.getTunnelKey();
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
        if (this.tunnelKey == null || tunnel.getTunnelKey() == null
                || !this.tunnelKey.equalsIgnoreCase(tunnel.getTunnelKey())){
            return false;
        } else if (this.srcAddress != null && this.dstAddress != null &&
                    tunnel.getSrcAddress() != null && tunnel.getDstAddress() != null &&
                    this.dstAddress.equals(tunnel.getSrcAddress()) && this.srcAddress.equals(tunnel.getDstAddress())){

            return true;
        }
        return false;
    }

    public void fillFromReverse(Tunnel reverseTunnel){
        if (!isReverse(reverseTunnel)) return;
        if (this.srcNodeConnector == null && reverseTunnel.getDstNodeConnector() != null){
            this.srcNodeConnector = reverseTunnel.getDstNodeConnector();
        }
        if (this.dstNodeConnector == null && reverseTunnel.getSrcNodeConnector() != null){
            this.dstNodeConnector = reverseTunnel.getSrcNodeConnector();
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof Tunnel))
            return false;
        Tunnel other = (Tunnel) obj;
        // Implicit assumption that key, addresses, and srcNC can not be null. This should be enforced in the constructor, etc.
        if (tunnelKey != null && other.tunnelKey != null &&
                srcAddress != null && other.srcAddress != null &&
                dstAddress != null && other.dstAddress != null &&
                srcNodeConnector != null && other.srcNodeConnector != null){

            if (dstNodeConnector != null && other.dstNodeConnector != null){
                if (tunnelKey.equalsIgnoreCase(other.tunnelKey) &&
                        srcAddress.equals(other.srcAddress) &&
                        dstAddress.equals(other.dstAddress) &&
                        srcNodeConnector.equals(other.srcNodeConnector) &&
                        dstNodeConnector.equals(other.dstNodeConnector)){
                    return true;
                } else {
                    return false;
                }
            } else if (dstNodeConnector == null && other.dstNodeConnector == null){
                if(tunnelKey.equalsIgnoreCase(other.tunnelKey) &&
                        srcAddress.equals(other.srcAddress) &&
                        dstAddress.equals(other.dstAddress) &&
                        srcNodeConnector.equals(other.srcNodeConnector)){
                    return true;
                } else {
                    return false;
                }

            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;

        result = prime
                * result
                + ((tunnelKey == null) ? 0 : tunnelKey.hashCode());
        result = prime
                * result
                + ((srcAddress == null) ? 0 : srcAddress.hashCode());
        result = prime
                * result
                + ((dstAddress == null) ? 0 : dstAddress.hashCode());
        result = prime
                * result
                + ((srcNodeConnector == null) ? 0 : srcNodeConnector.hashCode());
        result = prime
                * result
                + ((dstNodeConnector == null) ? 0 : dstNodeConnector.hashCode());

        return result;
    }

    @Override
    public String toString() {
        return "Tunnel: key(" + tunnelKey + ")" +
                srcNodeConnector + "(" + srcAddress + ")" +
                "->" +
                dstNodeConnector + "(" + dstAddress + ")";
    }

    public static List<Tunnel> createTunnels(Set<TunnelEndPoint> teps) throws Exception{
        List<Tunnel> tunnels = new ArrayList<>();

        for (TunnelEndPoint srcTep : teps) {
            for (TunnelEndPoint dstTep : teps) {
                if (srcTep.equals(dstTep))
                    continue;

                tunnels.add(new Tunnel(srcTep, dstTep));
            }
        }
        return tunnels;
    }
}
