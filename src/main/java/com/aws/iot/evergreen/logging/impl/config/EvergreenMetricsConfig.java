/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.logging.impl.config;

import lombok.Getter;
import lombok.Setter;
import org.slf4j.LoggerFactory;

import static com.aws.iot.evergreen.logging.impl.MetricsFactoryImpl.METRIC_LOGGER_NAME;

@Getter
public class EvergreenMetricsConfig extends PersistenceConfig {
    private static final Boolean DEFAULT_METRICS_SWITCH = true;

    public static final String METRICS_SWITCH_KEY = "metrics.enabled";
    public static final String CONFIG_PREFIX = "metrics";

    @Setter
    private boolean enabled;

    private static final EvergreenMetricsConfig INSTANCE = new EvergreenMetricsConfig();

    /**
     * Get default metrics configurations from system properties.
     */
    private EvergreenMetricsConfig() {
        super(CONFIG_PREFIX);
        boolean enabled;

        String enabledStr = System.getProperty(METRICS_SWITCH_KEY);
        enabled = DEFAULT_METRICS_SWITCH;

        if (enabledStr != null) {
            enabled = Boolean.parseBoolean(enabledStr);
        }
        this.enabled = enabled;
        reconfigure((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(METRIC_LOGGER_NAME));
    }

    public static EvergreenMetricsConfig getInstance() {
        return INSTANCE;
    }
}
