/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.logging.impl.config;

import ch.qos.logback.classic.Logger;
import com.aws.iot.evergreen.logging.impl.LogManager;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.netty.util.internal.logging.AbstractInternalLogger;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

@Getter
public class EvergreenLogConfig extends PersistenceConfig {
    // TODO: Replace reading from system properties with reading from Kernel Configuration.
    public static final String LOG_LEVEL_KEY = "log.level";
    private static final String DEFAULT_LOG_LEVEL = "INFO";
    public static final String CONFIG_PREFIX = "log";

    @Setter
    private Level level;

    private static final EvergreenLogConfig INSTANCE = new EvergreenLogConfig();

    public static EvergreenLogConfig getInstance() {
        return INSTANCE;
    }

    /**
     * Get default logging configuration from system properties.
     */
    protected EvergreenLogConfig() {
        super(CONFIG_PREFIX);

        this.level = Level.valueOf(System.getProperty(LOG_LEVEL_KEY, DEFAULT_LOG_LEVEL));
        reconfigure((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME));

        configureNettyLogging();
    }

    // Interface with Netty's internal logger so that it uses our logger
    private void configureNettyLogging() {
        InternalLoggerFactory.setDefaultFactory(new NettyLoggerFactory());
    }

    private static class NettyLoggerFactory extends InternalLoggerFactory {
        @Override
        protected InternalLogger newInstance(String name) {
            com.aws.iot.evergreen.logging.api.Logger logger = LogManager.getLogger(name);
            return new NettyLogAdapter(name, logger);
        }

        @SuppressFBWarnings("SE_BAD_FIELD")
        private static class NettyLogAdapter extends AbstractInternalLogger {
            private static final long serialVersionUID = -6382972526573193470L;
            private final com.aws.iot.evergreen.logging.api.Logger logger;

            public NettyLogAdapter(String name, com.aws.iot.evergreen.logging.api.Logger logger) {
                super(name);
                this.logger = logger;
            }

            @Override
            public boolean isTraceEnabled() {
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
            public boolean isDebugEnabled() {
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
            public boolean isInfoEnabled() {
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
            public boolean isWarnEnabled() {
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
            public boolean isErrorEnabled() {
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
        }
    }
}
