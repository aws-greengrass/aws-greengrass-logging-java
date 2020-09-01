/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.telemetry.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * MetricDataPoint is a class that encapsulates the metric along with its value and timestamp.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonDeserialize (using = MetricDataPointDeserializer.class)
public class MetricDataPoint {
    private static final long serialVersionUID = 0L;
    @JsonProperty("M")
    private Metric metric;
    @JsonProperty("V")
    private Object value;
    @JsonProperty("TS")
    private Long timestamp;
}
