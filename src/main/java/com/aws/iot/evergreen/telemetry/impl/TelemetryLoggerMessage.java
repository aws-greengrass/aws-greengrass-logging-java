
/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.telemetry.impl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

@Data
public class TelemetryLoggerMessage {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private MetricDataPoint metricDataPoint;

    public TelemetryLoggerMessage(MetricDataPoint metricDataPoint) {
        this.metricDataPoint = metricDataPoint;
    }

    /**
     * Get JSON encoded metric.
     * @return String
     */

    @JsonIgnore
    public String getJSONMessage() {
        try {
            return OBJECT_MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }
}
