/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.greengrass.logging.impl;

import com.aws.greengrass.logging.impl.config.LogConfig;
import com.aws.greengrass.logging.impl.config.PersistenceConfig;
import com.aws.greengrass.logging.impl.config.model.LogConfigUpdate;
import com.aws.greengrass.telemetry.impl.config.TelemetryConfig;
import lombok.Getter;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.aws.greengrass.logging.impl.config.LogConfig.LOGS_DIRECTORY;
import static com.aws.greengrass.logging.impl.config.LogConfig.LOG_FILE_EXTENSION;

/**
 * LogManager instances manufacture {@link com.aws.greengrass.logging.api.Logger} instances by name.
 */
public class LogManager {

    // key: name (String), value: a Logger;
    private static final ConcurrentMap<String, com.aws.greengrass.logging.api.Logger> loggerMap =
            new ConcurrentHashMap<>();
    // key: name (String), value: a Logger;
    private static final ConcurrentMap<String, com.aws.greengrass.logging.api.Logger> telemetryLoggerMap =
            new ConcurrentHashMap<>();
    @Getter
    private static final LogConfig rootLogConfiguration = LogConfig.getInstance();
    @Getter
    private static final Map<String, LogConfig> logConfigurations = new ConcurrentHashMap<>();
    @Getter
    private static final TelemetryConfig telemetryConfig = TelemetryConfig.getInstance();

    /**
     * Return an appropriate {@link com.aws.greengrass.logging.api.Logger} instance as specified by the name parameter.
     *
     * @param name the name of the Logger to return
     * @return a Logger instance
     */
    public static com.aws.greengrass.logging.api.Logger getLogger(String name) {
        return loggerMap.computeIfAbsent(name, n -> {
            Logger logger = rootLogConfiguration.getLogger(name);
            return new Slf4jLogAdapter(logger, rootLogConfiguration);
        });
    }

    /**
     * Return an appropriate {@link com.aws.greengrass.logging.api.Logger} instance as specified by the name parameter.
     *
     * @param name                the name of the Logger to return
     * @param logConfigUpdate the configuration for the Logger
     * @return a Logger instance
     */
    public static com.aws.greengrass.logging.api.Logger getLogger(String name, LogConfigUpdate logConfigUpdate) {
        return loggerMap.computeIfAbsent(name, n -> {
            LogConfig logConfig =
                    logConfigurations.computeIfAbsent(name, s -> LogConfig.newLogConfigFromRootConfig(logConfigUpdate));
            Logger logger = logConfig.getLogger(name);
            return new Slf4jLogAdapter(logger, logConfig);
        });
    }

    /**
     * Return an appropriate {@link com.aws.greengrass.logging.api.Logger} instance as specified by the name of the
     * clazz parameter.
     *
     * @param clazz the clazz name is used by the Logger to return
     * @return a Logger instance
     */
    public static com.aws.greengrass.logging.api.Logger getLogger(Class<?> clazz) {
        return getLogger(clazz.getName());
    }

    /**
     * Return an appropriate {@link com.aws.greengrass.logging.api.Logger} instance as specified by the name parameter.
     *
     * @param name the name of the Telemetry Logger to return
     * @return a telemetry Logger instance
     */
    public static com.aws.greengrass.logging.api.Logger getTelemetryLogger(String name) {
        return telemetryLoggerMap.computeIfAbsent(name, n -> {
            Logger logger = telemetryConfig.getLogger(name);
            return new Slf4jLogAdapter(logger, telemetryConfig);
        });
    }


    /**
     * Changes the logger config root path to new path .
     *
     * @param newPath new path
     */
    public static void setRoot(Path newPath) {
        if (newPath != null) {
            LogConfig rootConfig = LogConfig.getInstance();
            newPath = Paths.get(rootConfig.deTilde(newPath.resolve(LOGS_DIRECTORY).toString()));
            if (Objects.equals(rootLogConfiguration.getStoreDirectory(), newPath)) {
                return;
            }
            rootLogConfiguration.closeContext();
            rootLogConfiguration.setStoreDirectory(newPath);
            rootLogConfiguration.startContext();
            //Reconfigure all the loggers to use the store at new path.
            for (LogConfig logConfig : logConfigurations.values()) {
                logConfig.closeContext();
                logConfig.setStoreDirectory(newPath);
                logConfig.startContext();
            }
        }
    }

