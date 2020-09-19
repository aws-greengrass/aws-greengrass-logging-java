/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.greengrass.ipc;

import com.aws.greengrass.ipc.common.BuiltInServiceDestinationCode;
import com.aws.greengrass.ipc.common.FrameReader;
import com.aws.greengrass.ipc.config.KernelIPCClientConfig;
import com.aws.greengrass.ipc.services.authentication.AuthenticationResponse;
import com.aws.greengrass.ipc.services.common.ApplicationMessage;
import com.aws.greengrass.ipc.services.common.IPCUtil;
import com.aws.greengrass.logging.impl.Slf4jLogAdapter;
import org.junit.jupiter.api.Test;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.aws.greengrass.ipc.IPCClientImpl.MAX_CONNECTION_ATTEMPTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IPCReconnectTest {
    private ExecutorService executor = Executors.newCachedThreadPool();

    private IPCClientImpl ipc;
    private Socket sock;
    private ServerSocket server;
    private DataInputStream in;
    private DataOutputStream out;

    @Test
    void GIVEN_ipc_client_WHEN_server_dies_THEN_client_reconnects() throws Exception {
        AtomicInteger connectCount = new AtomicInteger();
        // First setup the server for connections
        server = new ServerSocket(0);
        executor.submit(() -> {
            while (true) {
                sock = server.accept();
                in = new DataInputStream(sock.getInputStream());
                out = new DataOutputStream(sock.getOutputStream());

                // Read and write authentication
                FrameReader.MessageFrame inFrame = FrameReader.readFrame(in);


                ApplicationMessage requestApplicationFrame = ApplicationMessage.fromBytes(inFrame.message.getPayload());
                AuthenticationResponse authenticationResponse = AuthenticationResponse.builder().serviceName("ABC").clientId("test").build();
                ApplicationMessage responsesAppFrame = ApplicationMessage.builder()
                        .version(requestApplicationFrame.getVersion())
                        .payload(IPCUtil.encode(authenticationResponse)).build();

                FrameReader.writeFrame(
                        new FrameReader.MessageFrame(inFrame.requestId, BuiltInServiceDestinationCode.AUTHENTICATION.getValue(),
                                new FrameReader.Message(responsesAppFrame.toByteArray()), FrameReader.FrameType.RESPONSE), out);
                connectCount.getAndIncrement();
            }
        });

        ipc = new IPCClientImpl(KernelIPCClientConfig.builder().port(server.getLocalPort()).build());
        while (connectCount.get() == 0) {
            Thread.sleep(1);
        }


        Future<?> fut = executor.submit(() -> {
            FrameReader.MessageFrame inFrame = FrameReader.readFrame(in);
            FrameReader.writeFrame(
                    new FrameReader.MessageFrame(inFrame.requestId, 254, new FrameReader.Message(new byte[0]),
                            FrameReader.FrameType.RESPONSE), out);
            return null;
        });

        ipc.sendRequest(254, new FrameReader.Message(new byte[0])).get(100, TimeUnit.MILLISECONDS);

        fut.get();

        // Kill the socket connection
        sock.close();

        // Wait for the client to reconnect
        while (connectCount.get() == 1) {
            Thread.sleep(2);
        }

        fut = executor.submit(() -> {
            FrameReader.MessageFrame inFrame = FrameReader.readFrame(in);
            FrameReader.writeFrame(
                    new FrameReader.MessageFrame(inFrame.requestId, 254, new FrameReader.Message(new byte[0]),
                            FrameReader.FrameType.RESPONSE), out);
            return null;
        });

        // Wait for the client to re-authenticate
        while (!ipc.isConnectedAndAuthenticated()) {
            Thread.sleep(2);
        }

        ipc.sendRequest(254, new FrameReader.Message(new byte[0])).get(100, TimeUnit.MILLISECONDS);

        fut.get();

        assertEquals(2, connectCount.get());

        ipc.disconnect();
        sock.close();
        server.close();
    }

    @Test
    void GIVEN_unconnected_ipc_client_WHEN_connect_fails_THEN_client_fails_to_connect() throws Exception {
        server = new ServerSocket(0);
        server.close();
        CountDownLatch reconnectLatch = new CountDownLatch(MAX_CONNECTION_ATTEMPTS);
        Slf4jLogAdapter.addGlobalListener(logEvent->{
            if (logEvent.getMessage().equals("Retrying to connect")) {
                reconnectLatch.countDown();
            }
        });
        assertThrows(IOException.class,
                () -> new IPCClientImpl(KernelIPCClientConfig.builder().port(server.getLocalPort()).build()));
        assertTrue(reconnectLatch.await(2, TimeUnit.SECONDS));
    }
}
