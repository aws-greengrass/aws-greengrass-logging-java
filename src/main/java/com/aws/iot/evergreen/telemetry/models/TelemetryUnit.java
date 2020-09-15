/*
 * Copyright Amazon.com Inc. or its affiliates.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.telemetry.models;

/**
 * Units available - CW MetricDatum reference
 * https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_MetricDatum.html
 *
 */
public enum TelemetryUnit {
    Bytes,
    BytesPerSecond,
    Count,
    CountPerSecond,
    Megabytes,
    None,
    Percent,
    Seconds
}
