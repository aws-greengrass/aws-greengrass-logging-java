/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.greengrass.telemetry.impl.config;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.RollingFileAppender;
import com.aws.greengrass.logging.impl.LogManager;
import com.aws.greengrass.logging.impl.config.LogFormat;
import com.aws.greengrass.logging.impl.config.PersistenceConfig;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.event.Level;

import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

import static com.aws.greengrass.telemetry.impl.MetricFactory.METRIC_LOGGER_PREFIX;

@Getter
public class TelemetryConfig extends PersistenceConfig {
    // TODO: Replace the default log level from Kernel Configuration.
    public static final String CONFIG_PREFIX = "log";
    public static final String METRICS_SWITCH_KEY = "log.metricsEnabled";
    public static final String TELEMETRY_DIRECTORY = "telemetry";
    private static final Boolean DEFAULT_METRICS_SWITCH = true;
    private static final String DEFAULT_TELEMETRY_LOG_LEVEL = "TRACE";
    private static final TelemetryConfig INSTANCE = new TelemetryConfig();
    private final LoggerContext context = new LoggerContext();
    private Path root = getRootStorePath().resolve(TELEMETRY_DIRECTORY);
    @Setter
    private boolean metricsEnabled;

    /**
     * Get default metrics configurations from system properties.
     */
    private TelemetryConfig() {
        super(CONFIG_PREFIX);
        boolean metricsEnabled = DEFAULT_METRICS_SWITCH;
        String enabledStr = System.getProperty(METRICS_SWITCH_KEY);
        if (enabledStr != null) {
            metricsEnabled = Boolean.parseBoolean(enabledStr);
        }
        this.metricsEnabled = metricsEnabled;
        this.setLevel(Level.valueOf(DEFAULT_TELEMETRY_LOG_LEVEL));
        this.setFormat(LogFormat.JSON);
    }

    public static TelemetryConfig getInstance() {
        return INSTANCE;
    }

    public static Path getTelemetryDirectory() {
        return INSTANCE.root;
    }

    /**
     * Sets up logger and store name.
     *
     * @param loggerName This is used as the name of the telemetry logger.
     */
    public void editConfigForLogger(String loggerName) {
        this.logger = getLogger(loggerName);
        this.setStorePath(Paths.get(getLogFileName(loggerName)));
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
     * This method is called only once from the setStorePath(..);
     */
    @Override
    protected void reconfigure() {
        reconfigure(logger);
    }

    @Override
    protected void reconfigure(Logger loggerToConfigure) {
        context.start();
        Objects.requireNonNull(loggerToConfigure);
        // Set sub-loggers to inherit this config
        loggerToConfigure.setAdditive(true);
        loggerToConfigure.setLevel(ch.qos.logback.classic.Level.TRACE);

        String fileAppenderName = METRIC_LOGGER_PREFIX + getFileName();
        RollingFileAppender<ILoggingEvent> logFileAppender = getAppenderForFile(loggerToConfigure, fileAppenderName);
        logFileAppender.start();
        // Add the replacement
        loggerToConfigure.detachAndStopAllAppenders();
        loggerToConfigure.addAppender(logFileAppender);
    }

    public Logger getLogger(String name) {
        return context.getLogger(name);
    }

    /**
     * Gets the name of the log file from the logger name passed. Telemetry logger names have "Metrics-" as prefix.
     *
     * @param loggerName "Metrics-{namespace}"
     * @return "{namespace}.log"
     */
    private String getLogFileName(String loggerName) {
        return loggerName.substring(METRIC_LOGGER_PREFIX.length()) + "." + CONFIG_PREFIX;
    }

    /**
     * Stop the logger context.
     */
    public void close() {
        context.stop();
    }

    /**
     * Changes the telemetry config root path to new path and moves files from old path to new path if the old path
     * exists.
     * If the new path already exists and is not empty, then the files from old path are not moved. However, the new
     * path will be used for all the new logs.
     *
     * @param newPath new path
     */
    public void setRoot(Path newPath) {
        if (Objects.equals(root, newPath)) {
            return;
        }
        try {
            close();
            if (root != null && Files.exists(root)) {
                Files.move(root, newPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            com.aws.greengrass.logging.api.Logger logging = LogManager.getLogger(TelemetryConfig.class);
            if (e.getClass().equals(DirectoryNotEmptyException.class)) {
                logging.atError().cause(e).log("Could not move the telemetry log files from source : {} to target : {}"
                        + ". However, new logs will be written to the target directory", root, newPath);
            } else {
                logging.atError().cause(e).log();
            }
        }
        /*
         * telemetry
         * |___ generic.log
         * |___ KernelComponents.log
         * |___ SystemMetrics.log
         * |___ ....
         */
        root = newPath.resolve(TELEMETRY_DIRECTORY);
        /*
            Reconfigure all the telemetry loggers to use the store at new path.
         */
        for (String loggerName : LogManager.getTelemetryLoggerMap().keySet()) {
            close();
            editConfigForLogger(loggerName);
        }
    }
}

