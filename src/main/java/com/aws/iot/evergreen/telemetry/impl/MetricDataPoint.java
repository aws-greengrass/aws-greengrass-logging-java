/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.telemetry.impl;

import com.aws.iot.evergreen.telemetry.models.TelemetryMetricName;
import com.aws.iot.evergreen.telemetry.models.TelemetryNamespace;
import com.aws.iot.evergreen.telemetry.models.TelemetryUnit;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

import java.util.Map;

/**
 * MetricDataPoint is a class that encapsulates the metric along with its value and timestamp.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonDeserialize (using = MetricDataPointSerializer.class)
public class MetricDataPoint {
    private static final long serialVersionUID = 0L;
    @JsonProperty("M")
    private Metric metric;
    @JsonProperty("V")
    private Object value;
    @JsonProperty("TS")
    private Long timestamp;
}
