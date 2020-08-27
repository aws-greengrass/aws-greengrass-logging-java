/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.telemetry.impl;

import com.aws.iot.evergreen.telemetry.api.MetricDataBuilder;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * An implementation of {@link MetricDataBuilder} providing a fluent API to generate metrics. Not thread safe.
 */
@NoArgsConstructor
@AllArgsConstructor
public class MetricData implements MetricDataBuilder {

    private MetricDataPoint metricDataPoint = new MetricDataPoint();
    private Metric metric;
    private transient MetricFactory metricFactory;

    public MetricData setLogger(MetricFactory metricFactory) {
        this.metricFactory = metricFactory;
        return this;
    }

    public MetricData setMetric(Metric metric) {
        this.metric = metric;
        return this;
    }

    /**
     * Creates a metric data point with value and timestamp of the metric.
     *
     * @param value metric data value
     * @return
     */

    @Override
    public MetricData putMetricData(Object value) {
        this.metricDataPoint.setValue(value);
        this.metricDataPoint.setTimestamp(Instant.now().toEpochMilli());
        this.metricDataPoint.setMetric(this.metric);
        return this;
    }

    /**
     * Emits the metric data point collected using the logger.
     *
     */

    @Override
    public void emit() {
        TelemetryLoggerMessage message = new TelemetryLoggerMessage(this.metricDataPoint);
        metricFactory.logMetrics(message);
    }
}
