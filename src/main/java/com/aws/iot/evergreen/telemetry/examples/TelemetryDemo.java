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

    /**
     * Metrics: {"M":{"NS":"Kernel","N":"CpuUsage","U":"Percent","A":"Average","D":null},"V":100,"TS":1598296716029}.
     */
    public static void main(String[] args) {
        Metric metric = Metric.builder()
                .metricNamespace(TelemetryNamespace.SystemMetrics)
                .metricName(TelemetryMetricName.CpuUsage)
                .metricUnit(TelemetryUnit.Percent)
                .metricAggregation(TelemetryAggregation.Average)
                .build();
        MetricDataBuilder metricDataBuilder = new MetricFactory().addMetric(metric);
        metricDataBuilder.putMetricData(100).emit();
        metricDataBuilder.putMetricData(120).emit();
        metricDataBuilder.putMetricData(150).emit();
        metricDataBuilder.putMetricData(180).emit();
        metricDataBuilder.putMetricData(180).emit();
        MetricDataBuilder mdb2 = new MetricFactory("french fries").addMetric(metric);
        mdb2.putMetricData(100).emit();
        metricDataBuilder.putMetricData(120).emit();
        metricDataBuilder.putMetricData(150).emit();
        metricDataBuilder.putMetricData(180).emit();
        mdb2.putMetricData(180).emit();
        MetricDataBuilder mdb3 = new MetricFactory("french fries").addMetric(metric);
        mdb3.putMetricData(123).emit();
    }
}