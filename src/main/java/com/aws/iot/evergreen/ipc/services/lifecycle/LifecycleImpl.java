/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.ipc.services.lifecycle;

import com.aws.iot.evergreen.ipc.IPCClient;
import com.aws.iot.evergreen.ipc.common.FrameReader;
import com.aws.iot.evergreen.ipc.services.common.ApplicationMessage;
import com.aws.iot.evergreen.ipc.services.common.IPCUtil;
import com.aws.iot.evergreen.ipc.services.lifecycle.exceptions.LifecycleIPCException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;

import static com.aws.iot.evergreen.ipc.common.BuiltInServiceDestinationCode.LIFECYCLE;

public class LifecycleImpl implements Lifecycle {
    public static final int API_VERSION = 1;
    // Service Name ==> list of [function (oldState, newState)]
    private final Map<String, Set<BiConsumer<String, String>>> stateTransitionCallbacks = new ConcurrentHashMap<>();
    private final IPCClient ipc;

    /**
     * Make the implementation of the Lifecycle client.
     *
     * @param ipc IPCClient used to talk to the server
     */
    public LifecycleImpl(IPCClient ipc) {
        this.ipc = ipc;
        ipc.registerMessageHandler(LIFECYCLE.getValue(), this::onStateTransition);
    }

    private FrameReader.Message onStateTransition(FrameReader.Message message) {
        try {
            ApplicationMessage request = ApplicationMessage.fromBytes(message.getPayload());
            LifecycleResponseStatus resp = LifecycleResponseStatus.Success;
            if (LifecycleClientOpCodes.STATE_TRANSITION.equals(LifecycleClientOpCodes.values()[request.getOpCode()])) {
                StateTransitionEvent transitionRequest =
                        IPCUtil.decode(request.getPayload(), StateTransitionEvent.class);
                Set<BiConsumer<String, String>> callbacks =
                        stateTransitionCallbacks.get(transitionRequest.getService());
                if (callbacks != null) {
                    callbacks.forEach(f -> f.accept(transitionRequest.getOldState(), transitionRequest.getNewState()));
                } else {
                    resp = LifecycleResponseStatus.StateTransitionCallbackNotFound;
                }
            } else {
                resp = LifecycleResponseStatus.InvalidRequest;
            }
            ApplicationMessage responseMessage = ApplicationMessage.builder()
                    .version(request.getVersion()).payload(IPCUtil.encode(resp)).build();

            return new FrameReader.Message(responseMessage.toByteArray());
        } catch (IOException ex) {
            // TODO: Log exception or something else.
            //  https://issues.amazon.com/issues/86453f7c-c94e-4a3c-b8ff-679767e7443c
        }
        return new FrameReader.Message(new byte[0]);
    }

    @Override
    public void onStopping(Runnable handler) throws LifecycleIPCException {
        String serviceName = ipc.getServiceName();

        listenToStateChanges(serviceName, (oldState, newState) -> {
            if (newState.equals("STOPPING")) {
                handler.run();
            }
        });
    }

    @Override
    public void reportState(String newState) throws LifecycleIPCException {
        StateChangeRequest stateChangeRequest = StateChangeRequest.builder().state(newState).build();
        sendAndReceive(LifecycleServiceOpCodes.REPORT_STATE, stateChangeRequest, LifecycleGenericResponse.class);
    }

    @Override
    public synchronized void listenToStateChanges(String serviceName, BiConsumer<String, String> onTransition)
            throws LifecycleIPCException {
        // Only register with the server if we haven't yet requested updates for a given service name
        if (!stateTransitionCallbacks.containsKey(serviceName)) {
            registerStateListener(serviceName);
            // Register with IPC to re-register the listener when the client reconnects
            ipc.onReconnect(() -> {
                try {
                    registerStateListener(serviceName);
                } catch (LifecycleIPCException e) {
                    // TODO: Log exception / retry
                }
            });
        }

        stateTransitionCallbacks.compute(serviceName, (key, old) -> {
            if (old == null) {
                old = new CopyOnWriteArraySet<>();
            }
            old.add(onTransition);
            return old;
        });
    }

    private void registerStateListener(String serviceName) throws LifecycleIPCException {
        LifecycleListenRequest listenRequest = LifecycleListenRequest.builder().serviceName(serviceName).build();
        sendAndReceive(LifecycleServiceOpCodes.REGISTER_LISTENER, listenRequest, LifecycleGenericResponse.class);
    }

    private <T> T sendAndReceive(LifecycleServiceOpCodes opCode, Object request, final Class<T> returnTypeClass)
            throws LifecycleIPCException {
        try {
            CompletableFuture<T> responseFuture = IPCUtil.sendAndReceive(
                    ipc, LIFECYCLE.getValue(), API_VERSION, opCode.ordinal(), request, returnTypeClass);
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
