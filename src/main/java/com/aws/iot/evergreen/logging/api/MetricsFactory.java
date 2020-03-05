/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.logging.api;

/**
 * Evergreen MetricsFactory interface for generating metrics.
 */
public interface MetricsFactory {
    /**
     * Add a metric dimension (of a key-value pair) as default, so that all metrics which are generated with this
     * MetricsFactory will inherit the dimension.
     *
     * @param key   dimension name
     * @param value dimension value
     * @return
     */
    MetricsFactory addDefaultDimension(String key, Object value);

    /**
     * Entry point for fluent APIs to emit metrics.
     *
     * @return MetricsBuilder instance
     */
    MetricsBuilder newMetrics();
}
