/*
 * Copyright (c) 2014, 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.restconf.impl.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import java.util.concurrent.Future;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.sal.core.api.Broker.ConsumerSession;
import org.opendaylight.netconf.sal.restconf.impl.BrokerFacade;
import org.opendaylight.netconf.sal.restconf.impl.ControllerContext;
import org.opendaylight.netconf.sal.restconf.impl.RestconfDocumentedException;
import org.opendaylight.netconf.sal.restconf.impl.RestconfError;
import org.opendaylight.netconf.sal.restconf.impl.RestconfError.ErrorTag;
import org.opendaylight.netconf.sal.restconf.impl.RestconfError.ErrorType;
import org.opendaylight.netconf.sal.streams.listeners.ListenerAdapter;
import org.opendaylight.netconf.sal.streams.listeners.Notificator;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

/**
 * Unit tests for BrokerFacade.
 *
 * @author Thomas Pantelis
 */
public class BrokerFacadeTest {
    @Mock private DOMDataBroker domDataBroker;
    @Mock private ConsumerSession context;
    @Mock private DOMRpcService mockRpcService;
    @Mock private DOMMountPoint mockMountInstance;

    private final BrokerFacade brokerFacade = BrokerFacade.getInstance();
    private final NormalizedNode<?, ?> dummyNode = createDummyNode("test:module", "2014-01-09", "interfaces");
    private final CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> dummyNodeInFuture =
            wrapDummyNode(dummyNode);
    private final QName qname = TestUtils.buildQName("interfaces","test:module", "2014-01-09");
    private final SchemaPath type = SchemaPath.create(true, qname);
    private final YangInstanceIdentifier instanceID = YangInstanceIdentifier.builder().node(qname).build();

    @Mock private DOMDataReadOnlyTransaction rTransaction;
    @Mock private DOMDataWriteTransaction wTransaction;
    @Mock private DOMDataReadWriteTransaction rwTransaction;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        brokerFacade.setDomDataBroker(domDataBroker);
        brokerFacade.setRpcService(mockRpcService);
        brokerFacade.setContext(context);
        when(domDataBroker.newReadOnlyTransaction()).thenReturn(rTransaction);
        when(domDataBroker.newWriteOnlyTransaction()).thenReturn(wTransaction);
        when(domDataBroker.newReadWriteTransaction()).thenReturn(rwTransaction);

