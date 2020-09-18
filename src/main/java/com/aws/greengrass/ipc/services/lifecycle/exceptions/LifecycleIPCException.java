/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.greengrass.ipc.services.lifecycle.exceptions;

public class LifecycleIPCException extends Exception {
    public LifecycleIPCException(Throwable e) {
        super(e);
    }

    public LifecycleIPCException(String errorMessage) {
        super(errorMessage);
    }
}
