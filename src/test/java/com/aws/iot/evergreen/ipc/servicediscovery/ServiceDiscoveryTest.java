/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.ipc.servicediscovery;

import com.aws.iot.evergreen.ipc.common.BaseIPCTest;
import com.aws.iot.evergreen.ipc.common.BuiltInServiceDestinationCode;
import com.aws.iot.evergreen.ipc.services.servicediscovery.RegisterResourceRequest;
import com.aws.iot.evergreen.ipc.services.servicediscovery.RegisterResourceResponse;
import com.aws.iot.evergreen.ipc.services.servicediscovery.Resource;
import com.aws.iot.evergreen.ipc.services.servicediscovery.ServiceDiscovery;
import com.aws.iot.evergreen.ipc.services.servicediscovery.ServiceDiscoveryImpl;
import com.aws.iot.evergreen.ipc.services.servicediscovery.ServiceDiscoveryResponseStatus;
import com.aws.iot.evergreen.ipc.services.servicediscovery.UpdateResourceRequest;
import com.aws.iot.evergreen.ipc.services.servicediscovery.exceptions.AlreadyRegisteredException;
import com.aws.iot.evergreen.ipc.services.servicediscovery.exceptions.ServiceDiscoveryException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.Future;

import static com.aws.iot.evergreen.ipc.common.FrameReader.FrameType;
import static com.aws.iot.evergreen.ipc.common.FrameReader.MessageFrame;
import static com.aws.iot.evergreen.ipc.common.FrameReader.readFrame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class ServiceDiscoveryTest extends BaseIPCTest {

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
            writeMessageToSockOutputStream(1, inFrame.requestId, registerResourceResponse,
                    FrameType.RESPONSE,
                    BuiltInServiceDestinationCode.SERVICE_DISCOVERY.getValue(),
                    ServiceDiscoveryImpl.API_VERSION);
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
            writeMessageToSockOutputStream(1, inFrame.requestId, registerResourceResponse,
                    FrameType.RESPONSE,
                    BuiltInServiceDestinationCode.SERVICE_DISCOVERY.getValue(),
                    ServiceDiscoveryImpl.API_VERSION);
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

            writeMessageToSockOutputStream(1, inFrame.requestId, updateResourceRequest,
                    FrameType.RESPONSE,
                    BuiltInServiceDestinationCode.SERVICE_DISCOVERY.getValue(),
                    ServiceDiscoveryImpl.API_VERSION);
            return null;
        });

        assertThrows(ServiceDiscoveryException.class, () -> sd.registerResource(req));
        fut.get();
    }
}