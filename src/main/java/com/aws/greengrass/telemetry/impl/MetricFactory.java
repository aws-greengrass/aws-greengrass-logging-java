/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.greengrass.telemetry.impl;

import com.aws.greengrass.logging.api.Logger;
import com.aws.greengrass.logging.impl.LogManager;
import com.aws.greengrass.telemetry.api.MetricFactoryBuilder;
import com.aws.greengrass.telemetry.impl.config.TelemetryConfig;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Objects;

/**
 * An implementation of {@link MetricFactoryBuilder} to generate metrics events.
 */
public class MetricFactory implements MetricFactoryBuilder {
    public static final String METRIC_LOGGER_PREFIX = "Metrics-";
    private static final String GENERIC_LOG_STORE = "generic";
    private TelemetryConfig telemetryConfig;
    @Setter(AccessLevel.PACKAGE)
    @Getter(AccessLevel.PACKAGE)
    private transient Logger logger;

    public MetricFactory() {
        constructorHelper(null);
    }

    public MetricFactory(String storeName) {
        constructorHelper(storeName);
    }

    /**
     * Helper function for both the constructors.
     *
     * @param storeName Creates a log file based on the store name passed. Set to "generic" if it is null or empty.
     */
    private void constructorHelper(String storeName) {
        if (storeName == null || storeName.equals("")) {
            storeName = GENERIC_LOG_STORE;
        }
        // TODO: get configurations from kernel config
        String loggerName = METRIC_LOGGER_PREFIX + storeName;
        this.telemetryConfig = TelemetryConfig.getInstance();
        this.telemetryConfig.editConfigForLogger(loggerName);
        this.logger = LogManager.getTelemetryLogger(loggerName);
    }

    /**
     * The value provided will be assigned to the metric along with the current timestamp.
     *
     * @param metric the metric to which the value has to be assigned
     * @param value  data value that has to be emitted.
     * @throws IllegalArgumentException This will throw an exception if namespace or name of the metric is not set.
     */
    public void putMetricData(Metric metric, Object value) throws IllegalArgumentException {
        if (telemetryConfig.isMetricsEnabled()) {
            Objects.requireNonNull(metric);
            synchronized (metric) {
                metric.setValue(value);
                metric.setTimestamp(Instant.now().toEpochMilli());
                putMetricData(metric);
            }
        }
    }

    /**
     * The metric passed in must have the value and timestamp assigned.
     *
     * @param metric emit the metric which has the value assigned to it.
     * @throws IllegalArgumentException This will throw an exception if namespace or name of the metric is not set.
     */
    public void putMetricData(Metric metric) throws IllegalArgumentException {
        TelemetryLoggerMessage message = new TelemetryLoggerMessage(metric);
        String name = formatString(metric.getName());
        String namespace = formatString(metric.getNamespace());
        if (name.length() == 0) {
            throw new IllegalArgumentException("Metric name cannot be empty. " + message.getJSONMessage());
        }
        if (namespace.length() == 0) {
            throw new IllegalArgumentException("Metric namespace cannot be empty. " + message.getJSONMessage());
        }
        metric.setName(name);
        metric.setNamespace(namespace);
        message = new TelemetryLoggerMessage(metric);
        logMetrics(message);
    }

    private String formatString(String name) {
        return name.replaceAll("\\s", "");
    }

    /**
     * Log the metrics.
     *
     * @param message message to be logged
     */
    public void logMetrics(TelemetryLoggerMessage message) {
        logger.trace(message.getJSONMessage());
    }
}
