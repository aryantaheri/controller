package org.opendaylight.controller.samples.differentiatedforwarding;

import java.net.InetAddress;

import org.opendaylight.controller.sal.core.NodeConnector;

public class TunnelEndPoint {
    NodeConnector tepNodeConnector;
    InetAddress tepAddress;
    String tunnelKey;
    public TunnelEndPoint(NodeConnector tepNodeConnector, InetAddress tepAddress, String tunnelKey) {
        this.tepAddress = tepAddress;
        this.tepNodeConnector = tepNodeConnector;
        this.tunnelKey = tunnelKey;
    }


    public NodeConnector getTepNodeConnector() {
        return tepNodeConnector;
    }


    public InetAddress getTepAddress() {
        return tepAddress;
    }


    public String getTunnelKey() {
        return tunnelKey;
    }


    @Override
    public String toString() {
        return "TunnelEndPoint: key(" + tunnelKey + ")" + tepNodeConnector + "(" + tepAddress + ")";
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof TunnelEndPoint))
            return false;
        TunnelEndPoint other = (TunnelEndPoint) obj;
        // Implicit assumption that key, addresses, and srcNC can not be null. This should be enforced in the constructor, etc.
        if (tunnelKey != null && other.tunnelKey != null &&
                tepAddress != null && other.tepAddress != null &&
                tepNodeConnector != null && other.tepNodeConnector != null &&
                tunnelKey.equalsIgnoreCase(other.tunnelKey) && tepNodeConnector.equals(other.tepNodeConnector) && tepAddress.equals(other.tepAddress)){
            return true;
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
                + ((tepAddress == null) ? 0 : tepAddress.hashCode());
        result = prime
                * result
                + ((tepNodeConnector == null) ? 0 : tepNodeConnector.hashCode());

        return result;
    }

}
