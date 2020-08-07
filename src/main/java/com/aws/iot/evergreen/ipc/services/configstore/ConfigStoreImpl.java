/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.ipc.services.configstore;

import com.aws.iot.evergreen.ipc.IPCClient;
import com.aws.iot.evergreen.ipc.IPCClientImpl;
import com.aws.iot.evergreen.ipc.common.FrameReader;
import com.aws.iot.evergreen.ipc.services.common.ApplicationMessage;
import com.aws.iot.evergreen.ipc.services.common.IPCUtil;
import com.aws.iot.evergreen.ipc.services.configstore.exceptions.ConfigStoreIPCException;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static com.aws.iot.evergreen.ipc.common.BuiltInServiceDestinationCode.CONFIG_STORE;

public class ConfigStoreImpl implements ConfigStore {
    public static final int API_VERSION = 1;
    private final Map<String, CopyOnWriteArraySet<Consumer<String>>> configUpdateCallbacks = new ConcurrentHashMap<>();
    private final IPCClient ipc;
    private AtomicReference<Consumer<Map<String, Object>>> configValidationCallback = new AtomicReference<>();

    /**
     * Make the implementation of the ConfigStore client.
     *
     * @param ipc IPCClient used to talk to the server
     */
    public ConfigStoreImpl(IPCClient ipc) {
        this.ipc = ipc;
        ipc.registerMessageHandler(CONFIG_STORE.getValue(), this::onServiceEvent);
    }

    @Override
    public synchronized void subscribeToConfigurationUpdate(String componentName, Consumer<String> onKeyChange)
            throws ConfigStoreIPCException {
        // Register with IPC to re-register the listener when the client reconnects
        ipc.onReconnect(() -> {
            try {
                registerConfigUpdateSubscription(componentName);
            } catch (ConfigStoreIPCException e) {
                // TODO: Log exception / retry
            }
        });

        configUpdateCallbacks.putIfAbsent(componentName, new CopyOnWriteArraySet<>());
        configUpdateCallbacks.get(componentName).add(onKeyChange);
        registerConfigUpdateSubscription(componentName);
    }

    @Override
    public Object getConfiguration(String componentName, String key) throws ConfigStoreIPCException {
        return sendAndReceive(ConfigStoreClientOpCodes.GET_CONFIG, new GetConfigurationRequest(componentName, key),
                GetConfigurationResponse.class).getValue();
    }

    @Override
    public void updateConfiguration(String componentName, String key, Object newValue, long timestamp)
            throws ConfigStoreIPCException {
        sendAndReceive(ConfigStoreClientOpCodes.GET_CONFIG,
                UpdateConfigurationRequest.builder().componentName(componentName).key(key).newValue(newValue)
                        .timestamp(timestamp).build(), UpdateConfigurationResponse.class);
    }

    @Override
    public synchronized void subscribeToValidateConfiguration(Consumer<Map<String, Object>> validationEventHandler)
            throws ConfigStoreIPCException {
        // Register with IPC to re-register the listener when the client reconnects
        ipc.onReconnect(() -> {
            try {
                registerConfigValidationSubscription();
            } catch (ConfigStoreIPCException e) {
                // TODO: Log exception / retry
            }
        });
        configValidationCallback.set(validationEventHandler);
        registerConfigValidationSubscription();
    }

    @Override
    public void sendConfigurationValidityReport(ConfigurationValidityStatus status, String message)
            throws ConfigStoreIPCException {
        sendAndReceive(ConfigStoreClientOpCodes.REPORT_CONFIG_VALIDITY,
                SendConfigurationValidityReportRequest.builder().status(status).message(message).build(),
                SendConfigurationValidityReportResponse.class);
    }

    private FrameReader.Message onServiceEvent(FrameReader.Message message) {
        try {
            ApplicationMessage request = ApplicationMessage.fromBytes(message.getPayload());
            ConfigStoreResponseStatus resp = ConfigStoreResponseStatus.Success;
            ConfigStoreServiceOpCodes opCode = ConfigStoreServiceOpCodes.values()[request.getOpCode()];
            switch (opCode) {
                case KEY_CHANGED:
                    ConfigurationUpdateEvent changedEvent =
                            IPCUtil.decode(request.getPayload(), ConfigurationUpdateEvent.class);
                    IPCClientImpl.EXECUTOR.execute(() -> configUpdateCallbacks.get(changedEvent.getComponentName())
                            .forEach(f -> f.accept(changedEvent.getChangedKey())));
                    break;
                case VALIDATION_EVENT:
                    ValidateConfigurationUpdateEvent validateEvent =
                            IPCUtil.decode(request.getPayload(), ValidateConfigurationUpdateEvent.class);
                    IPCClientImpl.EXECUTOR
                            .execute(() -> configValidationCallback.get().accept(validateEvent.getConfiguration()));
                    break;
                default:
                    resp = ConfigStoreResponseStatus.InvalidRequest;
                    break;
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

    private void registerConfigUpdateSubscription(String componentName) throws ConfigStoreIPCException {
        sendAndReceive(ConfigStoreClientOpCodes.SUBSCRIBE_TO_ALL_CONFIG_UPDATES,
                SubscribeToConfigurationUpdateRequest.builder().componentName(componentName).build(),
                ConfigStoreGenericResponse.class);
    }

    private void registerConfigValidationSubscription() throws ConfigStoreIPCException {
        sendAndReceive(ConfigStoreClientOpCodes.SUBSCRIBE_TO_CONFIG_VALIDATION,
                SubscribeToValidateConfigurationRequest.builder().build(), ConfigStoreGenericResponse.class);
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
