package org.opendaylight.controller.routing.yenkshortestpaths.internal;

import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.Edge;
import org.opendaylight.controller.sal.core.NodeConnector;

public class WeightedEdge extends Edge {

    private static final long serialVersionUID = -1775245183703241873L;
    private double weight = 1;

    public WeightedEdge(Edge src, double weight)
            throws ConstructionException {
        super(src);
        this.setWeight(weight);
    }

    public WeightedEdge(NodeConnector tailNodeConnector,
            NodeConnector headNodeConnector, double weight)
            throws ConstructionException {
        super(tailNodeConnector, headNodeConnector);
        this.setWeight(weight);
    }

    public double getWeight() {
        return weight;
    }

    private void setWeight(double weight) {
        this.weight = weight;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        // Casting here can be dangerous
        // FIXME: two edges with the same weight between similar endpoints
        // will result in the same hashCode and this is wrong
        result = (int) (prime * result + weight);

        return result;

    }

    @Override
    public String toString() {
        return "(" + this.getTailNodeConnector() + "-" + weight + "->"
                + this.getHeadNodeConnector() + ")";
    }
}