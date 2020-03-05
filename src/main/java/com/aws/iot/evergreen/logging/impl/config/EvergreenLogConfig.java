/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.logging.impl.config;

import lombok.Getter;
import org.apache.logging.log4j.Level;

@Getter
public class EvergreenLogConfig extends PersistenceConfig {
    // TODO: Replace reading from system properties with reading from Kernel Configuration.
    public static final String LOG_LEVEL_KEY = "log.level";
    private static final String DEFAULT_LOG_LEVEL = "INFO";
    private static final String DEFAULT_TEXT_LOG_PATTERN =
            "%d{yyyy MMM dd HH:mm:ss,SSS} %highlight{[%p]} (%t) %c: %m%n";
    public static final String CONFIG_PREFIX = "log";

    private Level level;

    /**
     * Create EvergreenLogConfig instance.
     *
     * @param level           log level
     * @param store           log storage option
     * @param format          log output format
     * @param fileSize        max log size to persist per rolling file
     * @param numRollingFiles number of files to keep rolling
     * @param pattern         Log4j text output pattern
     */
    public EvergreenLogConfig(Level level, LogStore store, String storeName, LogFormat format, String fileSize,
                              int numRollingFiles, String pattern) {
        super(store, storeName, format, fileSize, numRollingFiles, pattern);
        this.level = level;
    }

    /**
     * Get default logging configuration from system properties.
     */
    public EvergreenLogConfig() {
        super(CONFIG_PREFIX, DEFAULT_TEXT_LOG_PATTERN);

        this.level = Level.getLevel(System.getProperty(LOG_LEVEL_KEY, DEFAULT_LOG_LEVEL));
    }
}
