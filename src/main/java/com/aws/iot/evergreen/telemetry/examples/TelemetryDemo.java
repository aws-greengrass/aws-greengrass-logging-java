package com.aws.iot.evergreen.telemetry.examples;

import com.aws.iot.evergreen.telemetry.api.MetricDataBuilder;
import com.aws.iot.evergreen.telemetry.impl.Metric;
import com.aws.iot.evergreen.telemetry.impl.MetricFactory;
import com.aws.iot.evergreen.telemetry.models.*;

/**
 * A simple demo that creates a Telemetry.log file at the root directory with the metrics emitted.
 */

public class TelemetryDemo {

    static {
        System.setProperty("metrics.store", "FILE");
        System.setProperty("metrics.storeName", "Telemetry.log");
    }

    /**
     * Metrics: {"metricDataPoint":{"metric":{"metricNamespace":"KERNEL","metricName":"CPU_UTILIZATION",
     * "metricUnit":"COUNT","metricType":"TIME_BASED","metricDimensions":null},"value":100,"timestamp":1598035264534}}.
     */
    public static void main(String[] args) {
        Metric metric = Metric.builder()
                .metricNamespace(TelemetryNamespace.KERNEL)
                .metricName(TelemetryMetricName.SystemMetrics.CPU_UTILIZATION)
                .metricAggregation(TelemetryAggregation.AVERAGE)
                .metricType(TelemetryType.TIME_BASED)
                .metricUnit(TelemetryUnit.PERCENT)
                .build();
        MetricDataBuilder metricDataBuilder = new MetricFactory().addMetric(metric);
        metricDataBuilder.putMetricData(100).emit();
        metricDataBuilder.putMetricData(120).emit();
        metricDataBuilder.putMetricData(150).emit();
    }
}