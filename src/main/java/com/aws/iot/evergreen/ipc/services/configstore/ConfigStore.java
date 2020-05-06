/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.ipc.services.configstore;

import com.aws.iot.evergreen.ipc.services.configstore.exceptions.ConfigStoreIPCException;

import java.util.function.Consumer;

/**
 * Interface for ConfigStore operations.
 */
public interface ConfigStore {
    /**
     * Subscribe to ConfigStore changes.
     *
     * @param onKeyChange a callback function called with the config key which changed
     * @throws ConfigStoreIPCException for any error
     */
    void subscribe(Consumer<String> onKeyChange) throws ConfigStoreIPCException;

    /**
     * Read a value from the config store.
     *
     * @param key Which key to read
     * @throws ConfigStoreIPCException for any error
     */
    Object read(String key) throws ConfigStoreIPCException;
}
