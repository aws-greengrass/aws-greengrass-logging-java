/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.greengrass.logging.impl;

import com.aws.greengrass.logging.api.Logger;
import com.aws.greengrass.logging.impl.config.LogStore;
import com.aws.greengrass.logging.impl.config.model.LogConfigUpdate;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.io.FileMatchers.aFileNamed;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class FileLoggerTest {
    @TempDir
    static Path tempRootDir;
    private Logger logger;

    @BeforeAll
    static void setLogger() {
        LogManager.getRootLogConfiguration().setStore(LogStore.FILE);
    }

    @AfterAll
    static void cleanupLogger() {
        LogManager.getRootLogConfiguration().setStore(LogStore.CONSOLE);
    }

    @Test
    void GIVEN_root_logger_WHEN_get_logger_THEN_greengrass_log_file_is_created() throws IOException {
        String randomFolder = UUID.randomUUID().toString();
        String randomString = UUID.randomUUID().toString();
        logger = LogManager.getLogger(FileLoggerTest.class);
        LogManager.setRoot(tempRootDir.resolve(randomFolder).toAbsolutePath());

        File logFile = new File(LogManager.getRootLogConfiguration().getStoreName());
        this.logger.atInfo().log(randomString + "Something");
        MatcherAssert.assertThat(logFile, aFileNamed(equalToIgnoringCase("greengrass.log")));
        assertTrue(logFile.length() > 0);
        try (Stream<String> lines = Files.lines(Paths.get(LogManager.getRootLogConfiguration().getStoreName()))) {
            assertTrue(lines.allMatch(s -> s.contains(randomString + "Something")));
        }
    }

    @Test
    void GIVEN_root_logger_child_WHEN_get_logger_THEN_greengrass_log_file_is_created() throws IOException {
        String randomFolder = UUID.randomUUID().toString();
        String randomString = UUID.randomUUID().toString();
        logger = LogManager.getLogger(FileLoggerTest.class);
        LogManager.setRoot(tempRootDir.resolve(randomFolder).toAbsolutePath());
        Logger logger2 = logger.createChild();
        File logFile = new File(LogManager.getRootLogConfiguration().getStoreName());
        logger2.atInfo().log(randomString + "Nothing");
        MatcherAssert.assertThat(logFile, aFileNamed(equalToIgnoringCase("greengrass.log")));
        assertTrue(logFile.length() > 0);
        try (Stream<String> lines = Files.lines(Paths.get(logFile.toURI()))) {
            assertTrue(lines.anyMatch(s -> s.contains(randomString + "Nothing")));
        }
    }

    @Test
    void GIVEN_new_logger_with_config_WHEN_get_logger_THEN_correct_log_file_is_created() throws IOException {
        String randomFolder = UUID.randomUUID().toString();
        String randomLogFileName = UUID.randomUUID().toString() + ".log";
        String randomLoggerName = UUID.randomUUID().toString();
        String randomString = UUID.randomUUID().toString();
        LogManager.setRoot(tempRootDir.resolve(randomFolder).toAbsolutePath());
        Logger logger2 = LogManager.getLogger(randomLoggerName, LogConfigUpdate.builder()
                .fileName(randomLogFileName)
                .build());

        logger2.atInfo().log(randomString + "Something");
        String filePath = LogManager.getLogConfigurations().get(randomLoggerName).getStoreDirectory()
                .resolve(randomLogFileName).toAbsolutePath().toString();
        File logFile = new File(filePath);
        MatcherAssert.assertThat(logFile, aFileNamed(equalToIgnoringCase(randomLogFileName)));
        assertTrue(logFile.length() > 0);
        try (Stream<String> lines = Files.lines(Paths.get(filePath))) {
            assertTrue(lines.allMatch(s -> s.contains(randomString + "Something")));
        }
        if (Files.exists(Paths.get(LogManager.getRootLogConfiguration().getStoreName()))) {
            try (Stream<String> lines = Files.lines(Paths.get(LogManager.getRootLogConfiguration().getStoreName()))) {
                assertTrue(lines.noneMatch(s -> s.contains(randomString + "Something")
                        || s.contains(randomString + "Nothing")));
            }
        }
    }

    @Test
    void GIVEN_new_logger_child_with_config_WHEN_get_logger_THEN_correct_log_file_is_created() throws IOException {
        String randomFolder = UUID.randomUUID().toString();
        String randomLogFileName = UUID.randomUUID().toString() + ".log";
        String randomLoggerName = UUID.randomUUID().toString();
        String randomString = UUID.randomUUID().toString();
        Logger logger2 = LogManager.getLogger(randomLoggerName, LogConfigUpdate.builder()
                .fileName(randomLogFileName)
                .build());
        Logger logger2Child = logger2.createChild();
        LogManager.setRoot(tempRootDir.resolve(randomFolder).toAbsolutePath());
        logger2.atInfo().log(randomString + "Something");
        String filePath = LogManager.getLogConfigurations().get(randomLoggerName).getStoreDirectory()
                .resolve(randomLogFileName).toAbsolutePath().toString();

        File logFile = new File(filePath);
        MatcherAssert.assertThat(logFile, aFileNamed(equalToIgnoringCase(randomLogFileName)));

        logger2Child.atInfo().log(randomString + "Nothing");
        assertTrue(logFile.length() > 0);
        try (Stream<String> lines = Files.lines(Paths.get(filePath))) {
            assertTrue(lines.anyMatch(s -> s.contains(randomString + "Something")));
        }
        try (Stream<String> lines = Files.lines(Paths.get(filePath))) {
            assertTrue(lines.anyMatch(s -> s.contains(randomString + "Nothing")));
        }
        if (Files.exists(Paths.get(LogManager.getRootLogConfiguration().getStoreName()))) {
            try (Stream<String> lines = Files.lines(Paths.get(LogManager.getRootLogConfiguration().getStoreName()))) {
                assertTrue(lines.noneMatch(s -> s.contains(randomString + "Something")
                        || s.contains(randomString + "Nothing")));
            }
        }
    }

    @Test
    void GIVEN_2_loggers_WHEN_update_root_THEN_all_loggers_root_path_updated() throws IOException {
        String randomFolder = UUID.randomUUID().toString();
        String randomFolder2 = UUID.randomUUID().toString();
        String randomString = UUID.randomUUID().toString();
        String randomLogFileName = UUID.randomUUID().toString() + ".log";
        String randomLoggerName = UUID.randomUUID().toString();

        logger = LogManager.getLogger(FileLoggerTest.class);
        Logger logger2 = LogManager.getLogger(randomLoggerName, LogConfigUpdate.builder()
                .fileName(randomLogFileName)
                .build());

        LogManager.setRoot(tempRootDir.resolve(randomFolder).toAbsolutePath());
        File logFile = new File(LogManager.getRootLogConfiguration().getStoreName());
        logger.atInfo().log(randomString + "Something");

        MatcherAssert.assertThat(logFile, aFileNamed(equalToIgnoringCase("greengrass.log")));
        assertTrue(logFile.length() > 0);
        try (Stream<String> lines = Files.lines(Paths.get(logFile.toURI()))) {
            assertTrue(lines.anyMatch(s -> s.contains(randomString + "Something")));
        }
        logger2.atInfo().log(randomString + "Something");
        String filePath = LogManager.getLogConfigurations().get(randomLoggerName).getStoreDirectory()
                .resolve(randomLogFileName).toAbsolutePath().toString();
        File logFile2 = new File(filePath);
        MatcherAssert.assertThat(logFile2, aFileNamed(equalToIgnoringCase(randomLogFileName)));
        assertTrue(logFile2.length() > 0);
        try (Stream<String> lines = Files.lines(Paths.get(logFile2.toURI()))) {
            assertTrue(lines.allMatch(s -> s.contains(randomString + "Something")));
        }
        LogManager.setRoot(tempRootDir.resolve(randomFolder2));
        logger.atInfo().log(randomString + "SomeOtherThing");
        logger2 = LogManager.getLogger(randomLoggerName, LogConfigUpdate.builder()
                .fileName(randomLogFileName)
                .build());
        logger2.atInfo().log(randomString + "SomeOtherThing");

        File logFile3 = new File(LogManager.getRootLogConfiguration().getStoreName());
        MatcherAssert.assertThat(logFile3, aFileNamed(equalToIgnoringCase("greengrass.log")));
        assertTrue(logFile3.length() > 0);
        try (Stream<String> lines = Files.lines(Paths.get(logFile3.toURI()))) {
            assertTrue(lines.allMatch(s -> s.contains(randomString + "SomeOtherThing")));
        }
        String filePath2 = LogManager.getLogConfigurations().get(randomLoggerName).getStoreDirectory()
                .resolve(randomLogFileName).toAbsolutePath().toString();

        File logFile4 = new File(filePath2);
        MatcherAssert.assertThat(logFile4, aFileNamed(equalToIgnoringCase(randomLogFileName)));
        assertTrue(logFile4.length() > 0);
        try (Stream<String> lines = Files.lines(Paths.get(filePath2))) {
            assertTrue(lines.allMatch(s -> s.contains(randomString + "SomeOtherThing")));
        }
    }
}
