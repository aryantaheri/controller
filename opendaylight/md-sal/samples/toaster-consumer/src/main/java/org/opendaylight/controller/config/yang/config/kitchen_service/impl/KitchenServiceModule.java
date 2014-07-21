/**
 * Generated file

 * Generated from: yang module name: toaster-consumer-impl  yang module local name: toaster-consumer-impl
 * Generated by: org.opendaylight.controller.config.yangjmxgenerator.plugin.JMXGenerator
 * Generated at: Wed Feb 05 11:31:30 CET 2014
 *
 * Do not modify this file unless it is present under src/main directory
 */
package org.opendaylight.controller.config.yang.config.kitchen_service.impl;

import java.util.concurrent.Future;

import org.opendaylight.controller.sample.kitchen.api.EggsType;
import org.opendaylight.controller.sample.kitchen.api.KitchenService;
import org.opendaylight.controller.sample.kitchen.impl.KitchenServiceImpl;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.ToastType;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.ToasterService;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.NotificationListener;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public final class KitchenServiceModule extends AbstractKitchenServiceModule {
    private static final Logger log = LoggerFactory.getLogger(KitchenServiceModule.class);

    public KitchenServiceModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public KitchenServiceModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
            final KitchenServiceModule oldModule, final java.lang.AutoCloseable oldInstance) {

        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    protected void customValidation(){
        // No need to validate dependencies, since all dependencies have mandatory true flag in yang
        // config-subsystem will perform the validation
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        ToasterService toasterService = getRpcRegistryDependency().getRpcService(ToasterService.class);

        final KitchenServiceImpl kitchenService = new KitchenServiceImpl(toasterService);

        final Registration<NotificationListener> toasterListenerReg =
                getNotificationServiceDependency().registerNotificationListener( kitchenService );

        final KitchenServiceRuntimeRegistration runtimeReg =
                getRootRuntimeBeanRegistratorWrapper().register( kitchenService );

        final class AutoCloseableKitchenService implements KitchenService, AutoCloseable {

            @Override
            public void close() throws Exception {
                toasterListenerReg.close();
                runtimeReg.close();
                log.info("Toaster consumer (instance {}) torn down.", this);
            }

            @Override
            public Future<RpcResult<Void>> makeBreakfast( final EggsType eggs,
                                                          final Class<? extends ToastType> toast,
                                                          final int toastDoneness ) {
                return kitchenService.makeBreakfast( eggs, toast, toastDoneness );
            }
        }

        AutoCloseable ret = new AutoCloseableKitchenService();
        log.info("KitchenService (instance {}) initialized.", ret );
        return ret;
    }
}
