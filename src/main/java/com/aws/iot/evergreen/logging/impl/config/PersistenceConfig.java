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
import lombok.Setter;
import org.slf4j.event.Level;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;

/**
 * PersistenceConfig groups the persistence configuration for monitoring data.
 */
@Getter
public class PersistenceConfig {
    public static final String STORAGE_TYPE_SUFFIX = ".store";
    public static final String DATA_FORMAT_SUFFIX = ".fmt";
    public static final String TOTAL_STORE_SIZE_SUFFIX = ".file.sizeInKB";
    public static final String TOTAL_FILE_SIZE_SUFFIX = ".file.fileSizeInKB";
    public static final String STORE_NAME_SUFFIX = ".storeName";
    public static final String LOG_LEVEL_SUFFIX = ".level";

    private static final long DEFAULT_MAX_SIZE_IN_KB = 1024 * 10; // set 10 MB to be the default max size
    private static final int DEFAULT_MAX_FILE_SIZE_IN_KB = 1024; // set 1 MB to be the default max file size
    private static final int DEFAULT_FILE_ROLLOVER_TIME_MINS = 15; // set 15 mins.
    private static final String DEFAULT_STORAGE_TYPE = LogStore.CONSOLE.name();
    private static final String DEFAULT_DATA_FORMAT = LogFormat.TEXT.name();
    private static final String DEFAULT_STORE_NAME = "evergreen.";
    private static final String DEFAULT_LOG_LEVEL = Level.INFO.name();

    protected final String prefix;
    protected LogStore store;
    protected String storeName;
    protected Path storeDirectory;
    @Setter
    protected String fileName;
    @Setter
    protected LogFormat format;
    @Setter
    protected Level level;
    protected long fileSizeKB;
    protected long totalLogStoreSizeKB;
    private RollingFileAppender<ILoggingEvent> logFileAppender = null;
    private ConsoleAppender<ILoggingEvent> logConsoleAppender = null;
    protected Logger logger;

    /**
     * Get default PersistenceConfig from system properties.
     */
    public PersistenceConfig(String prefix) {
        this.prefix = prefix;
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

        Level level;
        try {
            level = Level.valueOf(System.getProperty(prefix + LOG_LEVEL_SUFFIX, DEFAULT_LOG_LEVEL));
        } catch (IllegalArgumentException e) {
            level = Level.INFO;
        }

        this.level = level;

        int fileSizeKB;
        try {
            fileSizeKB = Integer.parseInt(System.getProperty(prefix + TOTAL_FILE_SIZE_SUFFIX));
        } catch (NumberFormatException e) {
            fileSizeKB = DEFAULT_MAX_FILE_SIZE_IN_KB;
        }

        this.fileSizeKB = fileSizeKB;
        this.totalLogStoreSizeKB = totalLogStoreSizeKB;

        initializeStoreName(prefix);
    }

    /**
     * Change the configured store type.
     *
     * @param store new store type
     */
    public void setStoreType(LogStore store) {
        if (Objects.equals(this.store, store)) {
            return;
        }
        this.store = store;
        reconfigure();
    }

    /**
     * Change the configured store path (only applies for file output).
     * @param path The path passed in must contain the file name to which the logs will be written.
     */
    public void setStorePath(Path path) {
        String newStoreName = getRootStorePath().resolve(path).toAbsolutePath().toString();
        if (Objects.equals(this.storeName, newStoreName)) {
            return;
        }
        this.storeName = newStoreName;
        getFileNameFromStoreName();
        getStoreDirectoryFromStoreName();
        reconfigure();
    }

    /**
     * Change the configured max file size in KB before rolling over (only applies for file output).
     *
     * @param fileSizeKB new file size in KB
     */
    public void setFileSizeKB(long fileSizeKB) {
        if (Objects.equals(this.fileSizeKB, fileSizeKB)) {
            return;
        }
        this.fileSizeKB = fileSizeKB;
        reconfigure();
    }

    private void initializeStoreName(String prefix) {
        this.storeName = System.getProperty(prefix + STORE_NAME_SUFFIX,
                getRootStorePath().resolve(DEFAULT_STORE_NAME + prefix).toString());
        getFileNameFromStoreName();
        getStoreDirectoryFromStoreName();
    }

