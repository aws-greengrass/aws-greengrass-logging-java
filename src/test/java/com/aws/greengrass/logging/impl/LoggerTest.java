/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.greengrass.logging.impl;

import com.aws.greengrass.logging.api.Logger;
import com.aws.greengrass.logging.impl.config.LogConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.event.Level;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

// TODO: write proper unit tests https://issues.amazon.com/issues/P31936029
@ExtendWith(MockitoExtension.class)
@Tag("Integration")
class LoggerTest {

    @TempDir
    static Path tempDir;

    @Captor
    ArgumentCaptor<String> message;

    @BeforeEach
    void beforeTesting() {
        System.setProperty("root", tempDir.toAbsolutePath().toString());
        LogConfig.getInstance().setLevel(Level.TRACE);
    }

    @Test
    void GIVEN_logger_for_name_WHEN_check_level_THEN_level_is_info_by_default() {
        LogConfig.getInstance().setLevel(Level.INFO);
        Logger logger = LogManager.getLogger("test");

        assertEquals("test", logger.getName());
        assertTrue(logger.isInfoEnabled(), "info should be enabled");
        assertFalse(logger.isDebugEnabled(), "debug should not be enabled");
        assertFalse(logger.isTraceEnabled(), "trace should not be enabled");
    }

    @Test
    void GIVEN_logger_for_class_WHEN_getName_THEN_name_is_class_name() {
        Logger logger = LogManager.getLogger(this.getClass());

        assertEquals("com.aws.greengrass.logging.impl.LoggerTest", logger.getName());
    }

    @Test
    void GIVEN_logger_WHEN_log_at_level_below_setting_THEN_message_is_not_logged() {
        // Setup logger with spy
        LogConfig.getInstance().setLevel(Level.INFO);
        Slf4jLogAdapter logger = (Slf4jLogAdapter) LogManager.getLogger("test");
        org.slf4j.Logger loggerSpy = setupLoggerSpy(logger);

        logger.atTrace().log("HI@Trace");
        verify(loggerSpy, times(0)).trace(any());

        logger.atInfo().log("HI@Info");
        verify(loggerSpy).info(any());
    }

    @Test
    void GIVEN_logger_WHEN_logger_has_default_context_THEN_logline_contains_default_context() {
        // Setup logger with spy
        Slf4jLogAdapter logger = (Slf4jLogAdapter) LogManager.getLogger(this.getClass());
        org.slf4j.Logger loggerSpy = setupLoggerSpy(logger);
        AtomicInteger hitCount = new AtomicInteger(0);
        Consumer<GreengrassLogMessage> l = m -> {
            Map<String, String> context = m.getContexts();
            assertEquals(3, context.size());
            assertEquals("Data", context.get("Key"));
            assertEquals("Data2", context.get("Key2"));
            assertEquals("DataShortform", context.get("KeyShortform"));
            hitCount.incrementAndGet();
        };
        Slf4jLogAdapter.addGlobalListener(l);

        logger.addDefaultKeyValue("Key", "Data");

        logger.atInfo().addKeyValue("Key2", "Data2").kv("KeyShortform", "DataShortform").log("HI@Info");
        verify(loggerSpy).info(any());
        String event = message.getValue();

        assertThat(event, containsString("{Key2=Data2, KeyShortform=DataShortform, Key=Data}"));
        Slf4jLogAdapter.removeGlobalListener(l);
        assertEquals(1, hitCount.get());
    }

    @Test
    void GIVEN_logger_at_trace_WHEN_log_at_each_level_THEN_logs_at_each_level() {
        // Setup logger with spy
        Slf4jLogAdapter logger = (Slf4jLogAdapter) LogManager.getLogger(this.getClass());
        org.slf4j.Logger loggerSpy = setupLoggerSpy(logger);

        Runnable reset = () -> {
            Mockito.reset(loggerSpy);
            lenient().when(loggerSpy.isTraceEnabled()).thenReturn(true);
            lenient().when(loggerSpy.isDebugEnabled()).thenReturn(true);
        };
        reset.run();

        logger.atTrace().log();
        verify(loggerSpy).trace(any());
        reset.run();
        logger.trace("");
        verify(loggerSpy).trace(any());
        reset.run();

        logger.atDebug().log();
        verify(loggerSpy).debug(any());
        reset.run();
        logger.debug("");
        verify(loggerSpy).debug(any());
        reset.run();

        logger.atInfo().log();
        verify(loggerSpy).info(any());
        reset.run();
        logger.info("");
        verify(loggerSpy).info(any());
        reset.run();

        logger.atWarn().log();
        verify(loggerSpy).warn(any());
        reset.run();
        logger.warn("");
        verify(loggerSpy).warn(any());
        reset.run();

        logger.atError().log();
        verify(loggerSpy).error(any());
        reset.run();
        logger.error("");
        verify(loggerSpy).error(any());
        reset.run();
    }

