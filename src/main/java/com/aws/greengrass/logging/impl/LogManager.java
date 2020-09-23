/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.greengrass.logging.impl;

import com.aws.greengrass.logging.impl.config.LogConfig;
import com.aws.greengrass.telemetry.impl.config.TelemetryConfig;
import lombok.Getter;
import org.slf4j.Logger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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
    private static final LogConfig config = LogConfig.getInstance();
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
        return loggerMap.computeIfAbsent(name, n -> {
            Logger logger = config.getLogger(name);
            return new Slf4jLogAdapter(logger, config);
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

}
