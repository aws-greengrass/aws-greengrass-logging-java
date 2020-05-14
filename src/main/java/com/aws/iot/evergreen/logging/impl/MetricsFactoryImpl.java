/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.logging.impl;

import com.aws.iot.evergreen.logging.api.MetricsBuilder;
import com.aws.iot.evergreen.logging.api.MetricsFactory;
import com.aws.iot.evergreen.logging.impl.config.EvergreenMetricsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * An implementation of {@link MetricsFactory} to generate metrics events.
 */
public class MetricsFactoryImpl implements MetricsFactory {
    // Use a ThreadLocal for MetricsBuilder to reuse the object per thread.
    private static final ThreadLocal<MetricsBuilderImpl> metricsBuilder =
            ThreadLocal.withInitial(MetricsBuilderImpl::new);
    private static final MetricsFactory instance = new MetricsFactoryImpl();
    public static final String METRIC_LOGGER_NAME = "Metrics";
    private final EvergreenMetricsConfig config;

    private transient Logger logger;
    private final ConcurrentMap<String, String> defaultDimensions = new ConcurrentHashMap<>();

    private MetricsFactoryImpl() {
        logger = LoggerFactory.getLogger(METRIC_LOGGER_NAME);
        // TODO: get configurations from kernel config
        config = EvergreenMetricsConfig.getInstance();
    }

    /**
     * Get a singleton instance of MetricsFactory.
     *
     * @return singleton instance of MetricsFactoryImpl
     */
    public static MetricsFactory getInstance() {
        return instance;
    }

    @Override
    public MetricsFactory addDefaultDimension(String key, Object value) {
        defaultDimensions.put(key, value.toString());
        return this;
    }

    @Override
    public MetricsBuilder newMetrics() {
        if (isMetricsEnabled()) {
            return metricsBuilder.get().setDefaultDimensions(defaultDimensions).setLogger(this);
        }
        return MetricsBuilder.NOOP;
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
    public void logMetrics(EvergreenMetricsMessage message) {
        logger.trace(message.getJSONMessage());
    }

    public Logger getLogger() {
        return this.logger;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

}
