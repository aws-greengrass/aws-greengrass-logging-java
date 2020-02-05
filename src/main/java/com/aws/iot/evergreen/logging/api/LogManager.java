/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.aws.iot.evergreen.logging.api;

/**
 * LogManager instances manufacture {@link Logger} and {@link MetricsFactory} instances by name.
 */
public interface LogManager {
    /**
     * Return an appropriate {@link Logger} instance as specified by the name parameter.
     *
     * @param name the name of the Logger to return
     * @return a Logger instance
     */
    Logger getLogger(String name);

    /**
     * Return an appropriate {@link Logger} instance as specified by the name of the clazz parameter.
     *
     * @param clazz the clazz name is used by the Logger to return
     * @return a Logger instance
     */
    Logger getLogger(Class<?> clazz);

    /**
     * Return an appropriate {@link MetricsFactory} instance as specified by the name parameter.
     *
     * @param name the name of the MetricsFactory to return
     * @return a MetricsFactory instance
     */
    MetricsFactory getMetricsFactory(String name);

    /**
     * Return an appropriate {@link MetricsFactory} instance as specified by the name of the clazz parameter.
     *
     * @param clazz the clazz name is used by the MetricsFactory to return
     * @return a MetricsFactory instance
     */
    MetricsFactory getMetricsFactory(Class<?> clazz);
}
