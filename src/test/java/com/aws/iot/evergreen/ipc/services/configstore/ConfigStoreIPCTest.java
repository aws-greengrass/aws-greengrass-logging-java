/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.ipc.services.configstore;

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
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.aws.iot.evergreen.ipc.common.FrameReader.readFrame;
import static com.aws.iot.evergreen.ipc.common.FrameReader.writeFrame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class ConfigStoreIPCTest {
    private final ExecutorService executor = Executors.newCachedThreadPool();

    private IPCClient ipc;
    private Socket sock;
    private ServerSocket server;
    private DataInputStream in;
    private DataOutputStream out;
    private int connectionCount = 0;

    public <T> T readMessageFromSockInputStream(final MessageFrame inFrame, final Class<T> returnTypeClass)
            throws Exception {
        ApplicationMessage reqAppFrame = ApplicationMessage.fromBytes(inFrame.message.getPayload());
        return IPCUtil.decode(reqAppFrame.getPayload(), returnTypeClass);
    }

    public void writeMessageToSockOutputStream(int opCode, Integer requestId, Object data, FrameReader.FrameType type)
            throws Exception {
        ApplicationMessage transitionEventAppFrame =
                ApplicationMessage.builder().version(ConfigStoreImpl.API_VERSION).opCode(opCode)
                        .payload(IPCUtil.encode(data)).build();

        int destination = BuiltInServiceDestinationCode.CONFIG_STORE.getValue();
        FrameReader.Message message = new FrameReader.Message(transitionEventAppFrame.toByteArray());
        MessageFrame messageFrame = requestId == null ? new MessageFrame(destination, message, type)
                : new MessageFrame(requestId, destination, message, type);
        FrameReader.writeFrame(messageFrame, out);
    }

    public void writeMessageToSockOutputStream(int opCode, Object data, FrameReader.FrameType type) throws Exception {
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
    public void GIVEN_configstore_client_WHEN_subscribe_THEN_called_for_each_change() throws Exception {
        ConfigStore configStore = new ConfigStoreImpl(ipc);

        // validate subscribe request and respond success
        Future<?> fut = executor.submit(() -> {
            MessageFrame inFrame = FrameReader.readFrame(in);
            ConfigStoreSubscribeRequest subscribeRequest =
                    readMessageFromSockInputStream(inFrame, ConfigStoreSubscribeRequest.class);

            ConfigStoreGenericResponse successResponse =
                    ConfigStoreGenericResponse.builder().status(ConfigStoreResponseStatus.Success).build();
            writeMessageToSockOutputStream(1, inFrame.requestId, successResponse, FrameReader.FrameType.RESPONSE);
            return null;
        });

        HashMap<String, Object> newValue = new HashMap<String, Object>() {{
            put("a", "A");
        }};
        CountDownLatch cdl = new CountDownLatch(1);
        configStore.subscribe((newState) -> {
            assertEquals(newValue, newState);
            cdl.countDown();
        });
        fut.get();

        // Send a VALUE_CHANGED
        fut = executor.submit(() -> {
            ConfigValueChangedEvent valueChangedEvent = ConfigValueChangedEvent.builder().newValue(newValue).build();
            writeMessageToSockOutputStream(ConfigStoreServiceOpCodes.VALUE_CHANGED.ordinal(), valueChangedEvent,
                    FrameReader.FrameType.REQUEST);
            ConfigStoreResponseStatus ret =
                    readMessageFromSockInputStream(FrameReader.readFrame(in), ConfigStoreResponseStatus.class);
            assertEquals(ConfigStoreResponseStatus.Success, ret);
            return null;
        });
        fut.get();
        assertTrue(cdl.await(500, TimeUnit.MILLISECONDS));
    }

    @Test
    public void GIVEN_configstore_client_WHEN_subscribe_and_disconnect_THEN_reconnect_and_called_for_each_change()
            throws Exception {
        ConfigStore configStore = new ConfigStoreImpl(ipc);

        // validate subscribe request and respond success
        Future<?> fut = executor.submit(() -> {
            MessageFrame inFrame = FrameReader.readFrame(in);
            ConfigStoreSubscribeRequest subscribeRequest =
                    readMessageFromSockInputStream(inFrame, ConfigStoreSubscribeRequest.class);

            ConfigStoreGenericResponse successResponse =
                    ConfigStoreGenericResponse.builder().status(ConfigStoreResponseStatus.Success).build();
            writeMessageToSockOutputStream(1, inFrame.requestId, successResponse, FrameReader.FrameType.RESPONSE);
            return null;
        });

        HashMap<String, Object> newValue = new HashMap<String, Object>() {{
            put("a", "A");
        }};
        CountDownLatch cdl = new CountDownLatch(2);
        configStore.subscribe((newState) -> {
            assertEquals(newValue, newState);
            cdl.countDown();
        });
        fut.get();


        // Send a VALUE_CHANGED
        fut = executor.submit(() -> {
            ConfigValueChangedEvent valueChangedEvent = ConfigValueChangedEvent.builder().newValue(newValue).build();
            writeMessageToSockOutputStream(ConfigStoreServiceOpCodes.VALUE_CHANGED.ordinal(), valueChangedEvent,
                    FrameReader.FrameType.REQUEST);
            ConfigStoreResponseStatus ret =
                    readMessageFromSockInputStream(FrameReader.readFrame(in), ConfigStoreResponseStatus.class);
            assertEquals(ConfigStoreResponseStatus.Success, ret);
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
        // then send another state VALUE_CHANGED
        fut = executor.submit(() -> {
            // Respond to subscribe request
            MessageFrame inFrame = FrameReader.readFrame(in);
            ConfigStoreSubscribeRequest subscribeRequest =
                    readMessageFromSockInputStream(inFrame, ConfigStoreSubscribeRequest.class);

            ConfigStoreGenericResponse successResponse =
                    ConfigStoreGenericResponse.builder().status(ConfigStoreResponseStatus.Success).build();
            writeMessageToSockOutputStream(1, inFrame.requestId, successResponse, FrameReader.FrameType.RESPONSE);

            // Make a state VALUE_CHANGED request and send it
            ConfigValueChangedEvent valueChangedEvent = ConfigValueChangedEvent.builder().newValue(newValue).build();
            writeMessageToSockOutputStream(ConfigStoreServiceOpCodes.VALUE_CHANGED.ordinal(), valueChangedEvent,
                    FrameReader.FrameType.REQUEST);
            ConfigStoreResponseStatus ret =
                    readMessageFromSockInputStream(FrameReader.readFrame(in), ConfigStoreResponseStatus.class);
            assertEquals(ConfigStoreResponseStatus.Success, ret);
            return null;
        });
        fut.get();

        // Make sure the state VALUE_CHANGED handler got called twice despite the disconnection in between
        assertTrue(cdl.await(500, TimeUnit.MILLISECONDS));
    }
}
