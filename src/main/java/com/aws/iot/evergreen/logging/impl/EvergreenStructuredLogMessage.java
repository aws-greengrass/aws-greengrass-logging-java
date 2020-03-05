/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.logging.impl;

import com.aws.iot.evergreen.logging.impl.plugins.layouts.StructuredLayout;
import com.fasterxml.jackson.annotation.JsonIgnore;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.message.Message;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An implementation of {@link Message} interface to work with Evergreen {@link StructuredLayout}.
 */
@Data
@NoArgsConstructor
public class EvergreenStructuredLogMessage implements Message {
    private static final long serialVersionUID = 0L;

    private String level;
    private String eventType;
    private String message;
    private Map<String, String> contexts;
    private String loggerName;
    private long timestamp;
    @EqualsAndHashCode.Exclude
    private Throwable cause;

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

    @JsonIgnore
    @Override
    public String getFormattedMessage() {
        return Stream.of(eventType, message, contexts)
                .filter(Objects::nonNull)
                .map(Object::toString)
                .filter((x) -> !x.isEmpty())
                .collect(Collectors.joining(". "));
    }

    @JsonIgnore
    @Override
    public String getFormat() {
        return null;
    }

    @JsonIgnore
    @SuppressFBWarnings(value = "PZLA_PREFER_ZERO_LENGTH_ARRAYS",
            justification = "change the serialization format by design and skip parameter values")
    @Override
    public Object[] getParameters() {
        return null;
    }

    @JsonIgnore
    @Override
    public Throwable getThrowable() {
        return this.cause;
    }
}
