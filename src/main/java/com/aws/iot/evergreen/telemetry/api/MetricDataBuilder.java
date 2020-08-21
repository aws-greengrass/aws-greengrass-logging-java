package com.aws.iot.evergreen.telemetry.api;

import com.aws.iot.evergreen.telemetry.impl.Metric;

public interface MetricDataBuilder {
    MetricDataBuilder NOOP = new MetricDataBuilder() {

    };

    default MetricDataBuilder putMetricData(Object value) {
        return this;
    }

    default void emit() {

    }
}


