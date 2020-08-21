package com.aws.iot.evergreen.telemetry.models;

/**
 * Units available - CW MetricDatum reference
 * https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_MetricDatum.html
 *
 */
public enum TelemetryUnit {
    BYTES,
    BYTES_PER_SECOND,
    COUNT,
    COUNT_PER_SECOND,
    MEGABYTES,
    NONE,
    PERCENT,
    SECONDS
}
