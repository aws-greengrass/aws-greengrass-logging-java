/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.greengrass.logging.impl.config;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.encoder.EncoderBase;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy;
import ch.qos.logback.core.util.FileSize;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.event.Level;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PersistenceConfig groups the persistence configuration for monitoring data.
 */
@Getter
@SuppressFBWarnings("IS2_INCONSISTENT_SYNC")
public class PersistenceConfig {
    public static final String STORAGE_TYPE_SUFFIX = ".store";
    public static final String DATA_FORMAT_SUFFIX = ".fmt";
    public static final String TOTAL_STORE_SIZE_SUFFIX = ".file.sizeInKB";
    public static final String TOTAL_FILE_SIZE_SUFFIX = ".file.fileSizeInKB";
    public static final String STORE_NAME_SUFFIX = ".storeName";
    public static final String LOG_LEVEL_SUFFIX = ".level";
    public static final String APPENDER_PREFIX = "gg-";

    private static final long DEFAULT_MAX_SIZE_IN_KB = 1024 * 10L; // set 10 MB to be the default max size
    private static final int DEFAULT_MAX_FILE_SIZE_IN_KB = 1024; // set 1 MB to be the default max file size
    private static final int DEFAULT_FILE_ROLLOVER_TIME_MINS = 15; // set 15 mins.
    private static final String DEFAULT_STORAGE_TYPE = LogStore.CONSOLE.name();
    private static final String DEFAULT_DATA_FORMAT = LogFormat.TEXT.name();
    private static final String DEFAULT_STORE_NAME = "greengrass.";
    private static final String DEFAULT_LOG_LEVEL = Level.INFO.name();
    private static final String HOME_DIR_PREFIX = "~/";

    protected final String prefix;
    @Setter
    protected LogStore store;
    protected String storeName;
    protected Path storeDirectory;
    @Setter
    protected LogFormat format;
    @Setter
    protected Level level;
    protected long fileSizeKB;
    protected long totalLogStoreSizeKB;
    protected Logger logger;
    protected String fileName;
    private final Map<String, RollingFileAppender<ILoggingEvent>> logFileAppenders = new ConcurrentHashMap<>();
    private final Map<String, ConsoleAppender<ILoggingEvent>> logConsoleAppenders = new ConcurrentHashMap<>();

    /**
     * Get default PersistenceConfig from system properties.
     */
    public PersistenceConfig(String prefix) {
        this(prefix, "");
    }

