/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.aws.iot.evergreen.logging.impl;

import com.aws.iot.evergreen.logging.api.LogEventBuilder;
import org.apache.logging.log4j.Level;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * An implementation of {@link LogEventBuilder} providing a fluent API to generate log events.
 */
public class Log4jLogEventBuilder implements LogEventBuilder {
    final Level level;
    Throwable cause;
    String eventType;
    ConcurrentMap<String, String> eventContextData = new ConcurrentHashMap<>();
    Log4jLoggerAdapter logger;

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
    public LogEventBuilder setCause(Throwable cause) {
        this.cause = cause;
        return this;
    }

    @Override
    public LogEventBuilder setEventType(String type) {
        this.eventType = type;
        return this;
    }

    @Override
    public LogEventBuilder addKeyValue(String key, Object value) {
        this.eventContextData.put(key, value.toString());
        return this;
    }

    @Override
    public void log() {
        EvergreenStructuredLogMessage message = new EvergreenStructuredLogMessage(this.logger
                .getName(), this.level, this.eventType, "", this.eventContextData, this.cause);
        this.logger.logMessage(this.level, message);
    }

    @Override
    public void log(Object arg) {
        EvergreenStructuredLogMessage message = new EvergreenStructuredLogMessage(this.logger
                .getName(), this.level, this.eventType, arg.toString(), this.eventContextData, this.cause);
        this.logger.logMessage(this.level, message);
    }

    @Override
    public void log(String fmt, Object... args) {
        EvergreenStructuredLogMessage message = new EvergreenStructuredLogMessage(this.logger
                .getName(), this.level, this.eventType, String.format(fmt, args), this.eventContextData, this.cause);
        this.logger.logMessage(this.level, message);
    }
}
