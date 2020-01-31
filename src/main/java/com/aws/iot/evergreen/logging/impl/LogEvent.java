package com.aws.iot.evergreen.logging.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.logging.log4j.Level;

import java.util.Map;

public class LogEvent extends MonitoringEvent {
    private static final long serialVersionUID = 0L;
    @JsonProperty("LVL")
    final Level level;
    @JsonProperty("T")
    final String eventType;
    @JsonProperty("MSG")
    final String message;
    @JsonProperty("D")
    final Map<String, String> contexts;

    public LogEvent(String loggerName, Level level, String eventType, String msg, Map<String, String> context) {
        super(loggerName);
        this.level = level;
        this.eventType = eventType;
        this.message = msg;
        this.contexts = context;
    }
}