    @Test
    void GIVEN_logger_WHEN_log_with_cause_and_event_type_THEN_logline_contains_cause_and_event_type() {
        // Setup logger with spy
        Slf4jLogAdapter logger = (Slf4jLogAdapter) LogManager.getLogger(this.getClass());
        org.slf4j.Logger loggerSpy = setupLoggerSpy(logger);

        logger.atInfo().setCause(new IOException("hi")).setEventType("some type").log();
        verify(loggerSpy).info(any());

        String event = message.getValue();
        assertThat(event, containsString("hi"));
        assertThat(event, containsString("some type"));
    }

    @Test
    void GIVEN_logger_WHEN_log_with_cause_and_event_type_shortform_THEN_logline_contains_cause_and_event_type() {
        // Setup logger with spy
        Slf4jLogAdapter logger = (Slf4jLogAdapter) LogManager.getLogger(this.getClass());
        org.slf4j.Logger loggerSpy = setupLoggerSpy(logger);

        logger.atInfo().cause(new IOException("hi")).event("some type").log();
        verify(loggerSpy).info(any());

        String event = message.getValue();
        assertThat(event, containsString("hi"));
        assertThat(event, containsString("some type"));
    }

    @Test
    void GIVEN_logger_WHEN_trace_log_with_event_type_shortform_THEN_logline_contains_event_type() {
        // Setup logger with spy
        Slf4jLogAdapter logger = (Slf4jLogAdapter) LogManager.getLogger(this.getClass());
        org.slf4j.Logger loggerSpy = setupLoggerSpy(logger);

        logger.atTrace("some type").log();
        verify(loggerSpy).trace(any());

        String event = message.getValue();
        assertThat(event, containsString("some type"));
    }

    @Test
    void GIVEN_logger_WHEN_trace_log_with_cause_and_event_type_shortform_THEN_logline_contains_cause_and_event_type() {
        // Setup logger with spy
        Slf4jLogAdapter logger = (Slf4jLogAdapter) LogManager.getLogger(this.getClass());
        org.slf4j.Logger loggerSpy = setupLoggerSpy(logger);

        logger.atTrace("some type", new IOException("hi")).log();
        verify(loggerSpy).trace(any());
        String event = message.getValue();

        assertThat(event, containsString("hi"));
        assertThat(event, containsString("some type"));
    }

    @Test
    void GIVEN_logger_WHEN_debug_log_with_event_type_shortform_THEN_logline_contains_event_type() {
        // Setup logger with spy
        Slf4jLogAdapter logger = (Slf4jLogAdapter) LogManager.getLogger(this.getClass());
        org.slf4j.Logger loggerSpy = setupLoggerSpy(logger);

        logger.atDebug("some type").log();
        verify(loggerSpy).debug(any());

        String event = message.getValue();
        assertThat(event, containsString("some type"));
    }

    @Test
    void GIVEN_logger_WHEN_debug_log_with_cause_and_event_type_shortform_THEN_logline_contains_cause_and_event_type() {
        // Setup logger with spy
        Slf4jLogAdapter logger = (Slf4jLogAdapter) LogManager.getLogger(this.getClass());
        org.slf4j.Logger loggerSpy = setupLoggerSpy(logger);

        logger.atDebug("some type", new IOException("hi")).log();
        verify(loggerSpy).debug(any());

        String event = message.getValue();
        assertThat(event, containsString("hi"));
        assertThat(event, containsString("some type"));
    }

    @Test
    void GIVEN_logger_WHEN_info_log_with_event_type_shortform_THEN_logline_contains_event_type() {
        // Setup logger with spy
        Slf4jLogAdapter logger = (Slf4jLogAdapter) LogManager.getLogger(this.getClass());
        org.slf4j.Logger loggerSpy = setupLoggerSpy(logger);

        logger.atInfo("some type").log();
        verify(loggerSpy).info(any());

        String event = message.getValue();
        assertThat(event, containsString("some type"));
    }

    @Test
    void GIVEN_logger_WHEN_info_log_with_cause_and_event_type_shortform_THEN_logline_contains_cause_and_event_type() {
        // Setup logger with spy
        Slf4jLogAdapter logger = (Slf4jLogAdapter) LogManager.getLogger(this.getClass());
        org.slf4j.Logger loggerSpy = setupLoggerSpy(logger);

        logger.atInfo("some type", new IOException("hi")).log();
        verify(loggerSpy).info(any());

        String event = message.getValue();
        assertThat(event, containsString("hi"));
        assertThat(event, containsString("some type"));
    }