    /**
     * Reset the non-null fields of given config for all loggers back to the default value.
     * Supported RESET topics:
     *      level, format, outputType, fileSizeKB, totalLogsSizeKB, outputDirectory
     *
     * @param resetTopicName name of the topic to reset
     */
    public static void resetAllLoggers(String resetTopicName) {
        // Constructing a new PersistenceConfig here because its constructor can check system properties for logging
        // configs, which should be used as default
        PersistenceConfig defaultConfig = new PersistenceConfig(LOG_FILE_EXTENSION, LOGS_DIRECTORY);
        if (resetTopicName == null) {  // reset all to default
            reconfigureAllLoggers(new LogConfigUpdate(defaultConfig));
        } else {  // reset individual config
            LogConfigUpdate configToApply = LogConfigUpdate.builder().build();
            switch (resetTopicName) {
                case "level":
                    configToApply.setLevel(defaultConfig.getLevel());
                    break;
                case "format":
                    configToApply.setFormat(defaultConfig.getFormat());
                    break;
                case "outputType":
                    configToApply.setOutputType(defaultConfig.getStore());
                    break;
                case "fileSizeKB":
                    configToApply.setFileSizeKB(defaultConfig.getFileSizeKB());
                    break;
                case "totalLogsSizeKB":
                    configToApply.setTotalLogsSizeKB(defaultConfig.getTotalLogStoreSizeKB());
                    break;
                case "outputDirectory":
                    configToApply.setOutputDirectory(defaultConfig.getStoreDirectory().toString());
                    break;
                default:
                    // Unknown config topic. Do nothing
                    return;
            }
            reconfigureAllLoggers(configToApply);
        }
    }

    /**
     * Reconfigure all loggers to use the new configuration.
     *
     * @param logConfigUpdate configuration for all loggers.
     */
    public static void reconfigureAllLoggers(LogConfigUpdate logConfigUpdate) {
        Path storePath;
        if (logConfigUpdate.getOutputDirectory() == null || logConfigUpdate.getOutputDirectory().trim()
                .isEmpty()) {
            storePath = rootLogConfiguration.getStoreDirectory();
        } else {
            storePath = Paths.get(logConfigUpdate.getOutputDirectory());
        }

        // Only reconfigure file when directory, file size, or store size changes. Everything else
        // can be reconfigured without needing to recreate the log appender
        boolean reconfiguringFileOptions =
                !(Objects.equals(rootLogConfiguration.getStoreDirectory(), storePath) && Objects
                .equals(rootLogConfiguration.getFileSizeKB(), logConfigUpdate.getFileSizeKB()) && Objects
                .equals(rootLogConfiguration.getTotalLogStoreSizeKB(), logConfigUpdate.getTotalLogsSizeKB())
                && Objects.equals(rootLogConfiguration.getStore(), logConfigUpdate.getOutputType()));

        if (reconfiguringFileOptions) {
            rootLogConfiguration.closeContext();
            rootLogConfiguration.reconfigure(logConfigUpdate, storePath);
            rootLogConfiguration.startContext();
            // Reconfigure all the loggers to use the store at new path.
            for (LogConfig logConfig : logConfigurations.values()) {
                logConfig.closeContext();
                logConfig.reconfigure(logConfigUpdate, storePath);
                logConfig.startContext();
            }
            // Reconfigure the telemetry logger as well.
            telemetryConfig.reconfigure(logConfigUpdate, storePath);
        } else {
            // Set dynamically configurable options
            setLogConfig(rootLogConfiguration, logConfigUpdate);
            for (LogConfig logConfig : logConfigurations.values()) {
                setLogConfig(logConfig, logConfigUpdate);
            }
        }
    }

    private static void setLogConfig(LogConfig log, LogConfigUpdate config) {
        if (config.getLevel() != null) {
            log.setLevel(config.getLevel());
        }
        if (config.getFormat() != null) {
            log.setFormat(config.getFormat());
        }
    }
}
