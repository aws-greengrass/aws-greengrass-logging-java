package com.aws.iot.evergreen.logging.impl;

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
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StructuredLayoutTest {

    private static final StructuredLayout layout = StructuredLayout.createLayout(null, null);
    private static final JSON encoder = JSON.std.with(new JacksonJrsTreeCodec()).with(new CBORFactory());
    private static final ObjectMapper mapper = new CBORMapper();
    private static OutputStreamAppender outputStreamAppender;
    private static final ByteArrayOutputStream outContent = new ByteArrayOutputStream();

    @BeforeAll
    public static void setupAppender() {
        outputStreamAppender = OutputStreamAppender.createAppender(layout, null, outContent, "outputStreamAppender",
                true, false);
        outputStreamAppender.start();
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    @BeforeEach
    public void resetOutputStream() {
        outContent.reset();
    }

    @Test
    public void logRandomThings() throws IOException {
        List<SimpleMessage> messages = Arrays.asList(
                new SimpleMessage("message 1"), new SimpleMessage("message 2"), new SimpleMessage("msg 3"));
        messages.forEach((m) -> {
            Log4jLogEvent log4jLogEvent = new Log4jLogEvent.Builder().setMessage(m).build();
            outputStreamAppender.append(log4jLogEvent);
        });

        ByteBuffer buf = ByteBuffer.wrap(outContent.toByteArray());
        for (SimpleMessage message : messages) {
            int length = buf.getInt();
            byte[] arr = new byte[length];
            buf.get(arr);
            TreeNode tree = encoder.treeFrom(arr);
            SimpleMessage deserializedMessage = tree.traverse(mapper).readValueAs(SimpleMessage.class);
            //comparing the original message with the de-serialized message
            assertEquals(message, deserializedMessage);
        }
    }
}
