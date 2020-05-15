/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.logging.api;

/**
 * Evergreen Logger interface.
 */
public interface Logger {
    /**
     * Return the name of the Logger instance.
     *
     * @return name of the logger instance
     */
    String getName();

    /**
     * Add a key-value pair of common contextual information related to the logger.
     *
     * @param key   a unique key
     * @param value value of the key
     * @return the logger instance
     */
    Logger addDefaultKeyValue(String key, Object value);

    /**
     * Add a key-value pair of common contextual information related to the logger.
     *
     * @param key   a unique key
     * @param value value of the key
     * @return the logger instance
     */
    Logger dfltKv(String key, Object value);

    /**
     * Entry point for fluent logging in TRACE level.
     *
     * @return LogEventBuilder instance for level TRACE
     */
    LogEventBuilder atTrace();

    /**
     * Entry point for fluent logging in TRACE level.
     *
     * @param eventType Type of the event that generated the log
     *
     * @return LogEventBuilder instance for level TRACE
     */
    LogEventBuilder atTrace(final String eventType);

    /**
     * Entry point for fluent logging in TRACE level.
     *
     * @param eventType Type of the event that generated the log
     * @param cause Throwable object for cause if available
     *
     * @return LogEventBuilder instance for level TRACE
     */
    LogEventBuilder atTrace(final String eventType, final Throwable cause);

    /**
     * Entry point for fluent logging in DEBUG level.
     *
     * @return LogEventBuilder instance for level DEBUG
     */
    LogEventBuilder atDebug();

    /**
     * Entry point for fluent logging in DEBUG level.
     *
     * @param eventType Type of the event that generated the log
     *
     * @return LogEventBuilder instance for level DEBUG
     */
    LogEventBuilder atDebug(final String eventType);

    /**
     * Entry point for fluent logging in DEBUG level.
     *
     * @param eventType Type of the event that generated the log
     * @param cause Throwable object for cause if available
     *
     * @return LogEventBuilder instance for level DEBUG
     */
    LogEventBuilder atDebug(final String eventType, final Throwable cause);

    /**
     * Entry point for fluent logging in INFO level.
     *
     * @return LogEventBuilder instance for level INFO
     */
    LogEventBuilder atInfo();

    /**
     * Entry point for fluent logging in INFO level.
     *
     * @param eventType Type of the event that generated the log
     *
     * @return LogEventBuilder instance for level INFO
     */
    LogEventBuilder atInfo(final String eventType);

    /**
     * Entry point for fluent logging in INFO level.
     *
     * @param eventType Type of the event that generated the log
     * @param cause Throwable object for cause if available
     *
     * @return LogEventBuilder instance for level INFO
     */
    LogEventBuilder atInfo(final String eventType, final Throwable cause);

    /**
     * Entry point for fluent logging in WARN level.
     *
     * @return LogEventBuilder instance for level WARN
     */
    LogEventBuilder atWarn();

    /**
     * Entry point for fluent logging in WARN level.
     *
     * @param eventType Type of the event that generated the log
     *
     * @return LogEventBuilder instance for level WARN
     */
    LogEventBuilder atWarn(final String eventType);

    /**
     * Entry point for fluent logging in WARN level.
     *
     * @param eventType Type of the event that generated the log
     * @param cause Throwable object for cause if available
     *
     * @return LogEventBuilder instance for level WARN
     */
    LogEventBuilder atWarn(final String eventType, final Throwable cause);

    /**
     * Entry point for fluent logging in ERROR level.
     *
     * @return LogEventBuilder instance for level ERROR
     */
    LogEventBuilder atError();

    /**
     * Entry point for fluent logging in ERROR level.
     *
     * @param eventType Type of the event that generated the log
     *
     * @return LogEventBuilder instance for level ERROR
     */
    LogEventBuilder atError(final String eventType);

    /**
     * Entry point for fluent logging in ERROR level.
     *
     * @param eventType Type of the event that generated the log
     * @param cause Throwable object for cause if available
     *
     * @return LogEventBuilder instance for level ERROR
     */
    LogEventBuilder atError(final String eventType, final Throwable cause);

    /**
     * Check if TRACE level is enabled for the logger.
     *
     * @return True if the TRACE is enabled, false otherwise.
     */
    boolean isTraceEnabled();

    /**
     * Check if DEBUG level is enabled for the logger.
     *
     * @return True if the DEBUG is enabled, false otherwise.
     */
    boolean isDebugEnabled();

    /**
     * Check if INFO level is enabled for the logger.
     *
     * @return True if the INFO is enabled, false otherwise.
     */
    boolean isInfoEnabled();

    /**
     * Check if WARN level is enabled for the logger.
     *
     * @return True if the WARN is enabled, false otherwise.
     */
    boolean isWarnEnabled();

    /**
     * Check if ERROR level is enabled for the logger.
     *
     * @return True if the ERROR is enabled, false otherwise.
     */
    boolean isErrorEnabled();

    /**
     * Log a message at the TRACE level according to the specified format
     * and argument.
     *
     * <p>This form avoids superfluous object creation when the logger
     * is disabled for the TRACE level.
     *
     * @param message the format string
     * @param args    the arguments
     */
    void trace(String message, Object... args);

    /**
     * Log a message at the DEBUG level according to the specified format
     * and argument.
     *
     * <p>This form avoids superfluous object creation when the logger
     * is disabled for the DEBUG level.
     *
     * @param message the format string
     * @param args    the arguments
     */
    void debug(String message, Object... args);

    /**
     * Log a message at the INFO level according to the specified format
     * and argument.
     *
     * <p>This form avoids superfluous object creation when the logger
     * is disabled for the INFO level.
     *
     * @param message the format string
     * @param args    the arguments
     */
    void info(String message, Object... args);

    /**
     * Log a message at the WARN level according to the specified format
     * and argument.
     *
     * <p>This form avoids superfluous object creation when the logger
     * is disabled for the WARN level.
     *
     * @param message the format string
     * @param args    the arguments
     */
    void warn(String message, Object... args);

    /**
     * Log a message at the ERROR level according to the specified format
     * and argument.
     *
     * <p>This form avoids superfluous object creation when the logger
     * is disabled for the ERROR level.
     *
     * @param message the format string
     * @param args    the arguments
     */
    void error(String message, Object... args);

    // TODO: Add configuration interface and dynamic reload https://sim.amazon.com/issues/P31935972
    // void reloadConfig(topics config);
}
