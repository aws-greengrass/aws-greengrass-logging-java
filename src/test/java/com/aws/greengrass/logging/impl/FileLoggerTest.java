package com.aws.greengrass.logging.impl;

import com.aws.greengrass.logging.api.Logger;
import com.aws.greengrass.logging.impl.config.LogStore;
import com.aws.greengrass.logging.impl.config.model.LoggerConfiguration;
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
        String randomLogFileName = UUID.randomUUID().toString() + ".log";
        LogManager.getRootLogConfiguration().setStorePath(tempRootDir.resolve("logs").resolve(randomLogFileName)
                .toAbsolutePath());
        logger = LogManager.getLogger(FileLoggerTest.class);
        File logFile = new File(LogManager.getRootLogConfiguration().getStoreName());
        logger.atInfo().log("Something");
        MatcherAssert.assertThat(logFile, aFileNamed(equalToIgnoringCase(randomLogFileName)));
        try (Stream<String> lines = Files.lines(Paths.get(LogManager.getRootLogConfiguration().getStoreName()))) {
            assertTrue(lines.anyMatch(s -> s.contains("Something")));
        }
    }

    @Test
    void GIVEN_root_logger_child_WHEN_get_logger_THEN_greengrass_log_file_is_created() throws IOException {
        String randomLogFileName = UUID.randomUUID().toString() + ".log";
        LogManager.getRootLogConfiguration().setStorePath(tempRootDir.resolve("logs").resolve(randomLogFileName)
                .toAbsolutePath());
        logger = LogManager.getLogger(FileLoggerTest.class);
        Logger logger2 = logger.createChild();
        File logFile = new File(LogManager.getRootLogConfiguration().getStoreName());
        logger2.atInfo().log("Nothing");
        MatcherAssert.assertThat(logFile, aFileNamed(equalToIgnoringCase(randomLogFileName)));
        try (Stream<String> lines = Files.lines(Paths.get(LogManager.getRootLogConfiguration().getStoreName()))) {
            assertTrue(lines.anyMatch(s -> s.contains("Nothing")));
        }
    }

    @Test
    void GIVEN_new_logger_with_config_WHEN_get_logger_THEN_correct_log_file_is_created() throws IOException {
        String randomLogFileName = UUID.randomUUID().toString() + ".log";
        String randomLoggerName = UUID.randomUUID().toString();
        LogManager.getRootLogConfiguration().setStorePath(tempRootDir.resolve("logs").resolve("greengrass.log")
                .toAbsolutePath());
        logger = LogManager.getLogger(FileLoggerTest.class);
        Logger logger2 = LogManager.getLogger(randomLoggerName, LoggerConfiguration.builder()
                .fileName(randomLogFileName)
                .build());
        logger2.atInfo().log("Something");
        String filePath = LogManager.getLogConfigurations().get(randomLoggerName).getStoreDirectory()
                .resolve(randomLogFileName).toAbsolutePath().toString();
        File logFile = new File(filePath);
        MatcherAssert.assertThat(logFile, aFileNamed(equalToIgnoringCase(randomLogFileName)));
        try (Stream<String> lines = Files.lines(Paths.get(filePath))) {
            assertTrue(lines.allMatch(s -> s.contains("Something")));
        }
        if (Files.exists(Paths.get(LogManager.getRootLogConfiguration().getStoreName()))) {
            try (Stream<String> lines = Files.lines(Paths.get(LogManager.getRootLogConfiguration().getStoreName()))) {
                assertTrue(lines.noneMatch(s -> s.contains("Something") || s.contains("Nothing")));
            }
        }
    }

    @Test
    void GIVEN_new_logger_child_with_config_WHEN_get_logger_THEN_correct_log_file_is_created() throws IOException {
        String randomLogFileName = UUID.randomUUID().toString() + ".log";
        String randomLoggerName = UUID.randomUUID().toString();
        LogManager.getRootLogConfiguration().setStorePath(tempRootDir.resolve("logs").resolve("greengrass.log")
                .toAbsolutePath());
        logger = LogManager.getLogger(FileLoggerTest.class);
        Logger logger2 = LogManager.getLogger(randomLoggerName, LoggerConfiguration.builder()
                .fileName(randomLogFileName)
                .build());
        Logger logger2Child = logger2.createChild();
        logger2.atInfo().log("Something");
        String filePath = LogManager.getLogConfigurations().get(randomLoggerName).getStoreDirectory()
                .resolve(randomLogFileName).toAbsolutePath().toString();

        File logFile = new File(filePath);
        MatcherAssert.assertThat(logFile, aFileNamed(equalToIgnoringCase(randomLogFileName)));

        logger2Child.atInfo().log("Nothing");
        try (Stream<String> lines = Files.lines(Paths.get(filePath))) {
            assertTrue(lines.anyMatch(s -> s.contains("Something")));
        }
        try (Stream<String> lines = Files.lines(Paths.get(filePath))) {
            assertTrue(lines.anyMatch(s -> s.contains("Nothing")));
        }
        if (Files.exists(Paths.get(LogManager.getRootLogConfiguration().getStoreName()))) {
            try (Stream<String> lines = Files.lines(Paths.get(LogManager.getRootLogConfiguration().getStoreName()))) {
                assertTrue(lines.noneMatch(s -> s.contains("Something") || s.contains("Nothing")));
            }
        }
    }

    @Test
    void GIVEN_2_loggers_WHEN_update_root_THEN_all_loggers_root_path_updated() throws IOException {
        String randomFolder = UUID.randomUUID().toString();
        String randomFolder2 = UUID.randomUUID().toString();
        LogManager.getRootLogConfiguration().setStorePath(tempRootDir.resolve(randomFolder).resolve("logs")
                .resolve("greengrass.log")
                .toAbsolutePath());

        logger = LogManager.getLogger(FileLoggerTest.class);
        File logFile = new File(LogManager.getRootLogConfiguration().getStoreName());
        logger.atInfo().log("Something");
        MatcherAssert.assertThat(logFile, aFileNamed(equalToIgnoringCase("greengrass.log")));
        try (Stream<String> lines = Files.lines(Paths.get(LogManager.getRootLogConfiguration().getStoreName()))) {
            assertTrue(lines.anyMatch(s -> s.contains("Something")));
        }
        String randomLogFileName = UUID.randomUUID().toString() + ".log";
        String randomLoggerName = UUID.randomUUID().toString();
        Logger logger2 = LogManager.getLogger(randomLoggerName, LoggerConfiguration.builder()
                .fileName(randomLogFileName)
                .build());
        logger2.atInfo().log("Something");
        String filePath = LogManager.getLogConfigurations().get(randomLoggerName).getStoreDirectory()
                .resolve(randomLogFileName).toAbsolutePath().toString();
        File logFile2 = new File(filePath);
        MatcherAssert.assertThat(logFile2, aFileNamed(equalToIgnoringCase(randomLogFileName)));
        try (Stream<String> lines = Files.lines(Paths.get(filePath))) {
            assertTrue(lines.allMatch(s -> s.contains("Something")));
        }

        LogManager.setRoot(tempRootDir.resolve(randomFolder2));
        logger.atInfo().log("SomeOtherThing");
        logger2.atInfo().log("SomeOtherThing");

        File logFile3 = new File(LogManager.getRootLogConfiguration().getStoreName());
        assertTrue(logFile3.exists());
        try (Stream<String> lines = Files.lines(Paths.get(LogManager.getRootLogConfiguration().getStoreName()))) {
            assertTrue(lines.allMatch(s -> s.contains("SomeOtherThing")));
        }
        String filePath2 = LogManager.getLogConfigurations().get(randomLoggerName).getStoreDirectory()
                .resolve(randomLogFileName).toAbsolutePath().toString();

        File logFile4 = new File(filePath2);
        assertTrue(logFile4.exists());
        try (Stream<String> lines = Files.lines(Paths.get(filePath2))) {
            assertTrue(lines.allMatch(s -> s.contains("SomeOtherThing")));
        }
    }
}
