/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.aws.iot.evergreen.logging.impl;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.message.ObjectMessage;

import java.util.Map;

public class EvergreenStructuredLogMessage extends ObjectMessage {
    private static final long serialVersionUID = 0L;

    public EvergreenStructuredLogMessage(String loggerName, Level level, String eventType, String msg,
                                         Map<String, String> context) {
        super(new LogEvent(loggerName, level, eventType, msg, context));
    }
}
