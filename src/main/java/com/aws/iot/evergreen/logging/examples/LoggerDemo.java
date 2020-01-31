/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.aws.iot.evergreen.logging.examples;

import com.aws.iot.evergreen.logging.api.LogManager;
import com.aws.iot.evergreen.logging.api.Logger;
import com.aws.iot.evergreen.logging.impl.Log4jLogManager;

public class LoggerDemo {
    private static LogManager logManager;

    static {
        logManager = new Log4jLogManager();
    }

    /**
     * Sample Output. Note: . We will clean it up when we implement our own layout class
     * TODO: there're some duplicate information in the JSON object, clean it up when we implement our own layout class
     * https://sim.amazon.com/issues/P31936149
     {"thread":"Thread-1","level":"INFO","loggerName":"th1","message":{"LVL":"INFO","T":"th1-event","MSG":"test thread info","D":{"component":"th1-demo"},"LN":"th1","TS":{"epochSecond":1580841619,"nano":581000000}},"endOfBatch":false,"instant":{"epochSecond":1580841619,"nanoOfSecond":586000000},"threadPriority":5,"threadId":13}
     {"thread":"Thread-1","level":"DEBUG","loggerName":"th1","message":{"LVL":"DEBUG","MSG":"test thread debug","D":{"component":"override"},"LN":"th1","TS":{"epochSecond":1580841619,"nano":725000000}},"endOfBatch":false,"instant":{"epochSecond":1580841619,"nanoOfSecond":725000000},"threadPriority":5,"threadId":13}
     {"thread":"Thread-2","level":"INFO","loggerName":"main","message":{"LVL":"INFO","T":"th2-event","MSG":"test th2 info","D":{"component":"th2-demo"},"LN":"main","TS":{"epochSecond":1580841619,"nano":581000000}},"endOfBatch":false,"instant":{"epochSecond":1580841619,"nanoOfSecond":586000000},"threadPriority":5,"threadId":14}
     {"thread":"main","level":"INFO","loggerName":"main","message":{"LVL":"INFO","T":"main-some-event","MSG":"test info","D":{"component":"demo","device":"asdf","key":"value"},"LN":"main","TS":{"epochSecond":1580841619,"nano":583000000}},"endOfBatch":false,"instant":{"epochSecond":1580841619,"nanoOfSecond":587000000},"threadPriority":5,"threadId":1}
     {"thread":"main","level":"INFO","loggerName":"main","message":{"LVL":"INFO","MSG":"test info again","D":{"component":"demo","device":"asdf"},"LN":"main","TS":{"epochSecond":1580841619,"nano":726000000}},"endOfBatch":false,"instant":{"epochSecond":1580841619,"nanoOfSecond":726000000},"threadPriority":5,"threadId":1}
     {"thread":"main","level":"ERROR","loggerName":"main","message":{"LVL":"ERROR","T":"error-event","MSG":"test error","D":{"key2":"value2","component":"demo","device":"asdf"},"LN":"main","TS":{"epochSecond":1580841619,"nano":727000000}},"thrown":{"commonElementCount":0,"localizedMessage":"some error","message":"some error","name":"java.lang.Exception","extendedStackTrace":[{"class":"com.aws.iot.evergreen.logging.examples.LoggerDemo","method":"main","file":"LoggerDemo.java","line":51,"exact":true,"location":"classes/","version":"?"}]},"endOfBatch":false,"instant":{"epochSecond":1580841619,"nanoOfSecond":727000000},"threadPriority":5,"threadId":1}
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
            logger.addDefaultKeyValue("component", "th2-demo");
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
