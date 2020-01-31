/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.aws.iot.evergreen.logging.api;

public interface LogEventBuilder {

    LogEventBuilder NOOP = new LogEventBuilder() {
    };

    default LogEventBuilder setCause(Throwable cause) {
        return this;
    }

    default LogEventBuilder setEventType(String type) {
        return this;
    }

    default LogEventBuilder addKeyValue(String key, Object value) {
        return this;
    }

    default void log() {
    }

    default void log(Object arg) {
    }

    // For string templating
    default void log(String message, Object... args) {
    }
}
