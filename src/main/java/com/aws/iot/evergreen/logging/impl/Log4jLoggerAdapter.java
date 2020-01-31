package com.aws.iot.evergreen.logging.impl;

import com.aws.iot.evergreen.logging.api.LogEventBuilder;
import com.aws.iot.evergreen.logging.api.Logger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.message.ObjectMessage;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Log4jLoggerAdapter implements Logger {
    final transient org.apache.logging.log4j.Logger logger;
    final String name;
    ConcurrentMap<String, String> loggerContextData = new ConcurrentHashMap<>();

    public Log4jLoggerAdapter(org.apache.logging.log4j.Logger logger) {
        this.logger = logger;
        this.name = logger.getName();
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void addDefaultKeyValue(String key, Object value) {
        loggerContextData.put(key, value.toString());
    }

    @Override
    public LogEventBuilder atTrace() {
        if (isTraceEnabled()) {
            return new Log4jLogEventBuilder(this, org.apache.logging.log4j.Level.TRACE, Collections
                    .unmodifiableMap(loggerContextData));
        }
        return LogEventBuilder.NOOP;
    }

    @Override
    public LogEventBuilder atDebug() {
        if (isDebugEnabled()) {
            return new Log4jLogEventBuilder(this, org.apache.logging.log4j.Level.DEBUG, Collections
                    .unmodifiableMap(loggerContextData));
        }
        return LogEventBuilder.NOOP;
    }

    @Override
    public LogEventBuilder atInfo() {
        if (isInfoEnabled()) {
            return new Log4jLogEventBuilder(this, org.apache.logging.log4j.Level.INFO, Collections
                    .unmodifiableMap(loggerContextData));
        }
        return LogEventBuilder.NOOP;
    }

    @Override
    public LogEventBuilder atWarn() {
        if (isWarnEnabled()) {
            return new Log4jLogEventBuilder(this, org.apache.logging.log4j.Level.WARN, Collections
                    .unmodifiableMap(loggerContextData));
        }
        return LogEventBuilder.NOOP;
    }

    @Override
    public LogEventBuilder atError() {
        if (isErrorEnabled()) {
            return new Log4jLogEventBuilder(this, org.apache.logging.log4j.Level.ERROR, Collections
                    .unmodifiableMap(loggerContextData));
        }
        return LogEventBuilder.NOOP;
    }

    @Override
    public LogEventBuilder atFatal() {
        if (isFatalEnabled()) {
            return new Log4jLogEventBuilder(this, org.apache.logging.log4j.Level.FATAL, Collections
                    .unmodifiableMap(loggerContextData));
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
        ObjectMessage message = new ObjectMessage(new LogEvent(name, level, "DEFAULT", String
                .format(msg, args), loggerContextData));
        this.logger.logMessage(level, null, null, null, message, null);
    }

    public void logMessage(Level level, String eventType, String msg, Throwable throwable,
                           Map<String, String> contextData) {
        ObjectMessage message = new ObjectMessage(new LogEvent(name, level, eventType, msg, contextData));
        this.logger.logMessage(level, null, null, null, message, throwable);
    }

}
