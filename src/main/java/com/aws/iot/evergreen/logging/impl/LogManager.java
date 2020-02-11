/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.aws.iot.evergreen.logging.impl;

import com.aws.iot.evergreen.logging.api.MetricsFactory;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * LogManager instances manufacture {@link com.aws.iot.evergreen.logging.api.Logger}
 * and {@link MetricsFactory} instances by name.
 */
public class LogManager {

    // key: name (String), value: a Logger;
    static ConcurrentMap<String, com.aws.iot.evergreen.logging.api.Logger> loggerMap = new ConcurrentHashMap<>();

    protected LogManager() {
    }

    /**
     * Return an appropriate {@link com.aws.iot.evergreen.logging.api.Logger} instance
     * as specified by the name parameter.
     *
     * @param name the name of the Logger to return
     * @return a Logger instance
     */
    public static com.aws.iot.evergreen.logging.api.Logger getLogger(String name) {
        return loggerMap.computeIfAbsent(name, n -> {
            Logger log4jLogger = org.apache.logging.log4j.LogManager.getLogger(name);
            return new Log4jLoggerAdapter(log4jLogger);
        });
    }

    /**
     * Return an appropriate {@link com.aws.iot.evergreen.logging.api.Logger} instance as specified by the name
     * of the clazz parameter.
     *
     * @param clazz the clazz name is used by the Logger to return
     * @return a Logger instance
     */
    public static com.aws.iot.evergreen.logging.api.Logger getLogger(Class<?> clazz) {
        return getLogger(clazz.getName());
    }

    /**
     * Return an appropriate {@link MetricsFactory} instance as specified by the name parameter.
     * TODO: implement metrics generation https://sim.amazon.com/issues/P32636549
     *
     * @param name the name of the MetricsFactory to return
     * @return a MetricsFactory instance
     */
    public static MetricsFactory getMetricsFactory(String name) {
        return null;
    }

    /**
     * Return an appropriate {@link MetricsFactory} instance as specified by the name of the clazz parameter.
     *
     * @param clazz the clazz name is used by the MetricsFactory to return
     * @return a MetricsFactory instance
     */
    public static MetricsFactory getMetricsFactory(Class<?> clazz) {
        return getMetricsFactory(clazz.getName());
    }
}