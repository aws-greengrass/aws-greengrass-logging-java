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
import com.aws.iot.evergreen.logging.impl.config.LogFormat;
import com.aws.iot.evergreen.logging.impl.config.PersistenceConfig;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.event.Level;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import static com.aws.iot.evergreen.telemetry.impl.MetricFactory.METRIC_LOGGER_NAME;

@Getter
public class TelemetryConfig extends PersistenceConfig {
    // TODO: Replace the default log level from Kernel Configuration.
    public static final String CONFIG_PREFIX = "log";
    public static final String METRICS_SWITCH_KEY = "log.metricsEnabled";
    private static final Boolean DEFAULT_METRICS_SWITCH = true;
    private static final String TELEMETRY_LOG_DIRECTORY = "Telemetry";
    private static final String DEFAULT_TELEMETRY_LOG_LEVEL = "TRACE";
    @Setter
    private boolean metricsEnabled;
    private static final TelemetryConfig INSTANCE = new TelemetryConfig();
    private RollingFileAppender<ILoggingEvent> logFileAppender = null;
    private static final LoggerContext context = new LoggerContext();
    private String loggerName;

    /**
     * Get default metrics configurations from system properties.
     */
    protected TelemetryConfig() {
        super(CONFIG_PREFIX);
        boolean metricsEnabled = DEFAULT_METRICS_SWITCH;
        String enabledStr = System.getProperty(METRICS_SWITCH_KEY);
        if (enabledStr != null) {
            metricsEnabled = Boolean.parseBoolean(enabledStr);
        }
        this.metricsEnabled = metricsEnabled;
        this.setLevel(Level.valueOf(DEFAULT_TELEMETRY_LOG_LEVEL));
    }

    /**
     *  Set up logger and store name.
     * @param loggerName Uses a new logger
     * @param storeName creates a log file at the path specified
     */
    public void editConfig(String loggerName, String storeName) {
        this.loggerName = loggerName;

        /*
         * Telemetry
         *   |___ generic.log
         *   |___ Kernel.log
         *   |___ SystemMetrics.log
         *   |___ ....
         */
        this.setFormat(LogFormat.JSON);
        this.setStorePath(Paths.get(TELEMETRY_LOG_DIRECTORY, storeName + "." + CONFIG_PREFIX));
    }

    @Override
    protected void reconfigure() {
        reconfigure(getLogger(loggerName));
    }

    @Override
    protected void reconfigure(Logger loggerToConfigure) {
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

        String fileAppenderName = this.getLogger().getName().substring(METRIC_LOGGER_NAME.length() + 1);
        logFileAppender = new RollingFileAppender<>();
        logFileAppender.setContext(logCtx);
        logFileAppender.setName(fileAppenderName);
        logFileAppender.setAppend(true);
        logFileAppender.setFile(storeName);
        logFileAppender.setEncoder(basicEncoder);

        //TODO: Check how to make it rotate per x minutes.
        SizeAndTimeBasedRollingPolicy<ILoggingEvent> logFilePolicy = new SizeAndTimeBasedRollingPolicy<>();
        logFilePolicy.setContext(logCtx);
        logFilePolicy.setParent(logFileAppender);
        logFilePolicy.setFileNamePattern(storeDirectory.resolve(fileName + "_%d{yyyy_MM_dd_HH}_%i" + "." + prefix)
                .toString());
        logFilePolicy.setTotalSizeCap(new FileSize(totalLogStoreSizeKB * FileSize.KB_COEFFICIENT));
        logFilePolicy.setMaxFileSize(new FileSize(fileSizeKB * FileSize.KB_COEFFICIENT));
        logFilePolicy.start();

        logFileAppender.setRollingPolicy(logFilePolicy);
        logFileAppender.setTriggeringPolicy(logFilePolicy);
        logFileAppender.start();
        // Add the replacement
        loggerToConfigure.detachAndStopAllAppenders();
        loggerToConfigure.addAppender(logFileAppender);
    }

    public static TelemetryConfig getInstance() {
        return INSTANCE;
    }

    public Logger getLogger(String name) {
        return context.getLogger(name);
    }

    public static Path getTelemetryDirectory() {
        return Paths.get(INSTANCE.getStoreDirectory().toString());
    }

}

