/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.slf4j.impl;

import com.aws.greengrass.logging.impl.Slf4jFactory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.ILoggerFactory;
import org.slf4j.IMarkerFactory;
import org.slf4j.helpers.BasicMarkerFactory;
import org.slf4j.helpers.NOPMDCAdapter;
import org.slf4j.spi.MDCAdapter;
import org.slf4j.spi.SLF4JServiceProvider;

@SuppressFBWarnings({"MS_SHOULD_BE_FINAL"})
public class GreengrassServiceProvider implements SLF4JServiceProvider {
    // to avoid constant folding by the compiler, this field must *not* be final
    public static String REQUESTED_API_VERSION = "2.0.99";
    private ILoggerFactory loggerFactory;
    private IMarkerFactory markerFactory;
    private MDCAdapter mdcAdapter;

    public GreengrassServiceProvider() {
    }

    public ILoggerFactory getLoggerFactory() {
        return this.loggerFactory;
    }

    public IMarkerFactory getMarkerFactory() {
        return this.markerFactory;
    }

    public MDCAdapter getMDCAdapter() {
        return this.mdcAdapter;
    }

    public String getRequestedApiVersion() {
        return REQUESTED_API_VERSION;
    }

    /**
     * Initialize the loggers.
     */
    public void initialize() {
        this.loggerFactory = new Slf4jFactory();
        this.markerFactory = new BasicMarkerFactory();
        this.mdcAdapter = new NOPMDCAdapter();
    }
}
