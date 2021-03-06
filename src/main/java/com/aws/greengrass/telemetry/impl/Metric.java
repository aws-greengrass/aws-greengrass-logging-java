/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.greengrass.telemetry.impl;

import com.aws.greengrass.telemetry.models.TelemetryAggregation;
import com.aws.greengrass.telemetry.models.TelemetryUnit;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * Metric is a class encapsulating the namespace, name, unit, aggregation,value and timestamp of the metric.
 */
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
public class Metric {
    private static final long serialVersionUID = 0L;
    @NonNull
    @JsonProperty("NS")
    private String namespace;
    @NonNull
    @JsonProperty("N")
    private String name;
    @NonNull
    @JsonProperty("U")
    private TelemetryUnit unit;
    @NonNull
    @JsonProperty("A")
    private TelemetryAggregation aggregation;
    @JsonProperty("V")
    private Object value;
    @JsonProperty("TS")
    private Long timestamp;
}
