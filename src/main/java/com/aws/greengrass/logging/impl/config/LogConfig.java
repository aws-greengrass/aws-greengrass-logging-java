/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.greengrass.logging.impl.config;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.aws.greengrass.logging.impl.LogManager;
import com.aws.greengrass.logging.impl.config.model.LoggerConfiguration;
import lombok.Getter;
import org.slf4j.event.Level;

import java.nio.file.Paths;
import java.util.Optional;

@Getter
public class LogConfig extends PersistenceConfig {
    public static final String LOGS_DIRECTORY = "logs";
    public static final String LOG_FILE_EXTENSION = "log";
    private final LoggerContext context = new LoggerContext();

    private static final LogConfig INSTANCE = new LogConfig();

    /**
     * This is the root logger configuration. Child loggers may refer to this to find default config values
     */
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
     * @param loggerConfiguration the configuration for the logger.
     */
    public LogConfig(LoggerConfiguration loggerConfiguration) {
        super(LOG_FILE_EXTENSION, LOGS_DIRECTORY);
        LogManager.setNullFieldsFromRootConfig(loggerConfiguration);
        this.format = loggerConfiguration.getFormat();
        this.store = loggerConfiguration.getOutputType();
        if (loggerConfiguration.getOutputDirectory() == null) {
            this.storeDirectory = getInstance().getStoreDirectory();
        } else {
            this.storeDirectory = Paths.get(deTilde(loggerConfiguration.getOutputDirectory()));
        }
        Optional<String> fileNameWithoutExtension = stripExtension(loggerConfiguration.getFileName());
        this.fileName = fileNameWithoutExtension.orElseGet(() -> this.storeName);
        this.storeName = this.storeDirectory.resolve(loggerConfiguration.getFileName()).toAbsolutePath().toString();
        this.level = loggerConfiguration.getLevel();
        reconfigure(context.getLogger(Logger.ROOT_LOGGER_NAME));
        startContext();
    }

    public Logger getLogger(String name) {
        return context.getLogger(name);
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
     * Used in unit tests.
     */
    public void reset() {
        this.fileName = DEFAULT_STORE_NAME;
        this.level = Level.valueOf(DEFAULT_LOG_LEVEL);
        this.format = LogFormat.valueOf(DEFAULT_DATA_FORMAT);
        this.store = LogStore.valueOf(DEFAULT_STORAGE_TYPE);
        this.storeDirectory = getRootStorePath().resolve(LOGS_DIRECTORY);
        this.fileSizeKB = DEFAULT_MAX_FILE_SIZE_IN_KB;
        this.totalLogStoreSizeKB = DEFAULT_MAX_SIZE_IN_KB;
    }
}
