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
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class GetConfigurationResponse extends ConfigStoreGenericResponse {
    // TODO : component name seems redundant
    private String componentName;
    private Object value;

    @Builder
    public GetConfigurationResponse(ConfigStoreResponseStatus responseStatus, String errorMessage, Object value) {
        super(responseStatus, errorMessage);
        this.value = value;
    }
}