    /**
     * Helper function to get the path.
     */
    private Path getRootStorePath() {
        Path storePath;
        String rootPathStr = System.getProperty("root");
        if (rootPathStr != null) {
            // if root is set, use root as store path
            storePath = Paths.get(rootPathStr);
        } else {
            // if root is not set, use working directory as store path
            storePath = Paths.get(System.getProperty("user.dir"));
        }
        return storePath.toAbsolutePath();
    }

    private void getFileNameFromStoreName() {
        Path fullFileName = Paths.get(this.storeName).getFileName();
        if (this.storeName != null && fullFileName != null) {
            Optional<String> fileNameWithoutExtension = stripExtension(fullFileName.toString());
            this.fileName = fileNameWithoutExtension.orElseGet(() -> this.storeName);
        }
    }

    private void getStoreDirectoryFromStoreName() {
        if (this.storeName != null) {
            Path storeDirectoryPath = Paths.get(this.storeName).getParent();
            if (storeDirectoryPath != null) {
                this.storeDirectory = storeDirectoryPath.toAbsolutePath();
            }
        }
    }

    private Optional<String> stripExtension(String fileName) {
        // Handle null case specially.
        if (fileName == null) {
            return Optional.empty();
        }
        // Get position of last '.'.
        int pos = fileName.lastIndexOf(".");
        // If there wasn't any '.' just return the string as is.
        if (pos == -1) {
            return Optional.of(fileName);
        }
        // Otherwise return the string, up to the dot.
        return Optional.of(fileName.substring(0, pos));
    }

    protected void reconfigure() {
        reconfigure(logger);
    }

    protected void reconfigure(Logger loggerToConfigure) {
        Objects.requireNonNull(loggerToConfigure);

        logger = loggerToConfigure;
        LoggerContext logCtx = loggerToConfigure.getLoggerContext();

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
            final ConsoleAppender<ILoggingEvent> originalAppender = logConsoleAppender;
            final RollingFileAppender<ILoggingEvent> fileAppender = logFileAppender;
            logConsoleAppender = new ConsoleAppender<>();
            logConsoleAppender.setContext(logCtx);
            logConsoleAppender.setName("eg-console");
            logConsoleAppender.setEncoder(basicEncoder);
            logConsoleAppender.start();

            // Add the replacement
            loggerToConfigure.addAppender(logConsoleAppender);
            // Remove the original. These aren't atomic, but we won't be losing any logs
            if (originalAppender != null) {
                loggerToConfigure.detachAppender(originalAppender);
                originalAppender.stop();
            }
            if (fileAppender != null) {
                loggerToConfigure.detachAppender(fileAppender);
                fileAppender.stop();
            }
        } else if (LogStore.FILE.equals(store)) {
            final RollingFileAppender<ILoggingEvent> originalAppender = logFileAppender;
            final ConsoleAppender<ILoggingEvent> consoleAppender = logConsoleAppender;

            logFileAppender = new RollingFileAppender<>();
            logFileAppender.setContext(logCtx);
            logFileAppender.setName("eg-file");
            logFileAppender.setAppend(true);
            logFileAppender.setFile(storeName);
            logFileAppender.setEncoder(basicEncoder);

            //TODO: Check how to make it rotate per x minutes.
            SizeAndTimeBasedRollingPolicy<ILoggingEvent> logFilePolicy = new SizeAndTimeBasedRollingPolicy<>();
            logFilePolicy.setContext(logCtx);
            logFilePolicy.setParent(logFileAppender);
            logFilePolicy.setTotalSizeCap(new FileSize(totalLogStoreSizeKB * FileSize.KB_COEFFICIENT));
            logFilePolicy.setFileNamePattern(storeDirectory.resolve(fileName + "_%d{yyyy_MM_dd_HH}_%i" + "." + prefix)
                    .toString());
            logFilePolicy.setMaxFileSize(new FileSize(fileSizeKB * FileSize.KB_COEFFICIENT));
            logFilePolicy.start();

            logFileAppender.setRollingPolicy(logFilePolicy);
            logFileAppender.setTriggeringPolicy(logFilePolicy);
            logFileAppender.start();

            // Add the replacement
            loggerToConfigure.addAppender(logFileAppender);
            // Remove the original. These aren't atomic, but we won't be losing any logs
            if (originalAppender != null) {
                loggerToConfigure.detachAppender(originalAppender);
                originalAppender.stop();
            }
            if (consoleAppender != null) {
                loggerToConfigure.detachAppender(consoleAppender);
                consoleAppender.stop();
            }
        }
    }

    public static class BasicEncoder extends EncoderBase<ILoggingEvent> {
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