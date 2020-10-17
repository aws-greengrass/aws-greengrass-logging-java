/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.greengrass.ipc.services.lifecycle;

import com.aws.greengrass.ipc.common.GenericErrors;

public enum LifecycleResponseStatus implements GenericErrors {
    Success, InternalError, InvalidRequest, Unauthorized
}