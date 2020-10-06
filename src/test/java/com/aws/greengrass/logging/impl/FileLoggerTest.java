package com.aws.greengrass.logging.impl;

import com.aws.greengrass.logging.api.Logger;
import com.aws.greengrass.logging.impl.config.LogConfig;
import com.aws.greengrass.logging.impl.config.LogStore;
import com.aws.greengrass.logging.impl.config.model.LoggerConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(MockitoExtension.class)
class FileLoggerTest {
    @TempDir
    static Path tempRootDir;
    private static Logger logger;

    @BeforeAll
    static void setLogger() {
        LogManager.getRootLogConfiguration().setStore(LogStore.FILE);
        LogManager.getRootLogConfiguration().setStorePath(tempRootDir.resolve("logs").resolve("greengrass.log")
                .toAbsolutePath());
        logger = LogManager.getLogger(FileLoggerTest.class);
    }

    @AfterEach
    public void cleanup() {
        LogConfig.getInstance().closeContext();
    }

    @AfterAll
    static void cleanupLogger() {
        LogManager.getRootLogConfiguration().setStore(LogStore.CONSOLE);
    }
    @Test
    void GIVEN_root_logger_WHEN_get_logger_THEN_greengrass_log_file_is_created() {
        File logFile = new File(LogManager.getRootLogConfiguration().getStoreName());
        logger.atInfo().log("Something");
        assertTrue(logFile.exists());
    }

    @Test
    void GIVEN_root_logger_child_WHEN_get_logger_THEN_greengrass_log_file_is_created() {
        Logger logger2 = logger.createChild();
        File logFile = new File(LogManager.getRootLogConfiguration().getStoreName());
        logger2.atInfo().log("Something");
        assertTrue(logFile.exists());
        try (Stream<String> lines = Files.lines(Paths.get(LogManager.getRootLogConfiguration().getStoreName()))) {
            assertTrue(lines.allMatch(s -> s.contains("Something")));
        } catch (IOException e) {
            fail("Unable to read log file.");
        }
    }

    @Test
    void GIVEN_new_logger_with_config_WHEN_get_logger_THEN_correct_log_file_is_created() {
        String randomLogFileName = UUID.randomUUID().toString() + ".log";
        String randomLoggerName = UUID.randomUUID().toString();
        Logger logger2 = LogManager.getLogger(randomLoggerName, LoggerConfiguration.builder()
                .fileName(randomLogFileName)
                .build());
        logger2.atInfo().log("Something");
        String filePath = LogManager.getLogConfigurations().get(randomLoggerName).getStoreDirectory()
                .resolve(randomLogFileName).toAbsolutePath().toString();
        File logFile = new File(filePath);
        assertTrue(logFile.exists());
        try (Stream<String> lines = Files.lines(Paths.get(filePath))) {
            assertTrue(lines.allMatch(s -> s.contains("Something")));
        } catch (IOException e) {
            fail("Unable to read log file " + filePath);
        }
        if (Files.exists(Paths.get(LogManager.getRootLogConfiguration().getStoreName()))) {
            try (Stream<String> lines = Files.lines(Paths.get(LogManager.getRootLogConfiguration().getStoreName()))) {
                assertTrue(lines.noneMatch(s -> s.contains("Something") || s.contains("Nothing")));
            } catch (IOException e) {
                fail("Unable to read log file " + LogManager.getRootLogConfiguration().getStoreName());
            }
        }
    }

    @Test
    void GIVEN_new_logger_child_with_config_WHEN_get_logger_THEN_correct_log_file_is_created() {
        String randomLogFileName = UUID.randomUUID().toString() + ".log";
        String randomLoggerName = UUID.randomUUID().toString();
        Logger logger2 = LogManager.getLogger(randomLoggerName, LoggerConfiguration.builder()
                .fileName(randomLogFileName)
                .build());
        Logger logger2Child = logger2.createChild();
        logger2.atInfo().log("Something");
        String filePath = LogManager.getLogConfigurations().get(randomLoggerName).getStoreDirectory()
                .resolve(randomLogFileName).toAbsolutePath().toString();

        File logFile = new File(filePath);
        assertTrue(logFile.exists());

        logger2Child.atInfo().log("Nothing");
        try (Stream<String> lines = Files.lines(Paths.get(filePath))) {
            assertTrue(lines.anyMatch(s -> s.contains("Something")));
        } catch (IOException e) {
            fail("Unable to read log file " + filePath);
        }
        try (Stream<String> lines = Files.lines(Paths.get(filePath))) {
            assertTrue(lines.anyMatch(s -> s.contains("Nothing")));
        } catch (IOException e) {
            fail("Unable to read log file." + filePath);
        }
        if (Files.exists(Paths.get(LogManager.getRootLogConfiguration().getStoreName()))) {
            try (Stream<String> lines = Files.lines(Paths.get(LogManager.getRootLogConfiguration().getStoreName()))) {
                assertTrue(lines.noneMatch(s -> s.contains("Something") || s.contains("Nothing")));
            } catch (IOException e) {
                fail("Unable to read log file " + LogManager.getRootLogConfiguration().getStoreName());
            }
        }
    }
}
