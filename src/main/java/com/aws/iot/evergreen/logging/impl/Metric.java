/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.logging.impl;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.Quantity;
import javax.measure.unit.Unit;

/**
 * Metric is a data class encapsulating the name, value and unit of a metric.
 */
@Data
public class Metric<T extends Quantity> implements Serializable {
    private static final long serialVersionUID = 0L;

    private final String name;
    private final Object value;
    @JsonSerialize(using = ToStringSerializer.class)
    private final Unit<T> unit;

    /**
     * Metric constructor.
     *
     * @param name the name of the metric
     * @param value the value of the metric
     * @param unit the unit of the metric value
     */
    public Metric(String name, Object value, Unit<T> unit) {
        this.name = name;
        this.value = value;
        this.unit = unit;
    }

    /**
     * Get a Metric object where unit is not applicable.
     *
     * @param name the name of the metric
     * @param value the value of the metric
     * @return a Metric object
     */
    public static Metric<Dimensionless> of(String name, Object value) {
        return new Metric<>(name, value, Unit.ONE);
    }
}
