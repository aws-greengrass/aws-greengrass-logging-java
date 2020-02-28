package com.aws.iot.evergreen.logging.impl;

import com.aws.iot.evergreen.logging.impl.config.LogFormat;
import com.aws.iot.evergreen.logging.impl.plugins.layouts.StructuredLayout;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.appender.OutputStreamAppender;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class StructuredLayoutTest {

    private static final StructuredLayout CBOR_LAYOUT = StructuredLayout.createLayout(LogFormat.CBOR, null, null);
    private static final StructuredLayout JSON_LAYOUT =
            StructuredLayout.createLayout(LogFormat.JSON, null, StandardCharsets.UTF_8);
    private static final StructuredLayout TEXT_LAYOUT =
            StructuredLayout.createLayout(LogFormat.TEXT, "%m%n", StandardCharsets.UTF_8);
    private static final ObjectMapper CBOR_MAPPER = new CBORMapper();
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static OutputStreamAppender cborOutputStreamAppender;
    private static OutputStreamAppender jsonOutputStreamAppender;
    private static OutputStreamAppender textOutputStreamAppender;
    private static final ByteArrayOutputStream outContent = new ByteArrayOutputStream();

    @BeforeAll
    public static void setupAppender() {
        cborOutputStreamAppender =
                OutputStreamAppender.createAppender(CBOR_LAYOUT, null, outContent, "outputStreamAppender", true, false);
        cborOutputStreamAppender.start();
        jsonOutputStreamAppender =
                OutputStreamAppender.createAppender(JSON_LAYOUT, null, outContent, "outputStreamAppender", true, false);
        jsonOutputStreamAppender.start();
        textOutputStreamAppender =
                OutputStreamAppender.createAppender(TEXT_LAYOUT, null, outContent, "outputStreamAppender", true, false);
        textOutputStreamAppender.start();
        CBOR_MAPPER.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    @BeforeEach
    public void resetOutputStream() {
        outContent.reset();
    }

    @Test
    public void GIVEN_cbor_appender_WHEN_write_log_events_THEN_events_are_cbor_encoded() throws IOException {
        List<Message> messages = Arrays.asList(
                new EvergreenStructuredLogMessage("Logger", Level.INFO, "eventType", "message 1",
                        new HashMap<String, String>() {{
                            put("key", "value");
                        }}, new Exception("EX!")),
                new EvergreenStructuredLogMessage("Logger", Level.INFO, "eventType", "message 1", null, null),
                new EvergreenStructuredLogMessage("Logger", Level.INFO, null, "message 1",
                        new HashMap<String, String>() {{
                            put("key", "value");
                        }}, null));
        messages.forEach((m) -> {
            Log4jLogEvent log4jLogEvent = new Log4jLogEvent.Builder().setMessage(m).build();
            cborOutputStreamAppender.append(log4jLogEvent);
        });

        ByteBuffer buf = ByteBuffer.wrap(outContent.toByteArray());
        for (Message message : messages) {
            int length = buf.getInt();
            byte[] arr = new byte[length];
            buf.get(arr);
            EvergreenStructuredLogMessage deserializedMessage =
                    CBOR_MAPPER.readValue(arr, EvergreenStructuredLogMessage.class);
            // comparing the original message with the de-serialized message
            assertEquals(message, deserializedMessage);
        }
    }

    @Test
    public void GIVEN_json_appender_WHEN_write_evergreen_log_events_THEN_events_are_json_encoded() throws IOException {
        List<EvergreenStructuredLogMessage> messages = Arrays.asList(
                new EvergreenStructuredLogMessage("Logger", Level.INFO, "eventType", "message 1",
                        new HashMap<String, String>() {{
                            put("key", "value");
                        }}, new Exception("EX!")),
                new EvergreenStructuredLogMessage("Logger", Level.INFO, "eventType", "message 1", null, null),
                new EvergreenStructuredLogMessage("Logger", Level.INFO, null, "message 1",
                        new HashMap<String, String>() {{
                            put("key", "value");
                        }}, null));
        messages.forEach((m) -> {
            Log4jLogEvent log4jLogEvent = new Log4jLogEvent.Builder().setMessage(m).build();
            jsonOutputStreamAppender.append(log4jLogEvent);
        });

        byte[] data = outContent.toByteArray();
        String dataStr = new String(data, StandardCharsets.UTF_8);
        String[] appendedMessages = dataStr.split(System.lineSeparator());
        for (int i = 0; i < messages.size(); i++) {
            EvergreenStructuredLogMessage message = messages.get(i);
            EvergreenStructuredLogMessage deserializedMessage =
                    JSON_MAPPER.readValue(appendedMessages[i], EvergreenStructuredLogMessage.class);
            //comparing the original message with the de-serialized message
            assertEquals(message, deserializedMessage);

            // Separate compare the throwable because it doesn't deserialize to an exactly equal object
            if (message.getCause() == null) {
                assertNull(deserializedMessage.getCause());
            } else {
                assertEquals(message.getCause().getMessage(), deserializedMessage.getCause().getMessage());
            }
        }
        assertEquals(
                "{\"level\":\"INFO\",\"eventType\":null,\"message\":\"message 1\",\"contexts\":{\"key\":\"value\"},"
                        + "\"loggerName\":\"Logger\",\"timestamp\":" + messages.get(2).getTimestamp()
                        + ",\"cause\":null}", appendedMessages[2]);
    }

    @Test
    public void GIVEN_text_appender_WHEN_write_log_events_THEN_events_are_patterned_as_text() throws IOException {
        List<Message> messages = Arrays.asList(new SimpleMessage("message 1"), new SimpleMessage("message 2"),
                new SimpleMessage("msg 3"), new EvergreenStructuredLogMessage("Logger", Level.INFO, null, "message 1",
                        new HashMap<String, String>() {{
                            put("key", "value");
                        }}, null));
        messages.forEach((m) -> {
            Log4jLogEvent log4jLogEvent = new Log4jLogEvent.Builder().setMessage(m).build();
            textOutputStreamAppender.append(log4jLogEvent);
        });

        byte[] data = outContent.toByteArray();
        String dataStr = new String(data, StandardCharsets.UTF_8);
        String[] appendedMessages = dataStr.split(System.lineSeparator());
        for (int i = 0; i < messages.size(); i++) {
            Message message = messages.get(i);
            assertEquals(message.getFormattedMessage(), appendedMessages[i]);
        }
    }
}
