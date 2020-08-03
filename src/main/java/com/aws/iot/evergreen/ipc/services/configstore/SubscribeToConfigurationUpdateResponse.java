/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.ipc.services.configstore;

import lombok.Builder;
import lombok.Data;

@Data
public class SubscribeToConfigurationUpdateResponse extends ConfigStoreGenericResponse {

    @Builder
    public SubscribeToConfigurationUpdateResponse(ConfigStoreResponseStatus responseStatus, String errorMessage) {
        super(responseStatus, errorMessage);
    }
}
