/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.greengrass.logging.impl;

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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

public class StructuredLayoutTest {
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper().disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

    @Test
    public void GIVEN_json_appender_WHEN_write_log_events_THEN_events_are_json_encoded() throws IOException {
        List<GreengrassLogMessage> messages = Arrays.asList(
                new GreengrassLogMessage("Logger", Level.INFO, "eventType", "message 1",
                        new HashMap<String, String>() {{
                            put("key", "value");
                        }}, new Exception("EX!", new Exception("the \"cause\""))),
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
                assertEquals(message.getCause().getMessage(), deserializedMessage.getCause().getMessage());
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
    public void GIVEN_throwable_that_fails_default_serializer_WHEN_log_it_THEN_custom_serializer_succeeds()
            throws JsonProcessingException {
        try {
            // This will throw an exception that cannot be serialized successfully by Jackson's default serializer
            JSON_MAPPER.readValue("this will go wrong", Object.class);
            fail("Expected to raise exception");
        } catch (Throwable e) {
            try {
                JSON_MAPPER.writeValueAsString(e);
                fail("Expected default serializer to fail");
            } catch (Exception ignore) {
                // do nothing
            }
            GreengrassLogMessage message = new GreengrassLogMessage();
            message.setMessage("testing");
            message.setCause(e);
            GreengrassLogMessage messageDeserialized =
                    JSON_MAPPER.readValue(message.getJSONMessage(), GreengrassLogMessage.class);
            assertEquals(message, messageDeserialized);

            Throwable deserializedCause = messageDeserialized.getCause();
            assertEquals(e.getMessage(), deserializedCause.getMessage());
            assertEquals(e.getLocalizedMessage(), deserializedCause.getLocalizedMessage());
            assertEquals(e.getCause(), deserializedCause.getCause());
            assertArrayEquals(e.getSuppressed(), deserializedCause.getSuppressed());
            assertArrayEquals(e.getStackTrace(), deserializedCause.getStackTrace());
        }
    }
}
