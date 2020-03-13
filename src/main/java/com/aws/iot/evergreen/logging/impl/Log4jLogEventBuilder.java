/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.logging.impl;

import com.aws.iot.evergreen.logging.api.LogEventBuilder;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.message.ParameterizedMessage;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

/**
 * An implementation of {@link LogEventBuilder} providing a fluent API to generate log events.
 */
public class Log4jLogEventBuilder implements LogEventBuilder {
    private final Level level;
    private Throwable cause;
    private String eventType;
    private final Map<String, String> eventContextData = new ConcurrentHashMap<>();
    private transient Log4jLoggerAdapter logger;
    private static final CopyOnWriteArraySet<Consumer<EvergreenStructuredLogMessage>> listeners
            = new CopyOnWriteArraySet<>();

    /**
     * Log Event Builder constructor.
     * @param logger the Greengrass logger
     * @param level the log level setting on the logger
     * @param loggerContextData a map of key value pairs with contextual information for the logger
     */
    public Log4jLogEventBuilder(Log4jLoggerAdapter logger, Level level, Map<String, String> loggerContextData) {
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
        this.eventContextData.put(key, value.toString());
        return this;
    }

    @Override
    public void log() {
        log("");
    }

    @Override
    public void log(Object arg) {
        EvergreenStructuredLogMessage message = new EvergreenStructuredLogMessage(logger
                .getName(), level, eventType, arg.toString(), eventContextData, cause);
        listeners.forEach(l -> l.accept(message));
        logger.logMessage(level, message);
    }

    @Override
    public void log(String fmt, Object... args) {
        // If the cause wasn't set, try setting it from the last vararg if it is a Throwable
        if (cause == null && args.length > 0 && args[args.length - 1] instanceof Throwable) {
            cause = (Throwable) args[args.length - 1];
            args = Arrays.copyOfRange(args, 0, args.length - 1);
        }
        log(new ParameterizedMessage(fmt, args).getFormattedMessage());
    }
    
    public static void addGlobalListener(Consumer<EvergreenStructuredLogMessage> l) {
        listeners.add(l);
    }
    
    public static void removeGlobalListener(Consumer<EvergreenStructuredLogMessage> l) {
        listeners.remove(l);
        
    }
}
