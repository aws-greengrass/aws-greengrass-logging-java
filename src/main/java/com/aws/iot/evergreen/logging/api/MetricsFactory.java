package com.aws.iot.evergreen.logging.api;

public interface MetricsFactory {
    String getName();

    void addDefaultDimension(String key, Object value);

    MetricsBuilder newMetrics();
}
