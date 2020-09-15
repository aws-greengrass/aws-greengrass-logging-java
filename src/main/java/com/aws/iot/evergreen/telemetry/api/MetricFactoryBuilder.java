/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.telemetry.api;

import com.aws.iot.evergreen.telemetry.impl.Metric;

/**
 * Evergreen MetricsFactory interface for generating metrics.
 */
public interface MetricFactoryBuilder {

    /**
     * Entry point for fluent APIs to emit metrics.
     *
     * @return
     */
    void putMetricData(Metric metric, Object value);
}
