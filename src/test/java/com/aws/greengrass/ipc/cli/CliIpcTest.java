package com.aws.greengrass.ipc.cli;

import com.aws.greengrass.ipc.IPCClient;
import com.aws.greengrass.ipc.IPCClientImpl;
import com.aws.greengrass.ipc.common.BuiltInServiceDestinationCode;
import com.aws.greengrass.ipc.common.FrameReader;
import com.aws.greengrass.ipc.config.KernelIPCClientConfig;
import com.aws.greengrass.ipc.exceptions.IPCClientException;
import com.aws.greengrass.ipc.services.authentication.AuthenticationResponse;
import com.aws.greengrass.ipc.services.cli.CliImpl;
import com.aws.greengrass.ipc.services.cli.exceptions.ComponentNotFoundError;
import com.aws.greengrass.ipc.services.cli.models.CliGenericResponse;
import com.aws.greengrass.ipc.services.cli.models.ComponentDetails;
import com.aws.greengrass.ipc.services.cli.models.GetComponentDetailsRequest;
import com.aws.greengrass.ipc.services.cli.models.GetComponentDetailsResponse;
import com.aws.greengrass.ipc.services.cli.models.LifecycleState;
import com.aws.greengrass.ipc.services.cli.models.ListComponentsResponse;
import com.aws.greengrass.ipc.services.cli.models.RequestStatus;
import com.aws.greengrass.ipc.services.cli.models.RestartComponentRequest;
import com.aws.greengrass.ipc.services.cli.models.RestartComponentResponse;
import com.aws.greengrass.ipc.services.cli.models.StopComponentRequest;
import com.aws.greengrass.ipc.services.cli.models.StopComponentResponse;
import com.aws.greengrass.ipc.services.common.ApplicationMessage;
import com.aws.greengrass.ipc.services.common.IPCUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.aws.greengrass.ipc.common.FrameReader.readFrame;
import static com.aws.greengrass.ipc.common.FrameReader.writeFrame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CliIpcTest {

    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final String CANNOT_RESTART_MESSAGE = "Cannot restart at this time, critical function executing. Try " +
            "again later";
    private final String CANNOT_STOP_MESSAGE = "Cannot stop at this time, critical function executing. Try " +
            "again later";

    private IPCClient ipc;
    private Socket sock;
    private ServerSocket server;
    private DataInputStream in;
    private DataOutputStream out;
    private int connectionCount = 0;


    public static <T> T readMessageFromSockInputStream(final FrameReader.MessageFrame inFrame, final Class<T> returnTypeClass) throws Exception {
        String payloadString = new String(inFrame.message.getPayload());
        ApplicationMessage reqAppFrame = ApplicationMessage.fromBytes(inFrame.message.getPayload());
        return IPCUtil.decode(reqAppFrame.getPayload(), returnTypeClass);
    }

    private void writeMessageToSockOutputStream(int opCode, Integer requestId, Object data, FrameReader.FrameType type) throws Exception {
        ApplicationMessage transitionEventAppFrame = ApplicationMessage.builder()
                .version(CliImpl.API_VERSION).opCode(opCode)
                .payload(IPCUtil.encode(data)).build();

        int destination = BuiltInServiceDestinationCode.CLI.getValue();
        FrameReader.Message message = new FrameReader.Message(transitionEventAppFrame.toByteArray());
        FrameReader.MessageFrame messageFrame = requestId == null ?
                new FrameReader.MessageFrame(destination, message, type) :
                new FrameReader.MessageFrame(requestId, destination, message, type);
        FrameReader.writeFrame(messageFrame, out);
    }

    private void writeMessageToSockOutputStream(int opCode, Object data, FrameReader.FrameType type) throws Exception {
        writeMessageToSockOutputStream(opCode, null, data, type);
    }

    @BeforeEach
    public void before() throws IOException, InterruptedException, ExecutionException, IPCClientException {
        server = new ServerSocket(0);
        connectionCount = 0;
        executor.submit(() -> {
            while (true) {
                sock = server.accept();
                in = new DataInputStream(sock.getInputStream());
                out = new DataOutputStream(sock.getOutputStream());

                // Read and write auth
                FrameReader.MessageFrame inFrame = readFrame(in);
                ApplicationMessage requestApplicationFrame = ApplicationMessage.fromBytes(inFrame.message.getPayload());
                AuthenticationResponse authResponse = AuthenticationResponse.builder()
                        .serviceName("ABC").clientId("test").build();
                ApplicationMessage responsesAppFrame = ApplicationMessage.builder()
                        .version(requestApplicationFrame.getVersion())
                        .payload(IPCUtil.encode(authResponse)).build();

                writeFrame(new FrameReader.MessageFrame(inFrame.requestId,
                        BuiltInServiceDestinationCode.AUTHENTICATION.getValue(),
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
    public void GIVEN_cli_client_WHEN_get_component_request_sent_with_success_response_THEN_parse_response_successfully() throws Exception {
        CliImpl cliImpl = new CliImpl(ipc);
        Future<?> fut = executor.submit(() -> {
            FrameReader.MessageFrame inFrame = readFrame(in);
            GetComponentDetailsRequest getComponentDetailsRequest = readMessageFromSockInputStream(inFrame, GetComponentDetailsRequest.class);
            assertEquals("XYZ", getComponentDetailsRequest.getComponentName());

            GetComponentDetailsResponse getComponentDetailsResponse =
                    GetComponentDetailsResponse.builder().componentDetails(ComponentDetails.builder().componentName(
                            "XYZ").state(LifecycleState.RUNNING).version("1.0.0").build())
                            .build();
            writeMessageToSockOutputStream(5, inFrame.requestId, getComponentDetailsResponse,
                    FrameReader.FrameType.RESPONSE);
            return null;
        });

        GetComponentDetailsResponse response =
                cliImpl.getComponentDetails(GetComponentDetailsRequest.builder().componentName("XYZ").build());
        fut.get();
        assertEquals(LifecycleState.RUNNING, response.getComponentDetails().getState());
        assertEquals("XYZ", response.getComponentDetails().getComponentName());
        assertEquals("1.0.0", response.getComponentDetails().getVersion());
    }

    @Test
    public void GIVEN_get_component_request_sent_WHEN_component_not_found_THEN_parse_exception_successfully() throws Exception {
        CliImpl cliImpl = new CliImpl(ipc);
        Future<?> fut = executor.submit(() -> {
            FrameReader.MessageFrame inFrame = readFrame(in);
            GetComponentDetailsRequest getComponentDetailsRequest = readMessageFromSockInputStream(inFrame, GetComponentDetailsRequest.class);
            assertEquals("XYZ", getComponentDetailsRequest.getComponentName());

            ComponentNotFoundError error = new ComponentNotFoundError("XYZ component not found");
            error.setMessageType(CliGenericResponse.MessageType.APPLICATION_ERROR);
            error.setErrorType("ComponentNotFoundError");
            writeMessageToSockOutputStream(5, inFrame.requestId, error,
                    FrameReader.FrameType.RESPONSE);
            return null;
        });

        assertThrows(ComponentNotFoundError.class,()->
                cliImpl.getComponentDetails(GetComponentDetailsRequest.builder().componentName("XYZ").build()));
    }

    @Test
    public void GIVEN_cli_client_WHEN_list_components_request_sent_with_success_response_THEN_parse_response_successfully() throws Exception {
        CliImpl cliImpl = new CliImpl(ipc);
        ComponentDetails componentDetails1 =
                ComponentDetails.builder().componentName("Component1").state(LifecycleState.RUNNING).version("1.0.0").build();
        ComponentDetails componentDetails2 =
                ComponentDetails.builder().componentName("Component2").state(LifecycleState.ERRORED)
                        .version("0.9.1").build();
        List<ComponentDetails> listOfComponents = Arrays.asList(componentDetails1, componentDetails2);
        Future<?> fut = executor.submit(() -> {
            FrameReader.MessageFrame inFrame = readFrame(in);
            ListComponentsResponse listComponentsResponse =
                    ListComponentsResponse.builder().components(listOfComponents).build();
            writeMessageToSockOutputStream(5, inFrame.requestId, listComponentsResponse,
                    FrameReader.FrameType.RESPONSE);
            return null;
        });

        ListComponentsResponse response =
                cliImpl.listComponents();
        fut.get();
        assertEquals(listOfComponents, response.getComponents());
    }

    @Test
    public void GIVEN_cli_client_WHEN_restart_component_request_sent_with_success_response_THEN_parse_response_successfully() throws Exception {
        CliImpl cliImpl = new CliImpl(ipc);
        Future<?> fut = executor.submit(() -> {
            FrameReader.MessageFrame inFrame = readFrame(in);
            RestartComponentRequest restartComponentRequest = readMessageFromSockInputStream(inFrame,
                    RestartComponentRequest.class);
            assertEquals("XYZ", restartComponentRequest.getComponentName());
            RestartComponentResponse restartComponentResponse =
                    RestartComponentResponse.builder().requestStatus(RequestStatus.SUCCEEDED).build();
            writeMessageToSockOutputStream(5, inFrame.requestId, restartComponentResponse,
                    FrameReader.FrameType.RESPONSE);
            return null;
        });

        RestartComponentResponse response =
                cliImpl.restartComponent(RestartComponentRequest.builder().componentName("XYZ").build());
        fut.get();
        assertEquals(RequestStatus.SUCCEEDED, response.getRequestStatus());
    }

    @Test
    public void GIVEN_restart_component_request_sent_WHEN_request_failed_THEN_parse_response_successfully() throws Exception {
        CliImpl cliImpl = new CliImpl(ipc);
        Future<?> fut = executor.submit(() -> {
            FrameReader.MessageFrame inFrame = readFrame(in);
            RestartComponentRequest restartComponentRequest = readMessageFromSockInputStream(inFrame,
                    RestartComponentRequest.class);
            assertEquals("XYZ", restartComponentRequest.getComponentName());
            RestartComponentResponse restartComponentResponse =
                    RestartComponentResponse.builder().requestStatus(RequestStatus.FAILED)
                            .message(CANNOT_RESTART_MESSAGE).build();
            writeMessageToSockOutputStream(5, inFrame.requestId, restartComponentResponse,
                    FrameReader.FrameType.RESPONSE);
            return null;
        });

        RestartComponentResponse response =
                cliImpl.restartComponent(RestartComponentRequest.builder().componentName("XYZ").build());
        fut.get();
        assertEquals(RequestStatus.FAILED, response.getRequestStatus());
        assertEquals(CANNOT_RESTART_MESSAGE, response.getMessage());
    }

    @Test
    public void GIVEN_restart_component_request_sent_WHEN_component_not_found_THEN_parse_error_successfully() throws Exception {
        CliImpl cliImpl = new CliImpl(ipc);
        Future<?> fut = executor.submit(() -> {
            FrameReader.MessageFrame inFrame = readFrame(in);
            RestartComponentRequest restartComponentRequest = readMessageFromSockInputStream(inFrame,
                    RestartComponentRequest.class);
            assertEquals("XYZ", restartComponentRequest.getComponentName());

            ComponentNotFoundError error = new ComponentNotFoundError("Cannot find component XYZ");
            error.setMessageType(CliGenericResponse.MessageType.APPLICATION_ERROR);
            error.setErrorType("ComponentNotFoundError");
            writeMessageToSockOutputStream(5, inFrame.requestId, error,
                    FrameReader.FrameType.RESPONSE);
            return null;
        });
        assertThrows(ComponentNotFoundError.class,
               () -> cliImpl.restartComponent(RestartComponentRequest.builder().componentName("XYZ").build()));
    }

    @Test
    public void GIVEN_cli_client_WHEN_stop_component_request_sent_with_success_response_THEN_parse_response_successfully() throws Exception {
        CliImpl cliImpl = new CliImpl(ipc);
        Future<?> fut = executor.submit(() -> {
            FrameReader.MessageFrame inFrame = readFrame(in);
            StopComponentRequest stopComponentRequest = readMessageFromSockInputStream(inFrame,
                    StopComponentRequest.class);
            assertEquals("XYZ", stopComponentRequest.getComponentName());
            StopComponentResponse stopComponentResponse =
                    StopComponentResponse.builder().requestStatus(RequestStatus.SUCCEEDED).build();
            writeMessageToSockOutputStream(5, inFrame.requestId, stopComponentResponse,
                    FrameReader.FrameType.RESPONSE);
            return null;
        });

        StopComponentResponse response =
                cliImpl.stopComponent(StopComponentRequest.builder().componentName("XYZ").build());
        fut.get();
        assertEquals(RequestStatus.SUCCEEDED, response.getRequestStatus());
    }

    @Test
    public void GIVEN_stop_component_request_sent_WHEN_request_failed_THEN_parse_response_successfully() throws Exception {
        CliImpl cliImpl = new CliImpl(ipc);
        Future<?> fut = executor.submit(() -> {
            FrameReader.MessageFrame inFrame = readFrame(in);
            StopComponentRequest stopComponentRequest = readMessageFromSockInputStream(inFrame,
                    StopComponentRequest.class);
            assertEquals("XYZ", stopComponentRequest.getComponentName());
            StopComponentResponse stopComponentResponse =
                    StopComponentResponse.builder().requestStatus(RequestStatus.FAILED)
                            .message(CANNOT_STOP_MESSAGE).build();
            writeMessageToSockOutputStream(5, inFrame.requestId, stopComponentResponse,
                    FrameReader.FrameType.RESPONSE);
            return null;
        });

        StopComponentResponse response =
                cliImpl.stopComponent(StopComponentRequest.builder().componentName("XYZ").build());
        fut.get();
        assertEquals(RequestStatus.FAILED, response.getRequestStatus());
        assertEquals(CANNOT_STOP_MESSAGE, response.getMessage());
    }

    @Test
    public void GIVEN_stop_component_request_sent_WHEN_component_not_found_THEN_parse_error_successfully() throws Exception {
        CliImpl cliImpl = new CliImpl(ipc);
        Future<?> fut = executor.submit(() -> {
            FrameReader.MessageFrame inFrame = readFrame(in);
            StopComponentRequest stopComponentRequest = readMessageFromSockInputStream(inFrame,
                    StopComponentRequest.class);
            assertEquals("XYZ", stopComponentRequest.getComponentName());

            ComponentNotFoundError error = new ComponentNotFoundError("Cannot find component XYZ");
            error.setMessageType(CliGenericResponse.MessageType.APPLICATION_ERROR);
            error.setErrorType("ComponentNotFoundError");
            writeMessageToSockOutputStream(5, inFrame.requestId, error,
                    FrameReader.FrameType.RESPONSE);
            return null;
        });
        assertThrows(ComponentNotFoundError.class,
                () -> cliImpl.stopComponent(StopComponentRequest.builder().componentName("XYZ").build()));
    }

}
