/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.persist.impl;

import com.google.common.annotations.VisibleForTesting;
import org.opendaylight.controller.config.persist.api.ConfigSnapshotHolder;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Inspects snapshot xml to be stored, remove all capabilities that are not referenced by it.
 * Useful when persisting current configuration.
 */
public class CapabilityStrippingConfigSnapshotHolder implements ConfigSnapshotHolder {
    private static final Logger logger = LoggerFactory.getLogger(CapabilityStrippingConfigSnapshotHolder.class);

    private final String configSnapshot;
    private final StripCapabilitiesResult stripCapabilitiesResult;

    public CapabilityStrippingConfigSnapshotHolder(Element snapshot, Set<String> capabilities) {
        final XmlElement configElement = XmlElement.fromDomElement(snapshot);
        configSnapshot = XmlUtil.toString(configElement.getDomElement());
        stripCapabilitiesResult = stripCapabilities(configElement, capabilities);
    }

    private static class StripCapabilitiesResult {
        private final SortedSet<String> requiredCapabilities, obsoleteCapabilities;

        private StripCapabilitiesResult(SortedSet<String> requiredCapabilities, SortedSet<String> obsoleteCapabilities) {
            this.requiredCapabilities = Collections.unmodifiableSortedSet(requiredCapabilities);
            this.obsoleteCapabilities = Collections.unmodifiableSortedSet(obsoleteCapabilities);
        }
    }


    @VisibleForTesting
    static StripCapabilitiesResult stripCapabilities(XmlElement configElement, Set<String> allCapabilitiesFromHello) {
        // collect all namespaces
        Set<String> foundNamespacesInXML = getNamespaces(configElement);
        logger.trace("All capabilities {}\nFound namespaces in XML {}", allCapabilitiesFromHello, foundNamespacesInXML);
        // required are referenced both in xml and hello
        SortedSet<String> requiredCapabilities = new TreeSet<>();
        // can be removed
        SortedSet<String> obsoleteCapabilities = new TreeSet<>();
        for (String capability : allCapabilitiesFromHello) {
            String namespace = capability.replaceAll("\\?.*","");
            if (foundNamespacesInXML.contains(namespace)) {
                requiredCapabilities.add(capability);
            } else {
                obsoleteCapabilities.add(capability);
            }
        }

        logger.trace("Required capabilities {}, \nObsolete capabilities {}",
                requiredCapabilities, obsoleteCapabilities);

        return new StripCapabilitiesResult(requiredCapabilities, obsoleteCapabilities);
    }

    static Set<String> getNamespaces(XmlElement element){
        Set<String> result = new HashSet<>();
        for (Entry<String,Attr> attribute : element.getAttributes().entrySet()) {
            if  (attribute.getKey().startsWith("xmlns")){
                result.add(attribute.getValue().getValue());
            }
        }
        for(XmlElement child: element.getChildElements()) {
            result.addAll(getNamespaces(child));
        }
        return result;
    }

    @Override
    public SortedSet<String> getCapabilities() {
        return stripCapabilitiesResult.requiredCapabilities;
    }

    @VisibleForTesting
    Set<String> getObsoleteCapabilities(){
        return stripCapabilitiesResult.obsoleteCapabilities;
    }

    @Override
    public String getConfigSnapshot() {
        return configSnapshot;
    }
}