    /**
     * Get default PersistenceConfig from system properties.
     *
     * @param prefix    The prefix for the config.
     * @param directory The directory in which the logs/metrics will be written to.
     */
    public PersistenceConfig(String prefix, String directory) {
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

        initializeStoreName(prefix, directory);
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
     *
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

    private void initializeStoreName(String prefix, String logsDirectory) {
        this.storeName = System.getProperty(prefix + STORE_NAME_SUFFIX,
                getRootStorePath().resolve(logsDirectory).resolve(DEFAULT_STORE_NAME + prefix).toString());
        getFileNameFromStoreName();
        getStoreDirectoryFromStoreName();
    }

    /**
     * Helper function to get the path.
     */
    protected Path getRootStorePath() {
        return Paths.get(deTilde(System.getProperty("root", System.getProperty("user.dir")))).toAbsolutePath();
    }

    protected void getFileNameFromStoreName() {
        Path fullFileName = Paths.get(this.storeName).getFileName();
        if (this.storeName != null && fullFileName != null) {
            this.storeName = deTilde(this.storeName);
            Optional<String> fileNameWithoutExtension = stripExtension(fullFileName.toString());
            this.fileName = fileNameWithoutExtension.orElseGet(() -> this.storeName);
        }
    }

    protected String deTilde(String path) {
        // Get path if "~/" is used
        if (path.startsWith(HOME_DIR_PREFIX)) {
            return Paths.get(System.getProperty("user.home"))
                    .resolve(path.substring(HOME_DIR_PREFIX.length())).toString();
        }
        return path;
    }

    protected void getStoreDirectoryFromStoreName() {
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

    protected synchronized void reconfigure(Logger loggerToConfigure) {
        reconfigure(loggerToConfigure, fileName + "." + prefix, totalLogStoreSizeKB, fileSizeKB);
    }

    void reconfigure(Logger loggerToConfigure, String fileName, long totalLogStoreSizeKB, long fileSizeKB) {
        Objects.requireNonNull(loggerToConfigure);
        logger = loggerToConfigure;
        // Set sub-loggers to inherit this config
        loggerToConfigure.setAdditive(false);
        // set backend logger level to trace because we'll be filtering it in the frontend
        loggerToConfigure.setLevel(ch.qos.logback.classic.Level.TRACE);
        // remove all default appenders
        loggerToConfigure.iteratorForAppenders().forEachRemaining(a -> {
            if (!a.getName().startsWith(APPENDER_PREFIX)) {
                loggerToConfigure.detachAppender(a);
                a.stop();
            }
        });
        if (LogStore.CONSOLE.equals(store)) {
            final ConsoleAppender<ILoggingEvent> originalAppender =
                    logConsoleAppenders.getOrDefault(loggerToConfigure.getName(), null);
            final RollingFileAppender<ILoggingEvent> fileAppender =
                    logFileAppenders.getOrDefault(loggerToConfigure.getName(), null);
            final ConsoleAppender<ILoggingEvent> newConsoleAppender =
                    getAppenderForConsole(loggerToConfigure, APPENDER_PREFIX + "console");
            newConsoleAppender.start();
            // Add the replacement
            loggerToConfigure.addAppender(newConsoleAppender);
            // Remove the original. These aren't atomic, but we won't be losing any logs
            if (originalAppender != null) {
                loggerToConfigure.detachAppender(originalAppender);
                originalAppender.stop();
            }
            if (fileAppender != null) {
                loggerToConfigure.detachAppender(fileAppender);
                fileAppender.stop();
            }
            logConsoleAppenders.put(loggerToConfigure.getName(), newConsoleAppender);
        } else if (LogStore.FILE.equals(store)) {
            final RollingFileAppender<ILoggingEvent> originalAppender =
                    logFileAppenders.getOrDefault(loggerToConfigure.getName(), null);
            final ConsoleAppender<ILoggingEvent> consoleAppender =
                    logConsoleAppenders.getOrDefault(loggerToConfigure.getName(), null);
            final RollingFileAppender<ILoggingEvent> newLogFileAppender = getAppenderForFile(loggerToConfigure,
                    APPENDER_PREFIX + loggerToConfigure.getName(),
                    storeDirectory.resolve(fileName).toString(), totalLogStoreSizeKB, fileSizeKB, fileName);
            newLogFileAppender.start();
            // Add the replacement
            loggerToConfigure.addAppender(newLogFileAppender);
            // Remove the original. These aren't atomic, but we won't be losing any logs
            if (originalAppender != null) {
                loggerToConfigure.detachAppender(originalAppender);
                originalAppender.stop();
            }
            if (consoleAppender != null) {
                loggerToConfigure.detachAppender(consoleAppender);
                consoleAppender.stop();
            }
            logFileAppenders.put(loggerToConfigure.getName(), newLogFileAppender);
        }
    }

    protected RollingFileAppender<ILoggingEvent> getAppenderForFile(Logger loggerToConfigure, String appenderName,
                                                                    String loggerStoreName, long totalLogStoreSizeKB,
                                                                    long fileSizeKB, String fileName) {
        LoggerContext logCtx = loggerToConfigure.getLoggerContext();
        BasicEncoder basicEncoder = new BasicEncoder();
        basicEncoder.setContext(logCtx);
        basicEncoder.start();
        RollingFileAppender<ILoggingEvent> fileAppender = new RollingFileAppender<>();
        fileAppender.setContext(logCtx);
        fileAppender.setName(appenderName);
        fileAppender.setAppend(true);
        fileAppender.setFile(loggerStoreName);
        fileAppender.setEncoder(basicEncoder);

        //TODO: Check how to make it rotate per x minutes.

        // Max History is needed along with total cap size.
        int maxHistory = Math.toIntExact((totalLogStoreSizeKB * FileSize.KB_COEFFICIENT)
                / (fileSizeKB * FileSize.KB_COEFFICIENT));
        SizeAndTimeBasedRollingPolicy<ILoggingEvent> logFilePolicy = new SizeAndTimeBasedRollingPolicy<>();
        logFilePolicy.setContext(logCtx);
        logFilePolicy.setParent(fileAppender);
        logFilePolicy.setTotalSizeCap(new FileSize(totalLogStoreSizeKB * FileSize.KB_COEFFICIENT));
        logFilePolicy.setFileNamePattern(storeDirectory.resolve(fileName + "_%d{yyyy_MM_dd_HH}_%i" + "." + prefix)
                .toString());
        logFilePolicy.setMaxFileSize(new FileSize(fileSizeKB * FileSize.KB_COEFFICIENT));
        logFilePolicy.setMaxHistory(maxHistory);
        logFilePolicy.start();

        fileAppender.setRollingPolicy(logFilePolicy);
        fileAppender.setTriggeringPolicy(logFilePolicy);
        return fileAppender;
    }

    protected ConsoleAppender<ILoggingEvent> getAppenderForConsole(Logger loggerToConfigure, String appenderName) {
        LoggerContext logCtx = loggerToConfigure.getLoggerContext();
        BasicEncoder basicEncoder = new BasicEncoder();
        basicEncoder.setContext(logCtx);
        basicEncoder.start();
        ConsoleAppender<ILoggingEvent> consoleAppender = new ConsoleAppender<>();
        consoleAppender.setContext(logCtx);
        consoleAppender.setName(appenderName);
        consoleAppender.setEncoder(basicEncoder);
        return consoleAppender;
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
