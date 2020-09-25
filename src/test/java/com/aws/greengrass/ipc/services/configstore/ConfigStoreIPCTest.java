/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.greengrass.ipc.services.configstore;

import com.aws.greengrass.ipc.IPCClient;
import com.aws.greengrass.ipc.IPCClientImpl;
import com.aws.greengrass.ipc.common.BuiltInServiceDestinationCode;
import com.aws.greengrass.ipc.common.FrameReader;
import com.aws.greengrass.ipc.common.FrameReader.MessageFrame;
import com.aws.greengrass.ipc.config.KernelIPCClientConfig;
import com.aws.greengrass.ipc.exceptions.IPCClientException;
import com.aws.greengrass.ipc.services.authentication.AuthenticationResponse;
import com.aws.greengrass.ipc.services.common.ApplicationMessage;
import com.aws.greengrass.ipc.services.common.IPCUtil;
import org.hamcrest.collection.IsMapContaining;
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
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.aws.greengrass.ipc.common.FrameReader.readFrame;
import static com.aws.greengrass.ipc.common.FrameReader.writeFrame;
import static com.aws.greengrass.ipc.lifecycle.LifecycleIPCTest.readMessageFromSockInputStream;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    private void writeMessageToSockOutputStream(int opCode, Integer requestId, Object data, FrameReader.FrameType type)
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


    @BeforeEach
    public void before() throws IOException, InterruptedException, IPCClientException {
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
                ApplicationMessage responsesAppFrame =
                        ApplicationMessage.builder().version(requestApplicationFrame.getVersion())
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
    public void GIVEN_configstore_client_WHEN_subscribe_THEN_called_for_each_change() throws Exception {
        ConfigStore configStore = new ConfigStoreImpl(ipc);

        // validate subscribe request and respond success
        Future<?> fut = executor.submit(() -> {
            MessageFrame inFrame = FrameReader.readFrame(in);
            SubscribeToConfigurationUpdateRequest subscribeRequest =
                    readMessageFromSockInputStream(inFrame, SubscribeToConfigurationUpdateRequest.class);
            assertEquals("ABC", subscribeRequest.getComponentName());
            assertNull(subscribeRequest.getKeyPath());

            ConfigStoreGenericResponse successResponse =
                    new ConfigStoreGenericResponse(ConfigStoreResponseStatus.Success, null);
            writeMessageToSockOutputStream(1, inFrame.requestId, successResponse, FrameReader.FrameType.RESPONSE);
            return null;
        });

        CountDownLatch cdl = new CountDownLatch(1);
        configStore.subscribeToConfigurationUpdate("ABC", null, (changedKey) -> {
            assertEquals(Collections.singletonList("a"), changedKey);
            cdl.countDown();
        });
        fut.get();

        // Send a KEY_CHANGED
        fut = executor.submit(() -> {
            ConfigurationUpdateEvent valueChangedEvent = ConfigurationUpdateEvent.builder().componentName("ABC")
                    .changedKeyPath(Collections.singletonList("a")).build();
            writeMessageToSockOutputStream(ConfigStoreServiceOpCodes.KEY_CHANGED.ordinal(), null, valueChangedEvent,
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
    public void GIVEN_configstore_client_WHEN_read_THEN_response_has_value() throws Exception {
        ConfigStore configStore = new ConfigStoreImpl(ipc);

        // validate read request and respond success
        Future<?> fut = executor.submit(() -> {
            MessageFrame inFrame = FrameReader.readFrame(in);
            GetConfigurationRequest readRequest =
                    readMessageFromSockInputStream(inFrame, GetConfigurationRequest.class);

            assertEquals(Collections.singletonList("key"), readRequest.getKeyPath());

            GetConfigurationResponse successResponse =
                    GetConfigurationResponse.builder().value("ABC").responseStatus(ConfigStoreResponseStatus.Success)
                            .build();
            writeMessageToSockOutputStream(1, inFrame.requestId, successResponse, FrameReader.FrameType.RESPONSE);
            return null;
        });

        Object val = configStore.getConfiguration("SomeService", Collections.singletonList("key"));
        fut.get();
        assertEquals("ABC", val);
    }

    @Test
    public void GIVEN_subscribed_to_validate_config_WHEN_validation_event_sent_THEN_client_receives_event()
            throws Exception {
        ConfigStore configStore = new ConfigStoreImpl(ipc);

        // handle subscribe request
        Future<?> fut = executor.submit(() -> {
            MessageFrame inFrame = FrameReader.readFrame(in);
            SubscribeToValidateConfigurationRequest request =
                    readMessageFromSockInputStream(inFrame, SubscribeToValidateConfigurationRequest.class);

            ConfigStoreGenericResponse successResponse =
                    new ConfigStoreGenericResponse(ConfigStoreResponseStatus.Success, null);
            writeMessageToSockOutputStream(1, inFrame.requestId, successResponse, FrameReader.FrameType.RESPONSE);
            return null;
        });

        CountDownLatch cdl = new CountDownLatch(1);
        configStore.subscribeToValidateConfiguration(configToValidate -> {
            assertThat(configToValidate, IsMapContaining.hasEntry("key", "value"));
            cdl.countDown();
        });
        fut.get();

        // Send a VALIDATION_EVENT
        fut = executor.submit(() -> {
            ValidateConfigurationUpdateEvent validateEvent =
                    ValidateConfigurationUpdateEvent.builder().configuration(Collections.singletonMap("key", "value"))
                            .build();
            writeMessageToSockOutputStream(ConfigStoreServiceOpCodes.VALIDATION_EVENT.ordinal(), null, validateEvent,
                    FrameReader.FrameType.REQUEST);
            ConfigStoreResponseStatus ret =
                    readMessageFromSockInputStream(FrameReader.readFrame(in), ConfigStoreResponseStatus.class);
            assertEquals(ConfigStoreResponseStatus.Success, ret);
            return null;
        });
        fut.get();
        assertTrue(cdl.await(500, TimeUnit.MILLISECONDS));
    }
}
