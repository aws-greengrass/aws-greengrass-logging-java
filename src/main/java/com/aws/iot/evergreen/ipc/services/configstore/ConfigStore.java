/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.ipc.services.configstore;

import com.aws.iot.evergreen.ipc.services.configstore.exceptions.ConfigStoreIPCException;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Interface for ConfigStore operations.
 */
public interface ConfigStore {
    /**
     * Subscribe to config changes of a component.
     *
     * @param componentName whose config is to be read, null value treated as self
     * @param keyPath       key path to subscribe to, if null, subscribes to all configuration
     * @param onKeyChange   a callback function called with the config key which changed
     * @throws ConfigStoreIPCException for any error
     */
    void subscribeToConfigurationUpdate(String componentName, List<String> keyPath, Consumer<List<String>> onKeyChange)
            throws ConfigStoreIPCException;

    /**
     * Get a value of component's config keyPath from the config store.
     *
     * @param componentName whose config is to be read, null value treated as self
     * @param keyPath       Which key path to read
     * @throws ConfigStoreIPCException for any error
     */
    Object getConfiguration(String componentName, List<String> keyPath) throws ConfigStoreIPCException;

    /**
     * Update an existing / create new config at the given path.
     *
     * @param componentName Component name whose config is being updated
     * @param keyPath       Path of the key to update
     * @param newValue      Proposed value
     * @param timestamp     Proposed timestamp
     * @throws ConfigStoreIPCException for any error
     */
    void updateConfiguration(String componentName, List<String> keyPath, Object newValue, long timestamp)
            throws ConfigStoreIPCException;

    /**
     * Subscribe to validation of dynamically changing config.
     *
     * @param validationEventHandler a callback function called with the config to be validated
     * @throws ConfigStoreIPCException for any error
     */
    void subscribeToValidateConfiguration(Consumer<Map<String, Object>> validationEventHandler)
            throws ConfigStoreIPCException;

    /**
     * Respond to validate configuration request by reporting validity status.
     *
     * @param status  config validity status to be reported
     * @param message additional description to be used in case of failures in validating
     * @throws ConfigStoreIPCException for any error
     */
    void sendConfigurationValidityReport(ConfigurationValidityStatus status, String message)
            throws ConfigStoreIPCException;
}
