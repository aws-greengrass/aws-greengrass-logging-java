/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.aws.iot.evergreen.logging.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.logging.log4j.Level;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

public class LogEvent implements Serializable {
    private static final long serialVersionUID = 0L;

    @JsonProperty("LVL")
    final Level level;
    @JsonProperty("T")
    final String eventType;
    @JsonProperty("MSG")
    final String message;
    @JsonProperty("D")
    final Map<String, String> contexts;

    @JsonProperty("LN")
    public String loggerName;
    @JsonProperty("TS")
    public Instant timestamp;

    /**
     * Log Event constructor.
     * @param loggerName the name of the logger which the event will be appended to
     * @param level the log level of this log event
     * @param eventType the event type defined by each service or component
     * @param msg the text message
     * @param context a map of key value pairs with the contextual information related to the log event
     */
    public LogEvent(String loggerName, Level level, String eventType, String msg, Map<String,
            String> context) {
        this.level = level;
        this.message = msg;
        this.contexts = context;
        this.eventType = eventType;
        this.loggerName = loggerName;
        this.timestamp = Instant.now();
    }
}
