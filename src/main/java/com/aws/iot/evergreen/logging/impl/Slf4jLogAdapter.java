/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.logging.impl;

import com.aws.iot.evergreen.logging.api.LogEventBuilder;
import com.aws.iot.evergreen.logging.api.Logger;
import com.aws.iot.evergreen.logging.impl.config.LogFormat;
import com.aws.iot.evergreen.logging.impl.config.PersistenceConfig;
import org.slf4j.event.Level;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

/**
 * A wrapper over {@link org.slf4j.Logger} in conforming to the {@link com.aws.iot.evergreen.logging.api.Logger}
 * interface.
 */
public class Slf4jLogAdapter implements Logger {
    private static final CopyOnWriteArraySet<Consumer<EvergreenStructuredLogMessage>> listeners =
            new CopyOnWriteArraySet<>();
    private final Slf4jLogAdapter parentLogger;
    private transient org.slf4j.Logger logger;
    private final String name;
    private final Map<String, Object> loggerContextData = new ConcurrentHashMap<>();
    private final PersistenceConfig config;
    private Level individualLevel = null;

    /**
     * Create a {@link Logger} instance based on the given {@link org.slf4j.Logger} instance.
     *
     * @param logger logger implementation
     * @param config configuration for this logger
     */
    public Slf4jLogAdapter(org.slf4j.Logger logger, PersistenceConfig config) {
        this(logger, config, null);
    }

    private Slf4jLogAdapter(org.slf4j.Logger logger, PersistenceConfig config, Slf4jLogAdapter slf4jLogAdapter) {
        this.logger = logger;
        this.name = logger.getName();
        this.config = config;
        this.parentLogger = slf4jLogAdapter;
    }

    public static void addGlobalListener(Consumer<EvergreenStructuredLogMessage> l) {
        listeners.add(l);
    }

