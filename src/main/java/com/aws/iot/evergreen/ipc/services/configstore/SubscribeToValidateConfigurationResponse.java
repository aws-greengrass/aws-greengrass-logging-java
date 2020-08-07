/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.ipc.services.configstore;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SubscribeToValidateConfigurationResponse extends ConfigStoreGenericResponse {

    @Builder
    public SubscribeToValidateConfigurationResponse(ConfigStoreResponseStatus responseStatus, String errorMessage) {
        super(responseStatus, errorMessage);
    }
}
