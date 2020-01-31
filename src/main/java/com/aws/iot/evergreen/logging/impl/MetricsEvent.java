package com.aws.iot.evergreen.logging.impl;

import java.util.List;
import java.util.Map;

public class MetricsEvent extends MonitoringEvent {
    private static final long serialVersionUID = 0L;

    public String namespace;
    public List<Metric> metrics;
    public Map<String, String> dimensions;

    public MetricsEvent(String loggerName) {
        super(loggerName);
    }
}
