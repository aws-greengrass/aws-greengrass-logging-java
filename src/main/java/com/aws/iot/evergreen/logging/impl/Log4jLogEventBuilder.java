package com.aws.iot.evergreen.logging.impl;

import com.aws.iot.evergreen.logging.api.LogEventBuilder;
import org.apache.logging.log4j.Level;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Log4jLogEventBuilder implements LogEventBuilder {
    final Level level;
    Throwable cause;
    String eventType = "DEFAULT";
    ConcurrentMap<String, String> eventContextData = new ConcurrentHashMap<>();
    Log4jLoggerAdapter logger;

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
        this.logger.logMessage(this.level, this.eventType, "", this.cause, this.eventContextData);
    }

    @Override
    public void log(Object arg) {
        this.logger.logMessage(this.level, this.eventType, arg.toString(), this.cause, this.eventContextData);
    }

    @Override
    public void log(String message, Object... args) {
        String msg = String.format(message, args);
        this.logger.logMessage(this.level, this.eventType, msg, this.cause, this.eventContextData);
    }
}
