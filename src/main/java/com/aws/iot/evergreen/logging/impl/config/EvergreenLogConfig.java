/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.logging.impl.config;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.event.Level;

@Getter
public class EvergreenLogConfig extends PersistenceConfig {
    // TODO: Replace reading from system properties with reading from Kernel Configuration.
    public static final String LOG_LEVEL_KEY = "log.level";
    private static final String DEFAULT_LOG_LEVEL = "INFO";
    public static final String CONFIG_PREFIX = "log";
    private static final LoggerContext context = new LoggerContext();

    @Setter
    private Level level;

    private static final EvergreenLogConfig INSTANCE = new EvergreenLogConfig();

    public static EvergreenLogConfig getInstance() {
        return INSTANCE;
    }

    /**
     * Get default logging configuration from system properties.
     */
    protected EvergreenLogConfig() {
        super(CONFIG_PREFIX);

        this.level = Level.valueOf(System.getProperty(LOG_LEVEL_KEY, DEFAULT_LOG_LEVEL));
        reconfigure(context.getLogger(Logger.ROOT_LOGGER_NAME));
    }

    public Logger getLogger(String name) {
        return context.getLogger(name);
    }
}
