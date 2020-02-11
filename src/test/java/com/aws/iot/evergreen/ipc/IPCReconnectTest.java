package com.aws.iot.evergreen.ipc;

import com.aws.iot.evergreen.ipc.common.FrameReader;
import com.aws.iot.evergreen.ipc.common.GenericErrorCodes;
import com.aws.iot.evergreen.ipc.config.KernelIPCClientConfig;
import com.aws.iot.evergreen.ipc.services.common.GeneralResponse;
import com.aws.iot.evergreen.ipc.services.common.IPCUtil;
import org.junit.jupiter.api.Test;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.aws.iot.evergreen.ipc.common.Constants.AUTH_SERVICE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class IPCReconnectTest {
    private ExecutorService executor = Executors.newCachedThreadPool();

    private IPCClientImpl ipc;
    private Socket sock;
    private ServerSocket server;
    private DataInputStream in;
    private DataOutputStream out;

    @Test
    public void GIVEN_ipc_client_WHEN_server_dies_THEN_client_reconnects() throws Exception {
        AtomicInteger connectCount = new AtomicInteger();
        // First setup the server for connections
        server = new ServerSocket(0);
        executor.submit(() -> {
            while (true) {
                sock = server.accept();
                in = new DataInputStream(sock.getInputStream());
                out = new DataOutputStream(sock.getOutputStream());

                // Read and write auth
                FrameReader.MessageFrame inFrame = FrameReader.readFrame(in);
                FrameReader.writeFrame(new FrameReader.MessageFrame(inFrame.sequenceNumber, AUTH_SERVICE,
                        new FrameReader.Message(
                                IPCUtil.encode(GeneralResponse.builder().response("ABC").error(GenericErrorCodes.Success).build())),
                        FrameReader.FrameType.RESPONSE), out);
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
                    new FrameReader.MessageFrame(inFrame.sequenceNumber, "D", new FrameReader.Message(new byte[0]),
                            FrameReader.FrameType.RESPONSE), out);
            return null;
        });

        ipc.sendRequest("D", new FrameReader.Message(new byte[0])).get(100, TimeUnit.MILLISECONDS);

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
                    new FrameReader.MessageFrame(inFrame.sequenceNumber, "D", new FrameReader.Message(new byte[0]),
                            FrameReader.FrameType.RESPONSE), out);
            return null;
        });

        // Wait for the client to re-auth
        while (!ipc.isConnectedAndAuthenticated()) {
            Thread.sleep(2);
        }

        ipc.sendRequest("D", new FrameReader.Message(new byte[0])).get(100, TimeUnit.MILLISECONDS);

        fut.get();

        assertEquals(2, connectCount.get());

        ipc.disconnect();
        sock.close();
        server.close();
    }

    @Test
    public void GIVEN_unconnected_ipc_client_WHEN_connect_fails_THEN_client_fails_to_connect() throws Exception {
        server = new ServerSocket(0);
        server.close();

        assertThrows(IOException.class,
                () -> new IPCClientImpl(KernelIPCClientConfig.builder().port(server.getLocalPort()).build()));
    }
}