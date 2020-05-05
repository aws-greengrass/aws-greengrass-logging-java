/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.ipc.services.configstore;

import com.aws.iot.evergreen.ipc.IPCClient;
import com.aws.iot.evergreen.ipc.common.FrameReader;
import com.aws.iot.evergreen.ipc.services.common.ApplicationMessage;
import com.aws.iot.evergreen.ipc.services.common.IPCUtil;
import com.aws.iot.evergreen.ipc.services.configstore.exceptions.ConfigStoreIPCException;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import static com.aws.iot.evergreen.ipc.common.BuiltInServiceDestinationCode.CONFIG_STORE;

public class ConfigStoreImpl implements ConfigStore {
    public static final int API_VERSION = 1;
    private final Set<Consumer<String>> callbacks = new CopyOnWriteArraySet<>();
    private final IPCClient ipc;

    /**
     * Make the implementation of the ConfigStore client.
     *
     * @param ipc IPCClient used to talk to the server
     */
    public ConfigStoreImpl(IPCClient ipc) {
        this.ipc = ipc;
        ipc.registerMessageHandler(CONFIG_STORE.getValue(), this::onKeyChange);
    }

    @Override
    public synchronized void subscribe(Consumer<String> onKeyChange) throws ConfigStoreIPCException {
        // Register with IPC to re-register the listener when the client reconnects
        ipc.onReconnect(() -> {
            try {
                registerSubscription();
            } catch (ConfigStoreIPCException e) {
                // TODO: Log exception / retry
            }
        });

        callbacks.add(onKeyChange);
        registerSubscription();
    }

    @Override
    public Object read(String key) throws ConfigStoreIPCException {
        return sendAndReceive(ConfigStoreClientOpCodes.READ_KEY, new ConfigStoreReadValueRequest(key),
                ConfigStoreReadValueResponse.class).getValue();
    }

    private FrameReader.Message onKeyChange(FrameReader.Message message) {
        try {
            ApplicationMessage request = ApplicationMessage.fromBytes(message.getPayload());
            ConfigStoreResponseStatus resp = ConfigStoreResponseStatus.Success;
            if (ConfigStoreServiceOpCodes.KEY_CHANGED
                    .equals(ConfigStoreServiceOpCodes.values()[request.getOpCode()])) {
                ConfigKeyChangedEvent changedEvent =
                        IPCUtil.decode(request.getPayload(), ConfigKeyChangedEvent.class);

                callbacks.forEach(f -> f.accept(changedEvent.getChangedKey()));
            } else {
                resp = ConfigStoreResponseStatus.InvalidRequest;
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

    private void registerSubscription() throws ConfigStoreIPCException {
        sendAndReceive(ConfigStoreClientOpCodes.SUBSCRIBE_ALL, new ConfigStoreSubscribeRequest(),
                ConfigStoreGenericResponse.class);
    }

    private <T> T sendAndReceive(ConfigStoreClientOpCodes opCode, Object request, final Class<T> returnTypeClass)
            throws ConfigStoreIPCException {
        try {
            CompletableFuture<T> responseFuture =
                    IPCUtil.sendAndReceive(ipc, CONFIG_STORE.getValue(), API_VERSION, opCode.ordinal(), request,
                            returnTypeClass);
            ConfigStoreGenericResponse response = (ConfigStoreGenericResponse) responseFuture.get();
            if (!ConfigStoreResponseStatus.Success.equals(response.getStatus())) {
                throwOnError(response);
            }
            return responseFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new ConfigStoreIPCException(e);
        }
    }

    private void throwOnError(ConfigStoreGenericResponse response) throws ConfigStoreIPCException {
        switch (response.getStatus()) {
            default:
                throw new ConfigStoreIPCException(response.getErrorMessage());
        }
    }
}
