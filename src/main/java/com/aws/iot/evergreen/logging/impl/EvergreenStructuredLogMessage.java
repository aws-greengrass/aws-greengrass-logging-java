/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.aws.iot.evergreen.logging.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Data;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.message.Message;

import java.time.Instant;
import java.util.Map;

/**
 * An implementation of {@link Message} interface to work with Evergreen {@link CborLayout}.
 */
@Data
public class EvergreenStructuredLogMessage implements Message {
    private static final long serialVersionUID = 0L;

    final String level;

    final String eventType;

    final String message;

    final Map<String, String> contexts;

    final String loggerName;

    final Long timestamp;

    final Throwable cause;

    /**
     * Constructor for structured log {@link Message}.
     *
     * @param loggerName the name of the logger which the message will be appended to
     * @param level      the log {@link Level} of this log message
     * @param eventType  the event type defined by each service or component
     * @param msg        the text message
     * @param context    a map of key value pairs with the contextual information related to the log message
     * @param cause      the {@link Throwable} related to the log message
     */
    public EvergreenStructuredLogMessage(
            String loggerName, Level level, String eventType, String msg,
            Map<String, String> context, Throwable cause) {
        this.level = level.toString();
        this.message = msg;
        this.contexts = context;
        this.eventType = eventType;
        this.loggerName = loggerName;
        this.timestamp = Instant.now().toEpochMilli();
        this.cause = cause;
    }

    @Override
    public String getFormattedMessage() {
        return null;
    }

    @Override
    public String getFormat() {
        return null;
    }

    @SuppressFBWarnings(value = "PZLA_PREFER_ZERO_LENGTH_ARRAYS",
            justification = "change the serialization format by design and skip parameter values")
    @Override
    public Object[] getParameters() {
        return null;
    }

    @Override
    public Throwable getThrowable() {
        return null;
    }
}
