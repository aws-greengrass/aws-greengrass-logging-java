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
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * Metric is a class encapsulating the namespace, name, unit, aggregation and dimensions of a metric.
 */
@Builder
@Getter
@Setter
public class Metric {
    @NonNull
    @JsonProperty("NS")
    private TelemetryNamespace metricNamespace;
    @NonNull
    @JsonProperty("N")
    private TelemetryMetricName metricName;
    @NonNull
    @JsonProperty("U")
    private TelemetryUnit metricUnit;
}
