/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.ipc.services.configstore;

import lombok.Builder;
import lombok.Data;

@Data
public class UpdateConfigurationResponse extends ConfigStoreGenericResponse {
    @Builder
    public UpdateConfigurationResponse(ConfigStoreResponseStatus responseStatus, String errorMessage) {
        super(responseStatus, errorMessage);
    }
}