    @Test
    void GIVEN_logger_WHEN_error_log_with_event_type_shortform_THEN_logline_contains_event_type() {
        // Setup logger with spy
        Slf4jLogAdapter logger = (Slf4jLogAdapter) LogManager.getLogger(this.getClass());
        org.slf4j.Logger loggerSpy = setupLoggerSpy(logger);

        logger.atError("some type").log();
        verify(loggerSpy).error(any());

        String event = message.getValue();
        assertThat(event, containsString("some type"));
    }

    @Test
    void GIVEN_logger_WHEN_error_log_with_cause_and_event_type_shortform_THEN_logline_contains_cause_and_event_type() {
        // Setup logger with spy
        Slf4jLogAdapter logger = (Slf4jLogAdapter) LogManager.getLogger(this.getClass());
        org.slf4j.Logger loggerSpy = setupLoggerSpy(logger);

        logger.atError("some type", new IOException("hi")).log();
        verify(loggerSpy).error(any());
        String event = message.getValue();
        assertThat(event, containsString("hi"));
        assertThat(event, containsString("some type"));
    }

    @Test
    void GIVEN_logger_WHEN_warn_log_with_event_type_shortform_THEN_logline_contains_event_type() {
        // Setup logger with spy
        Slf4jLogAdapter logger = (Slf4jLogAdapter) LogManager.getLogger(this.getClass());
        org.slf4j.Logger loggerSpy = setupLoggerSpy(logger);

        logger.atWarn("some type").log();
        verify(loggerSpy).warn(any());

        String event = message.getValue();
        assertThat(event, containsString("some type"));
    }

    @Test
    void GIVEN_logger_WHEN_warn_log_with_cause_and_event_type_shortform_THEN_logline_contains_cause_and_event_type() {
        // Setup logger with spy
        Slf4jLogAdapter logger = (Slf4jLogAdapter) LogManager.getLogger(this.getClass());
        org.slf4j.Logger loggerSpy = setupLoggerSpy(logger);

        logger.atWarn("some type", new IOException("hi")).log();
        verify(loggerSpy).warn(any());
        String event = message.getValue();
        assertThat(event, containsString("hi"));
        assertThat(event, containsString("some type"));
    }

    @Test
    void GIVEN_logger_WHEN_log_nulls_THEN_logger_logs_string_null() {
        // Setup logger with spy
        Slf4jLogAdapter logger = (Slf4jLogAdapter) LogManager.getLogger("null_logger");
        org.slf4j.Logger loggerSpy = setupLoggerSpy(logger);

        logger.info(null);
        verify(loggerSpy).info(any());
        String event = message.getValue();
        assertThat(event, containsString("null. {}"));

        Mockito.reset(loggerSpy);
        loggerSpy = setupLoggerSpy(logger);

        logger.addDefaultKeyValue("k1", null).atInfo().kv("k", null).log();
        verify(loggerSpy).info(any());
        String event2 = message.getValue();
        assertThat(event2, containsString("{k1=null, k=null}"));
    }

    @Test
    void GIVEN_logger_WHEN_log_with_supplier_context_THEN_context_calls_supplier() {
        // Setup logger with spy
        Slf4jLogAdapter logger = (Slf4jLogAdapter) LogManager.getLogger("supplied");
        org.slf4j.Logger loggerSpy = setupLoggerSpy(logger);

        AtomicInteger i = new AtomicInteger();
        Supplier<String> s = () -> {
            int count = i.getAndIncrement();
            return "supplied-" + count;
        };
        AtomicInteger i2 = new AtomicInteger();
        Supplier<String> s2 = () -> {
            int count = i2.getAndIncrement();
            return "suppliedDefault-" + count;
        };

        logger.addDefaultKeyValue("k1", s2);
        logger.atInfo().kv("k", s).log();
        String event1 = message.getValue();
        assertThat(event1, containsString("{k1=suppliedDefault-0, k=supplied-0}"));

        logger.atInfo().kv("k", s).log();
        String event2 = message.getValue();
        assertThat(event2, containsString("{k1=suppliedDefault-1, k=supplied-1}"));
    }

    private org.slf4j.Logger setupLoggerSpy(Slf4jLogAdapter logger) {
        org.slf4j.Logger loggerSpy = spy(logger.getLogger());
        logger.setLogger(loggerSpy);
        lenient().doCallRealMethod().when(loggerSpy)
                .trace(message.capture());
        lenient().doCallRealMethod().when(loggerSpy)
                .debug(message.capture());
        lenient().doCallRealMethod().when(loggerSpy)
                .info(message.capture());
        lenient().doCallRealMethod().when(loggerSpy)
                .warn(message.capture());
        lenient().doCallRealMethod().when(loggerSpy)
                .error(message.capture());
        return loggerSpy;
    }
}
