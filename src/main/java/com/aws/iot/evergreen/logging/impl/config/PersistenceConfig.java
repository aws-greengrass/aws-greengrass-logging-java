/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.logging.impl.config;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.encoder.EncoderBase;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy;
import ch.qos.logback.core.util.FileSize;
import lombok.Getter;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * PersistenceConfig groups the persistence configuration for monitoring data.
 */
@Getter
public class PersistenceConfig {
    public static final String STORAGE_TYPE_SUFFIX = ".store";
    public static final String DATA_FORMAT_SUFFIX = ".fmt";
    public static final String TOTAL_STORE_SIZE_SUFFIX = ".file.sizeInKB";
    public static final String NUM_ROLLING_FILES_SUFFIX = ".file.numRollingFiles";
    public static final String STORE_NAME_SUFFIX = ".storeName";

    private static final long DEFAULT_MAX_SIZE_IN_KB = 1024 * 10; // set 10 MB to be the default max size
    private static final String DEFAULT_STORAGE_TYPE = LogStore.CONSOLE.name();
    private static final String DEFAULT_DATA_FORMAT = LogFormat.TEXT.name();
    private static final int DEFAULT_NUM_ROLLING_FILES = 5;
    private static final String DEFAULT_STORE_NAME = "evergreen.";

    protected LogStore store;
    protected String storeName;
    protected LogFormat format;
    protected long fileSizeKB;
    protected int numRollingFiles;
    private RollingFileAppender logFileAppender = null;

    /**
     * Get default PersistenceConfig from system properties.
     */
    public PersistenceConfig(String prefix) {
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
            format = LogFormat.JSON;
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

        this.fileSizeKB = totalLogStoreSizeKB / numRollingFiles;

        if (store.equals(LogStore.FILE)) {
            initializeStoreName(prefix);
        }
    }

    private void initializeStoreName(String prefix) {
        Path storePath;

        String rootPathStr = System.getProperty("root");

        if (rootPathStr != null) {
            // if root is set, use root as store path
            storePath = Paths.get(rootPathStr);
        } else {
            // if root is not set, use working directory as store path
            storePath = Paths.get(System.getProperty("user.dir"));
        }
        this.storeName = System.getProperty(prefix + STORE_NAME_SUFFIX,
                storePath.resolve(DEFAULT_STORE_NAME + prefix).toAbsolutePath().toString());
    }

    protected void reconfigure(Logger loggerToConfigure) {
        LoggerContext logCtx = (LoggerContext) LoggerFactory.getILoggerFactory();

        BasicEncoder basicEncoder = new BasicEncoder();
        basicEncoder.setContext(logCtx);
        basicEncoder.start();

        // Set sub-loggers to inherit this config
        loggerToConfigure.setAdditive(true);
        // set backend logger level to trace because we'll be filtering it in the frontend
        loggerToConfigure.setLevel(ch.qos.logback.classic.Level.TRACE);
        // remove all default appenders
        loggerToConfigure.iteratorForAppenders().forEachRemaining((a) -> {
            if (!a.getName().startsWith("eg-")) {
                loggerToConfigure.detachAppender(a);
                a.stop();
            }
        });

        if (LogStore.CONSOLE.equals(store)) {
            ConsoleAppender logConsoleAppender = new ConsoleAppender();
            logConsoleAppender.setContext(logCtx);
            logConsoleAppender.setName("eg-console");
            logConsoleAppender.setEncoder(basicEncoder);
            logConsoleAppender.start();
            loggerToConfigure.addAppender(logConsoleAppender);
        } else if (LogStore.FILE.equals(store)) {
            final RollingFileAppender originalAppender = logFileAppender;

            logFileAppender = new RollingFileAppender();
            logFileAppender.setContext(logCtx);
            logFileAppender.setName("eg-file");
            logFileAppender.setAppend(true);
            logFileAppender.setFile(storeName);
            logFileAppender.setEncoder(basicEncoder);

            SizeAndTimeBasedRollingPolicy logFilePolicy = new SizeAndTimeBasedRollingPolicy();
            logFilePolicy.setContext(logCtx);
            logFilePolicy.setParent(logFileAppender);
            logFilePolicy.setFileNamePattern(storeName + "_%d{yyyy-MM-dd_HH}");
            logFilePolicy.setMaxHistory(numRollingFiles);
            logFilePolicy.setMaxFileSize(new FileSize(fileSizeKB * FileSize.KB_COEFFICIENT));
            logFilePolicy.start();

            logFileAppender.setRollingPolicy(logFilePolicy);
            logFileAppender.start();

            // Add the replacement
            loggerToConfigure.addAppender(logFileAppender);
            // Remove the original. These aren't atomic, but we won't be losing any logs
            loggerToConfigure.detachAppender(originalAppender);
            originalAppender.stop();
        }
    }

    private static class BasicEncoder extends EncoderBase<ILoggingEvent> {
        @Override
        public byte[] headerBytes() {
            return new byte[0];
        }

        @Override
        public byte[] encode(ILoggingEvent event) {
            return (event.getFormattedMessage() + System.lineSeparator()).getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public byte[] footerBytes() {
            return new byte[0];
        }
    }
}
