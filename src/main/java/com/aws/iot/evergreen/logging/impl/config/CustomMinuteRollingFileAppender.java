/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.logging.impl.config;

import ch.qos.logback.core.rolling.RollingFileAppender;

/**
 * This class is an extension on {@link RollingFileAppender} class where we can specify the log file roll over
 * interval.
 * @param <E>   Type of logging event.
 */
public class CustomMinuteRollingFileAppender<E> extends RollingFileAppender<E> {
    private long start = System.currentTimeMillis();
    private int rollOverTimeInMinutes;

    /**
     * Constructor.
     *
     * @param rollOverTimeInMinutes The log file roll over interval.
     */
    public CustomMinuteRollingFileAppender(int rollOverTimeInMinutes) {
        this.rollOverTimeInMinutes = rollOverTimeInMinutes;
    }

    @Override
    public void rollover() {
        long currentTime = System.currentTimeMillis();
        int maxIntervalSinceLastLoggingInMillis = rollOverTimeInMinutes * 60 * 1000;

        if ((currentTime - start) >= maxIntervalSinceLastLoggingInMillis) {
            super.rollover();
            start = System.currentTimeMillis();
        }
    }
}