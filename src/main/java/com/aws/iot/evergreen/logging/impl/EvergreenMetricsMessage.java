/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.aws.iot.evergreen.logging.impl;

import com.aws.iot.evergreen.logging.impl.plugins.layouts.StructuredLayout;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.logging.log4j.message.Message;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An implementation of {@link Message} interface to work with Evergreen {@link StructuredLayout}.
 */
@Data
public class EvergreenMetricsMessage implements Message {
    private static final long serialVersionUID = 0L;

    private final String namespace;
    private final List<Metric> metrics = new LinkedList<>();
    private final Map<String, String> dimensions = new HashMap<>();
    @EqualsAndHashCode.Exclude
    private final Long timestamp;

    /**
     * Constructor for Metrics Message.
     *
     * @param namespace  the namespace of the metrics
     * @param metrics    list of {@link Metric}
     * @param dimensions a map of metrics dimensions
     */
    public EvergreenMetricsMessage(String namespace, List<Metric> metrics,
                                   Map<String, String> dimensions) {
        this.timestamp = Instant.now().toEpochMilli();
        this.dimensions.putAll(dimensions);
        this.metrics.addAll(metrics);
        this.namespace = String.valueOf(namespace);
    }

    @JsonIgnore
    @Override
    public String getFormattedMessage() {
        return Stream.of(namespace, metrics, dimensions)
                .filter(Objects::nonNull)
                .map(Object::toString)
                .filter((x) -> !x.isEmpty())
                .collect(Collectors.joining(" "));
    }

    @JsonIgnore
    @Override
    public String getFormat() {
        return null;
    }

    @JsonIgnore
    @Override
    public Object[] getParameters() {
        return new Object[0];
    }

    @JsonIgnore
    @Override
    public Throwable getThrowable() {
        return null;
    }
}
