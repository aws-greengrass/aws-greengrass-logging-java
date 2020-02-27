package com.aws.iot.evergreen.logging.impl.plugins.layouts;

import org.apache.logging.log4j.core.LogEvent;

import java.nio.charset.Charset;

class PatternLayout extends StructuredLayout {
    private final org.apache.logging.log4j.core.layout.PatternLayout layout;

    public PatternLayout(Charset charset, String pattern) {
        super(charset);
        layout = org.apache.logging.log4j.core.layout.PatternLayout.newBuilder().withCharset(charset)
                        .withPattern(pattern).build();
    }

    /**
     * Encodes the message in the log event as text.
     *
     * @return the serialized message.
     */
    @Override
    public byte[] toByteArray(LogEvent event) {
        return layout.toByteArray(event);
    }

    @Override
    public String getContentType() {
        return "text";
    }
}
