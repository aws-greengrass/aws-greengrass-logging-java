package com.aws.iot.evergreen.ipc.common;

import com.aws.iot.evergreen.ipc.IPCClient;
import com.aws.iot.evergreen.ipc.IPCClientImpl;
import com.aws.iot.evergreen.ipc.config.KernelIPCClientConfig;
import com.aws.iot.evergreen.ipc.exceptions.IPCClientException;
import com.aws.iot.evergreen.ipc.services.auth.AuthResponse;
import com.aws.iot.evergreen.ipc.services.common.ApplicationMessage;
import com.aws.iot.evergreen.ipc.services.common.IPCUtil;
import com.aws.iot.evergreen.ipc.services.servicediscovery.ServiceDiscoveryImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.aws.iot.evergreen.ipc.common.FrameReader.readFrame;
import static com.aws.iot.evergreen.ipc.common.FrameReader.writeFrame;

public class BaseIPCTest {
    public ExecutorService executor = Executors.newCachedThreadPool();

    public IPCClient ipc;
    public Socket sock;
    public ServerSocket server;
    public DataInputStream in;
    public DataOutputStream out;

    public void writeMessageToSockOutputStream(int opCode, Integer requestId, Object data, FrameReader.FrameType type)
            throws Exception {
        ApplicationMessage transitionEventAppFrame = ApplicationMessage.builder()
                .version(ServiceDiscoveryImpl.API_VERSION).opCode(opCode)
                .payload(IPCUtil.encode(data)).build();

        int destination = BuiltInServiceDestinationCode.SERVICE_DISCOVERY.getValue();
        FrameReader.Message message = new FrameReader.Message(transitionEventAppFrame.toByteArray());
        FrameReader.MessageFrame messageFrame = requestId == null ?
                new FrameReader.MessageFrame(destination, message, type) :
                new FrameReader.MessageFrame(requestId, destination, message, type);
        writeFrame(messageFrame, out);
    }

    @BeforeEach
    public void before() throws IOException, InterruptedException, ExecutionException, IPCClientException {
        server = new ServerSocket(0);
        Future<Object> fut = executor.submit(() -> {
            sock = server.accept();
            in = new DataInputStream(sock.getInputStream());
            out = new DataOutputStream(sock.getOutputStream());

            // Read and write auth
            FrameReader.MessageFrame inFrame = readFrame(in);
            ApplicationMessage request = ApplicationMessage.fromBytes(inFrame.message.getPayload());
            AuthResponse authResponse = AuthResponse.builder().serviceName("ABC").clientId("test").build();

            ApplicationMessage response = ApplicationMessage.builder().version(request.getVersion())
                    .payload(IPCUtil.encode(authResponse)).build();
            writeFrame(new FrameReader.MessageFrame(inFrame.requestId, BuiltInServiceDestinationCode.AUTH.getValue(),
                    new FrameReader.Message(response.toByteArray()), FrameReader.FrameType.RESPONSE), out);
            return null;
        });

        ipc = new IPCClientImpl(KernelIPCClientConfig.builder().port(server.getLocalPort()).build());
        fut.get();
    }

    @AfterEach
    public void after() throws IOException {
        ipc.disconnect();
        sock.close();
        server.close();
    }
}
