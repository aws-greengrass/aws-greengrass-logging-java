/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.ipc.services.configstore;

import com.aws.iot.evergreen.ipc.services.configstore.exceptions.ConfigStoreIPCException;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Interface for ConfigStore operations.
 */
public interface ConfigStore {
    /**
     * Subscribe to ConfigStore changes. Callback will be called immediately with the
     * initial values.
     *
     * @param onChange a callback function called the new state
     * @throws ConfigStoreIPCException for any error
     */
    void subscribe(Consumer<Map<String, Object>> onChange) throws ConfigStoreIPCException;
}
