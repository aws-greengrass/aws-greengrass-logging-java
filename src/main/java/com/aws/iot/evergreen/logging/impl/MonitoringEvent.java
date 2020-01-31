package com.aws.iot.evergreen.logging.impl;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.time.Instant;

public abstract class MonitoringEvent implements Serializable {
    @JsonProperty("LN")
    public String loggerName;
    @JsonProperty("TS")
    public Instant timestamp;

    public MonitoringEvent(String loggerName) {
        this.loggerName = loggerName;
        this.timestamp = Instant.now();
    }
}
