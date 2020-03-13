/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.logging.impl;

import com.aws.iot.evergreen.logging.api.LogEventBuilder;
import com.aws.iot.evergreen.logging.api.Logger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.message.Message;

import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A wrapper over {@link org.apache.logging.log4j.Logger} in conforming to the
 * {@link com.aws.iot.evergreen.logging.api.Logger} interface.
 */
public class Log4jLoggerAdapter implements Logger {
    private transient org.apache.logging.log4j.Logger logger;
    private final String name;
    private final ConcurrentMap<String, String> loggerContextData = new ConcurrentHashMap<>();

    /**
     * Create a {@link Logger} instance based on the given {@link org.apache.logging.log4j.Logger} instance.
     *
     * @param logger a {@link org.apache.logging.log4j.Logger} instance
     */
    public Log4jLoggerAdapter(org.apache.logging.log4j.Logger logger) {
        this.logger = logger;
        this.name = logger.getName();
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Logger addDefaultKeyValue(String key, Object value) {
        loggerContextData.put(key, value.toString());
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

    @Override
    public LogEventBuilder atFatal() {
        return atLevel(Level.FATAL, null, null);
    }

    @Override
    public LogEventBuilder atFatal(final String eventType) {
        return atLevel(Level.FATAL, eventType, null);
    }

    @Override
    public LogEventBuilder atFatal(final String eventType, final Throwable cause) {
        return atLevel(Level.FATAL, eventType, cause);
    }

    private LogEventBuilder atLevel(final Level logLevel,
                                    final String eventType,
                                    final Throwable cause) {
        if (isLogLevelEnabled(logLevel)) {
            return new Log4jLogEventBuilder(this, logLevel, Collections.unmodifiableMap(loggerContextData))
                    .setCause(cause)
                    .setEventType(eventType);
        }
        return LogEventBuilder.NOOP;
    }

    @Override
    public boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }

    @Override
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    @Override
    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    @Override
    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

    @Override
    public boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }

    @Override
    public boolean isFatalEnabled() {
        return logger.isFatalEnabled();
    }

    private boolean isLogLevelEnabled(final Level logLevel) {
        // Level is a class with static objects, not enum
        if (logLevel == Level.TRACE) {
            return logger.isTraceEnabled();
        } else if (logLevel == Level.DEBUG) {
            return logger.isDebugEnabled();
        } else if (logLevel == Level.INFO) {
            return logger.isInfoEnabled();
        } else if (logLevel == Level.WARN) {
            return logger.isWarnEnabled();
        } else if (logLevel == Level.ERROR) {
            return logger.isErrorEnabled();
        } else if (logLevel == Level.FATAL) {
            return logger.isFatalEnabled();
        }
        return false;
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
    public void fatal(String message, Object... args) {
        if (isFatalEnabled()) {
            this.log(Level.FATAL, message, args);
        }
    }

    private void log(Level level, String msg, Object... args) {
        new Log4jLogEventBuilder(this, level, Collections.unmodifiableMap(loggerContextData)).log(msg, args);
    }

    /**
     * Log a {@link Message} at the given log level.
     *
     * @param level   the log level
     * @param message the {@link Message} to be logged
     */
    public void logMessage(Level level, Message message) {
        this.logger.logMessage(level, null, null, null, message, message.getThrowable());
    }

    public org.apache.logging.log4j.Logger getLogger() {
        return this.logger;
    }

    public void setLogger(org.apache.logging.log4j.Logger logger) {
        this.logger = logger;
    }

}
