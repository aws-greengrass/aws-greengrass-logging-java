package com.aws.iot.evergreen.logging.impl;

import com.aws.iot.evergreen.logging.impl.config.LogFormat;
import com.aws.iot.evergreen.logging.impl.plugins.layouts.StructuredLayout;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper;
import com.fasterxml.jackson.jr.ob.JSON;
import com.fasterxml.jackson.jr.stree.JacksonJrsTreeCodec;
import org.apache.logging.log4j.core.appender.OutputStreamAppender;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StructuredLayoutTest {

    private static final StructuredLayout CBOR_LAYOUT = StructuredLayout.createLayout(LogFormat.CBOR, null, null);
    private static final StructuredLayout JSON_LAYOUT =
            StructuredLayout.createLayout(LogFormat.JSON, null, StandardCharsets.UTF_8);
    private static final StructuredLayout TEXT_LAYOUT =
            StructuredLayout.createLayout(LogFormat.TEXT, "%m%n", StandardCharsets.UTF_8);
    private static final JSON CBOR_ENCODER = JSON.std.with(new JacksonJrsTreeCodec()).with(new CBORFactory());
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
        List<SimpleMessage> messages = Arrays.asList(new SimpleMessage("message 1"), new SimpleMessage("message 2"),
                new SimpleMessage("msg 3"));
        messages.forEach((m) -> {
            Log4jLogEvent log4jLogEvent = new Log4jLogEvent.Builder().setMessage(m).build();
            cborOutputStreamAppender.append(log4jLogEvent);
        });

        ByteBuffer buf = ByteBuffer.wrap(outContent.toByteArray());
        for (SimpleMessage message : messages) {
            int length = buf.getInt();
            byte[] arr = new byte[length];
            buf.get(arr);
            TreeNode tree = CBOR_ENCODER.treeFrom(arr);
            SimpleMessage deserializedMessage = tree.traverse(CBOR_MAPPER).readValueAs(SimpleMessage.class);
            //comparing the original message with the de-serialized message
            assertEquals(message, deserializedMessage);
        }
    }

    @Test
    public void GIVEN_json_appender_WHEN_write_log_events_THEN_events_are_json_encoded() throws IOException {
        List<SimpleMessage> messages = Arrays.asList(new SimpleMessage("message 1"), new SimpleMessage("message 2"),
                new SimpleMessage("msg 3"));
        messages.forEach((m) -> {
            Log4jLogEvent log4jLogEvent = new Log4jLogEvent.Builder().setMessage(m).build();
            jsonOutputStreamAppender.append(log4jLogEvent);
        });

        byte[] data = outContent.toByteArray();
        String dataStr = new String(data, StandardCharsets.UTF_8);
        String[] appendedMessages = dataStr.split(System.lineSeparator());
        for (int i = 0; i < messages.size(); i++) {
            SimpleMessage message = messages.get(i);
            SimpleMessage deserializedMessage = JSON_MAPPER.readValue(appendedMessages[i], SimpleMessage.class);
            //comparing the original message with the de-serialized message
            assertEquals(message, deserializedMessage);
        }
    }

    @Test
    public void GIVEN_text_appender_WHEN_write_log_events_THEN_events_are_patterned_as_text() throws IOException {
        List<SimpleMessage> messages = Arrays.asList(new SimpleMessage("message 1"), new SimpleMessage("message 2"),
                new SimpleMessage("msg 3"));
        messages.forEach((m) -> {
            Log4jLogEvent log4jLogEvent = new Log4jLogEvent.Builder().setMessage(m).build();
            textOutputStreamAppender.append(log4jLogEvent);
        });

        byte[] data = outContent.toByteArray();
        String dataStr = new String(data, StandardCharsets.UTF_8);
        String[] appendedMessages = dataStr.split(System.lineSeparator());
        for (int i = 0; i < messages.size(); i++) {
            SimpleMessage message = messages.get(i);
            assertEquals(message.getFormattedMessage(), appendedMessages[i]);
        }
    }
}
