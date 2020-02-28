/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.metrics;

import com.aws.iot.evergreen.logging.api.MetricsBuilder;
import com.aws.iot.evergreen.logging.impl.MetricsFactoryImpl;

import java.io.Closeable;

public class MetricUtil {
    /**
     * Simple timer metric.
     * Use like {@code try (t = new Timer("someTimer", MetricsFactoryImpl.getInstance().newMetrics())) { // ... code to
     * measure ... }}
     */
    public static class Timer implements Closeable {
        private static final int NANO_TO_MILLI_DIVISOR = 1_000_000;
        private final long startTime;
        private final String name;
        private long stopTime;
        private final MetricsBuilder metricReporter;

        /**
         * Construct a timer to emit a metric with a given name.
         *
         * @param name metric name
         * @param namespace metric namespace
         */
        public Timer(String name, String namespace) {
            this(name, MetricsFactoryImpl.getInstance().newMetrics().setNamespace(namespace));
        }

        /**
         * Construct a timer to emit a metric with a given name.
         *
         * @param name metric name
         * @param builder metric builder. To add dimensions or set namespace, use the builder methods before passing
         *                it in here.
         */
        public Timer(String name, MetricsBuilder builder) {
            // Use System.nanoTime because it is independent of the system clock. This way if the clock changes
            // our measurement will still be correct.
            this.startTime = System.nanoTime() / NANO_TO_MILLI_DIVISOR;
            this.name = name;
            this.metricReporter = builder;
        }

        @Override
        public void close() {
            stop();
            report();
        }

        private void report() {
            metricReporter
                    .addMetric(name,
                    stopTime - startTime,
                    javax.measure.quantity.Duration.UNIT.divide(1000L))
                    // UNIT is seconds, but we're in ms, so /1000. *1000 gives kiloseconds
                    .emit();
        }

        private void stop() {
            stopTime = System.nanoTime() / NANO_TO_MILLI_DIVISOR;
        }
    }
}
