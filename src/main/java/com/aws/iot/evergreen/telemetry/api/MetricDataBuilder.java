/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.telemetry.api;

/**
 * A fluent API to emit metrics.
 */
public interface MetricDataBuilder {
    /**
     * An instance of NOOP MetricsBuilder.
     */
    MetricDataBuilder NOOP = new MetricDataBuilder() {

    };

    /**
     * Put metric data with a value.
     *
     * @param value metric value
     * @return
     */
    default MetricDataBuilder putMetricData(Object value) {
        return this;
    }

    /**
     * Emit the metrics.
     */
    default void emit() {

    }
}


