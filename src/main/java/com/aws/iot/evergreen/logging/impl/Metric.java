/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.aws.iot.evergreen.logging.impl;

import java.io.Serializable;
import javax.measure.unit.Unit;

public class Metric implements Serializable {
    private static final long serialVersionUID = 0L;

    String metricName;
    Object metricValue;
    Unit<?> unit = Unit.ONE;

    /**
     * Metric constructor.
     * @param metricName the name of the metric
     * @param metricValue the value of the metric
     * @param unit the unit of the metric value
     */
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
