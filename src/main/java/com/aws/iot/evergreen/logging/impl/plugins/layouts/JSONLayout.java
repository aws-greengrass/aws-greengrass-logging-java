package com.aws.iot.evergreen.logging.impl.plugins.layouts;

import com.fasterxml.jackson.jr.ob.JSON;
import org.apache.logging.log4j.core.LogEvent;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

class JSONLayout extends StructuredLayout {
    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
    private final JSON encoder = JSON.std;

    public JSONLayout(Charset charset) {
        super(charset);
    }

    /**
     * Encodes the message in the log event using JSON data format.
     *
     * @return the serialized message.
     */
    @Override
    public byte[] toByteArray(LogEvent event) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            encoder.write(event.getMessage(), byteArrayOutputStream);
            byteArrayOutputStream.write(System.lineSeparator().getBytes(charset));
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            LOGGER.error("Serialization of LogEvent to JSON failed.", e);
        }
        return EMPTY_BYTE_ARRAY;
    }

    @Override
    public String getContentType() {
        return "application/json";
    }
}