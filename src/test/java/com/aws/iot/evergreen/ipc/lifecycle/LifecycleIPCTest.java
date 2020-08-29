/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.ipc.lifecycle;

import com.aws.iot.evergreen.ipc.IPCClient;
import com.aws.iot.evergreen.ipc.IPCClientImpl;
import com.aws.iot.evergreen.ipc.common.BuiltInServiceDestinationCode;
import com.aws.iot.evergreen.ipc.common.FrameReader;
import com.aws.iot.evergreen.ipc.common.FrameReader.MessageFrame;
import com.aws.iot.evergreen.ipc.config.KernelIPCClientConfig;
import com.aws.iot.evergreen.ipc.exceptions.IPCClientException;
import com.aws.iot.evergreen.ipc.services.authentication.Authentication;
import com.aws.iot.evergreen.ipc.services.authentication.AuthenticationResponse;
import com.aws.iot.evergreen.ipc.services.common.ApplicationMessage;
import com.aws.iot.evergreen.ipc.services.common.IPCUtil;
import com.aws.iot.evergreen.ipc.services.lifecycle.Lifecycle;
import com.aws.iot.evergreen.ipc.services.lifecycle.LifecycleGenericResponse;
import com.aws.iot.evergreen.ipc.services.lifecycle.LifecycleImpl;
import com.aws.iot.evergreen.ipc.services.lifecycle.LifecycleResponseStatus;
import com.aws.iot.evergreen.ipc.services.lifecycle.LifecycleServiceOpCodes;
import com.aws.iot.evergreen.ipc.services.lifecycle.PostComponentUpdateEvent;
import com.aws.iot.evergreen.ipc.services.lifecycle.PreComponentUpdateEvent;
import com.aws.iot.evergreen.ipc.services.lifecycle.SubscribeToComponentUpdatesResponse;
import com.aws.iot.evergreen.ipc.services.lifecycle.UpdateStateRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.aws.iot.evergreen.ipc.common.FrameReader.readFrame;
import static com.aws.iot.evergreen.ipc.common.FrameReader.writeFrame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class LifecycleIPCTest {
    private final ExecutorService executor = Executors.newCachedThreadPool();

    private IPCClient ipc;
    private Socket sock;
    private ServerSocket server;
    private DataInputStream in;
    private DataOutputStream out;
    private int connectionCount = 0;


    public static <T> T readMessageFromSockInputStream(final MessageFrame inFrame, final Class<T> returnTypeClass) throws Exception {
        ApplicationMessage reqAppFrame = ApplicationMessage.fromBytes(inFrame.message.getPayload());
        return IPCUtil.decode(reqAppFrame.getPayload(), returnTypeClass);
    }

    private void writeMessageToSockOutputStream(int opCode, Integer requestId, Object data, FrameReader.FrameType type) throws Exception {
        ApplicationMessage transitionEventAppFrame = ApplicationMessage.builder()
                .version(LifecycleImpl.API_VERSION).opCode(opCode)
                .payload(IPCUtil.encode(data)).build();

        int destination = BuiltInServiceDestinationCode.LIFECYCLE.getValue();
        FrameReader.Message message = new FrameReader.Message(transitionEventAppFrame.toByteArray());
        MessageFrame messageFrame = requestId == null ?
                new MessageFrame(destination, message, type) :
                new MessageFrame(requestId, destination, message, type);
        FrameReader.writeFrame(messageFrame, out);
    }

    private void writeMessageToSockOutputStream(int opCode, Object data, FrameReader.FrameType type) throws Exception {
        writeMessageToSockOutputStream(opCode, null, data, type);
    }


    @BeforeEach
    public void before() throws IOException, InterruptedException, ExecutionException, IPCClientException {
        server = new ServerSocket(0);
        connectionCount = 0;
        executor.submit(() -> {
            while (true) {
                sock = server.accept();
                in = new DataInputStream(sock.getInputStream());
                out = new DataOutputStream(sock.getOutputStream());

                // Read and write authentication
                MessageFrame inFrame = readFrame(in);
                ApplicationMessage requestApplicationFrame = ApplicationMessage.fromBytes(inFrame.message.getPayload());
                AuthenticationResponse authenticationResponse = AuthenticationResponse.builder().serviceName("ABC").clientId("test").build();
                ApplicationMessage responsesAppFrame = ApplicationMessage.builder()
                        .version(requestApplicationFrame.getVersion())
                        .payload(IPCUtil.encode(authenticationResponse)).build();

                writeFrame(new MessageFrame(inFrame.requestId, BuiltInServiceDestinationCode.AUTHENTICATION.getValue(),
                        new FrameReader.Message(responsesAppFrame.toByteArray()), FrameReader.FrameType.RESPONSE), out);
                connectionCount++;
            }
        });

        ipc = new IPCClientImpl(KernelIPCClientConfig.builder().port(server.getLocalPort()).build());
        while (connectionCount == 0) {
            Thread.sleep(10);
        }
    }

    @AfterEach
    public void after() throws IOException {
        ipc.disconnect();
        sock.close();
        server.close();
    }

    @Test
    public void GIVEN_lifecycle_client_WHEN_requestStateChange_THEN_server_gets_request() throws Exception {
        Lifecycle lf = new LifecycleImpl(ipc);

        Future<?> fut = executor.submit(() -> {
            MessageFrame inFrame = readFrame(in);
            UpdateStateRequest updateStateRequest = readMessageFromSockInputStream(inFrame, UpdateStateRequest.class);
            assertEquals("Errored", updateStateRequest.getState());

            LifecycleGenericResponse lifeCycleGenericResponse =
                    new LifecycleGenericResponse(LifecycleResponseStatus.Success, null);
            writeMessageToSockOutputStream(1, inFrame.requestId, lifeCycleGenericResponse, FrameReader.FrameType.RESPONSE);
            return null;
        });

        lf.updateState("Errored");
        fut.get();
    }

    @Test
    public void GIVEN_lifecycle_client_WHEN_subscribed_to_component_Update_THEN_called_for_each_event()
            throws Exception {
        Lifecycle lf = new LifecycleImpl(ipc);

        // validate subscribe request and response
        Future<?> fut = executor.submit(() -> {
            MessageFrame inFrame = FrameReader.readFrame(in);
            byte[] dummypayload = readMessageFromSockInputStream(inFrame, byte[].class);
            assertTrue(dummypayload.length == 0);

            LifecycleGenericResponse successResponse = SubscribeToComponentUpdatesResponse.builder()
                    .responseStatus(LifecycleResponseStatus.Success).build();
            writeMessageToSockOutputStream(1, inFrame.requestId, successResponse, FrameReader.FrameType.RESPONSE);
            return null;
        });

        CountDownLatch cdl = new CountDownLatch(2);
        AtomicBoolean sawPreComponentUpdateEvent = new AtomicBoolean();
        AtomicBoolean sawPostComponentUpdateEvent = new AtomicBoolean();
        lf.subscribeToComponentUpdate(componentUpdateEvent -> {
            if (componentUpdateEvent instanceof PreComponentUpdateEvent) {
                sawPreComponentUpdateEvent.set(true);
            } else if (componentUpdateEvent instanceof PostComponentUpdateEvent) {
                sawPostComponentUpdateEvent.set(true);
            }
            cdl.countDown();

        });
        fut.get();

        // Send a both component update events
        executor.submit(() -> {

            PreComponentUpdateEvent preComponentUpdateEvent = new PreComponentUpdateEvent();
            writeMessageToSockOutputStream(LifecycleServiceOpCodes.PRE_COMPONENT_UPDATE_EVENT.ordinal(),
                    preComponentUpdateEvent, FrameReader.FrameType.REQUEST);
            LifecycleResponseStatus ret = readMessageFromSockInputStream(FrameReader.readFrame(in), LifecycleResponseStatus.class);
            assertEquals(LifecycleResponseStatus.Success, ret);

            PostComponentUpdateEvent postComponentUpdateEvent = new PostComponentUpdateEvent();
            writeMessageToSockOutputStream(LifecycleServiceOpCodes.POST_COMPONENT_UPDATE_EVENT.ordinal(),
                    postComponentUpdateEvent, FrameReader.FrameType.REQUEST);
            ret = readMessageFromSockInputStream(FrameReader.readFrame(in), LifecycleResponseStatus.class);
            assertEquals(LifecycleResponseStatus.Success, ret);
            return null;
        });
        assertTrue(cdl.await(500, TimeUnit.MILLISECONDS));
        assertTrue(sawPreComponentUpdateEvent.get());
        assertTrue(sawPostComponentUpdateEvent.get());
    }

    @Test
    public void GIVEN_lifecycle_client_WHEN_listenToStateChange_and_disconnect_THEN_reconnect_and_called_for_each_state_change()
            throws Exception {
        Lifecycle lf = new LifecycleImpl(ipc);

        // validate subscribe request and response
        Future<?> fut = executor.submit(() -> {
            MessageFrame inFrame = FrameReader.readFrame(in);
            byte[] dummypayload = readMessageFromSockInputStream(inFrame, byte[].class);
            assertTrue(dummypayload.length == 0);

            LifecycleGenericResponse successResponse = SubscribeToComponentUpdatesResponse.builder()
                    .responseStatus(LifecycleResponseStatus.Success).build();
            writeMessageToSockOutputStream(1, inFrame.requestId, successResponse, FrameReader.FrameType.RESPONSE);
            return null;
        });

        CountDownLatch cdl = new CountDownLatch(2);
        lf.subscribeToComponentUpdate( componentUpdateEvent -> {
            cdl.countDown();
        });
        fut.get();

        // Send a component update event
        fut = executor.submit(() -> {
            PreComponentUpdateEvent preComponentUpdateEvent = new PreComponentUpdateEvent();
            writeMessageToSockOutputStream(LifecycleServiceOpCodes.PRE_COMPONENT_UPDATE_EVENT.ordinal(),
                    preComponentUpdateEvent, FrameReader.FrameType.REQUEST);
            LifecycleResponseStatus ret = readMessageFromSockInputStream(FrameReader.readFrame(in), LifecycleResponseStatus.class);
            assertEquals(LifecycleResponseStatus.Success, ret);
            return null;
        });
        fut.get();

        // Kill the connection to force a reconnect
        sock.close();
        // Wait for the client to reconnect
        while (connectionCount == 1) {
            Thread.sleep(10);
        }

        // Since the client reconnected we expect a call to listen again.
        // Setup the response to the listen request
        // then send another state STATE_TRANSITION
        fut = executor.submit(() -> {
            // Respond to listen request
            MessageFrame inFrame = FrameReader.readFrame(in);
            byte[] dummypayload = readMessageFromSockInputStream(inFrame, byte[].class);
            assertTrue(dummypayload.length == 0);
            LifecycleGenericResponse successResponse = SubscribeToComponentUpdatesResponse.builder()
                    .responseStatus(LifecycleResponseStatus.Success).build();
            writeMessageToSockOutputStream(1, inFrame.requestId, successResponse, FrameReader.FrameType.RESPONSE);

            PreComponentUpdateEvent preComponentUpdateEvent = new PreComponentUpdateEvent();
            writeMessageToSockOutputStream(LifecycleServiceOpCodes.PRE_COMPONENT_UPDATE_EVENT.ordinal(),
                    preComponentUpdateEvent, FrameReader.FrameType.REQUEST);
            LifecycleResponseStatus ret = readMessageFromSockInputStream(FrameReader.readFrame(in), LifecycleResponseStatus.class);
            assertEquals(LifecycleResponseStatus.Success, ret);
            return null;
        });
        fut.get();

        // Make sure the state STATE_TRANSITION handler got called twice despite the disconnection in between
        assertTrue(cdl.await(500, TimeUnit.MILLISECONDS));
    }
}
