/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.aws.iot.evergreen.logging.examples;

import com.aws.iot.evergreen.logging.api.LogManager;
import com.aws.iot.evergreen.logging.api.Logger;
import com.aws.iot.evergreen.logging.impl.Log4jLogManager;

/**
 * A demo of using LogManager and Logger instances.
 */
public class LoggerDemo {
    private static LogManager logManager;

    static {
        logManager = new Log4jLogManager();
    }

    /**
     * Sample Output. Note: . We will clean it up when we implement our own layout class
     * TODO: there're some duplicate information in the JSON object, clean it up when we implement our own layout class
     * https://sim.amazon.com/issues/P31936149
     * {"contexts":{},"eventType":"th2-event","level":"INFO","loggerName":"main","message":"test th2 info", \
     * "timestamp":1580866598066}
     * {"contexts":{"component":"demo","device":"asdf","key":"value"},"eventType":"main-some-event","level":"INFO", \
     * "loggerName":"main","message":"test info","timestamp":1580866598067}
     * {"contexts":{"component":"th1-demo"},"eventType":"th1-event","level":"INFO","loggerName":"th1","message": \
     * "test thread info","timestamp":1580866598062}
     * {"contexts":{"component":"override"},"level":"DEBUG","loggerName":"th1","message":"test thread debug", \
     * "timestamp":1580866598079}
     * {"contexts":{"component":"demo","device":"asdf"},"level":"INFO","loggerName":"main","message":"test info \
     * again", "timestamp":1580866598079}
     * {"cause":{"localizedMessage":"some error","message":"some error","stackTrace":[{"className":"com.aws.iot \
     * .evergreen.logging.examples.LoggerDemo","fileName":"LoggerDemo.java","lineNumber":58,"methodName":"main",\
     * "nativeMethod":false}],"suppressed":[]},"contexts":{"key2":"value2","component":"demo","device":"asdf"}, \
     * "eventType":"error-event","level":"ERROR","loggerName":"main","message":"test error", \
     * "timestamp":1580866598080}
     */
    public static void main(String[] argv) {
        Runnable runnable1 = () -> {
            Logger logger = logManager.getLogger("th1");
            logger.addDefaultKeyValue("component", "th1-demo");
            logger.atInfo().setEventType("th1-event").log("test thread info");
            logger.atDebug().addKeyValue("component", "override").log("test thread debug");
        };
        Thread thread1 = new Thread(runnable1);
        thread1.start();

        Runnable runnable2 = () -> {
            Logger logger = logManager.getLogger("main");
            logger.atInfo().setEventType("th2-event").log("test th2 info");
        };
        Thread thread2 = new Thread(runnable2);
        thread2.start();

        Logger logger = logManager.getLogger("main");
        logger.addDefaultKeyValue("component", "demo").addDefaultKeyValue("device", "asdf");
        logger.atInfo().setEventType("main-some-event").addKeyValue("key", "value").log("test info");
        logger.atInfo().log("test info again");
        logger.atError().setCause(new Exception("some error")).setEventType("error-event").addKeyValue("key2", "value2")
                .log("test error");
    }
}
