package com.aws.iot.evergreen.ipc.services.lifecycle;

import com.aws.iot.evergreen.ipc.IPCClient;
import com.aws.iot.evergreen.ipc.common.FrameReader;
import com.aws.iot.evergreen.ipc.services.common.GeneralRequest;
import com.aws.iot.evergreen.ipc.services.common.GeneralResponse;
import com.aws.iot.evergreen.ipc.services.common.IPCUtil;
import com.aws.iot.evergreen.ipc.services.lifecycle.exceptions.LifecycleIPCException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;

public class LifecycleImpl implements Lifecycle {
    private static final Map<String, List<BiConsumer<String, String>>> stateTransitionCallbacks =
            new ConcurrentHashMap<>();
    private static ObjectMapper mapper = new CBORMapper();
    private final IPCClient ipc;

    public LifecycleImpl(IPCClient ipc) {
        this.ipc = ipc;
        ipc.registerDestination(LIFECYCLE_SERVICE_NAME, LifecycleImpl::onStateTransition);
    }

    private static FrameReader.Message onStateTransition(FrameReader.Message message) {
        try {
            GeneralRequest<Object, LifecycleRequestTypes> obj =
                    IPCUtil.decode(message, new TypeReference<GeneralRequest<Object, LifecycleRequestTypes>>() {
                    });

            GeneralResponse<Void, LifecycleResponseStatus> response =
                    GeneralResponse.<Void, LifecycleResponseStatus>builder().build();
            if (LifecycleRequestTypes.transition.equals(obj.getType())) {
                StateTransitionEvent transitionRequest =
                        mapper.convertValue(obj.getRequest(), StateTransitionEvent.class);
                List<BiConsumer<String, String>> callbacks =
                        stateTransitionCallbacks.get(transitionRequest.getService());
                if (callbacks != null) {
                    callbacks.forEach(f -> f.accept(transitionRequest.getOldState(), transitionRequest.getNewState()));

                    response.setError(LifecycleResponseStatus.Success);
                } else {
                    response.setError(LifecycleResponseStatus.StateTransitionCallbackNotFound);
                }
            } else {
                response.setError(LifecycleResponseStatus.InvalidRequest);
            }
            return new FrameReader.Message(IPCUtil.encode(response));
        } catch (IOException ex) {
            // TODO: Log exception or something else.
            //  https://issues.amazon.com/issues/86453f7c-c94e-4a3c-b8ff-679767e7443c
        }
        return new FrameReader.Message(new byte[0]);
    }

    @Override
    public void onStopping(Runnable handler) throws LifecycleIPCException {
        String serviceName = ipc.getServiceName();
        if (serviceName == null) {
            throw new LifecycleIPCException("This service name is unknown. Try re-connecting.");
        }

        listenToStateChanges(serviceName, (oldState, newState) -> {
            if (newState.equals("Shutdown")) {
                handler.run();
            }
        });
    }

    @Override
    public void reportState(String newState) throws LifecycleIPCException {
        GeneralRequest<Object, LifecycleRequestTypes> request =
                GeneralRequest.<Object, LifecycleRequestTypes>builder().type(LifecycleRequestTypes.setState)
                        .request(StateChangeRequest.builder().state(newState).build()).build();

        sendAndReceive(request, new TypeReference<GeneralResponse<Void, LifecycleResponseStatus>>() {
        });
    }

    @Override
    public synchronized void listenToStateChanges(String serviceName, BiConsumer<String, String> onTransition)
            throws LifecycleIPCException {
        registerStateListener(serviceName);

        // Register with IPC to re-register the listener when the client reconnects
        ipc.onReconnect(() -> {
            try {
                registerStateListener(serviceName);
            } catch (LifecycleIPCException e) {
                // TODO: Log exception / retry
            }
        });

        stateTransitionCallbacks.compute(serviceName, (key, old) -> {
            if (old == null) {
                old = new CopyOnWriteArrayList<>();
            }
            old.add(onTransition);
            return old;
        });
    }

    private void registerStateListener(String serviceName) throws LifecycleIPCException {
        GeneralRequest<Object, LifecycleRequestTypes> request =
                GeneralRequest.<Object, LifecycleRequestTypes>builder().type(LifecycleRequestTypes.listen)
                        .request(LifecycleListenRequest.builder().serviceName(serviceName).build()).build();
        sendAndReceive(request, new TypeReference<GeneralResponse<Void, LifecycleResponseStatus>>() {
        });
    }

    private <T> T sendAndReceive(GeneralRequest<Object, LifecycleRequestTypes> data,
                                 TypeReference<GeneralResponse<T, LifecycleResponseStatus>> clazz)
            throws LifecycleIPCException {
        try {
            GeneralResponse<T, LifecycleResponseStatus> req =
                    IPCUtil.sendAndReceive(ipc, LIFECYCLE_SERVICE_NAME, data, clazz).get();
            if (!LifecycleResponseStatus.Success.equals(req.getError())) {
                throwOnError(req);
            }

            return req.getResponse();
        } catch (InterruptedException | ExecutionException e) {
            throw new LifecycleIPCException(e);
        }
    }

    private void throwOnError(GeneralResponse<?, LifecycleResponseStatus> req) throws LifecycleIPCException {
        switch (req.getError()) {
            default:
                throw new LifecycleIPCException(req.getErrorMessage());
        }
    }
}
