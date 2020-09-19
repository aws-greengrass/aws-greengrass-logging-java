package com.aws.greengrass.ipc.common;

import com.aws.greengrass.ipc.IPCClient;
import com.aws.greengrass.ipc.IPCClientImpl;
import com.aws.greengrass.ipc.config.KernelIPCClientConfig;
import com.aws.greengrass.ipc.exceptions.IPCClientException;
import com.aws.greengrass.ipc.services.authentication.AuthenticationResponse;
import com.aws.greengrass.ipc.services.common.ApplicationMessage;
import com.aws.greengrass.ipc.services.common.IPCUtil;
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

import static com.aws.greengrass.ipc.common.FrameReader.readFrame;
import static com.aws.greengrass.ipc.common.FrameReader.writeFrame;

public class BaseIPCTest {
    public ExecutorService executor = Executors.newCachedThreadPool();

    public IPCClient ipc;
    public Socket sock;
    public ServerSocket server;
    public DataInputStream in;
    public DataOutputStream out;

    public void writeMessageToSockOutputStream(int opCode,
                                               Integer requestId,
                                               Object data,
                                               FrameReader.FrameType type,
                                               int destination,
                                               int version)
            throws Exception {
        ApplicationMessage transitionEventAppFrame = ApplicationMessage.builder()
                .version(version).opCode(opCode)
                .payload(IPCUtil.encode(data)).build();

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
            AuthenticationResponse authenticationResponse = AuthenticationResponse.builder().serviceName("ABC").clientId("test").build();

            ApplicationMessage response = ApplicationMessage.builder().version(request.getVersion())
                    .payload(IPCUtil.encode(authenticationResponse)).build();
            writeFrame(new FrameReader.MessageFrame(inFrame.requestId, BuiltInServiceDestinationCode.AUTHENTICATION.getValue(),
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
