/*
 * Copyright Amazon.com Inc. or its affiliates.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.ipc.services.pubsub;

import com.aws.iot.evergreen.ipc.IPCClient;
import com.aws.iot.evergreen.ipc.IPCClientImpl;
import com.aws.iot.evergreen.ipc.common.BuiltInServiceDestinationCode;
import com.aws.iot.evergreen.ipc.common.FrameReader;
import com.aws.iot.evergreen.ipc.common.FrameReader.MessageFrame;
import com.aws.iot.evergreen.ipc.config.KernelIPCClientConfig;
import com.aws.iot.evergreen.ipc.exceptions.IPCClientException;
import com.aws.iot.evergreen.ipc.services.auth.AuthResponse;
import com.aws.iot.evergreen.ipc.services.common.ApplicationMessage;
import com.aws.iot.evergreen.ipc.services.common.IPCUtil;
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
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.aws.iot.evergreen.ipc.common.FrameReader.readFrame;
import static com.aws.iot.evergreen.ipc.common.FrameReader.writeFrame;
import static com.aws.iot.evergreen.ipc.lifecycle.LifecycleIPCTest.readMessageFromSockInputStream;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class PubSubIPCTest {
    private final ExecutorService executor = Executors.newCachedThreadPool();

    private IPCClient ipc;
    private Socket sock;
    private ServerSocket server;
    private DataInputStream in;
    private DataOutputStream out;
    private int connectionCount = 0;

    private void writeMessageToSockOutputStream(int opCode, Integer requestId, Object data, FrameReader.FrameType type)
            throws Exception {
        ApplicationMessage transitionEventAppFrame =
                ApplicationMessage.builder().version(PubSubImpl.API_VERSION).opCode(opCode)
                        .payload(IPCUtil.encode(data)).build();

        int destination = BuiltInServiceDestinationCode.PUBSUB.getValue();
        FrameReader.Message message = new FrameReader.Message(transitionEventAppFrame.toByteArray());
        MessageFrame messageFrame = requestId == null ? new MessageFrame(destination, message, type)
                : new MessageFrame(requestId, destination, message, type);
        FrameReader.writeFrame(messageFrame, out);
    }


    @BeforeEach
    public void before() throws IOException, InterruptedException, IPCClientException {
        server = new ServerSocket(0);
        connectionCount = 0;
        executor.submit(() -> {
            while (true) {
                sock = server.accept();
                in = new DataInputStream(sock.getInputStream());
                out = new DataOutputStream(sock.getOutputStream());

                // Read and write auth
                MessageFrame inFrame = readFrame(in);
                ApplicationMessage requestApplicationFrame = ApplicationMessage.fromBytes(inFrame.message.getPayload());
                AuthResponse authResponse = AuthResponse.builder().serviceName("ABC").clientId("test").build();
                ApplicationMessage responsesAppFrame =
                        ApplicationMessage.builder().version(requestApplicationFrame.getVersion())
                                .payload(IPCUtil.encode(authResponse)).build();

                writeFrame(new MessageFrame(inFrame.requestId, BuiltInServiceDestinationCode.AUTH.getValue(),
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
    public void GIVEN_pubsub_client_WHEN_subscribe_THEN_called_for_each_message() throws Exception {
        PubSub pubSub = new PubSubImpl(ipc);

        // validate subscribe request and respond success
        Future<?> fut = executor.submit(() -> {
            MessageFrame inFrame = FrameReader.readFrame(in);
            PubSubSubscribeRequest subscribeRequest =
                    readMessageFromSockInputStream(inFrame, PubSubSubscribeRequest.class);

            PubSubGenericResponse successResponse = new PubSubGenericResponse(PubSubResponseStatus.Success, null);
            writeMessageToSockOutputStream(1, inFrame.requestId, successResponse, FrameReader.FrameType.RESPONSE);
            return null;
        });

        CountDownLatch cdl = new CountDownLatch(1);
        pubSub.subscribeToTopic("a", (payload) -> {
            assertEquals("a", new String(payload, StandardCharsets.UTF_8));
            cdl.countDown();
        });
        fut.get();

        // Send a PUBLISHED
        fut = executor.submit(() -> {
            MessagePublishedEvent valueChangedEvent =
                    MessagePublishedEvent.builder().topic("a").payload("a".getBytes(StandardCharsets.UTF_8)).build();
            writeMessageToSockOutputStream(PubSubServiceOpCodes.PUBLISHED.ordinal(), null, valueChangedEvent,
                    FrameReader.FrameType.REQUEST);
            PubSubResponseStatus ret =
                    readMessageFromSockInputStream(FrameReader.readFrame(in), PubSubResponseStatus.class);
            assertEquals(PubSubResponseStatus.Success, ret);
            return null;
        });
        fut.get();
        assertTrue(cdl.await(500, TimeUnit.MILLISECONDS));
    }

    @Test
    public void GIVEN_pubsub_client_WHEN_subscribe_and_disconnect_THEN_reconnect_and_called_for_each_change()
            throws Exception {
        PubSub pubSub = new PubSubImpl(ipc);

        // validate subscribe request and respond success
        Future<?> fut = executor.submit(() -> {
            MessageFrame inFrame = FrameReader.readFrame(in);
            PubSubSubscribeRequest subscribeRequest =
                    readMessageFromSockInputStream(inFrame, PubSubSubscribeRequest.class);

            PubSubGenericResponse successResponse = new PubSubGenericResponse(PubSubResponseStatus.Success, null);
            writeMessageToSockOutputStream(1, inFrame.requestId, successResponse, FrameReader.FrameType.RESPONSE);
            return null;
        });

        CountDownLatch cdl = new CountDownLatch(2);
        pubSub.subscribeToTopic("a", (payload) -> {
            if (cdl.getCount() == 2) {
                assertEquals("a", new String(payload, StandardCharsets.UTF_8));
            } else {
                assertEquals("b", new String(payload, StandardCharsets.UTF_8));
            }
            cdl.countDown();
        });
        fut.get();


        // Send a PUBLISHED
        fut = executor.submit(() -> {
            MessagePublishedEvent valueChangedEvent =
                    MessagePublishedEvent.builder().topic("a").payload("a".getBytes(StandardCharsets.UTF_8)).build();
            writeMessageToSockOutputStream(PubSubServiceOpCodes.PUBLISHED.ordinal(), null, valueChangedEvent,
                    FrameReader.FrameType.REQUEST);
            PubSubResponseStatus ret =
                    readMessageFromSockInputStream(FrameReader.readFrame(in), PubSubResponseStatus.class);
            assertEquals(PubSubResponseStatus.Success, ret);
            return null;
        });
        fut.get();


        // Kill the connection to force a reconnect
        sock.close();
        // Wait for the client to reconnect
        while (connectionCount == 1) {
            Thread.sleep(10);
        }

        // Since the client reconnected we expect a call to subscribe again.
        // Setup the response to the subscribe request
        // then send another state PUBLISHED
        fut = executor.submit(() -> {
            // Respond to subscribe request
            MessageFrame inFrame = FrameReader.readFrame(in);
            PubSubSubscribeRequest subscribeRequest =
                    readMessageFromSockInputStream(inFrame, PubSubSubscribeRequest.class);

            PubSubGenericResponse successResponse = new PubSubGenericResponse(PubSubResponseStatus.Success, null);
            writeMessageToSockOutputStream(1, inFrame.requestId, successResponse, FrameReader.FrameType.RESPONSE);

            // Make a PUBLISHED request and send it
            MessagePublishedEvent valueChangedEvent =
                    MessagePublishedEvent.builder().topic("a").payload("b".getBytes(StandardCharsets.UTF_8)).build();
            writeMessageToSockOutputStream(PubSubServiceOpCodes.PUBLISHED.ordinal(), null, valueChangedEvent,
                    FrameReader.FrameType.REQUEST);
            PubSubResponseStatus ret =
                    readMessageFromSockInputStream(FrameReader.readFrame(in), PubSubResponseStatus.class);
            assertEquals(PubSubResponseStatus.Success, ret);
            return null;
        });
        fut.get();

        // Make sure the PUBLISHED handler got called twice despite the disconnection in between
        assertTrue(cdl.await(500, TimeUnit.MILLISECONDS));
    }
}
