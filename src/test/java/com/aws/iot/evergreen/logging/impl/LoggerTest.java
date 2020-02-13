/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.aws.iot.evergreen.logging.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Tag("Integration")
public class LoggerTest {

    public static final String LOG_FILE_NAME = "demo.log";
//
//    @Test
//    public void testLoggerLevel() throws IOException {
//        // TODO: write proper unit tests https://issues.amazon.com/issues/P31936029
//        Files.deleteIfExists(Paths.get(LOG_FILE_NAME));
//
//        LogManager logManager = new Log4jLogManager();
//        Logger logger = logManager.getLogger("test");
//        logger.atTrace().log("should be filtered out");
//        logger.atInfo().log("should be logged");
//
//        FileReader input = null;
//        try {
//            input = new FileReader(LOG_FILE_NAME);
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//            fail("Failed to open log file");
//        }
//        int count = 0;
//        try {
//            LineNumberReader reader = new LineNumberReader(input);
//            while (reader.readLine() != null) {};
//            count = reader.getLineNumber();
//        } catch (IOException e) {
//            e.printStackTrace();
//            fail("Failed to read line from log file");
//        }
//        assertEquals(1, count, "Log level filter is not working as expected");
//    }

    @AfterEach
    public void cleanup() throws IOException {
        Files.deleteIfExists(Paths.get(LOG_FILE_NAME));
    }
}
