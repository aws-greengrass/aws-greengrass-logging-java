/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.logging.examples;

import com.aws.iot.evergreen.logging.api.Logger;
import com.aws.iot.evergreen.logging.impl.LogManager;

import java.time.Instant;

/**
 * A demo of using LogManager and Logger instances.
 */
public class LoggerDemo {
    private static Logger logger;

    static {
        System.setProperty("log.fmt", "JSON");
        System.setProperty("log.file.sizeInKB", "10240");
        System.setProperty("log.file.fileSizeInKB", "1024");
        //System.setProperty("log.store", "FILE");
        logger = LogManager.getLogger(LoggerDemo.class);
        logger.addDefaultKeyValue("component", "demo").addDefaultKeyValue("device", "asdf");
    }

    /**
     * Sample Output. Note: . We will clean it up when we implement our own layout class
     * TODO: there're some duplicate information in the JSON object, clean it up when we implement our own layout class
     * https://sim.amazon.com/issues/P31936149
     * {"contexts":{"component":"demo","device":"asdf"},"eventType":"th1-event","level":"INFO","loggerName":"com.aws
     *   .iot.evergreen.logging.examples.LoggerDemo","message":"test th1 info","timestamp":1581380225608}
     * {"contexts":{"component":"demo","device":"asdf"},"eventType":"th2-event","level":"INFO","loggerName":"com.aws
     *   .iot.evergreen.logging.examples.LoggerDemo","message":"test th2 info","timestamp":1581380225608}
     * {"contexts":{"component":"th1-override","device":"asdf"},"level":"DEBUG","loggerName":"com.aws.iot.evergreen
     *   .logging.examples.LoggerDemo","message":"test th1 debug","timestamp":1581380225630}
     * {"contexts":{"component":"demo","device":"asdf"},"level":"INFO","loggerName":"com.aws.iot.evergreen.logging
     *   .examples.LoggerDemo","message":"test main info","timestamp":1581380225608}
     * {"cause":{"localizedMessage":"some error","message":"some error","stackTrace":[{"className":"com.aws.iot
     *   .evergreen.logging.examples.LoggerDemo","fileName":"LoggerDemo.java","lineNumber":56,"methodName":"main",
     *   "nativeMethod":false}],"suppressed":[]},"contexts":{"key2":"value2","component":"demo","device":"asdf"}
     *   ,"eventType":"error-event","level":"ERROR","loggerName":"com.aws.iot.evergreen.logging.examples.LoggerDemo",
     *   "message":"test error","timestamp":1581380225631}
     */
    public static void main(String[] argv) {
        Runnable runnable1 = () -> {
            logger.atInfo().setEventType("th1-event").log("test th1 info");
            logger.atDebug().addKeyValue("component", "th1-override").log("test th1 debug");
        };
        Thread thread1 = new Thread(runnable1);
        thread1.start();

        Runnable runnable2 = () -> {
            logger.atInfo().setEventType("th2-event").log("test th2 info");
        };
        Thread thread2 = new Thread(runnable2);
        thread2.start();

        logger.atInfo().log("test main info");
        logger.atError().setCause(new Exception("some error")).setEventType("error-event").addKeyValue("key2", "value2")
                .log("test error");
    }
}
