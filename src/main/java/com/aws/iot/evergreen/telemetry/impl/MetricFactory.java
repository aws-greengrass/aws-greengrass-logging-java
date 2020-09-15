/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.telemetry.impl;

import com.aws.iot.evergreen.logging.api.Logger;
import com.aws.iot.evergreen.logging.impl.LogManager;
import com.aws.iot.evergreen.telemetry.api.MetricFactoryBuilder;
import com.aws.iot.evergreen.telemetry.impl.config.TelemetryConfig;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Objects;

/**
 * An implementation of {@link MetricFactoryBuilder} to generate metrics events.
 */
public class MetricFactory implements MetricFactoryBuilder {
    public static final String METRIC_LOGGER_NAME = "Metrics";
    private static final String GENERIC_LOG_STORE = "generic";
    private TelemetryConfig telemetryConfig;
    @Setter(AccessLevel.PACKAGE)
    @Getter(AccessLevel.PACKAGE)
    public transient Logger logger;

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
        String loggerName = METRIC_LOGGER_NAME + "-" + storeName;
        this.telemetryConfig = TelemetryConfig.getInstance();
        this.telemetryConfig.editConfig(loggerName, storeName);
        this.logger = LogManager.getTelemetryLogger(loggerName);
    }

    /**
     * Assign data to the metric.
     *
     * @param metric the metric to which the value has to be assigned
     * @param value  data value that has to be emitted.
     */
    public void putMetricData(Metric metric, Object value) {
        if (telemetryConfig.isMetricsEnabled()) {
            Objects.requireNonNull(metric);
            metric.setValue(value);
            putMetricData(metric);
        }
    }

    /**
     * Emit the metric after assigning the value.
     *
     * @param metric emit the metric which has the value assigned to it.
     */
    public void putMetricData(Metric metric) {
        metric.setTimestamp(Instant.now().toEpochMilli());
        TelemetryLoggerMessage message = new TelemetryLoggerMessage(metric);
        logMetrics(message);
    }

    /**
     * Log the metrics.
     *
     * @param message the EvergreenMetricsMessage to be logged
     */
    public void logMetrics(TelemetryLoggerMessage message) {
        logger.trace(message.getJSONMessage());
    }
}
