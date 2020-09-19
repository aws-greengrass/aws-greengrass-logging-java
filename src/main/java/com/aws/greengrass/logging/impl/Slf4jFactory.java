/*
 * Copyright Amazon.com Inc. or its affiliates.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.greengrass.logging.impl;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.Marker;

public class Slf4jFactory implements ILoggerFactory {
    public Slf4jFactory() {
    }

    @Override
    public Logger getLogger(String name) {
        return wrapLogger(LogManager.getLogger(name));
    }

    private Logger wrapLogger(com.aws.greengrass.logging.api.Logger logger) {
        return new LogAdapter(logger);
    }

    /**
     * Adapts Slf4j interface to our logger.
     */
    private static class LogAdapter implements Logger {
        private final com.aws.greengrass.logging.api.Logger logger;

        public LogAdapter(com.aws.greengrass.logging.api.Logger logger) {
            this.logger = logger;
        }

        @Override
        public String getName() {
            return logger.getName();
        }

        @Override
        public boolean isTraceEnabled() {
            return logger.isTraceEnabled();
        }

        @Override
        public boolean isTraceEnabled(Marker marker) {
            return logger.isTraceEnabled();
        }

        @Override
        public void trace(String msg) {
            logger.trace(msg);
        }

        @Override
        public void trace(String format, Object arg) {
            logger.trace(format, arg);
        }

        @Override
        public void trace(String format, Object argA, Object argB) {
            logger.trace(format, argA, argB);
        }

        @Override
        public void trace(String format, Object... arguments) {
            logger.trace(format, arguments);
        }

        @Override
        public void trace(String msg, Throwable t) {
            logger.trace(msg, t);
        }

        @Override
        public void trace(Marker marker, String msg) {
            logger.trace(msg);
        }

        @Override
        public void trace(Marker marker, String format, Object arg) {
            logger.trace(format, arg);
        }

        @Override
        public void trace(Marker marker, String format, Object arg1, Object arg2) {
            logger.trace(format, arg1, arg2);
        }

        @Override
        public void trace(Marker marker, String format, Object... argArray) {
            logger.trace(format, argArray);
        }

        @Override
        public void trace(Marker marker, String msg, Throwable t) {
            logger.trace(msg, t);
        }

        @Override
        public boolean isDebugEnabled() {
            return logger.isDebugEnabled();
        }

        @Override
        public boolean isDebugEnabled(Marker marker) {
            return logger.isDebugEnabled();
        }

        @Override
        public void debug(String msg) {
            logger.debug(msg);
        }

        @Override
        public void debug(String format, Object arg) {
            logger.debug(format, arg);
        }

        @Override
        public void debug(String format, Object argA, Object argB) {
            logger.debug(format, argA, argB);
        }

        @Override
        public void debug(String format, Object... arguments) {
            logger.debug(format, arguments);
        }

        @Override
        public void debug(String msg, Throwable t) {
            logger.debug(msg, t);
        }

        @Override
        public void debug(Marker marker, String msg) {
            logger.debug(msg);
        }

        @Override
        public void debug(Marker marker, String format, Object arg) {
            logger.debug(format, arg);
        }

        @Override
        public void debug(Marker marker, String format, Object arg1, Object arg2) {
            logger.debug(format, arg1, arg2);
        }

        @Override
        public void debug(Marker marker, String format, Object... arguments) {
            logger.debug(format, arguments);
        }

        @Override
        public void debug(Marker marker, String msg, Throwable t) {
            logger.debug(msg, t);
        }

        @Override
        public boolean isInfoEnabled() {
            return logger.isInfoEnabled();
        }

        @Override
        public boolean isInfoEnabled(Marker marker) {
            return logger.isInfoEnabled();
        }

        @Override
        public void info(String msg) {
            logger.info(msg);
        }

        @Override
        public void info(String format, Object arg) {
            logger.info(format, arg);
        }

        @Override
        public void info(String format, Object argA, Object argB) {
            logger.info(format, argA, argB);
        }

        @Override
        public void info(String format, Object... arguments) {
            logger.info(format, arguments);
        }

        @Override
        public void info(String msg, Throwable t) {
            logger.info(msg, t);
        }

        @Override
        public void info(Marker marker, String msg) {
            logger.info(msg);
        }

        @Override
        public void info(Marker marker, String format, Object arg) {
            logger.info(format, arg);
        }

        @Override
        public void info(Marker marker, String format, Object arg1, Object arg2) {
            logger.info(format, arg1, arg2);
        }

        @Override
        public void info(Marker marker, String format, Object... arguments) {
            logger.info(format, arguments);
        }

        @Override
        public void info(Marker marker, String msg, Throwable t) {
            logger.info(msg, t);
        }

        @Override
        public boolean isWarnEnabled() {
            return logger.isWarnEnabled();
        }

        @Override
        public boolean isWarnEnabled(Marker marker) {
            return logger.isWarnEnabled();
        }

        @Override
        public void warn(String msg) {
            logger.warn(msg);
        }

        @Override
        public void warn(String format, Object arg) {
            logger.warn(format, arg);
        }

        @Override
        public void warn(String format, Object argA, Object argB) {
            logger.warn(format, argA, argB);
        }

        @Override
        public void warn(String format, Object... arguments) {
            logger.warn(format, arguments);
        }

        @Override
        public void warn(String msg, Throwable t) {
            logger.warn(msg, t);
        }

        @Override
        public void warn(Marker marker, String msg) {
            logger.warn(msg);
        }

        @Override
        public void warn(Marker marker, String format, Object arg) {
            logger.warn(format, arg);
        }

        @Override
        public void warn(Marker marker, String format, Object arg1, Object arg2) {
            logger.warn(format, arg1, arg2);
        }

        @Override
        public void warn(Marker marker, String format, Object... arguments) {
            logger.warn(format, arguments);
        }

        @Override
        public void warn(Marker marker, String msg, Throwable t) {
            logger.warn(msg, t);
        }

        @Override
        public boolean isErrorEnabled() {
            return logger.isErrorEnabled();
        }

        @Override
        public boolean isErrorEnabled(Marker marker) {
            return logger.isErrorEnabled();
        }

        @Override
        public void error(String msg) {
            logger.error(msg);
        }

        @Override
        public void error(String format, Object arg) {
            logger.error(format, arg);
        }

        @Override
        public void error(String format, Object argA, Object argB) {
            logger.error(format, argA, argB);
        }

        @Override
        public void error(String format, Object... arguments) {
            logger.error(format, arguments);
        }

        @Override
        public void error(String msg, Throwable t) {
            logger.error(msg, t);
        }

        @Override
        public void error(Marker marker, String msg) {
            logger.error(msg);
        }

        @Override
        public void error(Marker marker, String format, Object arg) {
            logger.error(format, arg);
        }

        @Override
        public void error(Marker marker, String format, Object arg1, Object arg2) {
            logger.error(format, arg1, arg2);
        }

        @Override
        public void error(Marker marker, String format, Object... arguments) {
            logger.error(format, arguments);
        }

        @Override
        public void error(Marker marker, String msg, Throwable t) {
            logger.error(msg, t);
        }
    }
}
