/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.greengrass.logging.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.Test;
import org.slf4j.event.Level;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class StructuredLayoutTest {
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper().disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .addMixIn(Throwable.class, ThrowableMixin.class);

    @Test
    public void GIVEN_json_appender_WHEN_write_log_events_THEN_events_are_json_encoded() throws IOException {
        List<GreengrassLogMessage> messages = Arrays.asList(
                new GreengrassLogMessage("Logger", Level.INFO, "eventType", "message 1",
                        new HashMap<String, String>() {{
                            put("key", "value");
                        }}, new Exception("EX!")),
                new GreengrassLogMessage("Logger", Level.INFO, "eventType", "message 1", null, null),
                new GreengrassLogMessage("Logger", Level.INFO, null, "message 1",
                        new HashMap<String, String>() {{
                            put("key", "value");
                        }}, null));
        String dataStr = messages.stream().map(GreengrassLogMessage::getJSONMessage)
                .collect(Collectors.joining(System.lineSeparator()));

        String[] appendedMessages = dataStr.split(System.lineSeparator());
        for (int i = 0; i < messages.size(); i++) {
            GreengrassLogMessage message = messages.get(i);
            GreengrassLogMessage deserializedMessage =
                    JSON_MAPPER.readValue(appendedMessages[i], GreengrassLogMessage.class);
            //comparing the original message with the de-serialized message
            assertEquals(message, deserializedMessage);

            // Separate compare the throwable because it doesn't deserialize to an exactly equal object
            if (message.getCause() == null) {
                assertNull(deserializedMessage.getCause());
            } else {
                assertThrowableEquals(message.getCause(), deserializedMessage.getCause());
            }
        }
        assertEquals(
                "{\"thread\":\"main\",\"level\":\"INFO\",\"eventType\":null,\"message\":\"message 1\","
                        + "\"contexts\":{\"key\":\"value\"},"
                        + "\"loggerName\":\"Logger\",\"timestamp\":" + messages.get(2).getTimestamp()
                        + ",\"cause\":null}", appendedMessages[2]);
    }

    @Test
    public void GIVEN_text_appender_WHEN_write_log_events_THEN_events_are_patterned_as_text() {
        List<GreengrassLogMessage> messages = Arrays.asList(
                new GreengrassLogMessage("Logger", Level.INFO, null, "message 1",
                        new HashMap<String, String>() {{
                            put("key", "value");
                        }}, null));
        String dataStr = messages.stream().map(GreengrassLogMessage::getTextMessage)
                .collect(Collectors.joining(System.lineSeparator()));

        String[] appendedMessages = dataStr.split(System.lineSeparator());
        for (int i = 0; i < messages.size(); i++) {
            GreengrassLogMessage message = messages.get(i);
            assertEquals(message.getTextMessage(), appendedMessages[i]);
        }
    }

    @Test
    public void GIVEN_throwable_WHEN_use_custom_serializer_THEN_succeeds()
            throws JsonProcessingException {
        Throwable cause = assertThrows(JsonProcessingException.class,
                () -> JSON_MAPPER.readValue("this will go wrong", Object.class));

        GreengrassLogMessage message = new GreengrassLogMessage();
        message.setMessage("testing");
        message.setCause(cause);
        GreengrassLogMessage messageDeserialized =
                JSON_MAPPER.readValue(message.getJSONMessage(), GreengrassLogMessage.class);
        assertEquals(message, messageDeserialized);

        Throwable deserializedCause = messageDeserialized.getCause();
        assertThrowableEquals(cause, deserializedCause);
    }

    @Test
    public void GIVEN_message_WHEN_serialize_and_deserialize_THEN_throwable_fields_handled_properly()
            throws JsonProcessingException {
        String baseMessage = "base";
        String causeMessage = "cause with \"quotes\"";  // test that quotes are escaped properly
        Throwable base = new ThrowableWithExtra(baseMessage);
        Throwable suppressed = new ThrowableWithExtra("suppressed");
        Throwable cause = new ThrowableWithExtra(causeMessage);
        base.initCause(cause);
        base.addSuppressed(suppressed);
        cause.addSuppressed(suppressed);

        GreengrassLogMessage message = new GreengrassLogMessage();
        message.setMessage("testing");
        message.setCause(base);
        String jsonMessage = message.getJSONMessage();
        // check no extra fields serialized
        assertFalse(jsonMessage.contains("\"extra\""));

        Throwable deserializedCause = JSON_MAPPER.readValue(jsonMessage, GreengrassLogMessage.class).getCause();
        assertThrowableEquals(base, deserializedCause);
        // check no extra fields deserialized in the throwable itself, suppressed, and cause
        assertThrows(NoSuchFieldException.class, () -> deserializedCause.getClass().getField("extra"));
        assertThrows(NoSuchFieldException.class,
                () -> deserializedCause.getSuppressed()[0].getClass().getField("extra"));
        assertThrows(NoSuchFieldException.class, () -> deserializedCause.getCause().getClass().getField("extra"));
    }

    /**
     * Assert that two Throwables equal on fields that we care about
     */
    private static void assertThrowableEquals(Throwable expected, Throwable actual) {
        if (expected == null) {
            assertNull(actual);
            return;
        }
        assertEquals(expected.getMessage(), actual.getMessage());
        assertArrayEquals(expected.getStackTrace(), actual.getStackTrace());
        assertThrowableEquals(expected.getCause(), actual.getCause());

        assertEquals(expected.getSuppressed().length, actual.getSuppressed().length);
        for (int i = 0; i < expected.getSuppressed().length; i++) {
            assertThrowableEquals(expected.getSuppressed()[i], actual.getSuppressed()[i]);
        }
    }

    private static class ThrowableWithExtra extends Throwable {
        public String extra;
        ThrowableWithExtra(String extra) {
            super(extra);
            this.extra = extra;
        }
    }

    /**
     * Java's Throwable.suppressedExceptions naming is inconsistent with its getter (getSuppressed)
     * This mixin is required for Jackson to deserialize it properly
     */
    private static abstract class ThrowableMixin {
        @JsonProperty("suppressed")
        List<Throwable> suppressedExceptions;
    }
}
