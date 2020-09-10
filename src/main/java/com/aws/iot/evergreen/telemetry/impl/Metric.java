/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.telemetry.impl;

import com.aws.iot.evergreen.telemetry.models.TelemetryAggregation;
import com.aws.iot.evergreen.telemetry.models.TelemetryMetricName;
import com.aws.iot.evergreen.telemetry.models.TelemetryNamespace;
import com.aws.iot.evergreen.telemetry.models.TelemetryUnit;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

/**
 * Metric is a class encapsulating the namespace, name, unit, aggregation and dimensions of a metric.
 */
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Metric {
    @NonNull
    @JsonProperty("NS")
    private TelemetryNamespace namespace;
    @NonNull
    @JsonProperty("N")
    private TelemetryMetricName name;
    @NonNull
    @JsonProperty("U")
    private TelemetryUnit unit;
    @NonNull
    @JsonProperty("A")
    private TelemetryAggregation aggregation;
}
