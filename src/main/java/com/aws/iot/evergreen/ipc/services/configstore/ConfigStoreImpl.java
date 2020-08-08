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
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static com.aws.iot.evergreen.ipc.common.BuiltInServiceDestinationCode.CONFIG_STORE;

public class ConfigStoreImpl implements ConfigStore {
    public static final int API_VERSION = 1;
    private static final String CONFIGURATION_KEY_PATH_DELIMITER = ".";
    private final Map<ConfigUpdateListenerId, Consumer<String>> configUpdateCallbacks =
            new ConcurrentHashMap<>();
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
    public synchronized void subscribeToConfigurationUpdate(String componentName, String keyName,
                                                            Consumer<String> onKeyChange)
            throws ConfigStoreIPCException {
        // Register with IPC to re-register the listener when the client reconnects
        ipc.onReconnect(() -> {
            try {
                registerConfigUpdateSubscription(componentName, keyName);
            } catch (ConfigStoreIPCException e) {
                // TODO: Log exception / retry
            }
        });

        ConfigUpdateListenerId configUpdateListenerId = new ConfigUpdateListenerId(componentName, keyName);
        configUpdateCallbacks.putIfAbsent(configUpdateListenerId, onKeyChange);
        registerConfigUpdateSubscription(componentName, keyName);
    }

    @Override
    public Object getConfiguration(String componentName, String key) throws ConfigStoreIPCException {
        return sendAndReceive(ConfigStoreClientOpCodes.GET_CONFIG, new GetConfigurationRequest(componentName, key),
                GetConfigurationResponse.class).getValue();
    }

    @Override
    public void updateConfiguration(String componentName, String key, Object newValue, long timestamp)
            throws ConfigStoreIPCException {
        sendAndReceive(ConfigStoreClientOpCodes.UPDATE_CONFIG,
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
        sendAndReceive(ConfigStoreClientOpCodes.SEND_CONFIG_VALIDATION_REPORT,
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
                    IPCClientImpl.EXECUTOR.execute(() -> getListenersForUpdateEvent(changedEvent.getComponentName(),
                            changedEvent.getChangedKey()).forEach(f -> f.accept(changedEvent.getChangedKey())));
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

    private void registerConfigUpdateSubscription(String componentName, String keyName) throws ConfigStoreIPCException {
        sendAndReceive(ConfigStoreClientOpCodes.SUBSCRIBE_TO_ALL_CONFIG_UPDATES,
                SubscribeToConfigurationUpdateRequest.builder().componentName(componentName).keyName(keyName).build(),
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

    // This is a temporary workaround to make this one channel per client/component implementation work with
    // multiple update subscriptions between the client and a destination component as proposed in API design
    // and implemented on the server side. This workaround is not needed long term as the new SDK will implicitly
    // fulfill this requirement, for now this does the job while avoiding short term adjustments on the server
    // side. A more graceful intermediate approach would be to support multiple channels for one IPC client
    // in the current implementation
    private Set<Consumer<String>> getListenersForUpdateEvent(String componentName, String changedKey) {
        // Subscribed key should always be the parent/an ancestor of the changed key
        // e.g. with below config structure -
        // services:
        //   target_component:
        //     configuration:
        //       level1_key:
        //         level2_key:
        //           level3_key:
        //             level4_key: value
        // If the client has subscribed to level2_key and a change to level4_key triggers an update event,
        // subscription on client side will be tracked as {target_component, level1_key.level2_key} and the
        // key name in the update event from server side will be level1_key.level2_key.level3_key.level4_key
        // We exploit this fact to find out the subscribed key for a received changedKey since the event
        // has component name but not the original key for the subscription for which update event is sent.

        Set<Consumer<String>> listeners = new HashSet<>();
        if (!changedKey.contains(CONFIGURATION_KEY_PATH_DELIMITER)) {
            // changed key is the child of configuration namespace e.g.level1_key in above example
            // check if there is a subscription on all configuration, i.e. subscribe key is null
            Consumer<String> allConfigListener =
                    configUpdateCallbacks.get(new ConfigUpdateListenerId(componentName, null));
            if (allConfigListener != null) {
                listeners.add(allConfigListener);
            }
            Consumer<String> exactKeyListener =
                    configUpdateCallbacks.get(new ConfigUpdateListenerId(componentName, changedKey));
            if (exactKeyListener != null) {
                listeners.add(exactKeyListener);
            }
        }
        // subscribed to specific key under configuration namespace, can be a nested key
        // lookup all component - key subscriptions to find the ones eligible to handle the received changed key
        configUpdateCallbacks.entrySet().stream().filter(e -> e.getKey().componentName.equals(componentName))
                .filter(e -> e.getKey().keySubscribed != null && changedKey.startsWith(e.getKey().keySubscribed))
                .map(e -> listeners.add(e.getValue()));
        return listeners;
    }

    /**
     * Used to track subscriptions for config update by component and subscribed key.
     */
    @EqualsAndHashCode
    @ToString
    @AllArgsConstructor
    private static class ConfigUpdateListenerId {
        private String componentName;
        private String keySubscribed;
    }
}
