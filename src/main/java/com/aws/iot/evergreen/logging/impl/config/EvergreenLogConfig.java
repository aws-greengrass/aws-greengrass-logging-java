/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.logging.impl.config;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import lombok.Getter;
import org.slf4j.event.Level;

@Getter
public class EvergreenLogConfig extends PersistenceConfig {
    // TODO: Replace the default log level from Kernel Configuration.
    private static final String DEFAULT_LOG_LEVEL = "INFO";
    public static final String CONFIG_PREFIX = "log";
    private static final LoggerContext context = new LoggerContext();


    private static final EvergreenLogConfig INSTANCE = new EvergreenLogConfig();

    public static EvergreenLogConfig getInstance() {
        return INSTANCE;
    }

    /**
     * Get default logging configuration from system properties.
     */
    protected EvergreenLogConfig() {
        super(CONFIG_PREFIX);
        this.setLevel(Level.valueOf(DEFAULT_LOG_LEVEL));
        reconfigure(context.getLogger(Logger.ROOT_LOGGER_NAME));
    }

    public Logger getLogger(String name) {
        return context.getLogger(name);
    }
}
