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
    public void testGetThingShadowWithNoThingName() {
        final String expectedErrorMessage = "Thing Name is a required parameter";
        GetThingShadowRequest request = GetThingShadowRequest
                .builder()
                .build();

        Exception exception = assertThrows(ShadowIPCException.class, () -> {
            GetThingShadowResult result = shadow.getThingShadow(request);
        });

        assertEquals(expectedErrorMessage, exception.getMessage());
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
    public void testUpdateThingShadowWithNoThingName() {
        final String expectedErrorMessage = "Thing Name is a required parameter";

        UpdateThingShadowRequest request = UpdateThingShadowRequest
                .builder()
                .payload(PAYLOAD)
                .build();

        Exception exception = assertThrows(ShadowIPCException.class, () -> {
            UpdateThingShadowResult result = shadow.updateThingShadow(request);
        });

        assertEquals(expectedErrorMessage, exception.getMessage());
    }

    @Test
    public void testUpdateThingShadowWithNoPayload() {
        final String expectedErrorMessage = "Payload is a required parameter and cannot be null";

        UpdateThingShadowRequest request = UpdateThingShadowRequest
                .builder()
                .thingName(THING_NAME)
                .build();

        Exception exception = assertThrows(ShadowIPCException.class, () -> {
            UpdateThingShadowResult result = shadow.updateThingShadow(request);
        });

        assertEquals(expectedErrorMessage, exception.getMessage());
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
    public void testDeleteThingShadowWithNoThingName() {
        final String expectedErrorMessage = "Thing Name is a required parameter";

        DeleteThingShadowRequest request = DeleteThingShadowRequest
                .builder()
                .build();

        Exception exception = assertThrows(ShadowIPCException.class, () -> {
            DeleteThingShadowResult result = shadow.deleteThingShadow(request);
        });

        assertEquals(expectedErrorMessage, exception.getMessage());
    }


}

