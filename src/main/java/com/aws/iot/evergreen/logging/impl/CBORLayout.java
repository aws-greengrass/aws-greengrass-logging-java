package com.aws.iot.evergreen.logging.impl;

import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.fasterxml.jackson.jr.ob.JSON;
import com.fasterxml.jackson.jr.stree.JacksonJrsTreeCodec;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.AbstractLayout;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;


@Plugin(name = "CBORLayout", category = Node.CATEGORY, elementType = Layout.ELEMENT_TYPE, printObject = false)
public class CBORLayout extends AbstractLayout<LogEvent> {

    private static final JSON encoder = JSON.std.with(new JacksonJrsTreeCodec()).with(new CBORFactory());
    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
    private static final int NUM_OF_BYTES_REPRESENTING_MESSAGE_LENGTH = 4;
    private static final byte[] ARRAY_REPRESENTING_MESSAGE_LENGTH = new byte[NUM_OF_BYTES_REPRESENTING_MESSAGE_LENGTH];

    private CBORLayout(){
        super(null, null, null);
    }

    @PluginFactory
    public static CBORLayout createLayout() {
        return new CBORLayout();
    }

    /**
     *
     * Encodes the message in the log event using cbor data format
     *
     * @param event
     * @return  First 4 bytes represent the size of the serialized message followed
     * by message itself.
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
            buffer.putInt(0,eventInBytes.length - NUM_OF_BYTES_REPRESENTING_MESSAGE_LENGTH);
            return buffer.array();
        } catch (Exception e) {
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
