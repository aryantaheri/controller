/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.routing.yenkshortestpaths.internal;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.apache.felix.service.command.Descriptor;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.Path;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.osgi.framework.ServiceRegistration;

public class YKShortestPathsCLI {
    @SuppressWarnings("rawtypes")
    private ServiceRegistration sr = null;

    public void init() {
    }

    public void destroy() {
    }

    public void start() {
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("osgi.command.scope", "odpcontroller");
        props.put("osgi.command.function", new String[] { "getkShortestRoutes" });
        this.sr = ServiceHelper.registerGlobalServiceWReg(YKShortestPathsCLI.class, this, props);
    }

    public void stop() {
        if (this.sr != null) {
            this.sr.unregister();
            this.sr = null;
        }
    }

    @Descriptor("Retrieves K shortest routes between two Nodes in the discovered Topology DB")
    public void getkShortestRoutes(
            @Descriptor("Container on the context of which the routing service need to be looked up") String container,
            @Descriptor("String representation of the Source Node, this need to be consumable from Node.fromString()") String srcNode,
            @Descriptor("String representation of the Destination Node") String dstNode,
            @Descriptor("Number of shortest paths (K)") int k) {
        System.out.println("YKShortestPathsCLI.getkShortestRoutes src:" + srcNode + " dst: " + dstNode + " K: " + k);
        IKShortestRoutes r = null;
        Object[] rList = ServiceHelper.getInstances(IKShortestRoutes.class, container, this, null);
        for (int i = 0; i < rList.length; i++) {
            if (rList[i] instanceof YKShortestPaths)
                r = (IKShortestRoutes) rList[i];
        }

        if (r == null) {
            System.out.println("Cannot find the YKSP routing instance on container:" + container);
            return;
        }
        System.out.println("YKShortestPathsCLI.getkShortestRoutes IRouting is retrieved: " + r.getClass());
        final Node src = Node.fromString(srcNode);
        final Node dst = Node.fromString(dstNode);
        final List<Path> paths = r.getKShortestRoutes(src, dst, k);
        if (paths != null) {
            System.out.println("K Shortest Routes between srcNode:" + src + " and dstNode:" + dst + " = " + paths);
            int i = 1;
            for (Path path : paths) {
                System.out.println("Route "+ i + ": " + path);
                i++;
            }
        } else {
            System.out.println("There is no route between srcNode:" + src + " and dstNode:" + dst);
        }
    }
}
