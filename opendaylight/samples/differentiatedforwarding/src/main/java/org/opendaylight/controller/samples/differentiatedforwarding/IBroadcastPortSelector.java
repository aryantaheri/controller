/*
 * Copyright (c) 2013 IBM and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.samples.differentiatedforwarding;

import java.util.Set;

import org.opendaylight.controller.sal.core.NodeConnector;

public interface IBroadcastPortSelector {
    Set<NodeConnector> getBroadcastPorts();
}
