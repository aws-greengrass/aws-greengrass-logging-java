/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.greengrass.logging.impl;

import com.aws.greengrass.logging.api.LogEventBuilder;
import org.slf4j.event.Level;
import org.slf4j.helpers.MessageFormatter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * An implementation of {@link LogEventBuilder} providing a fluent API to generate log events.
 */
public class LogEventBuilderImpl implements LogEventBuilder {
    private final Level level;
    private Throwable cause;
    private String eventType;
    private final Map<String, Object> eventContextData = new ConcurrentHashMap<>();
    private final transient Slf4jLogAdapter logger;

    /**
     * Log Event Builder constructor.
     *
     * @param logger            the Greengrass logger
     * @param level             the log level setting on the logger
     * @param loggerContextData a map of key value pairs with contextual information for the logger
     */
    public LogEventBuilderImpl(Slf4jLogAdapter logger, Level level, Map<String, Object> loggerContextData) {
        this.logger = logger;
        this.level = level;
        eventContextData.putAll(loggerContextData);
    }

    @Override
    public LogEventBuilder cause(Throwable cause) {
        return setCause(cause);
    }

    @Override
    public LogEventBuilder setCause(Throwable cause) {
        this.cause = cause;
        return this;
    }

    @Override
    public LogEventBuilder event(String type) {
        return setEventType(type);
    }

    @Override
    public LogEventBuilder setEventType(String type) {
        this.eventType = type;
        return this;
    }

    @Override
    public LogEventBuilder kv(String key, Object value) {
        return addKeyValue(key, value);
    }

    @Override
    public LogEventBuilder addKeyValue(String key, Object value) {
        this.eventContextData.put(key, value == null ? "null" : value);
        return this;
    }

    @Override
    public void log() {
        log("");
    }

    @Override
    public void log(Object arg) {
        // Convert context to string, then log it out
        Map<String, String> contextMap = new HashMap<>();
        eventContextData.forEach((k, v) -> contextMap.put(k, convertToString(v)));

        GreengrassLogMessage message =
                new GreengrassLogMessage(logger.getName(), level, eventType, convertToString(arg), contextMap,
                        cause);
        logger.logMessage(message);
    }

    @Override
    public void log(String fmt, Object... args) {
        // If the cause wasn't set, try setting it from the last vararg if it is a Throwable
        if (cause == null && args.length > 0 && args[args.length - 1] instanceof Throwable) {
            cause = (Throwable) args[args.length - 1];
            args = Arrays.copyOfRange(args, 0, args.length - 1);
        }
        log(MessageFormatter.arrayFormat(fmt, args, null).getMessage());
    }

    private static String convertToString(Object o) {
        // If it is a function which we can call to get a result, then call it and use the output of the function
        if (o instanceof Supplier) {
            return convertToString(((Supplier) o).get());
        }
        return Objects.toString(o);
    }
}
