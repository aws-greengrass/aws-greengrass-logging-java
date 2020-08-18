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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static com.aws.iot.evergreen.ipc.common.BuiltInServiceDestinationCode.CONFIG_STORE;

public class ConfigStoreImpl implements ConfigStore {
    public static final int API_VERSION = 1;
    // Component + subscribed key path --> Set of registered actions that can handle the changed key path
    private final Map<ConfigUpdateListenerId, Set<Consumer<List<String>>>> configUpdateCallbacks =
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
    public synchronized void subscribeToConfigurationUpdate(String componentName, List<String> keyPath,
                                                            Consumer<List<String>> onKeyChange)
            throws ConfigStoreIPCException {
        // Register with IPC to re-register the listener when the client reconnects
        ipc.onReconnect(() -> {
            try {
                registerConfigUpdateSubscription(componentName, keyPath);
            } catch (ConfigStoreIPCException e) {
                // TODO: Log exception / retry
            }
        });

        ConfigUpdateListenerId configUpdateListenerId = new ConfigUpdateListenerId(componentName, keyPath);
        configUpdateCallbacks.putIfAbsent(configUpdateListenerId, new CopyOnWriteArraySet<>());
        configUpdateCallbacks.get(configUpdateListenerId).add(onKeyChange);
        registerConfigUpdateSubscription(componentName, keyPath);
    }

    @Override
    public Object getConfiguration(String componentName, List<String> keyPath) throws ConfigStoreIPCException {
        return sendAndReceive(ConfigStoreClientOpCodes.GET_CONFIG, new GetConfigurationRequest(componentName, keyPath),
                GetConfigurationResponse.class).getValue();
    }

    @Override
    public void updateConfiguration(String componentName, List<String> keyPath, Object newValue, long timestamp,
                                    Object currentValue) throws ConfigStoreIPCException {
        sendAndReceive(ConfigStoreClientOpCodes.UPDATE_CONFIG,
                UpdateConfigurationRequest.builder().componentName(componentName).keyPath(keyPath).newValue(newValue)
                        .timestamp(timestamp).currentValue(currentValue).build(), UpdateConfigurationResponse.class);
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
                SendConfigurationValidityReportRequest.builder().configurationValidityReport(
                        ConfigurationValidityReport.builder().status(status).message(message).build()).build(),
                SendConfigurationValidityReportResponse.class);
    }

    private FrameReader.Message onServiceEvent(FrameReader.Message message) {
        try {
            ApplicationMessage request = ApplicationMessage.fromBytes(message.getPayload());
            ConfigStoreResponseStatus resp = ConfigStoreResponseStatus.Success;
            if (ConfigStoreServiceOpCodes.values().length > request.getOpCode()) {
                ConfigStoreServiceOpCodes opCode = ConfigStoreServiceOpCodes.values()[request.getOpCode()];
                switch (opCode) {
                    case KEY_CHANGED:
                        ConfigurationUpdateEvent changedEvent =
                                IPCUtil.decode(request.getPayload(), ConfigurationUpdateEvent.class);
                        IPCClientImpl.EXECUTOR.execute(() -> getListenersForUpdateEvent(changedEvent.getComponentName(),
                                changedEvent.getChangedKeyPath())
                                .forEach(f -> f.accept(changedEvent.getChangedKeyPath())));
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

    private void registerConfigUpdateSubscription(String componentName, List<String> keyPath)
            throws ConfigStoreIPCException {
        sendAndReceive(ConfigStoreClientOpCodes.SUBSCRIBE_TO_ALL_CONFIG_UPDATES,
                SubscribeToConfigurationUpdateRequest.builder().componentName(componentName).keyPath(keyPath).build(),
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
    private Set<Consumer<List<String>>> getListenersForUpdateEvent(String componentName, List<String> changedKeyPath) {
        // Subscribed key should always be either be the parent/an ancestor of the changed key or same as the changed
        // key
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

        Set<Consumer<List<String>>> listeners = new HashSet<>();
        configUpdateCallbacks.entrySet().stream()
                .filter(callbacks -> callbacks.getKey().canHandle(componentName, changedKeyPath))
                .forEach(callbacks -> listeners.addAll(callbacks.getValue()));

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
        private List<String> subscribedKeyPath;

        boolean canHandle(String componentName, List<String> changedKeyPath) {
            return this.componentName.equals(componentName) && startsWith(changedKeyPath, this.subscribedKeyPath);
        }

        boolean startsWith(List<String> bigList, List<String> smallList) {
            if (smallList == null || smallList.isEmpty()) {
                return true;
            }
            if (bigList == null || bigList.isEmpty()) {
                return false;
            }
            if (bigList.size() < smallList.size()) {
                return false;
            }
            for (String part : smallList) {
                if (!part.equals(bigList.get(smallList.indexOf(part)))) {
                    return false;
                }
            }
            return true;
        }
    }
}
