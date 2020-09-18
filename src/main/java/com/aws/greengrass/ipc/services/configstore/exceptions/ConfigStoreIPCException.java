/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.greengrass.ipc.services.configstore.exceptions;

public class ConfigStoreIPCException extends Exception {
    public ConfigStoreIPCException(Throwable e) {
        super(e);
    }

    public ConfigStoreIPCException(String errorMessage) {
        super(errorMessage);
    }
}
