package com.aws.iot.evergreen.telemetry.impl;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MetricDataPoint {
    private Metric metric;
    private Object value;
    private Long timestamp;
}
