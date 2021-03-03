/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.greengrass.telemetry.impl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.io.JsonStringEncoder;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

@Data
public class TelemetryLoggerMessage {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private Object metricDataPoint;

    public TelemetryLoggerMessage(Object metricDataPoint) {
        this.metricDataPoint = metricDataPoint;
    }

    /**
     * Get JSON encoded metric.
     * @return String
     */
    @JsonIgnore
    public String getJSONMessage() {
        try {
            return OBJECT_MAPPER.writeValueAsString(this.metricDataPoint);
        } catch (JsonProcessingException e) {
            return "{\"error\": \"" + new String(JsonStringEncoder.getInstance().quoteAsString(e.getMessage())) + "\"}";
        }
    }
}
