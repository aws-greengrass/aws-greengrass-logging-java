package com.aws.iot.evergreen.ipc.services.lifecycle;

import com.aws.iot.evergreen.ipc.IPCClient;
import com.aws.iot.evergreen.ipc.common.FrameReader;
import com.aws.iot.evergreen.ipc.services.common.GeneralRequest;
import com.aws.iot.evergreen.ipc.services.common.GeneralResponse;
import com.aws.iot.evergreen.ipc.services.common.SendAndReceiveIPCUtil;
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
    private static ObjectMapper mapper = new CBORMapper();
    private final IPCClient ipc;
    private final static Map<String, List<BiConsumer<String, String>>> stateTransitionCallbacks = new ConcurrentHashMap<>();

    public LifecycleImpl(IPCClient ipc) {
        this.ipc = ipc;
        ipc.registerDestination(LIFECYCLE_SERVICE_NAME, LifecycleImpl::onStateTransition);
    }

    private static FrameReader.Message onStateTransition(FrameReader.Message message) {
        try {
            GeneralRequest<Object, LifecycleRequestTypes> obj = SendAndReceiveIPCUtil.decode(message, new TypeReference<GeneralRequest<Object, LifecycleRequestTypes>>() {});

            GeneralResponse<Void, LifecycleResponseStatus> response = GeneralResponse.<Void, LifecycleResponseStatus>builder().build();
            if (LifecycleRequestTypes.transition.equals(obj.getType())) {
                StateTransitionEvent transitionRequest = mapper.convertValue(obj.getRequest(), StateTransitionEvent.class);
                List<BiConsumer<String, String>> callbacks = stateTransitionCallbacks.get(transitionRequest.getService());
                if (callbacks != null) {
                    callbacks.forEach(f -> {
                        try {
                            f.accept(transitionRequest.getOldState(), transitionRequest.getNewState());
                        } catch (Throwable ignored) {
                        }
                    });

                    response.setError(LifecycleResponseStatus.Success);
                } else {
                    response.setError(LifecycleResponseStatus.Unknown);
                }
            } else {
                response.setError(LifecycleResponseStatus.Unknown);
            }
            return new FrameReader.Message(SendAndReceiveIPCUtil.encode(response));
        } catch (IOException ignored) {

        }
        return new FrameReader.Message(new byte[0]);
    }

    @Override
    public void requestStateChange(String newState) throws LifecycleIPCException {
        GeneralRequest<Object, LifecycleRequestTypes> request = GeneralRequest.<Object, LifecycleRequestTypes>builder().
                type(LifecycleRequestTypes.setState).
                request(StateChangeRequest.builder()
                        .state(newState).build())
                .build();

        sendAndReceive(request, new TypeReference<GeneralResponse<Void, LifecycleResponseStatus>>() {});
    }

    @Override
    public synchronized void listenToStateChanges(String serviceName, BiConsumer<String, String> onTransition) throws LifecycleIPCException {
        GeneralRequest<Object, LifecycleRequestTypes> request = GeneralRequest.<Object, LifecycleRequestTypes>builder()
                .type(LifecycleRequestTypes.listen)
                .request(LifecycleListenRequest.builder()
                        .serviceName(serviceName).build())
                .build();
        sendAndReceive(request, new TypeReference<GeneralResponse<Void, LifecycleResponseStatus>>() {
        });
        stateTransitionCallbacks.compute(serviceName, (key, old) -> {
            if (old == null) {
                old = new CopyOnWriteArrayList<>();
            }
            old.add(onTransition);
            return old;
        });
    }

    private <T> T sendAndReceive(GeneralRequest<Object, LifecycleRequestTypes> data, TypeReference<GeneralResponse<T, LifecycleResponseStatus>> clazz) throws LifecycleIPCException {
        try {
            GeneralResponse<T, LifecycleResponseStatus> req = SendAndReceiveIPCUtil.sendAndReceive(ipc, LIFECYCLE_SERVICE_NAME, data, clazz).get();
            if (!LifecycleResponseStatus.Success.equals(req.getError())) {
                throwOnError(req);
            }

            return req.getResponse();
        } catch (InterruptedException | ExecutionException e) {
            throw new LifecycleIPCException(e);
        }
    }

    private void throwOnError(GeneralResponse<?, LifecycleResponseStatus> req) throws LifecycleIPCException {
        switch (req.getError())  {
            default:
                throw new LifecycleIPCException(req.getErrorMessage());
        }
    }
}
