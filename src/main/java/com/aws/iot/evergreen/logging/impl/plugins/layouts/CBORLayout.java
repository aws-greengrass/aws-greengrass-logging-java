/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.logging.impl.plugins.layouts;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper;
import org.apache.logging.log4j.core.LogEvent;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

class CBORLayout extends StructuredLayout {
    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
    private static final int NUM_OF_BYTES_REPRESENTING_MESSAGE_LENGTH = 4;
    private static final byte[] ARRAY_REPRESENTING_MESSAGE_LENGTH = new byte[NUM_OF_BYTES_REPRESENTING_MESSAGE_LENGTH];
    private static final ObjectMapper OBJECT_MAPPER = new CBORMapper();

    protected CBORLayout(Charset charset) {
        super(charset);
    }

    /**
     * Encodes the message in the log event using cbor data format.
     *
     * @return First 4 bytes represent the size of the serialized message followed by message itself.
     */
    @Override
    public byte[] toByteArray(LogEvent event) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            // appending 4 bytes for storing the length of the serialized log message
            byteArrayOutputStream.write(ARRAY_REPRESENTING_MESSAGE_LENGTH);
            OBJECT_MAPPER.writeValue(byteArrayOutputStream, event.getMessage());
            byte[] eventInBytes = byteArrayOutputStream.toByteArray();
            // wrapping the bytes array in a buffer to write the size of the serialized payload
            // in the first four bytes
            ByteBuffer buffer = ByteBuffer.wrap(eventInBytes);
            buffer.putInt(0, eventInBytes.length - NUM_OF_BYTES_REPRESENTING_MESSAGE_LENGTH);
            return buffer.array();
        } catch (IOException e) {
            LOGGER.error("Serialization of LogEvent to CBOR failed.", e);
        }
        return EMPTY_BYTE_ARRAY;
    }

    @Override
    public String getContentType() {
        return "application/cbor";
    }
}
