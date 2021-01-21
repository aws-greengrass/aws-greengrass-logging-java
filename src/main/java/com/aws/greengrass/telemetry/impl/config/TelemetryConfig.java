/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.greengrass.telemetry.impl.config;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.RollingFileAppender;
import com.aws.greengrass.logging.impl.config.LogFormat;
import com.aws.greengrass.logging.impl.config.LogStore;
import com.aws.greengrass.logging.impl.config.PersistenceConfig;
import com.aws.greengrass.logging.impl.config.model.LoggerConfiguration;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.event.Level;

import java.nio.file.Path;
import java.nio.file.Paths;
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
        startContext();
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
    private void setStorePath(Path path) {
        String newStoreName = deTilde(root.resolve(path).toString());
        if (Objects.equals(this.storeName, newStoreName)) {
            return;
        }
        this.storeName = newStoreName;
        setFileNameFromStoreName();
        setStoreDirectoryFromStoreName();
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

    /**
     * Reconfigures the logger based on the logger configuration provided. Overriding this since we don't want
     * to change the telemetry directory or file name.
     *
     * @param loggerConfiguration   The configuration for the logger.
     * @param storePath             Ths output directory path.
     */
    @Override
    public synchronized void reconfigure(LoggerConfiguration loggerConfiguration, Path storePath) {
        store = loggerConfiguration.getOutputType();
        fileSizeKB = loggerConfiguration.getFileSizeKB();
        totalLogStoreSizeKB = loggerConfiguration.getTotalLogsSizeKB();
        closeContext();
        //Reconfigure all the telemetry loggers to use the store at new path.
        for (Logger logger : context.getLoggerList()) {
            if (!logger.getName().equals("ROOT")) {
                editConfigForLogger(logger.getName());
            }
        }
        startContext();
    }


    @Override
    protected synchronized void reconfigure(Logger loggerToConfigure) {
        Objects.requireNonNull(loggerToConfigure);
        // Set sub-loggers to inherit this config
        loggerToConfigure.setAdditive(true);
        loggerToConfigure.setLevel(ch.qos.logback.classic.Level.TRACE);

        String fileAppenderName = METRIC_LOGGER_PREFIX + getFileName();
        RollingFileAppender<ILoggingEvent> logFileAppender = getAppenderForFile(loggerToConfigure, fileAppenderName,
                storeName, totalLogStoreSizeKB, fileSizeKB, getFileName());
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
    public void closeContext() {
        context.stop();
    }

    /**
     * Start the logger context.
     */
    public void startContext() {
        context.start();
    }

    /**
     * Changes the telemetry config root path to new path .
     *
     * @param newPath new path
     */
    public void setRoot(Path newPath) {
        if (newPath != null) {
            newPath = Paths.get(deTilde(newPath.resolve(TELEMETRY_DIRECTORY).toString()));
            if (Objects.equals(root, newPath)) {
                return;
            }
            root = newPath;
            this.storeDirectory = root;
            closeContext();
            // Reconfigure all the telemetry loggers to use the store at new path.
            for (Logger logger : context.getLoggerList()) {
                if (!logger.getName().equals("ROOT")) {
                    editConfigForLogger(logger.getName());
                }
            }
            startContext();
        }
    }

    /**
     * Used in unit tests.
     */
    public void reset() {
        this.store = LogStore.valueOf(DEFAULT_STORAGE_TYPE);
        this.storeDirectory = getRootStorePath().resolve(TELEMETRY_DIRECTORY);
        this.fileSizeKB = DEFAULT_MAX_FILE_SIZE_IN_KB;
        this.totalLogStoreSizeKB = DEFAULT_MAX_SIZE_IN_KB;
    }
}

