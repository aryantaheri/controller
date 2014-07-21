package org.opendaylight.controller.routing.yenkshortestpaths.internal;

import java.util.List;

import org.opendaylight.controller.sal.core.Edge;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.Path;

public interface IKShortestRoutes {

    /**
     * Returns K shortest Paths leading from the source to the destination
     *
     *
     * @param src
     *            source {@link org.opendaylight.controller.sal.core.Node}
     *
     * @param dst
     *            destination {@link org.opendaylight.controller.sal.core.Node}
     * @return a List of {@link org.opendaylight.controller.sal.core.Path}
     */
    public List<Path> getKShortestRoutes(Node src, Node dst, int k);
    public void ignoreEdge(Edge edge, boolean ignoreReverseAsWell);
    public void clear();
}
