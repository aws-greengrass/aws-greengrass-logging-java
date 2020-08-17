/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.ipc.common;

public enum BuiltInServiceDestinationCode {

    AUTHENTICATION(0), LIFECYCLE(1), SERVICE_DISCOVERY(2), CONFIG_STORE(3), PUBSUB(4), SECRET(5),
    AUTHORIZATION(6), CLI(7), ERROR(255);

    private final int value;

    BuiltInServiceDestinationCode(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
