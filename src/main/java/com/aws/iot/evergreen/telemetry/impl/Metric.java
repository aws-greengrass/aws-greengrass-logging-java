package com.aws.iot.evergreen.telemetry.impl;

import com.aws.iot.evergreen.telemetry.models.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.util.Map;


@Builder
@Getter
@Setter

public class Metric {

    @NonNull
    private TelemetryNamespace metricNamespace;
    @NonNull
    private TelemetryMetricName metricName;
    @NonNull
    private  TelemetryUnit metricUnit;
    @NonNull
    private  TelemetryType metricType;
    @NonNull
    private TelemetryAggregation metricAggregation;

    private Map<String, String> metricDimensions;
}
