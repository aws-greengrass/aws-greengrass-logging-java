/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.greengrass.logging.api;

/**
 * A fluent API to create log events.
 */
public interface LogEventBuilder {

    /**
     * An instance of NOOP LogEventBuilder.
     */
    LogEventBuilder NOOP = new LogEventBuilder() {
    };

    /**
     * Set the throwable in LogEventBuilder.
     *
     * @param cause the throwable
     * @return the instance of LogEventBuilder
     */
    default LogEventBuilder cause(Throwable cause) {
        return this;
    }

    /**
     * Set the throwable in LogEventBuilder.
     *
     * @param cause the throwable
     * @return the instance of LogEventBuilder
     */
    default LogEventBuilder setCause(Throwable cause) {
        return this;
    }

    /**
     * Set the log event type in LogEventBuilder.
     *
     * @param type log event type
     * @return the instance of LogEventBuilder
     */
    default LogEventBuilder event(String type) {
        return this;
    }

    /**
     * Set the log event type in LogEventBuilder.
     *
     * @param type log event type
     * @return the instance of LogEventBuilder
     */
    default LogEventBuilder setEventType(String type) {
        return this;
    }

    /**
     * Add a key-value pair of contextual information in LogEventBuilder.
     *
     * @param key   a unique key
     * @param value value of the key
     * @return the instance of LogEventBuilder
     */
    default LogEventBuilder addKeyValue(String key, Object value) {
        return this;
    }

    /**
     * Add a key-value pair of contextual information in LogEventBuilder.
     *
     * @param key   a unique key
     * @param value value of the key
     * @return the instance of LogEventBuilder
     */
    default LogEventBuilder kv(String key, Object value) {
        return this;
    }

    /**
     * Log the event.
     */
    default void log() {
    }

    /**
     * Log the event with a message object.
     *
     * @param arg the message object
     */
    default void log(Object arg) {
    }

    /**
     * Log the event with a message according to the specified format and argument.
     *
     * @param message the format string
     * @param args    the arguments
     */
    default void log(String message, Object... args) {
    }
}
