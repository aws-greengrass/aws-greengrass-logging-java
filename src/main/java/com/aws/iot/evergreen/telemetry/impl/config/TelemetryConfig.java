/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.telemetry.impl.config;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.RollingFileAppender;
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
    private static final String DEFAULT_TELEMETRY_LOG_LEVEL = "TRACE";
    private static final TelemetryConfig INSTANCE = new TelemetryConfig();
    @Setter
    private static Path root = getRootStorePath();
    private final LoggerContext context = new LoggerContext();
    @Setter
    private boolean metricsEnabled;
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

    public static TelemetryConfig getInstance() {
        return INSTANCE;
    }

    public static Path getTelemetryDirectory() {
        return root;
    }

    /**
     * Sets up logger name and store name.
     *
     * @param loggerName This is used as the name of the telemetry logger.
     * @param storeName  The storeName passed will be the name of the log file created at the path shown below.
     */
    public void editConfig(String loggerName, String storeName) {
        this.loggerName = loggerName;

        /*
         * telemetry
         *   |___ generic.log
         *   |___ KernelComponents.log
         *   |___ SystemMetrics.log
         *   |___ ....
         */
        this.setFormat(LogFormat.JSON);
        this.setStorePath(Paths.get(storeName + "." + CONFIG_PREFIX));
    }

    /**
     * Change the configured store path (only applies for file output).
     *
     * @param path The path passed in must contain the file name to which the logs will be written.
     */
    @Override
    public void setStorePath(Path path) {
        String newStoreName = root.resolve(path).toAbsolutePath().toString();
        if (Objects.equals(this.storeName, newStoreName)) {
            return;
        }
        this.storeName = newStoreName;
        getFileNameFromStoreName();
        getStoreDirectoryFromStoreName();
        reconfigure();
    }

    /**
     * NOTE : Calling this method from elsewhere will NOT reconfigure the logger.
     * This method is called only once from the setStorePath(..); This is used only once from the MetricFactory
     * when we construct a metric to write metrics to a specific file.
     */
    @Override
    protected void reconfigure() {
        reconfigure(getLogger(loggerName));
    }

    @Override
    protected void reconfigure(Logger loggerToConfigure) {
        context.start();
        Objects.requireNonNull(loggerToConfigure);
        // Set sub-loggers to inherit this config
        loggerToConfigure.setAdditive(true);
        loggerToConfigure.setLevel(ch.qos.logback.classic.Level.TRACE);

        String fileAppenderName = loggerToConfigure.getName().substring(METRIC_LOGGER_NAME.length() + 1);
        RollingFileAppender<ILoggingEvent> logFileAppender = getAppenderForFile(loggerToConfigure, fileAppenderName);
        logFileAppender.start();
        // Add the replacement
        loggerToConfigure.detachAndStopAllAppenders();
        loggerToConfigure.addAppender(logFileAppender);
    }

    public Logger getLogger(String name) {
        return context.getLogger(name);
    }

    public void close() {
        context.stop();
    }
}

