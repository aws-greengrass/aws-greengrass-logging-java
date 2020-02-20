package com.aws.iot.evergreen.ipc.lifecycle;

import com.aws.iot.evergreen.ipc.IPCClient;
import com.aws.iot.evergreen.ipc.IPCClientImpl;
import com.aws.iot.evergreen.ipc.common.BuiltInServiceDestinationCode;
import com.aws.iot.evergreen.ipc.common.FrameReader;
import com.aws.iot.evergreen.ipc.common.FrameReader.MessageFrame;
import com.aws.iot.evergreen.ipc.config.KernelIPCClientConfig;
import com.aws.iot.evergreen.ipc.services.auth.AuthResponse;
import com.aws.iot.evergreen.ipc.services.common.ApplicationMessage;
import com.aws.iot.evergreen.ipc.services.common.IPCUtil;
import com.aws.iot.evergreen.ipc.services.lifecycle.*;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.fasterxml.jackson.jr.ob.JSON;
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
import java.util.concurrent.*;

import static com.aws.iot.evergreen.ipc.common.FrameReader.readFrame;
import static com.aws.iot.evergreen.ipc.common.FrameReader.writeFrame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class LifecycleIPCTest {
    private JSON encoder = JSON.std.with(new CBORFactory());
    private ExecutorService executor = Executors.newCachedThreadPool();

    private IPCClient ipc;
    private Socket sock;
    private ServerSocket server;
    private DataInputStream in;
    private DataOutputStream out;
    private int connectionCount = 0;


    public <T> T readMessageFromSockInputStream(final MessageFrame inFrame, final Class<T> returnTypeClass) throws Exception {
        ApplicationMessage reqAppFrame = new ApplicationMessage(inFrame.message.getPayload());
        return IPCUtil.decode(reqAppFrame.getPayload(), returnTypeClass);
    }

    public void writeMessageToSockOutputStream(int opCode, Integer requestId, Object data, FrameReader.FrameType type) throws Exception {
        ApplicationMessage transitionEventAppFrame = ApplicationMessage.builder()
                .version(LifecycleImpl.API_VERSION).opCode(opCode).payload(IPCUtil.encode(data)).build();

        int destination = BuiltInServiceDestinationCode.LIFECYCLE.getValue();
        FrameReader.Message message = new FrameReader.Message(transitionEventAppFrame.toByteArray());
        MessageFrame messageFrame = requestId == null ?
                new MessageFrame(destination, message, type) :
                new MessageFrame(requestId, destination, message, type);
        FrameReader.writeFrame(messageFrame, out);
    }

    public void writeMessageToSockOutputStream(int opCode, Object data, FrameReader.FrameType type) throws Exception {
        writeMessageToSockOutputStream(opCode, null, data, type);
    }


    @BeforeEach
    public void before() throws IOException, InterruptedException, ExecutionException {
        server = new ServerSocket(0);
        connectionCount = 0;
        executor.submit(() -> {
            while (true) {
                sock = server.accept();
                in = new DataInputStream(sock.getInputStream());
                out = new DataOutputStream(sock.getOutputStream());

                // Read and write auth
                MessageFrame inFrame = readFrame(in);
                ApplicationMessage requestApplicationFrame = new ApplicationMessage(inFrame.message.getPayload());
                AuthResponse authResponse = AuthResponse.builder().serviceName("ABC").clientId("test").build();
                ApplicationMessage responsesAppFrame = ApplicationMessage.builder()
                        .version(requestApplicationFrame.getVersion()).payload(IPCUtil.encode(authResponse)).build();

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
    public void GIVEN_lifecycle_client_WHEN_requestStateChange_THEN_server_gets_request() throws Exception {
        Lifecycle lf = new LifecycleImpl(ipc);

        Future<?> fut = executor.submit(() -> {
            MessageFrame inFrame = readFrame(in);
            StateChangeRequest stateChangeRequest = readMessageFromSockInputStream(inFrame, StateChangeRequest.class);
            assertEquals("Errored", stateChangeRequest.getState());

            LifecycleGenericResponse lifeCycleGenericResponse = LifecycleGenericResponse.builder().status(LifecycleResponseStatus.Success).build();
            writeMessageToSockOutputStream(1, inFrame.requestId, lifeCycleGenericResponse, FrameReader.FrameType.RESPONSE);
            return null;
        });

        lf.reportState("Errored");
        fut.get();
    }

    @Test
    public void GIVEN_lifecycle_client_WHEN_listenToStateChange_THEN_called_for_each_state_change() throws Exception {
        Lifecycle lf = new LifecycleImpl(ipc);

        // validate listenToStateChanges request and respond success
        Future<?> fut = executor.submit(() -> {
            MessageFrame inFrame = FrameReader.readFrame(in);
            LifecycleListenRequest lifecycleListenRequest = readMessageFromSockInputStream(inFrame, LifecycleListenRequest.class);
            assertEquals("me", lifecycleListenRequest.getServiceName());

            LifecycleGenericResponse successResponse = LifecycleGenericResponse.builder().status(LifecycleResponseStatus.Success).build();
            writeMessageToSockOutputStream(1, inFrame.requestId, successResponse, FrameReader.FrameType.RESPONSE);
            return null;
        });

        CountDownLatch cdl = new CountDownLatch(1);
        lf.listenToStateChanges("me", (oldState, newState) -> {
            assertEquals("New", newState);
            assertEquals("Old", oldState);
            cdl.countDown();
        });
        fut.get();

        // Send a state STATE_TRANSITION
        fut = executor.submit(() -> {

            StateTransitionEvent stateTransitionEvent = StateTransitionEvent.builder()
                    .service("me").newState("New").oldState("Old").build();
            writeMessageToSockOutputStream(LifecycleClientOpCodes.STATE_TRANSITION.ordinal(), stateTransitionEvent, FrameReader.FrameType.REQUEST);
            LifecycleResponseStatus ret = readMessageFromSockInputStream(FrameReader.readFrame(in), LifecycleResponseStatus.class);
            assertEquals(LifecycleResponseStatus.Success, ret);
            return null;
        });
        fut.get();
        assertTrue(cdl.await(500, TimeUnit.MILLISECONDS));
    }

    @Test
    public void GIVEN_lifecycle_client_WHEN_listenToStateChange_and_disconnect_THEN_reconnect_and_called_for_each_state_change()
            throws Exception {
        Lifecycle lf = new LifecycleImpl(ipc);

        Future<?> fut = executor.submit(() -> {
            MessageFrame inFrame = FrameReader.readFrame(in);
            LifecycleListenRequest lifecycleListenRequest = readMessageFromSockInputStream(inFrame, LifecycleListenRequest.class);
            assertEquals("me", lifecycleListenRequest.getServiceName());

            LifecycleGenericResponse successResponse = LifecycleGenericResponse.builder().status(LifecycleResponseStatus.Success).build();
            writeMessageToSockOutputStream(1, inFrame.requestId, successResponse, FrameReader.FrameType.RESPONSE);
            return null;
        });

        CountDownLatch cdl = new CountDownLatch(1);
        lf.listenToStateChanges("me", (oldState, newState) -> {
            assertEquals("New", newState);
            assertEquals("Old", oldState);
            cdl.countDown();
        });
        fut.get();


        // Send a state STATE_TRANSITION
        fut = executor.submit(() -> {
            StateTransitionEvent event = StateTransitionEvent.builder()
                    .service("me").newState("New").oldState("Old").build();
            writeMessageToSockOutputStream(LifecycleClientOpCodes.STATE_TRANSITION.ordinal(), event, FrameReader.FrameType.REQUEST);
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
            LifecycleListenRequest lifecycleListenRequest = readMessageFromSockInputStream(inFrame, LifecycleListenRequest.class);
            assertEquals("me", lifecycleListenRequest.getServiceName());

            LifecycleGenericResponse successResponse = LifecycleGenericResponse.builder().status(LifecycleResponseStatus.Success).build();

            writeMessageToSockOutputStream(1, inFrame.requestId, successResponse, FrameReader.FrameType.RESPONSE);

            // Make a state STATE_TRANSITION request and send it
            StateTransitionEvent stateTransitionEvent = StateTransitionEvent.builder()
                    .service("me").newState("New").oldState("Old").build();
            writeMessageToSockOutputStream(LifecycleClientOpCodes.STATE_TRANSITION.ordinal(), stateTransitionEvent, FrameReader.FrameType.REQUEST);
            LifecycleResponseStatus ret = readMessageFromSockInputStream(FrameReader.readFrame(in), LifecycleResponseStatus.class);
            assertEquals(LifecycleResponseStatus.Success, ret);
            return null;
        });
        fut.get();

        // Make sure the state STATE_TRANSITION handler got called twice despite the disconnection in between
        assertTrue(cdl.await(500, TimeUnit.MILLISECONDS));
    }
}
