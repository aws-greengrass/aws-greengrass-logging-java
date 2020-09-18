/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.greengrass.telemetry.examples;

import com.aws.greengrass.telemetry.impl.Metric;
import com.aws.greengrass.telemetry.impl.MetricFactory;
import com.aws.greengrass.telemetry.models.TelemetryAggregation;
import com.aws.greengrass.telemetry.models.TelemetryMetricName;
import com.aws.greengrass.telemetry.models.TelemetryNamespace;
import com.aws.greengrass.telemetry.models.TelemetryUnit;

/**
 * A simple demo that creates a Telemetry.log file at the root directory with the metrics emitted.
 */
public class TelemetryDemo {

    /**
     * Metrics: {"M":{"NS":"Kernel","N":"CpuUsage","U":"Percent","A":"Average","D":null},"V":100,"TS":1598296716029}.
     */
    public static void main(String[] args) {
        Metric metric = Metric.builder()
                .namespace(TelemetryNamespace.SystemMetrics)
                .name(TelemetryMetricName.CpuUsage)
                .unit(TelemetryUnit.Percent)
                .aggregation(TelemetryAggregation.Average)
                .build();
        MetricFactory mf = new MetricFactory("Test");
        mf.putMetricData(metric, 10);
        mf.putMetricData(metric, 230);
    }
}
