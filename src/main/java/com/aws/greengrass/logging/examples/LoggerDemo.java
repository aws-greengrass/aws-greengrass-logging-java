/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.greengrass.logging.examples;

import com.aws.greengrass.logging.api.Logger;
import com.aws.greengrass.logging.impl.LogManager;
import com.aws.greengrass.logging.impl.config.model.LogConfigUpdate;

import java.util.concurrent.CountDownLatch;

/**
 * A demo of using LogManager and Logger instances.
 */
public class LoggerDemo {
    private static final Logger logger1;
    private static final Logger logger2;
    private static final Logger logger1Child;
    private static final Logger randomNewLogger;
    private static final Logger logger2Child;

    static {
        System.setProperty("log.fmt", "JSON");
        System.setProperty("log.file.sizeInKB", "10240");
        System.setProperty("log.file.fileSizeInKB", "1024");
        //System.setProperty("log.store", "FILE");
        logger1 = LogManager.getLogger(LoggerDemo.class);
        randomNewLogger = LogManager.getLogger("RandomNewLogger");
        logger1Child = logger1.createChild();
        LogConfigUpdate logConfigUpdate = LogConfigUpdate.builder()
                .fileName("testComponent/testLog.log")
                .build();
        logger2 = LogManager.getLogger("newLogger", logConfigUpdate);
        logger2Child = logger2.createChild();
        logger1.addDefaultKeyValue("component", "demo").addDefaultKeyValue("device", "asdf");
    }

    /**
     * Sample Output. Note: . We will clean it up when we implement our own layout class
     * TODO: there're some duplicate information in the JSON object, clean it up when we implement our own layout class
     * https://sim.amazon.com/issues/P31936149
     * {"contexts":{"component":"demo","device":"asdf"},"eventType":"th1-event","level":"INFO","loggerName":"com.aws
     *   .greengrass.logging.examples.LoggerDemo","message":"test th1 info","timestamp":1581380225608}
     * {"contexts":{"component":"demo","device":"asdf"},"eventType":"th2-event","level":"INFO","loggerName":"com.aws
     *   .greengrass.logging.examples.LoggerDemo","message":"test th2 info","timestamp":1581380225608}
     * {"contexts":{"component":"th1-override","device":"asdf"},"level":"DEBUG","loggerName":"com.aws.greengrass
     *   .logging.examples.LoggerDemo","message":"test th1 debug","timestamp":1581380225630}
     * {"contexts":{"component":"demo","device":"asdf"},"level":"INFO","loggerName":"com.aws.greengrass.logging
     *   .examples.LoggerDemo","message":"test main info","timestamp":1581380225608}
     * {"cause":{"localizedMessage":"some error","message":"some error","stackTrace":[{"className":"com.aws.iot
     *   .greengrass.logging.examples.LoggerDemo","fileName":"LoggerDemo.java","lineNumber":56,"methodName":"main",
     *   "nativeMethod":false}],"suppressed":[]},"contexts":{"key2":"value2","component":"demo","device":"asdf"}
     *   ,"eventType":"error-event","level":"ERROR","loggerName":"com.aws.greengrass.logging.examples.LoggerDemo",
     *   "message":"test error","timestamp":1581380225631}
     */
    public static void main(String[] argv) throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(2);
        Runnable runnable1 = () -> {
            logger1.atInfo().setEventType("th1-event").log("test th1 info");
            logger1.atDebug().addKeyValue("component", "th1-override").log("test th1 debug");

            for (int i = 0; i < 100; i = i + 2) {
                logger1.atInfo().setEventType("th1-event").log(i);
            }
            for (int i = 1; i < 100; i = i + 2) {
                logger1Child.atInfo().setEventType("th3-event").log(i);
            }
            for (int i = 100; i < 200; i = i + 2) {
                randomNewLogger.atInfo().setEventType("th4-event").log(i);
            }
            for (int i = 101; i < 200; i = i + 2) {
                logger2Child.atInfo().setEventType("th5-event").log(i);
            }
            countDownLatch.countDown();
        };
        Thread thread1 = new Thread(runnable1);
        thread1.start();

        Runnable runnable2 = () -> {
            logger2.atInfo().setEventType("th2-event").log("test th2 info");
            for (int i = 1; i < 100; i = i + 2) {
                logger2.atInfo().setEventType("th2-event").log(i);
            }
            for (int i = 0; i < 100; i = i + 2) {
                logger2Child.atInfo().setEventType("th5-event").log(i);
            }
            for (int i = 101; i < 200; i = i + 2) {
                randomNewLogger.atInfo().setEventType("th4-event").log(i);
            }
            countDownLatch.countDown();
        };
        Thread thread2 = new Thread(runnable2);
        thread2.start();

        logger1.atInfo().log("test main info");
        logger1.atError().setCause(new Exception("some error")).setEventType("error-event")
                .addKeyValue("key2", "value2")
                .log("test error");
        logger2.atInfo().log("test main info");
        logger2.atError().setCause(new Exception("some error")).setEventType("error-event")
                .addKeyValue("key2", "value2").log("test error");
        countDownLatch.await();
    }
}
