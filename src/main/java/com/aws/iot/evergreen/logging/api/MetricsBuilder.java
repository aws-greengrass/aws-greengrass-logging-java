/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.aws.iot.evergreen.logging.api;

import javax.measure.unit.Unit;

public interface MetricsBuilder {
    MetricsBuilder setNamespace(String namespace);

    MetricsBuilder addDimension(String key, Object value);

    MetricsBuilder addMetric(String name, Object value, Unit<?> unit);

    void flush();
}