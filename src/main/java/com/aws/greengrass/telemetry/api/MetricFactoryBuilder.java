/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.greengrass.telemetry.api;

import com.aws.greengrass.telemetry.impl.Metric;

/**
 * Greengrass MetricsFactory interface for generating metrics.
 */
public interface MetricFactoryBuilder {

    /**
     * Entry point for fluent APIs to emit metrics.
     */
    void putMetricData(Metric metric, Object value);
}