        ControllerContext.getInstance().setSchemas(TestUtils.loadSchemaContext("/full-versions/test-module"));
    }

    private CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> wrapDummyNode(
            final NormalizedNode<?, ?> dummyNode) {
        return Futures.immediateCheckedFuture(Optional.<NormalizedNode<?, ?>> of(dummyNode));
    }

    private CheckedFuture<Boolean, ReadFailedException> wrapExistence(final Boolean exists) {
        return Futures.immediateCheckedFuture(exists);
    }

    /**
     * Value of this node shouldn't be important for testing purposes
     */
    private NormalizedNode<?, ?> createDummyNode(final String namespace, final String date, final String localName) {
        return Builders.containerBuilder()
                .withNodeIdentifier(new NodeIdentifier(QName.create(namespace, date, localName))).build();
    }

    @Test
    public void testReadConfigurationData() {
        when(rTransaction.read(any(LogicalDatastoreType.class), any(YangInstanceIdentifier.class))).thenReturn(
                dummyNodeInFuture);

        final NormalizedNode<?, ?> actualNode = brokerFacade.readConfigurationData(instanceID);

        assertSame("readConfigurationData", dummyNode, actualNode);
    }

    @Test
    public void testReadOperationalData() {
        when(rTransaction.read(any(LogicalDatastoreType.class), any(YangInstanceIdentifier.class))).thenReturn(
                dummyNodeInFuture);

        final NormalizedNode<?, ?> actualNode = brokerFacade.readOperationalData(instanceID);

        assertSame("readOperationalData", dummyNode, actualNode);
    }

    @Test(expected = RestconfDocumentedException.class)
    public void testReadOperationalDataWithNoDataBroker() {
        brokerFacade.setDomDataBroker(null);

        brokerFacade.readOperationalData(instanceID);
    }

    @Test
    public void testInvokeRpc() throws Exception {
        final DOMRpcResult expResult = mock(DOMRpcResult.class);
        final CheckedFuture<DOMRpcResult, DOMRpcException> future = Futures.immediateCheckedFuture(expResult);
        when(mockRpcService.invokeRpc(type, dummyNode)).thenReturn(future);

        final CheckedFuture<DOMRpcResult, DOMRpcException> actualFuture = brokerFacade.invokeRpc(type, dummyNode);
        assertNotNull("Future is null", actualFuture);
        final DOMRpcResult actualResult = actualFuture.get();
        assertSame("invokeRpc", expResult, actualResult);
    }

    @Test(expected = RestconfDocumentedException.class)
    public void testInvokeRpcWithNoConsumerSession() {
        brokerFacade.setContext(null);
        brokerFacade.invokeRpc(type, dummyNode);
    }

    @Ignore
    @Test
    public void testCommitConfigurationDataPut() {
        @SuppressWarnings("unchecked")
        final CheckedFuture<Void, TransactionCommitFailedException> expFuture = mock(CheckedFuture.class);

        when(wTransaction.submit()).thenReturn(expFuture);

        final Future<Void> actualFuture = brokerFacade.commitConfigurationDataPut((SchemaContext)null, instanceID, dummyNode);

        assertSame("commitConfigurationDataPut", expFuture, actualFuture);

        final InOrder inOrder = inOrder(domDataBroker, wTransaction);
        inOrder.verify(domDataBroker).newWriteOnlyTransaction();
        inOrder.verify(wTransaction).put(LogicalDatastoreType.CONFIGURATION, instanceID, dummyNode);
        inOrder.verify(wTransaction).submit();
    }

    @Test
    public void testCommitConfigurationDataPost() {
        @SuppressWarnings("unchecked")
        final CheckedFuture<Void, TransactionCommitFailedException> expFuture = mock(CheckedFuture.class);

        when(rwTransaction.exists(eq(LogicalDatastoreType.CONFIGURATION), any(YangInstanceIdentifier.class))).thenReturn(
            wrapExistence(false));

        when(rwTransaction.submit()).thenReturn(expFuture);

        final CheckedFuture<Void, TransactionCommitFailedException> actualFuture = brokerFacade.commitConfigurationDataPost(
                (SchemaContext)null, instanceID, dummyNode);

        assertSame("commitConfigurationDataPost", expFuture, actualFuture);

        final InOrder inOrder = inOrder(domDataBroker, rwTransaction);
        inOrder.verify(domDataBroker).newReadWriteTransaction();
        inOrder.verify(rwTransaction).exists(LogicalDatastoreType.CONFIGURATION, instanceID);
        inOrder.verify(rwTransaction).put(LogicalDatastoreType.CONFIGURATION, instanceID, dummyNode);
        inOrder.verify(rwTransaction).submit();
    }

    @Test(expected = RestconfDocumentedException.class)
    public void testCommitConfigurationDataPostAlreadyExists() {
        final CheckedFuture<Boolean, ReadFailedException> successFuture = Futures.immediateCheckedFuture(Boolean.TRUE);
        when(rwTransaction.exists(eq(LogicalDatastoreType.CONFIGURATION), any(YangInstanceIdentifier.class))).thenReturn(
                successFuture);
        try {
            // Schema context is only necessary for ensuring parent structure
            brokerFacade.commitConfigurationDataPost((SchemaContext)null, instanceID, dummyNode);
        } catch (final RestconfDocumentedException e) {
            assertEquals("getErrorTag", RestconfError.ErrorTag.DATA_EXISTS, e.getErrors().get(0).getErrorTag());
            throw e;
        }
    }

    /**
     * Positive test of delete operation when data to delete exits. Returned value and order of steps are validated.
     */
    @Test
    public void testCommitConfigurationDataDelete() throws Exception {
        // assume that data to delete exists
        prepareDataForDelete(true);

        // expected result
        final CheckedFuture<Void, TransactionCommitFailedException> expFuture = mock(CheckedFuture.class);
        when(rwTransaction.submit()).thenReturn(expFuture);

        // test
        final CheckedFuture<Void, TransactionCommitFailedException> actualFuture = brokerFacade
                .commitConfigurationDataDelete(instanceID);

        // verify result and interactions
        assertSame("commitConfigurationDataDelete", expFuture, actualFuture);

        // check exists, delete, submit
        final InOrder inOrder = inOrder(domDataBroker, rwTransaction);
        inOrder.verify(rwTransaction).exists(LogicalDatastoreType.CONFIGURATION, instanceID);
        inOrder.verify(rwTransaction).delete(LogicalDatastoreType.CONFIGURATION, instanceID);
        inOrder.verify(rwTransaction).submit();
    }

    /**
     * Negative test of delete operation when data to delete does not exist. Error 404 should be returned.
     */
    @Test
    public void testCommitConfigurationDataDeleteNoData() throws Exception {
        // assume that data to delete does not exist
        prepareDataForDelete(false);

        // try to delete and expect 404 error
        try {
            brokerFacade.commitConfigurationDataDelete(instanceID);
            fail("Delete operation should fail due to missing data");
        } catch (final RestconfDocumentedException e) {
            assertEquals(ErrorType.PROTOCOL, e.getErrors().get(0).getErrorType());
            assertEquals(ErrorTag.DATA_MISSING, e.getErrors().get(0).getErrorTag());
            assertEquals(404, e.getErrors().get(0).getErrorTag().getStatusCode());
        }
    }

    /**
     * Prepare conditions to test delete operation. Data to delete exists or does not exist according to value of
     * {@code assumeDataExists} parameter.
     * @param assumeDataExists
     * @throws Exception
     */
    private void prepareDataForDelete(final boolean assumeDataExists) throws Exception {
        final CheckedFuture<Boolean, ReadFailedException> checkDataExistFuture = mock(CheckedFuture.class);
        when(checkDataExistFuture.get()).thenReturn(assumeDataExists);
        when(rwTransaction.exists(LogicalDatastoreType.CONFIGURATION, instanceID)).thenReturn(checkDataExistFuture);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRegisterToListenDataChanges() {
        final ListenerAdapter listener = Notificator.createListener(instanceID, "stream");

        final ListenerRegistration<DOMDataChangeListener> mockRegistration = mock(ListenerRegistration.class);

        when(
                domDataBroker.registerDataChangeListener(any(LogicalDatastoreType.class), eq(instanceID), eq(listener),
                        eq(DataChangeScope.BASE))).thenReturn(mockRegistration);

        brokerFacade.registerToListenDataChanges(LogicalDatastoreType.CONFIGURATION, DataChangeScope.BASE, listener);

        verify(domDataBroker).registerDataChangeListener(LogicalDatastoreType.CONFIGURATION, instanceID, listener,
                DataChangeScope.BASE);

        assertEquals("isListening", true, listener.isListening());

        brokerFacade.registerToListenDataChanges(LogicalDatastoreType.CONFIGURATION, DataChangeScope.BASE, listener);
        verifyNoMoreInteractions(domDataBroker);
    }
}
