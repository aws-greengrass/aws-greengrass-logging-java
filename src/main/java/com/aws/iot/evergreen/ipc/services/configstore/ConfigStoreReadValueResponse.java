/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.ipc.services.configstore;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ConfigStoreReadValueResponse extends ConfigStoreGenericResponse {
    private Object value;

    @Builder
    public ConfigStoreReadValueResponse(ConfigStoreResponseStatus responseStatus, String errorMessage, Object value) {
        super(responseStatus, errorMessage);
        this.value = value;
    }
}
