package com.aws.iot.evergreen.telemetry.impl;

import com.aws.iot.evergreen.telemetry.api.MetricDataBuilder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
public class MetricData implements MetricDataBuilder {

    private MetricDataPoint metricDataPoint = new MetricDataPoint();
    private Metric metric;
    private transient MetricFactory logger;

    public MetricData setLogger(MetricFactory logger) {
        this.logger = logger;
        return this;
    }

    public MetricData setMetric(Metric metric) {
        this.metric = metric;
        return this;
    }

    @Override
    public MetricData putMetricData(Object value) {
        this.metricDataPoint.setValue(value);
        this.metricDataPoint.setTimestamp(Instant.now().toEpochMilli());
        this.metricDataPoint.setMetric(this.metric);
        return this;
    }

    @Override
    public void emit() {
        TelemetryLoggerMessage message = new TelemetryLoggerMessage(this.metricDataPoint);
        logger.logMetrics(message);
    }





}
