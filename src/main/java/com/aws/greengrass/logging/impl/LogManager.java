/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.greengrass.logging.impl;

import com.aws.greengrass.logging.impl.config.LogConfig;
import com.aws.greengrass.logging.impl.config.model.LoggerConfiguration;
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

/**
 * LogManager instances manufacture {@link com.aws.greengrass.logging.api.Logger}
 * instances by name.
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
     * Return an appropriate {@link com.aws.greengrass.logging.api.Logger} instance as specified by the name
     * parameter.
     *
     * @param name the name of the Logger to return
     * @return a Logger instance
     */
    public static com.aws.greengrass.logging.api.Logger getLogger(String name) {
        logConfigurations.put(name, rootLogConfiguration);
        return loggerMap.computeIfAbsent(name, n -> {
            Logger logger = rootLogConfiguration.getLogger(name);
            return new Slf4jLogAdapter(logger, rootLogConfiguration);
        });
    }

    /**
     * Return an appropriate {@link com.aws.greengrass.logging.api.Logger} instance as specified by the name
     * parameter.
     *
     * @param name                  the name of the Logger to return
     * @param loggerConfiguration   the configuration for the Logger
     * @return a Logger instance
     */
    public static com.aws.greengrass.logging.api.Logger getLogger(String name,
                                                                  LoggerConfiguration loggerConfiguration) {
        LogConfig logConfig = logConfigurations.computeIfAbsent(name, s ->
                new LogConfig(name, loggerConfiguration, rootLogConfiguration.getStore(),
                        rootLogConfiguration.getFormat(), rootLogConfiguration.getStoreDirectory()));
        if (loggerConfiguration != null && loggerConfiguration.getLevel() != null) {
            logConfig.setLevel(loggerConfiguration.getLevel());
        }
        return loggerMap.computeIfAbsent(name, n -> {
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
     * Return an appropriate {@link com.aws.greengrass.logging.api.Logger} instance as specified by the name
     * parameter.
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
            Path root = rootConfig.getRoot();
            newPath = Paths.get(rootConfig.deTilde(newPath.resolve(LOGS_DIRECTORY).toString()));
            if (Objects.equals(root, newPath)) {
                return;
            }
            //Reconfigure all the loggers to use the store at new path.
            for (LogConfig logConfig: logConfigurations.values()) {
                logConfig.closeContext();
                logConfig.setStorePath(newPath
                        .resolve(logConfig.getFileName() + "." + logConfig.getExtension()));
                logConfig.startContext();
            }
        }
    }
}
