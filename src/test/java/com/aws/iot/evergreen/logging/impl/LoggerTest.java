/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.aws.iot.evergreen.logging.impl;

import com.aws.iot.evergreen.logging.api.Logger;
import com.aws.iot.evergreen.logging.api.LogManager;
import com.aws.iot.evergreen.logging.impl.Log4jLogManager;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@Tag("Integration")
public class LoggerTest {
    @Test
    public void testLoggerLevel() {
        // TODO: write proper unit tests https://issues.amazon.com/issues/P31936029
        String logFileName = "demo.log";
        try {
            Files.deleteIfExists(Paths.get(logFileName));
        } catch (IOException e) {
            e.printStackTrace();
        }

        LogManager logManager = new Log4jLogManager();
        Logger logger = logManager.getLogger("test");
        logger.atTrace().log("should be filtered out");
        logger.atInfo().log("should be logged");

        FileReader input = null;
        try {
            input = new FileReader(logFileName);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            fail("Failed to open log file");
        }
        int count = 0;
        try {
            LineNumberReader reader = new LineNumberReader(input);
            while (reader.readLine() != null) {};
            count = reader.getLineNumber();
        } catch (IOException e) {
            e.printStackTrace();
            fail("Failed to read line from log file");
        }
        assertEquals(1, count, "Log level filter is not working as expected");
    }
}
