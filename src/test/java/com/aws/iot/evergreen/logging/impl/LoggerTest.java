/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.aws.iot.evergreen.logging.impl;

import com.aws.iot.evergreen.logging.api.Logger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.message.Message;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

// TODO: write proper unit tests https://issues.amazon.com/issues/P31936029
@ExtendWith(MockitoExtension.class)
@Tag("Integration")
public class LoggerTest {
    @Captor
    ArgumentCaptor<Level> logLevel;
    @Captor
    ArgumentCaptor<Message> message;

    @Test
    public void GIVEN_logger_for_name_WHEN_check_level_THEN_level_is_info_by_default() {
        Logger logger = LogManager.getLogger("test");

        assertEquals("test", logger.getName());
        assertTrue(logger.isInfoEnabled(), "info should be enabled");
        assertFalse(logger.isDebugEnabled(), "debug should not be enabled");
        assertFalse(logger.isTraceEnabled(), "trace should not be enabled");
    }

    @Test
    public void GIVEN_logger_for_class_WHEN_getName_THEN_name_is_class_name() {
        Logger logger = LogManager.getLogger(this.getClass());

        assertEquals("com.aws.iot.evergreen.logging.impl.LoggerTest", logger.getName());
    }

    @Test
    public void GIVEN_logger_WHEN_log_at_level_below_setting_THEN_message_is_not_logged() {
        // Setup logger with spy
        Log4jLoggerAdapter logger = (Log4jLoggerAdapter) LogManager.getLogger(this.getClass());
        org.apache.logging.log4j.Logger loggerSpy = setupLoggerSpy(logger);

        logger.atTrace().log("HI@Trace");
        verify(loggerSpy, times(0)).logMessage(eq(Level.TRACE), any(), any(), any(), any(), any());

        logger.atInfo().log("HI@Info");
        verify(loggerSpy).logMessage(eq(Level.INFO), any(), any(), any(), any(), any());
    }

    @Test
    public void GIVEN_logger_WHEN_logger_has_default_context_THEN_logline_contains_default_context() {
        // Setup logger with spy
        Log4jLoggerAdapter logger = (Log4jLoggerAdapter) LogManager.getLogger(this.getClass());
        org.apache.logging.log4j.Logger loggerSpy = setupLoggerSpy(logger);

        logger.addDefaultKeyValue("Key", "Data");

        logger.atInfo().addKeyValue("Key2", "Data2").log("HI@Info");
        verify(loggerSpy).logMessage(eq(Level.INFO), any(), any(), any(), any(), any());
        Map<String, String> context = ((EvergreenStructuredLogMessage) message.getValue()).getContexts();

        assertEquals(2, context.size());
        assertEquals("Data", context.get("Key"));
        assertEquals("Data2", context.get("Key2"));
    }

    @Test
    public void GIVEN_logger_at_trace_WHEN_log_at_each_level_THEN_logs_at_each_level() {
        // Setup logger with spy
        Log4jLoggerAdapter logger = (Log4jLoggerAdapter) LogManager.getLogger(this.getClass());
        org.apache.logging.log4j.Logger loggerSpy = setupLoggerSpy(logger);

        Runnable reset = () -> {
            Mockito.reset(loggerSpy);
            lenient().when(loggerSpy.isTraceEnabled()).thenReturn(true);
            lenient().when(loggerSpy.isDebugEnabled()).thenReturn(true);
        };
        reset.run();

        logger.atTrace().log();
        verify(loggerSpy).logMessage(eq(Level.TRACE), any(), any(), any(), any(), any());
        reset.run();
        logger.trace("");
        verify(loggerSpy).logMessage(eq(Level.TRACE), any(), any(), any(), any(), any());
        reset.run();

        logger.atDebug().log();
        verify(loggerSpy).logMessage(eq(Level.DEBUG), any(), any(), any(), any(), any());
        reset.run();
        logger.debug("");
        verify(loggerSpy).logMessage(eq(Level.DEBUG), any(), any(), any(), any(), any());
        reset.run();

        logger.atInfo().log();
        verify(loggerSpy).logMessage(eq(Level.INFO), any(), any(), any(), any(), any());
        reset.run();
        logger.info("");
        verify(loggerSpy).logMessage(eq(Level.INFO), any(), any(), any(), any(), any());
        reset.run();

        logger.atWarn().log();
        verify(loggerSpy).logMessage(eq(Level.WARN), any(), any(), any(), any(), any());
        reset.run();
        logger.warn("");
        verify(loggerSpy).logMessage(eq(Level.WARN), any(), any(), any(), any(), any());
        reset.run();

        logger.atError().log();
        verify(loggerSpy).logMessage(eq(Level.ERROR), any(), any(), any(), any(), any());
        reset.run();
        logger.error("");
        verify(loggerSpy).logMessage(eq(Level.ERROR), any(), any(), any(), any(), any());
        reset.run();

        logger.atFatal().log();
        verify(loggerSpy).logMessage(eq(Level.FATAL), any(), any(), any(), any(), any());
        reset.run();
        logger.fatal("");
        verify(loggerSpy).logMessage(eq(Level.FATAL), any(), any(), any(), any(), any());
        reset.run();
    }

    @Test
    public void GIVEN_logger_WHEN_log_with_cause_and_event_type_THEN_logline_contains_cause_and_event_type() {
        // Setup logger with spy
        Log4jLoggerAdapter logger = (Log4jLoggerAdapter) LogManager.getLogger(this.getClass());
        org.apache.logging.log4j.Logger loggerSpy = setupLoggerSpy(logger);

        logger.atInfo().setCause(new IOException("hi")).setEventType("some type").log();
        verify(loggerSpy).logMessage(eq(Level.INFO), any(), any(), any(), any(), any());
        Throwable t = ((EvergreenStructuredLogMessage) message.getValue()).getCause();
        assertEquals("hi", t.getMessage());

        String event = ((EvergreenStructuredLogMessage) message.getValue()).getEventType();
        assertEquals("some type", event);
    }

    private org.apache.logging.log4j.Logger setupLoggerSpy(Log4jLoggerAdapter logger) {
        org.apache.logging.log4j.Logger loggerSpy = spy(logger.getLogger());
        logger.setLogger(loggerSpy);
        doCallRealMethod().when(loggerSpy).logMessage(logLevel.capture(), any(), any(), any(), message.capture(), any());
        return loggerSpy;
    }
}
