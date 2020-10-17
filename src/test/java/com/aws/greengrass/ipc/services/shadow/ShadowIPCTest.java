package com.aws.greengrass.ipc.services.shadow;

import com.aws.greengrass.ipc.common.BaseIPCTest;
import com.aws.greengrass.ipc.common.BuiltInServiceDestinationCode;
import com.aws.greengrass.ipc.common.FrameReader;
import com.aws.greengrass.ipc.exceptions.IPCClientException;
import com.aws.greengrass.ipc.services.shadow.exception.ShadowIPCException;
import com.aws.greengrass.ipc.services.shadow.models.DeleteThingShadowRequest;
import com.aws.greengrass.ipc.services.shadow.models.DeleteThingShadowResult;
import com.aws.greengrass.ipc.services.shadow.models.GetThingShadowRequest;
import com.aws.greengrass.ipc.services.shadow.models.GetThingShadowResult;
import com.aws.greengrass.ipc.services.shadow.models.ShadowGenericResponse;
import com.aws.greengrass.ipc.services.shadow.models.ShadowResponseStatus;
import com.aws.greengrass.ipc.services.shadow.models.UpdateThingShadowRequest;
import com.aws.greengrass.ipc.services.shadow.models.UpdateThingShadowResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.aws.greengrass.ipc.common.FrameReader.readFrame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

@ExtendWith(MockitoExtension.class)
public class ShadowIPCTest extends BaseIPCTest {

    private static final String THING_NAME = "testThing";
    private static final byte[] PAYLOAD =  "{\"id\": 1, \"name\": \"The Beatles\"}".getBytes();
    private Shadow shadow;

    private static final String ERROR_MESSAGE_FOR_NO_THING_NAME = "thingName is marked non-null but is null";
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
            GetThingShadowResult response = GetThingShadowResult.builder()
                    .responseStatus(ShadowResponseStatus.Success)
                    .payload(PAYLOAD)
                    .build();

            writeMessageToSockOutputStream(1, inFrame.requestId, response,
                    FrameReader.FrameType.RESPONSE,
                    BuiltInServiceDestinationCode.SHADOW.getValue(),
                    ShadowImpl.API_VERSION);
            return null;
        });

        GetThingShadowResult result = shadow.getThingShadow(request);
        fut.get(1L, TimeUnit.SECONDS);
        assertArrayEquals(PAYLOAD, result.getPayload());
    }

    @Test
    public void testGetThingShadowRequestCreation() {
        Exception exception = assertThrows(NullPointerException.class, () -> GetThingShadowRequest
                .builder()
                .build());

        assertEquals(ERROR_MESSAGE_FOR_NO_THING_NAME, exception.getMessage());
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
            UpdateThingShadowResult response = UpdateThingShadowResult.builder()
                    .responseStatus(ShadowResponseStatus.Success)
                    .payload(PAYLOAD)
                    .build();

            writeMessageToSockOutputStream(1, inFrame.requestId, response,
                    FrameReader.FrameType.RESPONSE,
                    BuiltInServiceDestinationCode.SHADOW.getValue(),
                    ShadowImpl.API_VERSION);
            return null;
        });

        UpdateThingShadowResult result = shadow.updateThingShadow(request);
        fut.get(1L, TimeUnit.SECONDS);
        assertArrayEquals(PAYLOAD, result.getPayload());
    }

    @Test
    public void testUpdateThingShadowRequestCreation() {
        Exception exception = assertThrows(NullPointerException.class, () -> UpdateThingShadowRequest
                .builder()
                .payload(PAYLOAD)
                .build());

        assertEquals(ERROR_MESSAGE_FOR_NO_THING_NAME, exception.getMessage());

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
            DeleteThingShadowResult response = DeleteThingShadowResult.builder()
                    .responseStatus(ShadowResponseStatus.Success)
                    .payload(PAYLOAD)
                    .build();

            writeMessageToSockOutputStream(1, inFrame.requestId, response,
                    FrameReader.FrameType.RESPONSE,
                    BuiltInServiceDestinationCode.SHADOW.getValue(),
                    ShadowImpl.API_VERSION);
            return null;
        });

        DeleteThingShadowResult result = shadow.deleteThingShadow(request);
        fut.get(1L, TimeUnit.SECONDS);
        assertArrayEquals(PAYLOAD, result.getPayload());
    }

    @Test
    public void testDeleteThingShadowRequestCreation() {
        Exception exception = assertThrows(NullPointerException.class, () -> DeleteThingShadowRequest
                .builder()
                .build());

        assertEquals(ERROR_MESSAGE_FOR_NO_THING_NAME, exception.getMessage());
    }


}
