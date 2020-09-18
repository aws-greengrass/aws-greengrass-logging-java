/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.greengrass.ipc.exceptions;

public class IPCClientException extends Exception {
    public IPCClientException() {
        super();
    }

    public IPCClientException(String message) {
        super(message);
    }

    public IPCClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public IPCClientException(Throwable cause) {
        super(cause);
    }
}
