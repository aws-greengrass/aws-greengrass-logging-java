/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.logging.impl.plugins.layouts;

import com.aws.iot.evergreen.logging.impl.config.LogFormat;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.AbstractLayout;
import org.apache.logging.log4j.message.Message;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Serializes the {@link Message} in {@link LogEvent} to CBOR or JSON format.
 *
 * <p>For each Message the CBOR layout reserves the initial four bytes to store the length of the serialized
 * message.
 */
@Plugin(name = "StructuredLayout", category = Node.CATEGORY, elementType = Layout.ELEMENT_TYPE, printObject = false)
public abstract class StructuredLayout extends AbstractLayout<LogEvent> {
    protected Charset charset;

    protected StructuredLayout(Charset charset) {
        super(null, null, null);

        this.charset = charset == null ? StandardCharsets.UTF_8 : charset;
    }

    /**
     * Factor to get a StucturedLayout implementation from the given format and charset.
     *
     * @param format format one of {@link LogFormat}
     * @param charset character set for strings.
     */
    @PluginFactory
    public static StructuredLayout createLayout(@PluginAttribute(value = "format") LogFormat format,
                                                @PluginAttribute(value = "pattern") String pattern,
                                                @PluginAttribute(value = "charset") Charset charset) {
        if (format == null) {
            format = LogFormat.CBOR;
        }
        switch (format) {
            case JSON:
                return new JSONLayout(charset);
            case TEXT:
                return new PatternLayout(charset, pattern);
            case CBOR:
            default:
                return new CBORLayout(charset);
        }
    }

    @Override
    public LogEvent toSerializable(LogEvent event) {
        return event;
    }

}
