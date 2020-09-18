/*
 * Copyright Amazon.com Inc. or its affiliates.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.slf4j.impl;

import com.aws.greengrass.logging.impl.Slf4jFactory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LoggerFactoryBinder;

/**
 * The binding of {@link LoggerFactory} class with an actual instance of {@link ILoggerFactory} is performed using
 * information returned by this class.
 *
 * <p>Modified from Logback's implementation
 */
@SuppressFBWarnings({"MS_SHOULD_BE_FINAL", "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD"})
public class StaticLoggerBinder implements LoggerFactoryBinder {
    private static final StaticLoggerBinder INSTANCE = new StaticLoggerBinder();
    private static final Slf4jFactory factory = new Slf4jFactory();
    // to avoid constant folding by the compiler, this field must *not* be final
    public static String REQUESTED_API_VERSION = "1.7.16"; // !final

    private StaticLoggerBinder() {
    }

    public static StaticLoggerBinder getSingleton() {
        return INSTANCE;
    }

    public ILoggerFactory getLoggerFactory() {
        return factory;
    }

    public String getLoggerFactoryClassStr() {
        return factory.getClass().getName();
    }

}
