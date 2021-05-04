/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.greengrass.logging.impl.config;

import com.aws.greengrass.logging.impl.config.model.LoggerConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.event.Level;

import static com.aws.greengrass.logging.impl.config.PersistenceConfig.DEFAULT_STORE_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogConfigTest {
    @AfterEach
    void cleanup() {
        LogConfig.getInstance().reset();
    }


    @Test
    void GIVEN_provided_LoggerConfiguration_THEN_LogConfig_is_configured_the_same() {
        LoggerConfiguration builder = LoggerConfiguration.builder().build();
        LogConfig config = new LogConfig(builder);

        assertEquals(LogStore.CONSOLE, config.getStore());
        assertEquals(Level.INFO, config.getLevel());
        assertEquals(LogFormat.TEXT, config.getFormat());
        assertTrue(config.getStoreName().endsWith(DEFAULT_STORE_NAME));
        assertEquals(DEFAULT_STORE_NAME, config.getFileName());

        builder = LoggerConfiguration.builder().level(Level.TRACE).build();
        config = new LogConfig(builder);

        assertEquals(Level.TRACE, config.getLevel());

        builder = LoggerConfiguration.builder().format(LogFormat.JSON).build();
        config = new LogConfig(builder);

        assertEquals(LogFormat.JSON, config.getFormat());

        builder = LoggerConfiguration.builder().fileName("abc").build();
        config = new LogConfig(builder);

        assertTrue(config.getStoreName().endsWith("abc"));
        assertEquals("abc", config.getFileName());
    }

    @Test
    void GIVEN_provided_LoggerConfiguration_and_global_THEN_LogConfig_is_configured_with_merged() {
        LogConfig root = LogConfig.getInstance();
        root.setLevel(Level.DEBUG);
        root.setFormat(LogFormat.JSON);

        LoggerConfiguration builder = LoggerConfiguration.builder().build();
        LogConfig config = new LogConfig(builder);

        assertEquals(LogStore.CONSOLE, config.getStore());
        assertEquals(Level.DEBUG, config.getLevel());
        assertEquals(LogFormat.JSON, config.getFormat());
        assertTrue(config.getStoreName().endsWith(DEFAULT_STORE_NAME));
        assertEquals(DEFAULT_STORE_NAME, config.getFileName());

        builder = LoggerConfiguration.builder().level(Level.TRACE).build();
        config = new LogConfig(builder);

        assertEquals(Level.TRACE, config.getLevel());

        builder = LoggerConfiguration.builder().format(LogFormat.TEXT).build();
        config = new LogConfig(builder);

        assertEquals(LogFormat.TEXT, config.getFormat());

        root.setStore(LogStore.FILE);
        builder = LoggerConfiguration.builder().outputType(LogStore.CONSOLE).build();
        config = new LogConfig(builder);

        assertEquals(LogStore.CONSOLE, config.getStore());
    }
}
