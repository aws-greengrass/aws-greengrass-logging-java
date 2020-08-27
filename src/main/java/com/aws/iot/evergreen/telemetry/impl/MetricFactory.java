/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.telemetry.impl;

import com.aws.iot.evergreen.logging.impl.LogManager;
import com.aws.iot.evergreen.logging.impl.Slf4jLogAdapter;
import com.aws.iot.evergreen.logging.impl.config.EvergreenLogConfig;
import com.aws.iot.evergreen.telemetry.api.MetricDataBuilder;
import com.aws.iot.evergreen.telemetry.api.MetricFactoryBuilder;
import com.aws.iot.evergreen.telemetry.impl.config.TelemetryConfig;
import lombok.Getter;
import lombok.Setter;

/**
 * An implementation of {@link MetricFactoryBuilder} to generate metrics events.
 */
public class MetricFactory implements MetricFactoryBuilder {
    // Use a ThreadLocal for MetricsBuilder to reuse the object per thread.
    private ThreadLocal<MetricData> metricData = ThreadLocal.withInitial(MetricData::new);
    private TelemetryConfig telemetryConfig;
    public static final String METRIC_LOGGER_NAME = "Metrics";
    public static final String GENERIC_LOG_STORE = "generic";
    @Setter
    @Getter
    private transient Slf4jLogAdapter logger;

    public MetricFactory() {
        constructorHelper(null);
    }

    public MetricFactory(String storePath) {
        constructorHelper(storePath);
    }

    /**
     * Helper function for both the constructors.
     * @param storeName Creates a log file based on the store name passed. Set to "generic" if it is null or empty.
     */
    public void constructorHelper(String storeName) {
        if (storeName == null || storeName.equals("")) {
            storeName = GENERIC_LOG_STORE;
        }
        // TODO: get configurations from kernel config
        String loggerName = METRIC_LOGGER_NAME + "-" + storeName;
        this.telemetryConfig = TelemetryConfig.getInstance();
        this.telemetryConfig.editConfig(EvergreenLogConfig.getInstance().getLogger(loggerName), storeName);
        this.logger = (Slf4jLogAdapter) LogManager.getLogger(loggerName);
        this.logger.setLevel("trace");
    }

    /**
     * Check if metrics are enabled. We can make it specific to a metric level.
     *
     * @return MetricDataBuilder to emit if metrics are enabled or NOOP otherwise.
     */
    @Override
    public MetricDataBuilder addMetric(Metric metric) {
        if (this.telemetryConfig.isMetricsEnabled()) {
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
