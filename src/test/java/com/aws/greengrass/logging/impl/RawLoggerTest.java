/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.greengrass.logging.impl;

import com.aws.greengrass.logging.api.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RawLoggerTest {

    @TempDir
    Path tempDir;

    @Test
    void GIVEN_raw_logger_WHEN_log_message_THEN_only_raw_message_written() throws IOException {
        String name = "test-raw-" + UUID.randomUUID();
        Logger rawLogger = LogManager.getRawLogger(name, tempDir);

        String emfJson = "{\"_aws\":{\"Timestamp\":1234567890},\"metric\":42}";
        rawLogger.atInfo().log(emfJson);

        // Verify multiple lines are written raw
        rawLogger.atInfo().log("second-line");

        Path logFile = tempDir.resolve(name + ".log");
        assertTrue(Files.exists(logFile), "Raw log file should be created");

        List<String> lines = Files.readAllLines(logFile);
        // Each line should be the raw message — no timestamp, level, logger name, or thread
        assertEquals(emfJson, lines.get(0));
        assertEquals("second-line", lines.get(1));
        assertFalse(lines.get(0).contains("[INFO]"), "Should not contain log level envelope");
        assertFalse(lines.get(0).contains("raw."), "Should not contain logger name");
    }

    @Test
    void GIVEN_raw_logger_WHEN_log_with_placeholders_THEN_placeholders_resolved() throws IOException {
        String name = "fmt-test-" + UUID.randomUUID();
        Logger rawLogger = LogManager.getRawLogger(name, tempDir);

        rawLogger.atInfo().log("value is {}", 42);

        Path logFile = tempDir.resolve(name + ".log");
        List<String> lines = Files.readAllLines(logFile);
        assertEquals("value is 42", lines.get(0),
                "Placeholders should be resolved — getMessage() returns the formatted string");
    }

    @Test
    void GIVEN_raw_logger_WHEN_log_message_THEN_does_not_propagate_to_root() throws IOException {
        String name = "no-propagate-" + UUID.randomUUID();
        Logger rawLogger = LogManager.getRawLogger(name, tempDir);

        // Get root logger and log something to establish baseline
        Logger rootLogger = LogManager.getLogger("root-test");
        String rootMarker = "ROOT-" + UUID.randomUUID();
        rootLogger.atInfo().log(rootMarker);

        // Log through raw logger
        String rawMarker = "RAW-" + UUID.randomUUID();
        rawLogger.atInfo().log(rawMarker);

        // Raw log file should contain the raw message
        Path rawLogFile = tempDir.resolve(name + ".log");
        assertTrue(Files.exists(rawLogFile));
        List<String> rawLines = Files.readAllLines(rawLogFile);
        assertTrue(rawLines.stream().anyMatch(l -> l.equals(rawMarker)),
                "Raw log file should contain the raw message");

        // Root log file should NOT contain the raw message — serialize() applies RAW format per-logger
        String rootStoreName = LogManager.getRootLogConfiguration().getStoreName();
        if (rootStoreName != null && Files.exists(Paths.get(rootStoreName))) {
            List<String> rootLines = Files.readAllLines(Paths.get(rootStoreName));
            assertFalse(rootLines.stream().anyMatch(l -> l.contains(rawMarker)),
                    "Root log should not contain raw logger messages");
        }
    }

}
