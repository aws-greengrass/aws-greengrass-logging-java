/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.greengrass.ipc.services.configstore;

public enum ConfigStoreClientOpCodes {
    SUBSCRIBE_TO_ALL_CONFIG_UPDATES, GET_CONFIG, UPDATE_CONFIG, SUBSCRIBE_TO_CONFIG_VALIDATION,
    SEND_CONFIG_VALIDATION_REPORT;
}
