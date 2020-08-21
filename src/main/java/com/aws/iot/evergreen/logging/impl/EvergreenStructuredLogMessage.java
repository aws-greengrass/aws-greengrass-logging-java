/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.logging.impl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.slf4j.event.Level;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
@NoArgsConstructor
public class EvergreenStructuredLogMessage {
    private String thread;
    private String level;
    private String eventType;
    private String message;
    private Map<String, String> contexts;
    private String loggerName;
    private long timestamp;
    @EqualsAndHashCode.Exclude
    private Throwable cause;

    @JsonIgnore
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Constructor for structured log message.
     *
     * @param loggerName the name of the logger which the message will be appended to
     * @param level      the log {@link Level} of this log message
     * @param eventType  the event type defined by each service or component
     * @param msg        the text message
     * @param context    a map of key value pairs with the contextual information related to the log message
     * @param cause      the {@link Throwable} related to the log message
     */
    public EvergreenStructuredLogMessage(String loggerName, Level level, String eventType, String msg,
                                         Map<String, String> context, Throwable cause) {
        this.level = level.toString();
        this.message = msg;
        this.contexts = context;
        this.eventType = eventType;
        this.loggerName = loggerName;
        this.timestamp = Instant.now().toEpochMilli();
        this.cause = cause;
        this.thread = Thread.currentThread().getName();
    }

    /**
     * Get basic formatted message containing only the message and context.
     *
     * @return String
     */
    @JsonIgnore
    private String getFormattedMessage() {
        return Stream.of(eventType, message, contexts).filter(Objects::nonNull).map(Object::toString)
                .filter((x) -> !x.isEmpty()).collect(Collectors.joining(". "));
    }

    /**
     * Get fully formatted message including all fields.
     *
     * @return String
     */
    @JsonIgnore
    @SuppressWarnings("checkstyle:emptycatchblock")
    public String getTextMessage() {
        // Create a new SDF every time because SDF isn't threadsafe
        StringBuilder msg = new StringBuilder(new SimpleDateFormat("yyyy MMM dd hh:mm:ss,SSSZ")
                                              .format(new Date(timestamp)));
        // Equivalent to String.format("%s [%s] (%s) %s: %s", SDF, level, thread, loggerName, formattedMessage)
        msg.append(" [").append(level).append("] (")
                .append(thread).append(") ")
                .append(loggerName).append(": ")
                .append(getFormattedMessage());

        if (cause == null) {
            return msg.toString();
        }

        try (StringWriter sw = new StringWriter(); PrintWriter pw = new PrintWriter(sw)) {
            cause.printStackTrace(pw);
            msg.append(System.lineSeparator()).append(sw.toString());
            return msg.toString();
        } catch (IOException ignore) {
            // Not possible
        }
        return msg.toString();
    }

    /**
     * Get the whole message encoded as JSON.
     *
     * @return String
     */
    @JsonIgnore
    public String getJSONMessage() {
        try {
            return OBJECT_MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }
}
