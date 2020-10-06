/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.greengrass.logging.impl.config;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.aws.greengrass.logging.impl.config.model.LoggerConfiguration;
import lombok.Getter;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import static com.aws.greengrass.telemetry.impl.MetricFactory.METRIC_LOGGER_PREFIX;

@Getter
public class LogConfig extends PersistenceConfig {
    // TODO: Replace the default log level from Kernel Configuration.
    public static final String LOGS_DIRECTORY = "logs";
    public static final String CONFIG_PREFIX = "log";
    private static final LoggerContext context = new LoggerContext();
    private Path root = getRootStorePath().resolve(LOGS_DIRECTORY);


    private static final LogConfig INSTANCE = new LogConfig();

    public static LogConfig getInstance() {
        return INSTANCE;
    }

    /**
     * Get default logging configuration from system properties.
     */
    protected LogConfig() {
        super(CONFIG_PREFIX, LOGS_DIRECTORY);
        reconfigure(context.getLogger(Logger.ROOT_LOGGER_NAME));
        startContext();
    }

    /**
     * Get default logging configuration from system properties.
     *
     * @param name                  the name of the logger.
     * @param loggerConfiguration   the configuration for the logger.
     */
    public LogConfig(String name, LoggerConfiguration loggerConfiguration, LogStore logStore, LogFormat logFormat,
                     Path storeDirectory) {
        super(CONFIG_PREFIX, LOGS_DIRECTORY);
        this.format = logFormat;
        this.store = logStore;
        this.storeDirectory = storeDirectory;
        reconfigure(context.getLogger(name), loggerConfiguration);
    }

    public Logger getLogger(String name) {
        return context.getLogger(name);
    }

    private synchronized void reconfigure(Logger loggerToConfigure, LoggerConfiguration loggerConfiguration) {
        String loggerFileName = this.fileName + "." + prefix;
        if (loggerConfiguration != null && !loggerConfiguration.getFileName().isEmpty()) {
            loggerFileName = loggerConfiguration.getFileName();
        }
        long loggerTotalLogStoreSizeKB = totalLogStoreSizeKB;
        if (loggerConfiguration != null && loggerConfiguration.getTotalLogStoreSizeKB() != -1) {
            loggerTotalLogStoreSizeKB = loggerConfiguration.getTotalLogStoreSizeKB();
        }

        long loggerFileSizeKB = fileSizeKB;
        if (loggerConfiguration != null && loggerConfiguration.getTotalLogStoreSizeKB() != -1) {
            loggerFileSizeKB = loggerConfiguration.getFileSizeKB();
        }
        reconfigure(loggerToConfigure, loggerFileName, loggerTotalLogStoreSizeKB, loggerFileSizeKB);
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
    private void startContext() {
        context.start();
    }

    /**
     * Changes the telemetry config root path to new path .
     *
     * @param newPath new path
     */
    public void setRoot(Path newPath) {
        if (newPath != null) {
            newPath = Paths.get(deTilde(newPath.resolve(LOGS_DIRECTORY).toString()));
            if (Objects.equals(root, newPath)) {
                return;
            }
            root = newPath;
            closeContext();
            //Reconfigure all the telemetry loggers to use the store at new path.
            for (Logger logger : context.getLoggerList()) {
                editConfigForLogger(logger.getName());
            }
            startContext();
        }
    }

    /**
     * Sets up logger and store name.
     *
     * @param loggerName This is used as the name of the telemetry logger.
     */
    private void editConfigForLogger(String loggerName) {
        this.logger = getLogger(loggerName);
        this.setStorePath(Paths.get(getLogFileName(loggerName)));
    }

    /**
     * Gets the name of the log file from the logger name passed. Telemetry logger names have "Metrics-" as prefix.
     *
     * @param loggerName "Metrics-{namespace}"
     * @return "{namespace}.log"
     */
    private String getLogFileName(String loggerName) {
        return loggerName.substring(APPENDER_PREFIX.length()) + "." + CONFIG_PREFIX;
    }
}
