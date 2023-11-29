/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.greengrass.logging.impl.config;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.aws.greengrass.logging.impl.LogManager;
import com.aws.greengrass.logging.impl.config.model.LogConfigUpdate;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import org.slf4j.event.Level;
import org.slf4j.impl.StaticMDCBinder;

import java.nio.file.Paths;
import java.util.Optional;

@Getter
public class LogConfig extends PersistenceConfig {
    public static final String LOGS_DIRECTORY = "logs";
    public static final String LOG_FILE_EXTENSION = "log";
    private final LoggerContext context = new LoggerContext();

    private static final LogConfig ROOT_LOG_CONFIG = new LogConfig();

    /**
     * This is the root logger configuration. Child loggers may refer to this to find default config values
     */
    public static LogConfig getRootLogConfig() {
        return ROOT_LOG_CONFIG;
    }

    /**
     * Get default logging configuration from system properties.
     */
    @SuppressFBWarnings("MC_OVERRIDABLE_METHOD_CALL_IN_CONSTRUCTOR")
    protected LogConfig() {
        super(LOG_FILE_EXTENSION, LOGS_DIRECTORY);
        // Must set an MDC adapter for 1.3.8+. https://github.com/qos-ch/logback/issues/709
        context.setMDCAdapter(StaticMDCBinder.SINGLETON.getMDCA());
        reconfigure(context.getLogger(Logger.ROOT_LOGGER_NAME));
        startContext();
    }

    /**
     * Create a new instance of LogConfig by inheriting configs from current root config.
     *
     * @param configOverrides parameters to override the root config
     * @return a new instance of LogConfig
     */
    public static LogConfig newLogConfigFromRootConfig(LogConfigUpdate configOverrides) {
        LogConfigUpdate configUpdate = fillNullFieldsFromRootConfig(configOverrides);
        LogConfig newConfig = new LogConfig();
        newConfig.format = configUpdate.getFormat();
        newConfig.store = configUpdate.getOutputType();
        newConfig.fileSizeKB = configUpdate.getFileSizeKB();
        newConfig.totalLogStoreSizeKB = configUpdate.getTotalLogsSizeKB();
        if (configUpdate.getOutputDirectory() == null) {
            newConfig.storeDirectory = getRootLogConfig().getStoreDirectory();
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
     * Create a new LogConfigUpdate from current root config, but override it with the given configUpdate.
     * Effectively inheriting the root config with overrides.
     *
     * @param configOverrides params to override the root config
     * @return a new instance of LogConfigUpdate
     */
    private static LogConfigUpdate fillNullFieldsFromRootConfig(LogConfigUpdate configOverrides) {
        LogConfig rootLogConfiguration = LogManager.getRootLogConfiguration();
        LogConfigUpdate.LogConfigUpdateBuilder newConfigUpdate = configOverrides.toBuilder();
        if (configOverrides.getFileName() == null || configOverrides.getFileName().trim().isEmpty()) {
            newConfigUpdate.fileName(rootLogConfiguration.getFileName());
        }
        if (configOverrides.getFileSizeKB() == null) {
            newConfigUpdate.fileSizeKB(rootLogConfiguration.getFileSizeKB());
        }
        if (configOverrides.getTotalLogsSizeKB() == null) {
            newConfigUpdate.totalLogsSizeKB(rootLogConfiguration.getTotalLogStoreSizeKB());
        }
        if (configOverrides.getFormat() == null) {
            newConfigUpdate.format(rootLogConfiguration.getFormat());
        }
        if (configOverrides.getLevel() == null) {
            newConfigUpdate.level(rootLogConfiguration.getLevel());
        }
        if (configOverrides.getOutputType() == null) {
            newConfigUpdate.outputType(rootLogConfiguration.getStore());
        }
        if (configOverrides.getOutputDirectory() == null && LogStore.FILE
                .equals(configOverrides.getOutputType())) {
            newConfigUpdate.outputDirectory(rootLogConfiguration.getStoreDirectory().toString());
        }
        return newConfigUpdate.build();
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