    public static void removeGlobalListener(Consumer<EvergreenStructuredLogMessage> l) {
        listeners.remove(l);
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Logger addDefaultKeyValue(String key, Object value) {
        loggerContextData.put(key, value == null ? "null" : value);
        return this;
    }

    @Override
    public Logger dfltKv(String key, Object value) {
        return addDefaultKeyValue(key, value);
    }

    @Override
    public LogEventBuilder atTrace() {
        return atLevel(Level.TRACE, null, null);
    }

    @Override
    public LogEventBuilder atTrace(final String eventType) {
        return atLevel(Level.TRACE, eventType, null);
    }

    @Override
    public LogEventBuilder atTrace(final String eventType, final Throwable cause) {
        return atLevel(Level.TRACE, eventType, cause);
    }

    @Override
    public LogEventBuilder atDebug() {
        return atLevel(Level.DEBUG, null, null);
    }

    @Override
    public LogEventBuilder atDebug(final String eventType) {
        return atLevel(Level.DEBUG, eventType, null);
    }

    @Override
    public LogEventBuilder atDebug(final String eventType, final Throwable cause) {
        return atLevel(Level.DEBUG, eventType, cause);
    }

    @Override
    public LogEventBuilder atInfo() {
        return atLevel(Level.INFO, null, null);
    }

    @Override
    public LogEventBuilder atInfo(final String eventType) {
        return atLevel(Level.INFO, eventType, null);
    }

    @Override
    public LogEventBuilder atInfo(final String eventType, final Throwable cause) {
        return atLevel(Level.INFO, eventType, cause);
    }

    @Override
    public LogEventBuilder atWarn() {
        return atLevel(Level.WARN, null, null);
    }

    @Override
    public LogEventBuilder atWarn(final String eventType) {
        return atLevel(Level.WARN, eventType, null);
    }

    @Override
    public LogEventBuilder atWarn(final String eventType, final Throwable cause) {
        return atLevel(Level.WARN, eventType, cause);
    }

    @Override
    public LogEventBuilder atError() {
        return atLevel(Level.ERROR, null, null);
    }

    @Override
    public LogEventBuilder atError(final String eventType) {
        return atLevel(Level.ERROR, eventType, null);
    }

    @Override
    public LogEventBuilder atError(final String eventType, final Throwable cause) {
        return atLevel(Level.ERROR, eventType, cause);
    }

    private LogEventBuilder atLevel(final Level logLevel, final String eventType, final Throwable cause) {
        if (isLogLevelEnabled(logLevel)) {
            Map<String, Object> context;
            if (parentLogger == null) {
                context = loggerContextData;
            } else {
                context = new HashMap<>(parentLogger.loggerContextData);
                context.putAll(loggerContextData);
            }

            return new EGLogEventBuilder(this, logLevel, Collections.unmodifiableMap(context)).setCause(cause)
                    .setEventType(eventType);
        }
        return LogEventBuilder.NOOP;
    }

    @Override
    public boolean isTraceEnabled() {
        return isLogLevelEnabled(Level.TRACE);
    }

    @Override
    public boolean isDebugEnabled() {
        return isLogLevelEnabled(Level.DEBUG);
    }

    @Override
    public boolean isInfoEnabled() {
        return isLogLevelEnabled(Level.INFO);
    }

    @Override
    public boolean isWarnEnabled() {
        return isLogLevelEnabled(Level.WARN);
    }

    @Override
    public boolean isErrorEnabled() {
        return isLogLevelEnabled(Level.ERROR);
    }

    private boolean isLogLevelEnabled(final Level logLevel) {
        Level runningLevel = config.getLevel();
        if (individualLevel != null) {
            runningLevel = individualLevel;
        } else if (parentLogger != null && parentLogger.individualLevel != null) {
            runningLevel = parentLogger.individualLevel;
        }
        return runningLevel.toInt() <= logLevel.toInt();
    }

    @Override
    public void trace(String message, Object... args) {
        if (isTraceEnabled()) {
            this.log(Level.TRACE, message, args);
        }
    }

    @Override
    public void debug(String message, Object... args) {
        if (isDebugEnabled()) {
            this.log(Level.DEBUG, message, args);
        }
    }

    @Override
    public void info(String message, Object... args) {
        if (isInfoEnabled()) {
            this.log(Level.INFO, message, args);
        }
    }

    @Override
    public void warn(String message, Object... args) {
        if (isWarnEnabled()) {
            this.log(Level.WARN, message, args);
        }
    }

    @Override
    public void error(String message, Object... args) {
        if (isErrorEnabled()) {
            this.log(Level.ERROR, message, args);
        }
    }

    @Override
    public void setLevel(String level) {
        if (level == null || level.isEmpty()) {
            this.individualLevel = null;
        } else {
            this.individualLevel = Level.valueOf(level.toUpperCase());
        }
    }

    @Override
    public Logger createChild() {
        return new Slf4jLogAdapter(this.logger, config, this);
    }

    private void log(Level level, String msg, Object... args) {
        new EGLogEventBuilder(this, level, Collections.unmodifiableMap(loggerContextData)).log(msg, args);
    }

    private String serialize(EvergreenStructuredLogMessage message) {
        if (LogFormat.TEXT.equals(config.getFormat())) {
            return message.getTextMessage();
        } else if (LogFormat.JSON.equals(config.getFormat())) {
            return message.getJSONMessage();
        }
        return "ERROR Unknown LogFormat " + config.getFormat();
    }

    /**
     * Log a String at the given log level.
     *
     * @param m the message to be logged
     */
    void logMessage(EvergreenStructuredLogMessage m) {
        listeners.forEach(l -> l.accept(m));
        String message = serialize(m);
        switch (Level.valueOf(m.getLevel())) {
            case ERROR:
                logger.error(message);
                break;
            case WARN:
                logger.warn(message);
                break;
            case INFO:
                logger.info(message);
                break;
            case DEBUG:
                logger.debug(message);
                break;
            case TRACE:
            default:
                logger.trace(message);
                break;
        }
    }

    org.slf4j.Logger getLogger() {
        return this.logger;
    }

    void setLogger(org.slf4j.Logger logger) {
        this.logger = logger;
    }
}
