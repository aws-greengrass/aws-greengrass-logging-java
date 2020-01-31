package com.aws.iot.evergreen.logging.api;

public interface LogManager {
    Logger getLogger(String name);

    Logger getLogger(Class<?> clazz);

    MetricsFactory getMetricsFactory(String name);

    MetricsFactory getMetricsFactory(Class<?> clazz);
}
