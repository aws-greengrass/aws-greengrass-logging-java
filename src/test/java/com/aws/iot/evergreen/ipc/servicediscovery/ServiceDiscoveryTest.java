package com.aws.iot.evergreen.ipc.servicediscovery;

import com.aws.iot.evergreen.ipc.IPCClient;
import com.aws.iot.evergreen.ipc.IPCClientImpl;
import com.aws.iot.evergreen.ipc.common.BuiltInServiceDestinationCode;
import com.aws.iot.evergreen.ipc.config.KernelIPCClientConfig;
import com.aws.iot.evergreen.ipc.services.auth.AuthResponse;
import com.aws.iot.evergreen.ipc.services.common.ApplicationMessage;
import com.aws.iot.evergreen.ipc.services.common.IPCUtil;
import com.aws.iot.evergreen.ipc.services.servicediscovery.*;
import com.aws.iot.evergreen.ipc.services.servicediscovery.exceptions.AlreadyRegisteredException;
import com.aws.iot.evergreen.ipc.services.servicediscovery.exceptions.ServiceDiscoveryException;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.aws.iot.evergreen.ipc.common.FrameReader.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class ServiceDiscoveryTest {
    private JSON encoder = JSON.std.with(new CBORFactory());
    private ExecutorService executor = Executors.newCachedThreadPool();

    private IPCClient ipc;
    private Socket sock;
    private ServerSocket server;
    private DataInputStream in;
    private DataOutputStream out;


    public <T> T readMessageFromSockInputStream(final MessageFrame inFrame, final Class<T> returnTypeClass)
            throws Exception {
        ApplicationMessage reqAppFrame = new ApplicationMessage(inFrame.message.getPayload());
        return IPCUtil.decode(reqAppFrame.getPayload(), returnTypeClass);
    }

    public void writeMessageToSockOutputStream(int opCode, Integer requestId, Object data, FrameType type)
            throws Exception {
        ApplicationMessage transitionEventAppFrame = ApplicationMessage.builder()
                .version(ServiceDiscoveryImpl.API_VERSION).opCode(opCode).payload(IPCUtil.encode(data)).build();

        int destination = BuiltInServiceDestinationCode.SERVICE_DISCOVERY.getValue();
        Message message = new Message(transitionEventAppFrame.toByteArray());
        MessageFrame messageFrame = requestId == null ?
                new MessageFrame(destination, message, type) :
                new MessageFrame(requestId, destination, message, type);
        writeFrame(messageFrame, out);
    }

    public void writeMessageToSockOutputStream(int opCode, Object data, FrameType type) throws Exception {
        writeMessageToSockOutputStream(opCode, null, data, type);
    }

    @BeforeEach
    public void before() throws IOException, InterruptedException, ExecutionException {
        server = new ServerSocket(0);
        Future<Object> fut = executor.submit(() -> {
            sock = server.accept();
            in = new DataInputStream(sock.getInputStream());
            out = new DataOutputStream(sock.getOutputStream());

            // Read and write auth
            MessageFrame inFrame = readFrame(in);
            ApplicationMessage request = new ApplicationMessage(inFrame.message.getPayload());
            AuthResponse authResponse = AuthResponse.builder().serviceName("ABC").clientId("test").build();

            ApplicationMessage response = ApplicationMessage.builder().version(request.getVersion())
                    .payload(IPCUtil.encode(authResponse)).build();
            writeFrame(new MessageFrame(inFrame.requestId, BuiltInServiceDestinationCode.AUTH.getValue(),
                    new Message(response.toByteArray()), FrameType.RESPONSE), out);
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

    @Test
    public void testRegister() throws Exception {
        ServiceDiscovery sd = new ServiceDiscoveryImpl(ipc);
        RegisterResourceRequest req = RegisterResourceRequest.builder()
                .resource(Resource.builder().name("ABC").build()).build();

        Future<?> fut = executor.submit(() -> {
            MessageFrame inFrame = readFrame(in);

            RegisterResourceResponse registerResourceResponse = RegisterResourceResponse.builder()
                    .resource(Resource.builder().name("ABC").build())
                    .responseStatus(ServiceDiscoveryResponseStatus.Success).build();
            writeMessageToSockOutputStream(1, inFrame.requestId, registerResourceResponse, FrameType.RESPONSE);
            return null;
        });

        Resource res = sd.registerResource(req);
        fut.get();
        assertEquals("ABC", res.getName());
    }

    @Test
    public void testRegisterWithException() throws Exception {
        ServiceDiscovery sd = new ServiceDiscoveryImpl(ipc);
        RegisterResourceRequest req =
                RegisterResourceRequest.builder().resource(Resource.builder().name("ABC").build()).build();

        RegisterResourceResponse registerResourceResponse = RegisterResourceResponse.builder()
                .responseStatus(ServiceDiscoveryResponseStatus.AlreadyRegistered)
                .errorMessage("Service 'ABC' is already registered").build();

        Future<?> fut = executor.submit(() -> {
            MessageFrame inFrame = readFrame(in);
            writeMessageToSockOutputStream(1, inFrame.requestId, registerResourceResponse, FrameType.RESPONSE);
            return null;
        });
        AlreadyRegisteredException ex = assertThrows(AlreadyRegisteredException.class, () -> sd.registerResource(req));
        fut.get();
        assertEquals(registerResourceResponse.getErrorMessage(), ex.getMessage());
    }

    @Test
    public void testWrongReturnType() throws Exception {
        ServiceDiscovery sd = new ServiceDiscoveryImpl(ipc);
        RegisterResourceRequest req = RegisterResourceRequest.builder()
                .resource(Resource.builder().name("ABC").build()).build();

        Future<?> fut = executor.submit(() -> {
            MessageFrame inFrame = readFrame(in);

            UpdateResourceRequest updateResourceRequest = UpdateResourceRequest.builder()
                    .resource(Resource.builder().name("ABC").build())
                    .publishToDNSSD(true).publishToDNSSD(true).build();

            writeMessageToSockOutputStream(1, inFrame.requestId, updateResourceRequest, FrameType.RESPONSE);
            return null;
        });

        assertThrows(ServiceDiscoveryException.class, () -> sd.registerResource(req));
        fut.get();
    }
}
