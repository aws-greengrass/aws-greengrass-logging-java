/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.logging.impl;

import com.aws.iot.evergreen.logging.impl.config.EvergreenLogConfig;
import com.aws.iot.evergreen.telemetry.api.MetricFactoryBuilder;
import com.aws.iot.evergreen.telemetry.impl.MetricFactory;
import lombok.Getter;
import org.slf4j.Logger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * LogManager instances manufacture {@link com.aws.iot.evergreen.logging.api.Logger} and {@link MetricFactory}
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

}
