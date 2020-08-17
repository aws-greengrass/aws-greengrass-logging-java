/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.ipc.services.lifecycle;

import com.aws.iot.evergreen.ipc.IPCClient;
import com.aws.iot.evergreen.ipc.IPCClientImpl;
import com.aws.iot.evergreen.ipc.common.FrameReader;
import com.aws.iot.evergreen.ipc.services.common.ApplicationMessage;
import com.aws.iot.evergreen.ipc.services.common.IPCUtil;
import com.aws.iot.evergreen.ipc.services.lifecycle.exceptions.LifecycleIPCException;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import static com.aws.iot.evergreen.ipc.common.BuiltInServiceDestinationCode.LIFECYCLE;

public class LifecycleImpl implements Lifecycle {
    public static final int API_VERSION = 1;
    private final IPCClient ipc;

    private final List<Consumer<ComponentUpdateEvent>> callbacks = new CopyOnWriteArrayList<>();

    /**
     * Make the implementation of the Lifecycle client.
     *
     * @param ipc IPCClient used to talk to the server
     */
    public LifecycleImpl(IPCClient ipc) {
        this.ipc = ipc;
        ipc.registerMessageHandler(LIFECYCLE.getValue(), this::onServiceEvent);
    }
    
    private FrameReader.Message onServiceEvent(FrameReader.Message message) {
        try {
            ApplicationMessage request = ApplicationMessage.fromBytes(message.getPayload());
            LifecycleResponseStatus resp = LifecycleResponseStatus.Success;
            if (LifecycleServiceOpCodes.values().length > request.getOpCode()) {
                LifecycleServiceOpCodes opCode = LifecycleServiceOpCodes.values()[request.getOpCode()];
                switch (opCode) {
                    case PRE_COMPONENT_UPDATE_EVENT:
                        invokeCallBacks(IPCUtil.decode(request.getPayload(), PreComponentUpdateEvent.class));
                        break;
                    case POST_COMPONENT_UPDATE_EVENT:
                        invokeCallBacks(IPCUtil.decode(request.getPayload(), PostComponentUpdateEvent.class));
                        break;
                    default:
                        resp = LifecycleResponseStatus.InvalidRequest;
                        break;
                }
            } else {
                resp = LifecycleResponseStatus.InvalidRequest;
            }
            ApplicationMessage responseMessage =
                    ApplicationMessage.builder().version(request.getVersion()).payload(IPCUtil.encode(resp)).build();

            return new FrameReader.Message(responseMessage.toByteArray());
        } catch (IOException ex) {
            // TODO: Log exception or something else.
            //  https://issues.amazon.com/issues/86453f7c-c94e-4a3c-b8ff-679767e7443c
        }
        return new FrameReader.Message(new byte[0]);
    }

    private void invokeCallBacks(ComponentUpdateEvent componentUpdateEvent) {
        for (Consumer<ComponentUpdateEvent> callback : callbacks) {
            IPCClientImpl.EXECUTOR.execute(() -> {
                callback.accept(componentUpdateEvent);
            });
        }
    }

    @Override
    public void updateState(String newState) throws LifecycleIPCException {
        UpdateStateRequest updateStateRequest = UpdateStateRequest.builder().state(newState).build();
        sendAndReceive(LifecycleClientOpCodes.UPDATE_STATE, updateStateRequest, LifecycleGenericResponse.class);
    }

    @Override
    public void subscribeToComponentUpdate(Consumer<ComponentUpdateEvent> componentUpdateEventConsumer)
            throws LifecycleIPCException {

        // Register with IPC to re-register the listener when the client reconnects
        ipc.onReconnect(() -> {
            try {
                subscribeToComponentUpdate();
            } catch (LifecycleIPCException e) {
                // TODO: Log exception / retry
            }
        });
        callbacks.add(componentUpdateEventConsumer);
        subscribeToComponentUpdate();
    }

    private void subscribeToComponentUpdate() throws LifecycleIPCException {
        sendAndReceive(LifecycleClientOpCodes.SUBSCRIBE_COMPONENT_UPDATE, new byte[0], LifecycleGenericResponse.class);
    }

    @Override
    public void deferComponentUpdate(String componentName, long recheckTimeInMs) throws LifecycleIPCException {
        sendAndReceive(LifecycleClientOpCodes.DEFER_COMPONENT_UPDATE,
                new DeferComponentUpdateRequest(componentName, recheckTimeInMs), LifecycleGenericResponse.class);
    }


    private <T> T sendAndReceive(LifecycleClientOpCodes opCode, Object request, final Class<T> returnTypeClass)
            throws LifecycleIPCException {
        try {
            CompletableFuture<T> responseFuture =
                    IPCUtil.sendAndReceive(ipc, LIFECYCLE.getValue(), API_VERSION, opCode.ordinal(), request,
                            returnTypeClass);
            LifecycleGenericResponse response = (LifecycleGenericResponse) responseFuture.get();
            if (!LifecycleResponseStatus.Success.equals(response.getStatus())) {
                throwOnError(response);
            }
            return responseFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new LifecycleIPCException(e);
        }
    }

    private void throwOnError(LifecycleGenericResponse response) throws LifecycleIPCException {
        switch (response.getStatus()) {
            default:
                throw new LifecycleIPCException(response.getErrorMessage());
        }
    }
}
