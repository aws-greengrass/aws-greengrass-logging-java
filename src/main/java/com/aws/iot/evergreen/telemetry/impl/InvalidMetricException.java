package com.aws.iot.evergreen.telemetry.impl;

public class InvalidMetricException extends Exception {
    public InvalidMetricException(String message) {
        super(message);
    }
}
