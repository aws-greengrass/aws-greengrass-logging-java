/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.telemetry.examples;

import com.aws.iot.evergreen.telemetry.api.MetricDataBuilder;
import com.aws.iot.evergreen.telemetry.impl.Metric;
import com.aws.iot.evergreen.telemetry.impl.MetricFactory;
import com.aws.iot.evergreen.telemetry.models.TelemetryAggregation;
import com.aws.iot.evergreen.telemetry.models.TelemetryMetricName;
import com.aws.iot.evergreen.telemetry.models.TelemetryNamespace;
import com.aws.iot.evergreen.telemetry.models.TelemetryUnit;

/**
 * A simple demo that creates a Telemetry.log file at the root directory with the metrics emitted.
 */
public class TelemetryDemo {
    static {
        System.setProperty("metrics.store", "FILE");
        System.setProperty("log.level", "DEBUG");
        System.setProperty("metrics.storeName", "Telemetry.log");
    }

    /**
     * Metrics: {"M":{"NS":"Kernel","N":"CpuUsage","U":"Percent","A":"Average","D":null},"V":100,"TS":1598296716029}.
     */
    public static void main(String[] args) {
        Metric metric = Metric.builder()
                .metricNamespace(TelemetryNamespace.Kernel)
                .metricName(TelemetryMetricName.SystemMetrics.CpuUsage)
                .metricAggregation(TelemetryAggregation.Average)
                .metricUnit(TelemetryUnit.Percent)
                .build();
        MetricDataBuilder metricDataBuilder = new MetricFactory().addMetric(metric);
        metricDataBuilder.putMetricData(100).emit();
        metricDataBuilder.putMetricData(120).emit();
        metricDataBuilder.putMetricData(150).emit();
        metricDataBuilder.putMetricData(180).emit();

    }
}