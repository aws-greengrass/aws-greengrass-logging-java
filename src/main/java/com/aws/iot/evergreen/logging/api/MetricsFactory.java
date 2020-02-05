/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.aws.iot.evergreen.logging.api;

public interface MetricsFactory {
    String getName();

    void addDefaultDimension(String key, Object value);

    MetricsBuilder newMetrics();
}
