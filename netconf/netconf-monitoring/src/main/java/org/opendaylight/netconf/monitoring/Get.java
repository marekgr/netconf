/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netconf.monitoring;

import java.util.Collections;
import org.opendaylight.netconf.api.DocumentedException;
import org.opendaylight.netconf.api.monitoring.NetconfMonitoringService;
import org.opendaylight.netconf.api.xml.XmlElement;
import org.opendaylight.netconf.api.xml.XmlNetconfConstants;
import org.opendaylight.netconf.mapping.api.HandlingPriority;
import org.opendaylight.netconf.mapping.api.NetconfOperationChainedExecution;
import org.opendaylight.netconf.monitoring.xml.JaxBSerializer;
import org.opendaylight.netconf.monitoring.xml.model.NetconfState;
import org.opendaylight.netconf.util.mapping.AbstractNetconfOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class Get extends AbstractNetconfOperation {

    private static final Logger LOG = LoggerFactory.getLogger(Get.class);
    private final NetconfMonitoringService netconfMonitor;

    public Get(final NetconfMonitoringService netconfMonitor) {
        super(MonitoringConstants.MODULE_NAME);
        this.netconfMonitor = netconfMonitor;
    }

    private Element getPlaceholder(final Document innerResult)
            throws DocumentedException {
        final XmlElement rootElement = XmlElement.fromDomElementWithExpected(
                innerResult.getDocumentElement(), XmlNetconfConstants.RPC_REPLY_KEY,
                XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0);
        return rootElement.getOnlyChildElement(XmlNetconfConstants.DATA_KEY).getDomElement();
    }

    @Override
    protected String getOperationName() {
        return XmlNetconfConstants.GET;
    }

    @Override
    protected HandlingPriority getHandlingPriority() {
        return HandlingPriority.HANDLE_WITH_DEFAULT_PRIORITY.increasePriority(1);
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    @Override
    public Document handle(final Document requestMessage, final NetconfOperationChainedExecution subsequentOperation)
            throws DocumentedException {
        if (subsequentOperation.isExecutionTermination()) {
            throw new DocumentedException(String.format("Subsequent netconf operation expected by %s", this),
                    DocumentedException.ErrorType.APPLICATION,
                    DocumentedException.ErrorTag.OPERATION_FAILED,
                    DocumentedException.ErrorSeverity.ERROR);
        }

        try {
            final Document innerResult = subsequentOperation.execute(requestMessage);

            final NetconfState netconfMonitoring = new NetconfState(netconfMonitor);
            Element monitoringXmlElement = new JaxBSerializer().toXml(netconfMonitoring);

            monitoringXmlElement = (Element) innerResult.importNode(monitoringXmlElement, true);
            final Element monitoringXmlElementPlaceholder = getPlaceholder(innerResult);
            monitoringXmlElementPlaceholder.appendChild(monitoringXmlElement);

            return innerResult;
        } catch (final RuntimeException e) {
            final String errorMessage = "Get operation for netconf-state subtree failed";
            LOG.warn(errorMessage, e);

            throw new DocumentedException(errorMessage, e, DocumentedException.ErrorType.APPLICATION,
                    DocumentedException.ErrorTag.OPERATION_FAILED,
                    DocumentedException.ErrorSeverity.ERROR,
                    Collections.singletonMap(DocumentedException.ErrorSeverity.ERROR.toString(), e.getMessage()));
        }
    }

    @Override
    protected Element handle(final Document document, final XmlElement message,
                             final NetconfOperationChainedExecution subsequentOperation)
            throws DocumentedException {
        throw new UnsupportedOperationException("Never gets called");
    }
}
