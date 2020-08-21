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
     * @return MetricsBuilder instance
     */
    MetricDataBuilder addMetric(Metric m);
}
