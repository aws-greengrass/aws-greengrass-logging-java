/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.logging.impl;

import com.aws.iot.evergreen.logging.api.MetricsBuilder;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.measure.unit.Unit;

/**
 * An implementation of {@link MetricsBuilder} providing a fluent API to generate metrics. Not thread safe.
 */
@NoArgsConstructor
public class MetricsBuilderImpl implements MetricsBuilder {
    private final Map<String, String> dimensions = new HashMap<>();
    private final Map<String, String> defaultDimensions = new HashMap<>();
    private transient MetricsFactoryImpl logger;
    private String namespace;
    private final List<Metric<?>> metrics = new LinkedList<>();

    /**
     * MetricsBuilder constructor.
     *
     * @param logger            the metrics logger
     * @param defaultDimensions default metrics dimensions
     */
    protected MetricsBuilderImpl(MetricsFactoryImpl logger, Map<String, String> defaultDimensions) {
        this.logger = logger;
        this.dimensions.putAll(defaultDimensions);
    }

    protected MetricsBuilderImpl setLogger(MetricsFactoryImpl logger) {
        this.logger = logger;
        return this;
    }

    protected MetricsBuilderImpl setDefaultDimensions(Map<String, String> defaultDimensions) {
        this.defaultDimensions.clear();
        this.defaultDimensions.putAll(defaultDimensions);
        return this;
    }

    @Override
    public MetricsBuilder setNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    @Override
    public MetricsBuilder addDimension(String key, Object value) {
        this.dimensions.put(key, value.toString());
        return this;
    }

    @Override
    public MetricsBuilder addMetric(String name, Object value, Unit<?> unit) {
        metrics.add(new Metric<>(name, value, unit));
        return this;
    }

    @Override
    public MetricsBuilder addMetric(String name, Object value) {
        metrics.add(Metric.of(name, value));
        return this;
    }

    @Override
    public void emit() {
        defaultDimensions.forEach(this.dimensions::putIfAbsent);
        EvergreenMetricsMessage message = new EvergreenMetricsMessage(namespace, metrics, dimensions);
        this.logger.logMetrics(message);
        this.dimensions.clear();
        this.namespace = "";
        this.metrics.clear();
    }
}
