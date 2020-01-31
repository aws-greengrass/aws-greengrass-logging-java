/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.aws.iot.evergreen.logging.api;

public interface Logger {
    String getName();

    Logger addDefaultKeyValue(String key, Object value);

    LogEventBuilder atTrace();

    LogEventBuilder atDebug();

    LogEventBuilder atInfo();

    LogEventBuilder atWarn();

    LogEventBuilder atError();

    LogEventBuilder atFatal();

    boolean isTraceEnabled();

    boolean isDebugEnabled();

    boolean isInfoEnabled();

    boolean isWarnEnabled();

    boolean isErrorEnabled();

    boolean isFatalEnabled();

    // For string templating
    void trace(String message, Object... args);

    void debug(String message, Object... args);

    void info(String message, Object... args);

    void warn(String message, Object... args);

    void error(String message, Object... args);

    void fatal(String message, Object... args);

    // TODO: Add configuration interface and dynamic reload https://sim.amazon.com/issues/P31935972
    // void reloadConfig(topics config);
}
