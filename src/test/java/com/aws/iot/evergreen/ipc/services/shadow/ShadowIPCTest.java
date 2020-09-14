package com.aws.iot.evergreen.ipc.services.shadow;

import com.aws.iot.evergreen.ipc.common.BaseIPCTest;
import com.aws.iot.evergreen.ipc.common.BuiltInServiceDestinationCode;
import com.aws.iot.evergreen.ipc.common.FrameReader;
import com.aws.iot.evergreen.ipc.exceptions.IPCClientException;
import com.aws.iot.evergreen.ipc.services.shadow.exception.ShadowIPCException;
import com.aws.iot.evergreen.ipc.services.shadow.models.DeleteThingShadowRequest;
import com.aws.iot.evergreen.ipc.services.shadow.models.DeleteThingShadowResult;
import com.aws.iot.evergreen.ipc.services.shadow.models.GetThingShadowRequest;
import com.aws.iot.evergreen.ipc.services.shadow.models.GetThingShadowResult;
import com.aws.iot.evergreen.ipc.services.shadow.models.ShadowGenericResponse;
import com.aws.iot.evergreen.ipc.services.shadow.models.ShadowResponseStatus;
import com.aws.iot.evergreen.ipc.services.shadow.models.UpdateThingShadowRequest;
import com.aws.iot.evergreen.ipc.services.shadow.models.UpdateThingShadowResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.aws.iot.evergreen.ipc.common.FrameReader.readFrame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class ShadowIPCTest extends BaseIPCTest {

    private static final String THING_NAME = "testThing";
    private static final ByteBuffer PAYLOAD =  ByteBuffer.wrap("{\"id\": 1, \"name\": \"The Beatles\"}".getBytes());
    private Shadow shadow;

    private static final String ERROR_MESSAGE_FOR_NO_THING_NAME = "thingName is marked non-null but is null";
    private static final String ERROR_MESSAGE_FOR_EMPTY_THING_NAME = "thingName cannot be empty";
    private static final String ERROR_MESSAGE_FOR_NO_PAYLOAD = "payload is marked non-null but is null";

    @Override
    @BeforeEach
    public void before() throws IOException, InterruptedException, ExecutionException, IPCClientException {
        super.before();
        shadow = new ShadowImpl(ipc);
    }

    @Test
    public void testGetThingShadow() throws ShadowIPCException, ExecutionException, InterruptedException, TimeoutException {
        GetThingShadowRequest request = GetThingShadowRequest
                .builder()
                .thingName(THING_NAME)
                .build();

        Future<?> fut = executor.submit(() -> {
            FrameReader.MessageFrame inFrame = readFrame(in);
            ShadowGenericResponse response = new ShadowGenericResponse();
            response.setStatus(ShadowResponseStatus.Success);
            response.setPayload(PAYLOAD);

            writeMessageToSockOutputStream(1, inFrame.requestId, response,
                    FrameReader.FrameType.RESPONSE,
                    BuiltInServiceDestinationCode.SHADOW.getValue(),
                    ShadowImpl.API_VERSION);
            return null;
        });

        GetThingShadowResult result = shadow.getThingShadow(request);
        fut.get(1L, TimeUnit.SECONDS);
        assertEquals(PAYLOAD, result.getPayload());
    }

    @Test
    public void testGetThingShadowRequestCreation() {
        Exception exception = assertThrows(NullPointerException.class, () -> GetThingShadowRequest
                .builder()
                .build());

        assertEquals(ERROR_MESSAGE_FOR_NO_THING_NAME, exception.getMessage());

        exception = assertThrows(IllegalArgumentException.class, () -> GetThingShadowRequest
                .builder()
                .thingName("")
                .build());

        assertEquals(ERROR_MESSAGE_FOR_EMPTY_THING_NAME, exception.getMessage());
    }

    @Test
    public void testUpdateThingShadow() throws ShadowIPCException, ExecutionException, InterruptedException, TimeoutException {
        UpdateThingShadowRequest request = UpdateThingShadowRequest
                .builder()
                .thingName(THING_NAME)
                .payload(PAYLOAD)
                .build();

        Future<?> fut = executor.submit(() -> {
            FrameReader.MessageFrame inFrame = readFrame(in);
            ShadowGenericResponse response = new ShadowGenericResponse();
            response.setStatus(ShadowResponseStatus.Success);
            response.setPayload(PAYLOAD);

            writeMessageToSockOutputStream(1, inFrame.requestId, response,
                    FrameReader.FrameType.RESPONSE,
                    BuiltInServiceDestinationCode.SHADOW.getValue(),
                    ShadowImpl.API_VERSION);
            return null;
        });

        UpdateThingShadowResult result = shadow.updateThingShadow(request);
        fut.get(1L, TimeUnit.SECONDS);
        assertEquals(PAYLOAD, result.getPayload());
    }

    @Test
    public void testUpdateThingShadowRequestCreation() {
        Exception exception = assertThrows(NullPointerException.class, () -> UpdateThingShadowRequest
                .builder()
                .payload(PAYLOAD)
                .build());

        assertEquals(ERROR_MESSAGE_FOR_NO_THING_NAME, exception.getMessage());

        exception = assertThrows(IllegalArgumentException.class, () -> UpdateThingShadowRequest
                .builder()
                .thingName("")
                .payload(PAYLOAD)
                .build());

        assertEquals(ERROR_MESSAGE_FOR_EMPTY_THING_NAME, exception.getMessage());

        exception = assertThrows(NullPointerException.class, () -> UpdateThingShadowRequest
                .builder()
                .thingName(THING_NAME)
                .build());

        assertEquals(ERROR_MESSAGE_FOR_NO_PAYLOAD, exception.getMessage());
    }

    @Test
    public void testDeleteThingShadow() throws ShadowIPCException, ExecutionException, InterruptedException, TimeoutException {
        DeleteThingShadowRequest request = DeleteThingShadowRequest
                .builder()
                .thingName(THING_NAME)
                .build();

        Future<?> fut = executor.submit(() -> {
            FrameReader.MessageFrame inFrame = readFrame(in);
            ShadowGenericResponse response = new ShadowGenericResponse();
            response.setStatus(ShadowResponseStatus.Success);
            response.setPayload(PAYLOAD);

            writeMessageToSockOutputStream(1, inFrame.requestId, response,
                    FrameReader.FrameType.RESPONSE,
                    BuiltInServiceDestinationCode.SHADOW.getValue(),
                    ShadowImpl.API_VERSION);
            return null;
        });

        DeleteThingShadowResult result = shadow.deleteThingShadow(request);
        fut.get(1L, TimeUnit.SECONDS);
        assertEquals(PAYLOAD, result.getPayload());
    }

    @Test
    public void testDeleteThingShadowRequestCreation() {
        Exception exception = assertThrows(NullPointerException.class, () -> DeleteThingShadowRequest
                .builder()
                .build());

        assertEquals(ERROR_MESSAGE_FOR_NO_THING_NAME, exception.getMessage());

        exception = assertThrows(IllegalArgumentException.class, () -> DeleteThingShadowRequest
                .builder()
                .thingName("")
                .build());

        assertEquals(ERROR_MESSAGE_FOR_EMPTY_THING_NAME, exception.getMessage());
    }


}

