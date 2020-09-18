/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.greengrass.ipc.services.configstore;

import com.aws.greengrass.ipc.common.GenericErrors;

public enum ConfigStoreResponseStatus implements GenericErrors {
    Success, InternalError, InvalidRequest, Unauthorized, ResourceNotFoundError, FailedUpdateConditionCheck;
}
