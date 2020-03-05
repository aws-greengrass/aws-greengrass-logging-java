/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.logging.impl;

import com.aws.iot.evergreen.logging.api.MetricsBuilder;
import com.aws.iot.evergreen.logging.api.MetricsFactory;
import com.aws.iot.evergreen.logging.impl.config.EvergreenMetricsConfig;
import com.aws.iot.evergreen.logging.impl.plugins.Log4jConfigurationFactory;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An implementation of {@link MetricsFactory} to generate metrics events.
 */
public class MetricsFactoryImpl implements MetricsFactory {
    // Use a ThreadLocal for MetricsBuilder to reuse the object per thread.
    private static final ThreadLocal<MetricsBuilderImpl> metricsBuilder = ThreadLocal.withInitial(
        () -> new MetricsBuilderImpl()
    );
    private static MetricsFactory instance = new MetricsFactoryImpl();

    private transient org.apache.logging.log4j.Logger logger;
    private final ConcurrentMap<String, String> defaultDimensions = new ConcurrentHashMap<>();
    private AtomicBoolean enabled = new AtomicBoolean();

    private MetricsFactoryImpl() {
        logger = LogManager.getLogger(Log4jConfigurationFactory.METRICS_LOGGER_NAME);
        // TODO: get configurations from kernel config
        setConfig(new EvergreenMetricsConfig());
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
        return enabled.get();
    }

    /**
     * Log the metrics.
     *
     * @param message the EvergreenMetricsMessage to be logged
     */
    public void logMetrics(EvergreenMetricsMessage message) {
        logger.logMessage(Level.ALL, null, null, null, message, null);
    }

    public void setConfig(EvergreenMetricsConfig config) {
        enabled.set(config.isEnabled());
    }

    public org.apache.logging.log4j.Logger getLogger() {
        return this.logger;
    }

    public void setLogger(org.apache.logging.log4j.Logger logger) {
        this.logger = logger;
    }

}
