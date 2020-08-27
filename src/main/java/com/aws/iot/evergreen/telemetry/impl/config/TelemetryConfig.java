/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.telemetry.impl.config;

import ch.qos.logback.classic.Logger;
import com.aws.iot.evergreen.logging.impl.config.LogStore;
import com.aws.iot.evergreen.logging.impl.config.PersistenceConfig;
import lombok.Getter;
import lombok.Setter;

@Getter
public class TelemetryConfig extends PersistenceConfig {
    public static final String CONFIG_PREFIX = "log";
    public static final String METRICS_SWITCH_KEY = "log.metricsEnabled";
    private static final Boolean DEFAULT_METRICS_SWITCH = true;
    private static final String TELEMETRY_LOG_DIRECTORY = "Telemetry";
    @Setter
    private boolean metricsEnabled;
    private static final TelemetryConfig INSTANCE = new TelemetryConfig();
    @Setter
    public Logger logger;

    /**
     * Get default metrics configurations from system properties.
     */
    public TelemetryConfig() {
        super(CONFIG_PREFIX);
        boolean metricsEnabled = DEFAULT_METRICS_SWITCH;
        String enabledStr = System.getProperty(METRICS_SWITCH_KEY);
        if (enabledStr != null) {
            metricsEnabled = Boolean.parseBoolean(enabledStr);
        }
        this.metricsEnabled = metricsEnabled;
    }

    /**
     *  Set up logger and store name.
     * @param logger Uses a new logger
     * @param storeName creates a log file at the path specified
     */
    public void editConfig(Logger logger, String storeName) {
        this.logger = logger;

        /* Log to a file - this has to be set before setting the store path. */
        setStoreType(LogStore.FILE);

        /*
         * Telemetry
         *   |___ generic.log
         *   |___ Kernel.log
         *   |___ SystemMetrics.log
         *   |___ ....
         */
        setStoreName(TELEMETRY_LOG_DIRECTORY + "/" + storeName + "." + CONFIG_PREFIX);
        reconfigure(this.logger);
    }

    public static TelemetryConfig getInstance() {
        return INSTANCE;
    }
}

