/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.greengrass.logging.impl.config;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import lombok.Getter;

@Getter
public class LogConfig extends PersistenceConfig {
    // TODO: Replace the default log level from Kernel Configuration.
    public static final String CONFIG_PREFIX = "log";
    private static final LoggerContext context = new LoggerContext();


    private static final LogConfig INSTANCE = new LogConfig();

    public static LogConfig getInstance() {
        return INSTANCE;
    }

    /**
     * Get default logging configuration from system properties.
     */
    protected LogConfig() {
        super(CONFIG_PREFIX);
        reconfigure(context.getLogger(Logger.ROOT_LOGGER_NAME));
    }

    public Logger getLogger(String name) {
        return context.getLogger(name);
    }
}
