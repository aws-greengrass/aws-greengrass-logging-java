/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.logging.impl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Data
public class EvergreenMetricsMessage {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final String namespace;
    private final List<Metric> metrics = new LinkedList<>();
    private final Map<String, String> dimensions = new HashMap<>();
    @EqualsAndHashCode.Exclude
    private final Long timestamp;

    /**
     * Constructor for Metrics Message.
     *
     * @param namespace  the namespace of the metrics
     * @param metrics    list of {@link Metric}
     * @param dimensions a map of metrics dimensions
     */
    public EvergreenMetricsMessage(String namespace, List<Metric<?>> metrics,
                                   Map<String, String> dimensions) {
        this.timestamp = Instant.now().toEpochMilli();
        this.dimensions.putAll(dimensions);
        this.metrics.addAll(metrics);
        this.namespace = String.valueOf(namespace);
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
