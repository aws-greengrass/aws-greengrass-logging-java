/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.logging.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.slf4j.event.Level;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class StructuredLayoutTest {
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

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
        String dataStr = messages.stream().map(EvergreenStructuredLogMessage::getJSONMessage)
                .collect(Collectors.joining(System.lineSeparator()));

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
                "{\"thread\":\"main\",\"level\":\"INFO\",\"eventType\":null,\"message\":\"message 1\","
                        + "\"contexts\":{\"key\":\"value\"},"
                        + "\"loggerName\":\"Logger\",\"timestamp\":" + messages.get(2).getTimestamp()
                        + ",\"cause\":null}", appendedMessages[2]);
    }

    @Test
    public void GIVEN_text_appender_WHEN_write_log_events_THEN_events_are_patterned_as_text() {
        List<EvergreenStructuredLogMessage> messages = Arrays.asList(
                new EvergreenStructuredLogMessage("Logger", Level.INFO, null, "message 1",
                        new HashMap<String, String>() {{
                            put("key", "value");
                        }}, null));
        String dataStr = messages.stream().map(EvergreenStructuredLogMessage::getTextMessage)
                .collect(Collectors.joining(System.lineSeparator()));

        String[] appendedMessages = dataStr.split(System.lineSeparator());
        for (int i = 0; i < messages.size(); i++) {
            EvergreenStructuredLogMessage message = messages.get(i);
            assertEquals(message.getTextMessage(), appendedMessages[i]);
        }
    }
}
