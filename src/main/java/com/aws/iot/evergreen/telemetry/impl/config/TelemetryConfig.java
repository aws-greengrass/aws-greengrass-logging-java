/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.telemetry.impl.config;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy;
import ch.qos.logback.core.util.FileSize;
import com.aws.iot.evergreen.logging.impl.config.LogStore;
import com.aws.iot.evergreen.logging.impl.config.PersistenceConfig;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

import static com.aws.iot.evergreen.telemetry.impl.MetricFactory.METRIC_LOGGER_NAME;

@Getter
public class TelemetryConfig extends PersistenceConfig {
    public static final String CONFIG_PREFIX = "log";
    public static final String METRICS_SWITCH_KEY = "log.metricsEnabled";
    private static final Boolean DEFAULT_METRICS_SWITCH = true;
    private static final String TELEMETRY_LOG_DIRECTORY = "Telemetry";
    @Setter
    private boolean metricsEnabled;
    private static final TelemetryConfig INSTANCE = new TelemetryConfig();
    private RollingFileAppender<ILoggingEvent> logFileAppender = null;

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
        this.setStoreType(LogStore.FILE);

        /*
         * Telemetry
         *   |___ generic.log
         *   |___ Kernel.log
         *   |___ SystemMetrics.log
         *   |___ ....
         */
        this.setStoreName(TELEMETRY_LOG_DIRECTORY + "/" + storeName + "." + CONFIG_PREFIX);
    }

    /**
     * Overriding this function to keep the Telemetry logs at user.dir level irrespective of the system properties
     * @param newStoreName new store name
     */
    @Override
    public void setStoreName(String newStoreName) {
        if (Objects.equals(this.storeName,newStoreName)) {
            return;
        }
        this.storeName = newStoreName;
    }

    @Override
    public void reconfigure(Logger loggerToConfigure) {
        Objects.requireNonNull(loggerToConfigure);

        logger = loggerToConfigure;
        LoggerContext logCtx = loggerToConfigure.getLoggerContext();

        BasicEncoder basicEncoder = new BasicEncoder();
        basicEncoder.setContext(logCtx);
        basicEncoder.start();

        // Set sub-loggers to inherit this config
        loggerToConfigure.setAdditive(true);
        // set backend logger level to trace because we'll be filtering it in the frontend
        loggerToConfigure.setLevel(ch.qos.logback.classic.Level.TRACE);
        loggerToConfigure.detachAndStopAllAppenders();

        String fileAppenderName = this.getLogger().getName().substring(METRIC_LOGGER_NAME.length() + 1);
        logFileAppender = new RollingFileAppender<>();
        logFileAppender.setContext(logCtx);
        logFileAppender.setName(fileAppenderName);
        logFileAppender.setAppend(true);
        logFileAppender.setFile(storeName);
        logFileAppender.setEncoder(basicEncoder);

        SizeAndTimeBasedRollingPolicy<ILoggingEvent> logFilePolicy = new SizeAndTimeBasedRollingPolicy<>();
        logFilePolicy.setContext(logCtx);
        logFilePolicy.setParent(logFileAppender);
        logFilePolicy.setFileNamePattern(storeName + "_%d{yyyy-MM-dd_HH}_%i");
        logFilePolicy.setMaxHistory(numRollingFiles);
        logFilePolicy.setMaxFileSize(new FileSize(fileSizeKB * FileSize.KB_COEFFICIENT));
        logFilePolicy.start();

        logFileAppender.setRollingPolicy(logFilePolicy);
        logFileAppender.setTriggeringPolicy(logFilePolicy);
        logFileAppender.start();
        // Add the replacement
        loggerToConfigure.addAppender(logFileAppender);
    }

    public static TelemetryConfig getInstance() {
        return INSTANCE;
    }
}

