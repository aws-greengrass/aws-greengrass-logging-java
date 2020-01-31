package com.aws.iot.evergreen.logging.impl;

import java.io.Serializable;
import javax.measure.unit.Unit;

public class Metric implements Serializable {
    private static final long serialVersionUID = 0L;

    String metricName;
    Object metricValue;
    Unit<?> unit = Unit.ONE;

    public Metric(String metricName, Object metricValue, Unit<?> unit) {
        this.metricName = metricName;
        this.metricValue = metricValue;
        this.unit = unit;
    }

    public Metric(String metricName, Object metricValue) {
        this.metricName = metricName;
        this.metricValue = metricValue;
    }
}
