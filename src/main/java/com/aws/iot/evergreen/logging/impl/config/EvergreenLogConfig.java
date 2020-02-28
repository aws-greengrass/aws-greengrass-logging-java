/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.aws.iot.evergreen.logging.impl.config;

import lombok.Getter;
import org.apache.logging.log4j.Level;

@Getter
public class EvergreenLogConfig {
    // TODO: Replace reading from system properties with reading from Kernel Configuration.
    private static final long DEFAULT_MAX_SIZE_IN_KB = 1024 * 10; // set 10 MB to be the default max size
    public static final String STORAGE_TYPE_KEY = "log.store";
    private static final String DEFAULT_STORAGE_TYPE = LogStore.FILE.name();
    public static final String LOG_FORMAT_KEY = "log.fmt";
    private static final String DEFAULT_LOG_FORMAT = LogFormat.CBOR.name();
    public static final String TOTAL_LOG_STORE_SIZE_KEY = "log.file.sizeInKB";
    public static final String LOG_LEVEL_KEY = "log.level";
    private static final String DEFAULT_LOG_LEVEL = "INFO";
    public static final String LOG_STORE_NAME_KEY = "log.storeName";
    private static final String DEFAULT_LOG_STORE_NAME = "evergreen.log";
    public static final String NUM_ROLLING_FILES_KEY = "log.store.numRollingFiles";
    private static final int DEFAULT_NUM_ROLLING_FILES = 5;
    public static final String TEXT_LOG_PATTERN_KEY = "log.pattern";
    private static final String DEFAULT_TEXT_LOG_PATTERN =
            "%d{yyyy MMM dd HH:mm:ss,SSS} %highlight{[%p]} (%t) %c: %m%n";

    private Level level;
    private LogStore store;
    private String storeName;
    private LogFormat format;
    private String fileSizeKB;
    private int numRollingFiles;
    private String pattern;

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
        this.level = level;
        this.store = store;
        this.storeName = storeName;
        this.format = format;
        this.fileSizeKB = fileSize;
        this.numRollingFiles = numRollingFiles;
        this.pattern = pattern;
    }

    /**
     * Load logging configuration from system properties.
     *
     * @return EvergreenLogConfig instance
     */
    public static EvergreenLogConfig loadDefaultConfig() {
        LogStore store;
        try {
            store = LogStore.valueOf(System.getProperty(STORAGE_TYPE_KEY, DEFAULT_STORAGE_TYPE));
        } catch (IllegalArgumentException e) {
            store = LogStore.FILE;
        }

        LogFormat format;
        try {
            format = LogFormat.valueOf(System.getProperty(LOG_FORMAT_KEY, DEFAULT_LOG_FORMAT));
        } catch (IllegalArgumentException e) {
            format = LogFormat.CBOR;
        }

        long totalLogStoreSizeKB;
        try {
            totalLogStoreSizeKB = Long.parseLong(System.getProperty(TOTAL_LOG_STORE_SIZE_KEY));
        } catch (NumberFormatException e) {
            totalLogStoreSizeKB = DEFAULT_MAX_SIZE_IN_KB;
        }

        int numRollingFiles;
        try {
            numRollingFiles = Integer.parseInt(System.getProperty(NUM_ROLLING_FILES_KEY));
        } catch (NumberFormatException e) {
            numRollingFiles = DEFAULT_NUM_ROLLING_FILES;
        }
        String fileSizeKB = Long.toString(totalLogStoreSizeKB / numRollingFiles);

        Level level = Level.getLevel(System.getProperty(LOG_LEVEL_KEY, DEFAULT_LOG_LEVEL));
        String storeName = System.getProperty(LOG_STORE_NAME_KEY, DEFAULT_LOG_STORE_NAME);
        String pattern = System.getProperty(TEXT_LOG_PATTERN_KEY, DEFAULT_TEXT_LOG_PATTERN);

        return new EvergreenLogConfig(level, store, storeName, format, fileSizeKB, numRollingFiles, pattern);
    }
}
