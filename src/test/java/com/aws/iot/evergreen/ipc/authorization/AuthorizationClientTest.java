package com.aws.iot.evergreen.ipc.authorization;

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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AuthorizationClientTest {

    private final ExecutorService executor = Executors.newCachedThreadPool();

    private IPCClient ipc;
    private Socket sock;
    private ServerSocket server;
    private DataInputStream in;
    private DataOutputStream out;
    private int connectionCount = 0;

    private AuthorizationClient authorizationClient;

    private void writeMessageToSockOutputStream(int opCode, Integer requestId, Object data, FrameReader.FrameType type)
            throws Exception {
        ApplicationMessage transitionEventAppFrame =
                ApplicationMessage.builder().version(AuthorizationClient.AUTHORIZATION_API_VERSION).opCode(opCode)
                        .payload(IPCUtil.encode(data)).build();

        int destination = BuiltInServiceDestinationCode.AUTHORIZATION.getValue();
        FrameReader.Message message = new FrameReader.Message(transitionEventAppFrame.toByteArray());
        MessageFrame messageFrame = requestId == null ? new MessageFrame(destination, message, type)
                : new MessageFrame(requestId, destination, message, type);
        writeFrame(messageFrame, out);
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
    public void GIVEN_authorized_token_WHEN_validating_token_THEN_succeed() throws AuthorizationException, ExecutionException, InterruptedException {

        authorizationClient = new AuthorizationClient(ipc);

        Future<?> fut = executor.submit(() -> {
            MessageFrame inFrame = FrameReader.readFrame(in);
            AuthorizationResponse successResponse = new AuthorizationResponse(true, null);
            writeMessageToSockOutputStream(1, inFrame.requestId, successResponse, FrameReader.FrameType.RESPONSE);
            return null;
        });

        AuthorizationResponse response = authorizationClient.validateToken("fakeToken");
        assertTrue(response.isAuthorized());
        assertNull(response.getErrorMessage());
    }

    @Test
    public void GIVEN_unauthorized_token_WHEN_validating_token_THEN_throws() {

        authorizationClient = new AuthorizationClient(ipc);

        Future<?> fut = executor.submit(() -> {
            MessageFrame inFrame = FrameReader.readFrame(in);
            AuthorizationResponse successResponse = new AuthorizationResponse(false, "unauthorized token");
            writeMessageToSockOutputStream(1, inFrame.requestId, successResponse, FrameReader.FrameType.RESPONSE);
            return null;
        });

        final AuthorizationException ex
                = assertThrows(AuthorizationException.class, () -> authorizationClient.validateToken("fakeToken"));
        assertTrue(ex.getMessage().contains("unauthorized token"));
    }

    @Test
    public void GIVEN_null_token_WHEN_validating_token_THEN_throws() {

        authorizationClient = new AuthorizationClient(ipc);

        final AuthorizationException ex
                = assertThrows(AuthorizationException.class, () -> authorizationClient.validateToken(null));
        assertTrue(ex.getMessage().contains("Provided auth token is null"));
    }

    @Test
    public void GIVEN_empty_token_WHEN_validating_token_THEN_throws() {

        authorizationClient = new AuthorizationClient(ipc);

        final AuthorizationException ex
                = assertThrows(AuthorizationException.class, () -> authorizationClient.validateToken(""));
        assertTrue(ex.getMessage().contains("Provided auth token is empty"));
    }
}
