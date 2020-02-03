package com.aws.iot.evergreen.ipc.servicediscovery;

import com.aws.iot.evergreen.ipc.IPCClient;
import com.aws.iot.evergreen.ipc.IPCClientImpl;
import com.aws.iot.evergreen.ipc.common.FrameReader;
import com.aws.iot.evergreen.ipc.common.GenericErrorCodes;
import com.aws.iot.evergreen.ipc.config.KernelIPCClientConfig;
import com.aws.iot.evergreen.ipc.services.common.SendAndReceiveIPCUtil;
import com.aws.iot.evergreen.ipc.services.servicediscovery.exceptions.AlreadyRegisteredException;
import com.aws.iot.evergreen.ipc.services.servicediscovery.exceptions.ServiceDiscoveryException;
import com.aws.iot.evergreen.ipc.services.servicediscovery.RegisterResourceRequest;
import com.aws.iot.evergreen.ipc.services.servicediscovery.Resource;
import com.aws.iot.evergreen.ipc.services.servicediscovery.ServiceDiscovery;
import com.aws.iot.evergreen.ipc.services.servicediscovery.ServiceDiscoveryResponseStatus;
import com.aws.iot.evergreen.ipc.services.servicediscovery.ServiceDiscoveryImpl;
import com.aws.iot.evergreen.ipc.services.servicediscovery.UpdateResourceRequest;
import com.aws.iot.evergreen.ipc.services.common.GeneralResponse;
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

import static com.aws.iot.evergreen.ipc.common.Constants.AUTH_SERVICE;
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

    @BeforeEach
    public void before() throws IOException, InterruptedException, ExecutionException {
        server = new ServerSocket(9000);
        Future<Object> fut = executor.submit(() -> {
            sock = server.accept();
            in = new DataInputStream(sock.getInputStream());
            out = new DataOutputStream(sock.getOutputStream());

            // Read and write auth
            FrameReader.MessageFrame inFrame = FrameReader.readFrame(in);
            FrameReader.writeFrame(new FrameReader.MessageFrame(inFrame.sequenceNumber, AUTH_SERVICE,
                    new FrameReader.Message(SendAndReceiveIPCUtil.encode(GeneralResponse.builder().error(GenericErrorCodes.Success).build())),
                            FrameReader.FrameType.RESPONSE), out);
            return null;
        });

        ipc = new IPCClientImpl(KernelIPCClientConfig.builder().port(9000).build());
        ipc.connect();
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

        GeneralResponse<Resource, ServiceDiscoveryResponseStatus> genReq = GeneralResponse.<Resource, ServiceDiscoveryResponseStatus>builder().
                response(Resource.builder().name("ABC").build())
                .error(ServiceDiscoveryResponseStatus.Success).build();

        FrameReader.Message message = new FrameReader.Message(encoder.asBytes(genReq));

        Future<?> fut = executor.submit(() -> {
            FrameReader.MessageFrame inFrame = FrameReader.readFrame(in);
            FrameReader.writeFrame(new FrameReader.MessageFrame(inFrame.sequenceNumber, ServiceDiscoveryImpl.SERVICE_DISCOVERY_NAME, message, FrameReader.FrameType.RESPONSE), out);
            return null;
        });

        Resource res = sd.registerResource(req);

        fut.get();

        assertEquals("ABC", res.getName());
    }

    @Test
    public void testRegisterWithException() throws Exception {
        ServiceDiscovery sd = new ServiceDiscoveryImpl(ipc);
        RegisterResourceRequest req = RegisterResourceRequest.builder()
                .resource(Resource.builder().name("ABC").build()).build();

        GeneralResponse<Resource, ServiceDiscoveryResponseStatus> genReq = GeneralResponse.<Resource, ServiceDiscoveryResponseStatus>builder()
                .error(ServiceDiscoveryResponseStatus.AlreadyRegistered)
                .errorMessage("Service 'ABC' is already registered").build();

        FrameReader.Message message = new FrameReader.Message(encoder.asBytes(genReq));

        Future<?> fut = executor.submit(() -> {
            FrameReader.MessageFrame inFrame = FrameReader.readFrame(in);
            FrameReader.writeFrame(new FrameReader.MessageFrame(inFrame.sequenceNumber, ServiceDiscoveryImpl.SERVICE_DISCOVERY_NAME, message, FrameReader.FrameType.RESPONSE), out);
            return null;
        });

        AlreadyRegisteredException ex =
                assertThrows(AlreadyRegisteredException.class, () -> sd.registerResource(req));

        fut.get();

        assertEquals(genReq.getErrorMessage(), ex.getMessage());
    }

    @Test
    public void testWrongReturnType() throws Exception {
        ServiceDiscovery sd = new ServiceDiscoveryImpl(ipc);
        RegisterResourceRequest req = RegisterResourceRequest.builder()
                .resource(Resource.builder().name("ABC").build()).build();

        GeneralResponse<UpdateResourceRequest, ServiceDiscoveryResponseStatus> genReq = GeneralResponse.<UpdateResourceRequest, ServiceDiscoveryResponseStatus>builder()
                .response(UpdateResourceRequest.builder()
                        .resource(Resource.builder()
                                .name("ABC").build())
                        .publishToDNSSD(true).build())
                .error(ServiceDiscoveryResponseStatus.Success).build();

        FrameReader.Message message = new FrameReader.Message(encoder.asBytes(genReq));

        Future<?> fut = executor.submit(() -> {
            FrameReader.MessageFrame inFrame = FrameReader.readFrame(in);
            FrameReader.writeFrame(new FrameReader.MessageFrame(inFrame.sequenceNumber, ServiceDiscoveryImpl.SERVICE_DISCOVERY_NAME, message, FrameReader.FrameType.RESPONSE), out);
            return null;
        });

        assertThrows(ServiceDiscoveryException.class, () -> sd.registerResource(req));

        fut.get();
    }
}
