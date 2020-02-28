/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.aws.iot.evergreen.logging.impl.config;

import lombok.Getter;

/**
 * PersistenceConfig groups the persistence configuration for monitoring data.
 */
@Getter
public class PersistenceConfig {
    public static final String STORAGE_TYPE_SUFFIX = ".store";
    public static final String DATA_FORMAT_SUFFIX = ".fmt";
    public static final String TOTAL_STORE_SIZE_SUFFIX = ".file.sizeInKB";
    public static final String NUM_ROLLING_FILES_SUFFIX = ".file.numRollingFiles";
    public static final String TEXT_PATTERN_SUFFIX = ".pattern";
    public static final String STORE_NAME_SUFFIX = ".storeName";

    private static final long DEFAULT_MAX_SIZE_IN_KB = 1024 * 10; // set 10 MB to be the default max size
    private static final String DEFAULT_STORAGE_TYPE = LogStore.FILE.name();
    private static final String DEFAULT_DATA_FORMAT = LogFormat.JSON.name();
    private static final int DEFAULT_NUM_ROLLING_FILES = 5;
    private static final String DEFAULT_STORE_NAME = "evergreen.";

    protected LogStore store;
    protected String storeName;
    protected LogFormat format;
    protected String fileSizeKB;
    protected int numRollingFiles;
    protected String pattern;

    /**
     * Create PersistenceConfig instance.
     *
     * @param store           data storage option
     * @param format          data output format
     * @param fileSize        max file size to persist per rolling file
     * @param numRollingFiles number of files to keep rolling
     * @param pattern         Log4j text output pattern
     */
    public PersistenceConfig(LogStore store, String storeName, LogFormat format, String fileSize, int numRollingFiles,
                             String pattern) {
        this.store = store;
        this.storeName = storeName;
        this.format = format;
        this.fileSizeKB = fileSize;
        this.numRollingFiles = numRollingFiles;
        this.pattern = pattern;
    }

    /**
     * Get default PersistenceConfig from system properties.
     */
    public PersistenceConfig(String prefix, String defaultPattern) {
        LogStore store;
        try {
            store = LogStore.valueOf(System.getProperty(prefix + STORAGE_TYPE_SUFFIX, DEFAULT_STORAGE_TYPE));
        } catch (IllegalArgumentException e) {
            store = LogStore.FILE;
        }
        this.store = store;

        LogFormat format;
        try {
            format = LogFormat.valueOf(System.getProperty(prefix + DATA_FORMAT_SUFFIX, DEFAULT_DATA_FORMAT));
        } catch (IllegalArgumentException e) {
            format = LogFormat.CBOR;
        }
        this.format = format;

        long totalLogStoreSizeKB;
        try {
            totalLogStoreSizeKB = Long.parseLong(System.getProperty(prefix + TOTAL_STORE_SIZE_SUFFIX));
        } catch (NumberFormatException e) {
            totalLogStoreSizeKB = DEFAULT_MAX_SIZE_IN_KB;
        }

        int numRollingFiles;
        try {
            numRollingFiles = Integer.parseInt(System.getProperty(prefix + NUM_ROLLING_FILES_SUFFIX));
        } catch (NumberFormatException e) {
            numRollingFiles = DEFAULT_NUM_ROLLING_FILES;
        }
        this.numRollingFiles = numRollingFiles;

        this.fileSizeKB = Long.toString(totalLogStoreSizeKB / numRollingFiles);

        this.storeName = System.getProperty(prefix + STORE_NAME_SUFFIX, DEFAULT_STORE_NAME + prefix);
        this.pattern = System.getProperty(prefix + TEXT_PATTERN_SUFFIX, defaultPattern);
    }
}
