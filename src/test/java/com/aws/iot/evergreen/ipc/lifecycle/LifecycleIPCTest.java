package com.aws.iot.evergreen.ipc.lifecycle;

import com.aws.iot.evergreen.ipc.IPCClient;
import com.aws.iot.evergreen.ipc.IPCClientImpl;
import com.aws.iot.evergreen.ipc.common.FrameReader;
import com.aws.iot.evergreen.ipc.common.GenericErrorCodes;
import com.aws.iot.evergreen.ipc.config.KernelIPCClientConfig;
import com.aws.iot.evergreen.ipc.services.common.GeneralRequest;
import com.aws.iot.evergreen.ipc.services.common.GeneralResponse;
import com.aws.iot.evergreen.ipc.services.common.IPCUtil;
import com.aws.iot.evergreen.ipc.services.lifecycle.Lifecycle;
import com.aws.iot.evergreen.ipc.services.lifecycle.LifecycleImpl;
import com.aws.iot.evergreen.ipc.services.lifecycle.LifecycleListenRequest;
import com.aws.iot.evergreen.ipc.services.lifecycle.LifecycleRequestTypes;
import com.aws.iot.evergreen.ipc.services.lifecycle.LifecycleResponseStatus;
import com.aws.iot.evergreen.ipc.services.lifecycle.StateChangeRequest;
import com.aws.iot.evergreen.ipc.services.lifecycle.StateTransitionEvent;
import com.fasterxml.jackson.core.type.TypeReference;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.aws.iot.evergreen.ipc.common.Constants.AUTH_SERVICE;
import static com.aws.iot.evergreen.ipc.services.lifecycle.Lifecycle.LIFECYCLE_SERVICE_NAME;
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
                FrameReader.MessageFrame inFrame = FrameReader.readFrame(in);
                FrameReader.writeFrame(new FrameReader.MessageFrame(inFrame.sequenceNumber, AUTH_SERVICE,
                        new FrameReader.Message(IPCUtil.encode(GeneralResponse.builder().error(GenericErrorCodes.Success).build())),
                        FrameReader.FrameType.RESPONSE), out);
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

        GeneralResponse<Void, LifecycleResponseStatus> genReq =
                GeneralResponse.<Void, LifecycleResponseStatus>builder().error(LifecycleResponseStatus.Success).build();

        FrameReader.Message message = new FrameReader.Message(encoder.asBytes(genReq));

        Future<?> fut = executor.submit(() -> {
            FrameReader.MessageFrame inFrame = FrameReader.readFrame(in);

            GeneralRequest<StateChangeRequest, LifecycleRequestTypes> req = IPCUtil.decode(inFrame.message,
                    new TypeReference<GeneralRequest<StateChangeRequest, LifecycleRequestTypes>>() {
                    });

            assertEquals(LifecycleRequestTypes.setState, req.getType());
            assertEquals("Errored", req.getRequest().getState());

            FrameReader.writeFrame(new FrameReader.MessageFrame(inFrame.sequenceNumber, LIFECYCLE_SERVICE_NAME, message,
                    FrameReader.FrameType.RESPONSE), out);
            return null;
        });

        lf.reportState("Errored");

        fut.get();
    }

    @Test
    public void GIVEN_lifecycle_client_WHEN_listenToStateChange_THEN_called_for_each_state_change() throws Exception {
        Lifecycle lf = new LifecycleImpl(ipc);

        GeneralResponse<Void, LifecycleResponseStatus> genReq =
                GeneralResponse.<Void, LifecycleResponseStatus>builder().error(LifecycleResponseStatus.Success).build();

        FrameReader.Message message = new FrameReader.Message(encoder.asBytes(genReq));

        Future<?> fut = executor.submit(() -> {
            FrameReader.MessageFrame inFrame = FrameReader.readFrame(in);

            GeneralRequest<LifecycleListenRequest, LifecycleRequestTypes> req = IPCUtil.decode(inFrame.message,
                    new TypeReference<GeneralRequest<LifecycleListenRequest, LifecycleRequestTypes>>() {
                    });

            assertEquals(LifecycleRequestTypes.listen, req.getType());
            assertEquals("me", req.getRequest().getServiceName());

            FrameReader.writeFrame(new FrameReader.MessageFrame(inFrame.sequenceNumber, LIFECYCLE_SERVICE_NAME, message,
                    FrameReader.FrameType.RESPONSE), out);
            return null;
        });

        CountDownLatch cdl = new CountDownLatch(1);
        lf.listenToStateChanges("me", (oldState, newState) -> {
            assertEquals("New", newState);
            assertEquals("Old", oldState);
            cdl.countDown();
        });
        fut.get();

        fut = executor.submit(() -> {
            GeneralRequest<StateTransitionEvent, LifecycleRequestTypes> genReq2 =
                    GeneralRequest.<StateTransitionEvent, LifecycleRequestTypes>builder()
                            .type(LifecycleRequestTypes.transition).request(
                            StateTransitionEvent.builder().service("me").newState("New").oldState("Old").build())
                            .build();

            FrameReader.Message message2 = new FrameReader.Message(encoder.asBytes(genReq2));
            FrameReader.writeFrame(
                    new FrameReader.MessageFrame(LIFECYCLE_SERVICE_NAME, message2, FrameReader.FrameType.REQUEST), out);

            FrameReader.MessageFrame inFrame = FrameReader.readFrame(in);
            GeneralResponse<Void, LifecycleResponseStatus> ret = IPCUtil.decode(inFrame.message,
                    new TypeReference<GeneralResponse<Void, LifecycleResponseStatus>>() {
                    });

            assertEquals(LifecycleResponseStatus.Success, ret.getError());

            return null;
        });
        fut.get();

        assertTrue(cdl.await(500, TimeUnit.MILLISECONDS));
    }

    @Test
    public void GIVEN_lifecycle_client_WHEN_listenToStateChange_and_disconnect_THEN_reconnect_and_called_for_each_state_change() throws Exception {
        Lifecycle lf = new LifecycleImpl(ipc);

        GeneralResponse<Void, LifecycleResponseStatus> genReq =
                GeneralResponse.<Void, LifecycleResponseStatus>builder().error(LifecycleResponseStatus.Success).build();

        FrameReader.Message message = new FrameReader.Message(encoder.asBytes(genReq));

        // Setup listen request's response
        Future<?> fut = executor.submit(() -> {
            FrameReader.MessageFrame inFrame = FrameReader.readFrame(in);

            GeneralRequest<LifecycleListenRequest, LifecycleRequestTypes> req = IPCUtil.decode(inFrame.message,
                    new TypeReference<GeneralRequest<LifecycleListenRequest, LifecycleRequestTypes>>() {
                    });

            assertEquals(LifecycleRequestTypes.listen, req.getType());
            assertEquals("me", req.getRequest().getServiceName());

            FrameReader.writeFrame(new FrameReader.MessageFrame(inFrame.sequenceNumber, LIFECYCLE_SERVICE_NAME, message,
                    FrameReader.FrameType.RESPONSE), out);
            return null;
        });

        CountDownLatch cdl = new CountDownLatch(2);
        lf.listenToStateChanges("me", (oldState, newState) -> {
            assertEquals("New", newState);
            assertEquals("Old", oldState);
            cdl.countDown();
        });
        fut.get();

        // Send a state transition
        fut = executor.submit(() -> {
            GeneralRequest<StateTransitionEvent, LifecycleRequestTypes> genReq2 =
                    GeneralRequest.<StateTransitionEvent, LifecycleRequestTypes>builder()
                            .type(LifecycleRequestTypes.transition).request(
                            StateTransitionEvent.builder().service("me").newState("New").oldState("Old").build())
                            .build();

            FrameReader.Message message2 = new FrameReader.Message(encoder.asBytes(genReq2));
            FrameReader.writeFrame(
                    new FrameReader.MessageFrame(LIFECYCLE_SERVICE_NAME, message2, FrameReader.FrameType.REQUEST), out);

            FrameReader.MessageFrame inFrame = FrameReader.readFrame(in);
            GeneralResponse<Void, LifecycleResponseStatus> ret = IPCUtil.decode(inFrame.message,
                    new TypeReference<GeneralResponse<Void, LifecycleResponseStatus>>() {
                    });

            assertEquals(LifecycleResponseStatus.Success, ret.getError());

            return null;
        });
        fut.get();

        // Kill the connection to force a reconnect
        sock.close();
        // Wait for the client to reconnect
        while(connectionCount == 1) {
            Thread.sleep(10);
        }

        // Since the client reconnected we expect a call to listen again.
        // Setup the response to the listen request
        // then send another state transition
        fut = executor.submit(() -> {
            // Respond to listen request
            FrameReader.MessageFrame inFrame = FrameReader.readFrame(in);
            GeneralRequest<LifecycleListenRequest, LifecycleRequestTypes> req = IPCUtil.decode(inFrame.message,
                    new TypeReference<GeneralRequest<LifecycleListenRequest, LifecycleRequestTypes>>() {
                    });

            assertEquals(LifecycleRequestTypes.listen, req.getType());
            assertEquals("me", req.getRequest().getServiceName());

            FrameReader.writeFrame(new FrameReader.MessageFrame(inFrame.sequenceNumber, LIFECYCLE_SERVICE_NAME, message,
                    FrameReader.FrameType.RESPONSE), out);

            // Make a state transition request and send it
            GeneralRequest<StateTransitionEvent, LifecycleRequestTypes> genReq2 =
                    GeneralRequest.<StateTransitionEvent, LifecycleRequestTypes>builder()
                            .type(LifecycleRequestTypes.transition).request(
                            StateTransitionEvent.builder().service("me").newState("New").oldState("Old").build())
                            .build();

            FrameReader.Message message2 = new FrameReader.Message(encoder.asBytes(genReq2));
            FrameReader.writeFrame(
                    new FrameReader.MessageFrame(LIFECYCLE_SERVICE_NAME, message2, FrameReader.FrameType.REQUEST), out);

            inFrame = FrameReader.readFrame(in);
            GeneralResponse<Void, LifecycleResponseStatus> ret = IPCUtil.decode(inFrame.message,
                    new TypeReference<GeneralResponse<Void, LifecycleResponseStatus>>() {
                    });

            assertEquals(LifecycleResponseStatus.Success, ret.getError());

            return null;
        });
        fut.get();

        // Make sure the state transition handler got called twice despite the disconnection in between
        assertTrue(cdl.await(500, TimeUnit.MILLISECONDS));
    }
}
