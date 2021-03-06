/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netconf.test.tool.rpchandler;

import java.util.Optional;
import org.opendaylight.netconf.api.xml.XmlElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

public class RpcHandlerDefault implements RpcHandler {

    private static final Logger LOG = LoggerFactory.getLogger(RpcHandlerDefault.class);

    @Override
    public Optional<Document> getResponse(XmlElement rpcElement) {
        LOG.info("getResponse: {}", rpcElement.toString());
        return Optional.empty();
    }

}
