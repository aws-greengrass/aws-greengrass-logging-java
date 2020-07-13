/*
 * Copyright Amazon.com Inc. or its affiliates.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.ipc.services.pubsub;

public class PubSubException extends Exception {
    public PubSubException(String message) {
        super(message);
    }

    public PubSubException(Throwable cause) {
        super(cause);
    }
}
