/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.aws.iot.evergreen.logging.impl;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public class MetricsEvent implements Serializable {
    private static final long serialVersionUID = 0L;

    public String namespace;
    public List<Metric> metrics;
    public Map<String, String> dimensions;

    @JsonProperty("LN")
    public String loggerName;
    @JsonProperty("TS")
    public Instant timestamp;

    public MetricsEvent(String loggerName) {
        this.loggerName = loggerName;
        this.timestamp = Instant.now();
    }
}
