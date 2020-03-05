/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.logging.impl.plugins.layouts;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.core.LogEvent;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

class JSONLayout extends StructuredLayout {
    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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
            OBJECT_MAPPER.writeValue(byteArrayOutputStream, event.getMessage());
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
