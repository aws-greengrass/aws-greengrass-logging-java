/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.telemetry.impl;

import com.aws.iot.evergreen.telemetry.api.MetricDataBuilder;
import com.aws.iot.evergreen.telemetry.api.MetricFactoryBuilder;
import com.aws.iot.evergreen.telemetry.impl.config.TelemetryConfig;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of {@link MetricFactoryBuilder} to generate metrics events.
 */
public class MetricFactory implements MetricFactoryBuilder {
    // Use a ThreadLocal for MetricsBuilder to reuse the object per thread.
    private ThreadLocal<MetricData> metricData = ThreadLocal.withInitial(MetricData::new);
    private static final MetricFactory instance = new MetricFactory();
    public static final String METRIC_LOGGER_NAME = "Metrics";
    private final TelemetryConfig config;
    @Setter
    @Getter
    private transient Logger logger;

    /**
     * Constructor.
     *
     *@Initialises metric logger and evergreen telemetry config
     */
    public MetricFactory() {
        logger = LoggerFactory.getLogger(METRIC_LOGGER_NAME);
        // TODO: get configurations from kernel config
        config = TelemetryConfig.getInstance();
    }

    /**
     * Get a singleton instance of MetricsFactory.
     *
     * @return singleton instance of MetricsFactoryImpl
     */
    public static MetricFactory getInstance() {
        return instance;
    }

    @Override
    public MetricDataBuilder addMetric(Metric metric) {
        if (isMetricsEnabled()) {
            return metricData.get().setLogger(this).setMetric(metric);
        }
        return MetricDataBuilder.NOOP;
    }

    /**
     * Check if metrics are enabled.
     *
     * @return true if metrics are enabled, false otherwise.
     */
    public boolean isMetricsEnabled() {
        return config.isEnabled();
    }

    /**
     * Log the metrics.
     *
     * @param message the EvergreenMetricsMessage to be logged
     */
    public void logMetrics(TelemetryLoggerMessage message) {
        logger.info(message.getJSONMessage());
    }
}
