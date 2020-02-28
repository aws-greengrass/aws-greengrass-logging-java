/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.logging.api;

import java.util.Map;
import javax.measure.unit.Unit;

/**
 * A fluent API to emit metrics.
 */
public interface MetricsBuilder {

    /**
     * An instance of NOOP MetricsBuilder.
     */
    MetricsBuilder NOOP = new MetricsBuilder() {
    };

    /**
     * Set the namespace for the metrics.
     *
     * @param namespace the namespace for the metrics
     * @return the instance of MetricsBuilder
     */
    default MetricsBuilder setNamespace(String namespace) {
        return this;
    }

    /**
     * Add a metric dimension.
     *
     * @param key   dimension name
     * @param value dimension value
     * @return
     */
    default MetricsBuilder addDimension(String key, Object value) {
        return this;
    }

    /**
     * Add a metric dimensions.
     *
     * @param dimensions dimensions to add
     * @return
     */
    default MetricsBuilder addDimensions(Map<String, Object> dimensions) {
        return this;
    }

    /**
     * Add a metric.
     *
     * @param name  metric name
     * @param value metric value
     * @param unit  unit of the metric value
     * @return
     */
    default MetricsBuilder addMetric(String name, Object value, Unit<?> unit) {
        return this;
    }

    /**
     * Add a metric.
     *
     * @param name  metric name
     * @param value metric value
     * @return
     */
    default MetricsBuilder addMetric(String name, Object value) {
        return this;
    }

    /**
     * Emit the metrics.
     */
    default void emit() {
    }
}
