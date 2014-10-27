package org.opendaylight.controller.samples.differentiatedforwarding;

import java.net.InetAddress;

import org.opendaylight.controller.sal.core.NodeConnector;

public class TunnelEndPoint {
    NodeConnector tepNodeConnector;
    InetAddress tepLocalAddress;
    InetAddress tepRemoteAddress; // Note: This can be remote=flow to support Nicira Extension
    String tunnelKey;

    public TunnelEndPoint(NodeConnector tepNodeConnector, InetAddress tepLocalAddress, InetAddress tepRemoteAddress, String tunnelKey) {
        this.tepLocalAddress = tepLocalAddress;
        this.tepRemoteAddress = tepRemoteAddress;
        this.tepNodeConnector = tepNodeConnector;
        this.tunnelKey = tunnelKey;
    }


    public NodeConnector getTepNodeConnector() {
        return tepNodeConnector;
    }


    public InetAddress getTepLocalAddress() {
        return tepLocalAddress;
    }

    public InetAddress getTepRemoteAddress() {
        return tepRemoteAddress;
    }

    public String getTunnelKey() {
        return tunnelKey;
    }


    @Override
    public String toString() {
        return "TunnelEndPoint: key(" + tunnelKey + ")" + tepNodeConnector + "(" + tepLocalAddress + "->" + tepRemoteAddress + ")";
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
                tepLocalAddress != null && other.tepLocalAddress != null &&
                tepRemoteAddress != null && other.tepRemoteAddress != null &&
                tepNodeConnector != null && other.tepNodeConnector != null &&
                tunnelKey.equalsIgnoreCase(other.tunnelKey) && tepNodeConnector.equals(other.tepNodeConnector) &&
                tepLocalAddress.equals(other.tepLocalAddress) && tepRemoteAddress.equals(other.tepRemoteAddress)){
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
                + ((tepLocalAddress == null) ? 0 : tepLocalAddress.hashCode());
        result = prime
                * result
                + ((tepRemoteAddress == null) ? 0 : tepRemoteAddress.hashCode());
        result = prime
                * result
                + ((tepNodeConnector == null) ? 0 : tepNodeConnector.hashCode());

        return result;
    }

}
