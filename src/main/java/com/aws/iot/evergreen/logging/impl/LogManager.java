/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.logging.impl;

import com.aws.iot.evergreen.logging.api.MetricsFactory;
import com.aws.iot.evergreen.logging.impl.config.EvergreenLogConfig;
import com.aws.iot.evergreen.logging.impl.config.LogStore;
import lombok.Getter;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * LogManager instances manufacture {@link com.aws.iot.evergreen.logging.api.Logger} and {@link MetricsFactory}
 * instances by name.
 */
public class LogManager {

    // key: name (String), value: a Logger;
    private static final ConcurrentMap<String, com.aws.iot.evergreen.logging.api.Logger> loggerMap =
            new ConcurrentHashMap<>();
    @Getter
    private static final EvergreenLogConfig config = EvergreenLogConfig.getInstance();

    /**
     * Return an appropriate {@link com.aws.iot.evergreen.logging.api.Logger} instance as specified by the name
     * parameter.
     *
     * @param name the name of the Logger to return
     * @return a Logger instance
     */
    public static com.aws.iot.evergreen.logging.api.Logger getLogger(String name) {
        return loggerMap.computeIfAbsent(name, n -> {
            Logger logger = config.getLogger(name);
            return new Slf4jLogAdapter(logger, config);
        });
    }

    /**
     * Return an appropriate {@link com.aws.iot.evergreen.logging.api.Logger} instance as specified by the name of the
     * clazz parameter.
     *
     * @param clazz the clazz name is used by the Logger to return
     * @return a Logger instance
     */
    public static com.aws.iot.evergreen.logging.api.Logger getLogger(Class<?> clazz) {
        return getLogger(clazz.getName());
    }

    /**
     * Return a {@link MetricsFactory} instance.
     *
     * @return a MetricsFactory instance
     */
    public static MetricsFactory getMetricsFactory() {
        return MetricsFactoryImpl.getInstance();
    }

    /**
     * Gets the directory path where the logs are located for the logger.
     *
     * @return directory path containing the log files.
     * */
    public static Optional<Path> getLoggerDirectoryPath() {
        if (LogStore.FILE.equals(config.getStore())) {
            Path path = Paths.get(config.getLogFileAppender().getRollingPolicy().getActiveFileName());
            return Optional.of(path.getParent());
        }
        return Optional.empty();
    }
}
