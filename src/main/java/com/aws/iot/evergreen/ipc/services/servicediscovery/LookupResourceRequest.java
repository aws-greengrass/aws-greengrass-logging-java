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
 * Request to lookup a resource.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LookupResourceRequest {
    /**
     * Resource to look up. If any field in this resource is null, then a fuzzy search will be performed,
     * accepting any value for the null field.
     */
    private Resource resource;
}
