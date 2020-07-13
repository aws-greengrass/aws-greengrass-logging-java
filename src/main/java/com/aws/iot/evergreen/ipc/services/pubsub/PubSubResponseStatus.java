/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.ipc.services.pubsub;

import com.aws.iot.evergreen.ipc.common.GenericErrors;

public enum PubSubResponseStatus implements GenericErrors {
    Success, InternalError, InvalidRequest, TopicNotFound, Unauthorized;
}
