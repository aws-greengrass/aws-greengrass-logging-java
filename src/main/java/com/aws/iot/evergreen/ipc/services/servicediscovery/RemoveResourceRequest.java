/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.ipc.services.servicediscovery;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Remove a resource.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RemoveResourceRequest {
    /**
     * Definition of the resource to remove.
     */
    private Resource resource;
}
