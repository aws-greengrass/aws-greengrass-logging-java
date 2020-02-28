/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.aws.iot.evergreen.logging.impl.config;

import lombok.Getter;

@Getter
public class EvergreenMetricsConfig extends PersistenceConfig {
    private static final String DEFAULT_TEXT_METRICS_PATTERN = "%d{yyyy MMM dd HH:mm:ss,SSS} %m%n";
    private static final Boolean DEFAULT_METRICS_SWITCH = true;

    public static final String METRICS_SWITCH_KEY = "metrics.enabled";
    public static final String CONFIG_PREFIX = "metrics";

    private final boolean enabled;

    /**
     * Create EvergreenMetricsConfig instance with metrics configurations.
     *
     * @param enabled         whether metrics are enabled
     * @param store           metrics storage option
     * @param format          metrics output format
     * @param fileSize        max file size to persist metrics per rolling file
     * @param numRollingFiles number of files to keep rolling
     * @param pattern         Log4j text output pattern
     */
    public EvergreenMetricsConfig(boolean enabled, LogStore store, String storeName, LogFormat format, String fileSize,
                                  int numRollingFiles, String pattern) {
        super(store, storeName, format, fileSize, numRollingFiles, pattern);
        this.enabled = enabled;
    }

    /**
     * Get default metrics configurations from system properties.
     */
    public EvergreenMetricsConfig() {
        super(CONFIG_PREFIX, DEFAULT_TEXT_METRICS_PATTERN);
        boolean enabled;

        String enabledStr = System.getProperty(METRICS_SWITCH_KEY);
        enabled = DEFAULT_METRICS_SWITCH;

        if (enabledStr != null) {
            enabled = Boolean.parseBoolean(enabledStr);
        }
        this.enabled = enabled;
    }

    public EvergreenMetricsConfig(boolean enabled) {
        super(CONFIG_PREFIX, DEFAULT_TEXT_METRICS_PATTERN);
        this.enabled = enabled;
    }
}
