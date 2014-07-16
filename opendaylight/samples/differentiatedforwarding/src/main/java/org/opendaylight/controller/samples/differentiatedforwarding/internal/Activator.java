
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.samples.differentiatedforwarding.internal;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.dm.Component;
import org.opendaylight.controller.clustering.services.IClusterContainerServices;
import org.opendaylight.controller.hosttracker.IfNewHostNotify;
import org.opendaylight.controller.routing.yenkshortestpaths.internal.IKShortestRoutes;
import org.opendaylight.controller.sal.core.ComponentActivatorAbstractBase;
import org.opendaylight.controller.sal.packet.IListenDataPacket;
import org.opendaylight.controller.sal.routing.IListenRoutingUpdates;
import org.opendaylight.controller.samples.differentiatedforwarding.IForwarding;
import org.opendaylight.controller.samples.differentiatedforwarding.ITunnelObserver;
import org.opendaylight.controller.switchmanager.IInventoryListener;
import org.opendaylight.ovsdb.plugin.OVSDBConfigService;
import org.opendaylight.ovsdb.plugin.OVSDBInventoryListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator extends ComponentActivatorAbstractBase {
    protected static final Logger logger = LoggerFactory
            .getLogger(Activator.class);


    /**
     * Function that is used to communicate to dependency manager the
     * list of known implementations for services inside a container
     *
     *
     * @return An array containing all the CLASS objects that will be
     * instantiated in order to get an fully working implementation
     * Object
     */
    @Override
    public Object[] getImplementations() {
        Object[] res = { TenantTunnelObserver.class, DifferentiatedForwardingImpl.class };
        return res;
    }

    @Override
    protected Object[] getGlobalImplementations() {
        final Object[] res = { TenantTunnelObserverCLI.class, DifferentiatedForwardingCLI.class };
        return res;
    }

    /**
     * Function that is called when configuration of the dependencies
     * is required.
     *
     * @param c dependency manager Component object, used for
     * configuring the dependencies exported and imported
     * @param imp Implementation class that is being configured,
     * needed as long as the same routine can configure multiple
     * implementations
     * @param containerName The containerName being configured, this allow
     * also optional per-container different behavior if needed, usually
     * should not be the case though.
     */
    @Override
    public void configureInstance(Component c, Object imp, String containerName) {
        if (imp.equals(TenantTunnelObserver.class)) {
            Dictionary<String, Object> props = new Hashtable<String, Object>();
            props.put("salListenerName", "tenanttunnelobserver");

            // export the service
            c.setInterface(new String[] { OVSDBInventoryListener.class.getName(),
                    ITunnelObserver.class.getName()}, props);

            c.add(createContainerServiceDependency(containerName).setService(
                    IClusterContainerServices.class).setCallbacks(
                    "setClusterContainerService",
                    "unsetClusterContainerService").setRequired(false));

//            c.add(createContainerServiceDependency(containerName).setService(
//                    ISwitchManager.class).setCallbacks("setSwitchManager",
//                    "unsetSwitchManager").setRequired(true));
//
//            c.add(createContainerServiceDependency(containerName).setService(
//                    IfIptoHost.class).setCallbacks("setHostTracker",
//                    "unsetHostTracker").setRequired(false));
//
//            c.add(createContainerServiceDependency(containerName).setService(
//                    IForwardingRulesManager.class).setCallbacks(
//                    "setForwardingRulesManager", "unsetForwardingRulesManager")
//                    .setRequired(true));
//
//            c.add(createContainerServiceDependency(containerName).setService(
//                    ITopologyManager.class).setCallbacks("setTopologyManager",
//                    "unsetTopologyManager").setRequired(true));
//
//            c.add(createContainerServiceDependency(containerName).setService(
//                    IRouting.class).setCallbacks("setRouting", "unsetRouting")
//                    .setRequired(true));
//
//            c.add(createContainerServiceDependency(containerName).setService(
//                    IDataPacketService.class).setCallbacks("setDataPacketService",
//                   "unsetDataPacketService").setRequired(false));



            c.add(createContainerServiceDependency(containerName).setService(
                    OVSDBConfigService.class).setCallbacks("setOVSDBConfigService",
                    "unsetOVSDBConfigService").setRequired(false));

        }
        else if (imp.equals(DifferentiatedForwardingImpl.class)){
            Dictionary<String, Object> props = new Hashtable<String, Object>();
            props.put("salListenerName", "differentiatedforwardingimpl");

            // export the service
            c.setInterface(new String[] { /**OVSDBInventoryListener.class.getName(),**/
                    IInventoryListener.class.getName(),
                    IfNewHostNotify.class.getName(),
                    IListenRoutingUpdates.class.getName(),
                    IListenDataPacket.class.getName(),

                    IForwarding.class.getName()}, props);


            c.add(createContainerServiceDependency(containerName)
                    .setService(IKShortestRoutes.class)
                    .setCallbacks("setKShortestRoutes",
                            "unsetKShortestRoutes").setRequired(true));
            c.add(createContainerServiceDependency(containerName).setService(
                    IClusterContainerServices.class).setCallbacks(
                    "setClusterContainerService",
                    "unsetClusterContainerService").setRequired(true));
        }


//        else if (imp.equals(SimpleBroadcastHandlerImpl.class)) {
//            Dictionary<String, String> props = new Hashtable<String, String>();
//            props.put("salListenerName", "simplebroadcasthandler");
//
//            // export the service
//            c.setInterface(new String[] { IBroadcastHandler.class.getName(),
//                    IListenDataPacket.class.getName() }, props);
//
//            c.add(createContainerServiceDependency(containerName).setService(
//                    IDataPacketService.class).setCallbacks("setDataPacketService",
//                   "unsetDataPacketService").setRequired(false));
//
//            c.add(createContainerServiceDependency(containerName).setService(
//                   ITopologyManager.class).setCallbacks("setTopologyManager",
//                   "unsetTopologyManager").setRequired(true));
//
//            c.add(createContainerServiceDependency(containerName).setService(
//                   IBroadcastPortSelector.class).setCallbacks("setBroadcastPortSelector",
//                   "unsetBroadcastPortSelector").setRequired(false));
//
//            c.add(createContainerServiceDependency(containerName).setService(
//                   ISwitchManager.class).setCallbacks("setSwitchManager",
//                   "unsetSwitchManager").setRequired(false));
//        }
    }
}
