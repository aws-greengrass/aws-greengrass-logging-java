/*
 * Copyright Amazon.com Inc. or its affiliates.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.ipc.services.secret;

import com.aws.iot.evergreen.ipc.common.BaseIPCTest;
import com.aws.iot.evergreen.ipc.common.BuiltInServiceDestinationCode;
import com.aws.iot.evergreen.ipc.common.FrameReader;
import com.aws.iot.evergreen.ipc.services.secret.exception.SecretIPCException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.aws.iot.evergreen.ipc.common.FrameReader.readFrame;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class SecretIPCTest extends BaseIPCTest {

    private static final String SECRET_ID = "secret";
    private static final String VERSION_ID = "Id";
    private static final String VERSION_LABEL = "Label";
    private static final String SECRET_VALUE = "Secret";

    @Test
    public void testGetSecret() throws SecretIPCException, ExecutionException, InterruptedException, TimeoutException {
        Secret secret = new SecretImpl(ipc);
        GetSecretValueRequest request = GetSecretValueRequest
                .builder()
                .secretId(SECRET_ID)
                .versionId(VERSION_ID)
                .versionStage(VERSION_LABEL)
                .build();

        Future<?> fut = executor.submit(() -> {
            FrameReader.MessageFrame inFrame = readFrame(in);
            GetSecretValueResult result = GetSecretValueResult.builder().secretId(SECRET_ID)
                    .versionId(VERSION_ID).versionStages(Collections.singletonList(VERSION_LABEL))
                    .secretString(SECRET_VALUE).responseStatus(SecretResponseStatus.Success)
                    .build();

            writeMessageToSockOutputStream(1, inFrame.requestId, result,
                    FrameReader.FrameType.RESPONSE, BuiltInServiceDestinationCode.SECRET.getValue());
            return null;
        });

        GetSecretValueResult result = secret.getSecretValue(request);
        fut.get(1L, TimeUnit.SECONDS);
        assertEquals(SECRET_ID, result.getSecretId());
        assertEquals(VERSION_LABEL, result.getVersionStages().get(0));
        assertEquals(VERSION_ID, result.getVersionId());
        assertEquals(SECRET_VALUE, result.getSecretString());
    }
}
