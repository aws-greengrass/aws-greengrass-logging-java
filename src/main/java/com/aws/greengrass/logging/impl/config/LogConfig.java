/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.greengrass.logging.impl.config;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.aws.greengrass.logging.impl.LogManager;
import com.aws.greengrass.logging.impl.config.model.LogConfigUpdate;
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
     * Create a new instance of LogConfig by inheriting configs from current root config.
     *
     * @param configUpdate parameters to override the root config
     * @return a new instance of LogConfig
     */
    public static LogConfig newLogConfigFromRootConfig(LogConfigUpdate configUpdate) {
        fillNullFieldsFromRootConfig(configUpdate);
        LogConfig newConfig = new LogConfig();
        newConfig.format = configUpdate.getFormat();
        newConfig.store = configUpdate.getOutputType();
        if (configUpdate.getOutputDirectory() == null) {
            newConfig.storeDirectory = getInstance().getStoreDirectory();
        } else {
            newConfig.storeDirectory = Paths.get(deTilde(configUpdate.getOutputDirectory()));
        }
        Optional<String> fileNameWithoutExtension = stripExtension(configUpdate.getFileName());
        newConfig.fileName = fileNameWithoutExtension.orElseGet(() -> newConfig.storeName);
        newConfig.storeName =
                newConfig.storeDirectory.resolve(configUpdate.getFileName()).toAbsolutePath().toString();
        newConfig.level = configUpdate.getLevel();
        newConfig.reconfigure(newConfig.context.getLogger(Logger.ROOT_LOGGER_NAME));
        return newConfig;
    }

    /**
     * If a field is null in the given configUpdate, set it using the value from root logging config.
     * Effectively inheriting the root config
     */
    private static void fillNullFieldsFromRootConfig(LogConfigUpdate configUpdate) {
        LogConfig rootLogConfiguration = LogManager.getRootLogConfiguration();
        if (configUpdate.getFileName() == null || configUpdate.getFileName().trim().isEmpty()) {
            configUpdate.setFileName(rootLogConfiguration.getFileName());
        }
        if (configUpdate.getFileSizeKB() == null) {
            configUpdate.setFileSizeKB(rootLogConfiguration.getFileSizeKB());
        }
        if (configUpdate.getTotalLogsSizeKB() == null) {
            configUpdate.setTotalLogsSizeKB(rootLogConfiguration.getTotalLogStoreSizeKB());
        }
        if (configUpdate.getFormat() == null) {
            configUpdate.setFormat(rootLogConfiguration.getFormat());
        }
        if (configUpdate.getLevel() == null) {
            configUpdate.setLevel(rootLogConfiguration.getLevel());
        }
        if (configUpdate.getOutputType() == null) {
            configUpdate.setOutputType(rootLogConfiguration.getStore());
        }
        if (configUpdate.getOutputDirectory() == null && LogStore.FILE
                .equals(configUpdate.getOutputType())) {
            configUpdate.setOutputDirectory(rootLogConfiguration.getStoreDirectory().toString());
        }
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
