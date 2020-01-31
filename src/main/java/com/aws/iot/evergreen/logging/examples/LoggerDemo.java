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
     * Sample Output. Note: there're some duplicate information in the JSON object. We will clean it up when we implement our own layout class
     * TODO: https://sim.amazon.com/issues/P31936149
     * {"thread":"main","level":"INFO","loggerName":"main","message":{"LN":"main","TS":{"epochSecond":1580507648,"nano":43000000},"LVL":"INFO","T":"main-some-event","MSG":"test info","D":{"component":"demo","key":"value"}},"endOfBatch":false,"instant":{"epochSecond":1580507648,"nanoOfSecond":43000000},"threadPriority":5,"threadId":1}
     * {"thread":"Thread-1","level":"INFO","loggerName":"newthread","message":{"LN":"newthread","TS":{"epochSecond":1580507648,"nano":20000000},"LVL":"INFO","T":"thread-event","MSG":"test thread info","D":{"component":"newthread-demo"}},"endOfBatch":false,"instant":{"epochSecond":1580507648,"nanoOfSecond":23000000},"threadPriority":5,"threadId":13}
     * {"thread":"main","level":"INFO","loggerName":"main","message":{"LN":"main","TS":{"epochSecond":1580507648,"nano":160000000},"LVL":"INFO","T":"DEFAULT","MSG":"test info again","D":{"component":"demo"}},"endOfBatch":false,"instant":{"epochSecond":1580507648,"nanoOfSecond":160000000},"threadPriority":5,"threadId":1}
     * {"thread":"Thread-1","level":"DEBUG","loggerName":"newthread","message":{"LN":"newthread","TS":{"epochSecond":1580507648,"nano":161000000},"LVL":"DEBUG","T":"DEFAULT","MSG":"test thread debug","D":{"component":"newthread-demo"}},"endOfBatch":false,"instant":{"epochSecond":1580507648,"nanoOfSecond":161000000},"threadPriority":5,"threadId":13}
     * {"thread":"Thread-1","level":"ERROR","loggerName":"newthread","message":{"LN":"newthread","TS":{"epochSecond":1580507648,"nano":162000000},"LVL":"ERROR","T":"thread-error-event","MSG":"test thread error","D":{"component":"newthread-demo","key3":"value3"}},"thrown":{"commonElementCount":0,"localizedMessage":"thread error","message":"thread error","name":"java.lang.Exception","extendedStackTrace":[{"class":"com.aws.iot.evergreen.logging.examples.LoggerDemo","method":"lambda$main$0","file":"LoggerDemo.java","line":20,"exact":false,"location":"classes/","version":"?"},{"class":"java.lang.Thread","method":"run","file":"Thread.java","line":748,"exact":true,"location":"?","version":"1.8.0_231"}]},"endOfBatch":false,"instant":{"epochSecond":1580507648,"nanoOfSecond":162000000},"threadPriority":5,"threadId":13}
     * {"thread":"main","level":"ERROR","loggerName":"main","message":{"LN":"main","TS":{"epochSecond":1580507648,"nano":162000000},"LVL":"ERROR","T":"error-event","MSG":"test error","D":{"key2":"value2","component":"demo"}},"thrown":{"commonElementCount":0,"localizedMessage":"some error","message":"some error","name":"java.lang.Exception","extendedStackTrace":[{"class":"com.aws.iot.evergreen.logging.examples.LoggerDemo","method":"main","file":"LoggerDemo.java","line":29,"exact":true,"location":"classes/","version":"?"}]},"endOfBatch":false,"instant":{"epochSecond":1580507648,"nanoOfSecond":162000000},"threadPriority":5,"threadId":1}
     */
    public static void main(String[] argv) {
        Runnable runnable = () -> {
            Logger logger = logManager.getLogger("newthread");
            logger.addDefaultKeyValue("component", "newthread-demo");
            logger.atInfo().setEventType("thread-event").log("test thread info");
            logger.atDebug().log("test thread debug");
            logger.atError().setCause(new Exception("thread error")).setEventType("thread-error-event")
                    .addKeyValue("key3", "value3").log("test thread error");
        };
        Thread thread = new Thread(runnable);
        thread.start();

        Logger logger = logManager.getLogger("main");
        logger.addDefaultKeyValue("component", "demo");
        logger.atInfo().setEventType("main-some-event").addKeyValue("key", "value").log("test info");
        logger.atInfo().log("test info again");
        logger.atError().setCause(new Exception("some error")).setEventType("error-event").addKeyValue("key2", "value2")
                .log("test error");
    }
}