package com.aws.iot.evergreen.logging.impl;

import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.fasterxml.jackson.jr.ob.JSON;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.AbstractLayout;
import org.apache.logging.log4j.message.Message;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Serialises the {@link Message} in {@link LogEvent} to CBOR format.
 *
 * <p>For each {@link Message} the layout reserves the initial four bytes to store the length of the serialized message.
 */
@Plugin(name = "CborLayout", category = Node.CATEGORY, elementType = Layout.ELEMENT_TYPE, printObject = false)
public class CborLayout extends AbstractLayout<LogEvent> {

    private static final JSON encoder = JSON.std.with(new CBORFactory());
    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
    private static final int NUM_OF_BYTES_REPRESENTING_MESSAGE_LENGTH = 4;
    private static final byte[] ARRAY_REPRESENTING_MESSAGE_LENGTH = new byte[NUM_OF_BYTES_REPRESENTING_MESSAGE_LENGTH];

    private CborLayout() {
        super(null, null, null);
    }

    @PluginFactory
    public static CborLayout createLayout() {
        return new CborLayout();
    }

    /**
     * Encodes the message in the log event using cbor data format.
     * @return First 4 bytes represent the size of the serialized message followed
     *         by message itself.
     */
    @Override
    public byte[] toByteArray(LogEvent event) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            // appending 4 bytes for storing the length of the serialized log message
            byteArrayOutputStream.write(ARRAY_REPRESENTING_MESSAGE_LENGTH);
            encoder.write(event.getMessage(), byteArrayOutputStream);
            byte[] eventInBytes = byteArrayOutputStream.toByteArray();
            // wrapping the bytes array in a buffer to write the size of the serialized payload
            // in the first four bytes
            ByteBuffer buffer = ByteBuffer.wrap(eventInBytes);
            buffer.putInt(0, eventInBytes.length - NUM_OF_BYTES_REPRESENTING_MESSAGE_LENGTH);
            return buffer.array();
        } catch (IOException e) {
            LOGGER.error("Serialization of LogEvent failed.", e);
        }
        return EMPTY_BYTE_ARRAY;
    }

    @Override
    public LogEvent toSerializable(LogEvent event) {
        return event;
    }

    @Override
    public String getContentType() {
        return "application/cbor";
    }
}
