/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.ipc.services.configstore;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetConfigurationRequest {
    private String componentName;
    // TODO : 1. Key should probably be string array to denote path for nested config keys
    //  using '.' as delimiter can prevent configuration key names from containing the delimiter
    //  2. Currently null key denotes all component configuration for Get/Subscribe operations,
    //  decide if this is desirable for series of calls and overall experience
    private String key;
}
