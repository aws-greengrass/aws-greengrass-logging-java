/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.telemetry.impl.config;


import com.aws.iot.evergreen.logging.impl.config.EvergreenLogConfig;
import com.aws.iot.evergreen.logging.impl.config.PersistenceConfig;
import lombok.Getter;
import lombok.Setter;

import static com.aws.iot.evergreen.telemetry.impl.MetricFactory.METRIC_LOGGER_NAME;

@Getter
public class TelemetryConfig extends PersistenceConfig {

    public static final String CONFIG_PREFIX = "metrics";
    public static final String METRICS_SWITCH_KEY = "metrics.enabled";
    private static final Boolean DEFAULT_METRICS_SWITCH = true;

    @Setter
    private boolean enabled;
    private static final TelemetryConfig INSTANCE = new TelemetryConfig();

    /**
     * Get default metrics configurations from system properties.
     */
    private TelemetryConfig() {
        super(CONFIG_PREFIX);
        boolean enabled = DEFAULT_METRICS_SWITCH;
        String enabledStr = System.getProperty(METRICS_SWITCH_KEY);
        if (enabledStr != null) {
            enabled = Boolean.parseBoolean(enabledStr);
        }
        this.enabled = enabled;
        reconfigure(EvergreenLogConfig.getInstance().getLogger(METRIC_LOGGER_NAME));
    }

    public static TelemetryConfig getInstance() {
        return INSTANCE;
    }
}

