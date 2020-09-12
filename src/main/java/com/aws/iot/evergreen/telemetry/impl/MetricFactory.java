/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.telemetry.impl;

import com.aws.iot.evergreen.logging.api.Logger;
import com.aws.iot.evergreen.logging.impl.LogManager;
import com.aws.iot.evergreen.telemetry.api.MetricDataBuilder;
import com.aws.iot.evergreen.telemetry.api.MetricFactoryBuilder;
import com.aws.iot.evergreen.telemetry.impl.config.TelemetryConfig;
import lombok.Getter;
import lombok.Setter;

/**
 * An implementation of {@link MetricFactoryBuilder} to generate metrics events.
 */
public class MetricFactory implements MetricFactoryBuilder {
    // Use a ThreadLocal for MetricDataBuilder to reuse the object per thread.
    private ThreadLocal<MetricData> metricData;
    private TelemetryConfig telemetryConfig;
    public static final String METRIC_LOGGER_NAME = "Metrics";
    private static final String GENERIC_LOG_STORE = "generic";
    @Setter
    @Getter
    private transient Logger logger;

    public MetricFactory() {
        constructorHelper(null);
    }

    public MetricFactory(String storeName) {
        constructorHelper(storeName);
    }

    /**
     * Helper function for both the constructors.
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
     * Check if metrics are enabled. We can make it specific to a metric level.
     *
     * @return MetricDataBuilder to emit if metrics are enabled or NOOP otherwise.
     */
    @Override
    public MetricDataBuilder addMetric(Metric metric) {
        if (this.telemetryConfig.isMetricsEnabled()) {
            metricData = ThreadLocal.withInitial(MetricData::new);
            return metricData.get().setLogger(this).setMetric(metric);
        }
        return MetricDataBuilder.NOOP;
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
