/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.ipc.services.servicediscovery;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RegisterResourceResponse extends ServiceDiscoveryGenericResponse {

    /**
     * Resource that was registered.
     */
    private Resource resource;

    @Builder
    public RegisterResourceResponse(ServiceDiscoveryResponseStatus responseStatus,
                                    String errorMessage, Resource resource) {
        super(responseStatus, errorMessage);
        this.resource = resource;
    }
}
