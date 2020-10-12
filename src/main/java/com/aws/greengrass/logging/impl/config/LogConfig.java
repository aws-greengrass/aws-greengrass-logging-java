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
import java.util.Optional;

@Getter
public class LogConfig extends PersistenceConfig {
    // TODO: Replace the default log level from Kernel Configuration.
    public static final String LOGS_DIRECTORY = "logs";
    public static final String LOG_FILE_EXTENSION = "log";
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
        super(LOG_FILE_EXTENSION, LOGS_DIRECTORY);
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
        super(LOG_FILE_EXTENSION, LOGS_DIRECTORY);
        this.format = logFormat;
        this.store = logStore;
        this.storeDirectory = storeDirectory;
        Optional<String> fileNameWithoutExtension = stripExtension(loggerConfiguration.getFileName());
        this.fileName = fileNameWithoutExtension.orElseGet(() -> this.storeName);
        reconfigure(context.getLogger(name), loggerConfiguration);
    }

    public Logger getLogger(String name) {
        return context.getLogger(name);
    }

    private synchronized void reconfigure(Logger loggerToConfigure, LoggerConfiguration loggerConfiguration) {
        String loggerFileName = this.fileName + "." + extension;
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
    public void startContext() {
        context.start();
    }
}
