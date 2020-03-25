/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.logging.impl;

import com.aws.iot.evergreen.logging.impl.config.EvergreenMetricsConfig;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.measure.quantity.Duration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MetricsFactoryTest {

    @TempDir
    static Path tempDir;

    @BeforeAll
    static void beforeAll() {
        System.setProperty("root", tempDir.toAbsolutePath().toString());
    }

    @Captor
    ArgumentCaptor<EvergreenMetricsMessage> message;

    @Test
    void GIVEN_metricsFactory_WHEN_metrics_are_enabled_THEN_metrics_should_be_logged() {
        MetricsFactoryImpl mf = (MetricsFactoryImpl) MetricsFactoryImpl.getInstance();
        org.apache.logging.log4j.Logger loggerSpy = setupLoggerSpy(mf);

        mf.setConfig(new EvergreenMetricsConfig(false));
        assertFalse(mf.isMetricsEnabled());
        mf.newMetrics().addDimension("key", "value").setNamespace("test").addMetric("count", 1).emit();
        verify(loggerSpy, times(0)).logMessage(eq(Level.ALL), any(), any(), any(), any(), any());

        mf.setConfig(new EvergreenMetricsConfig(true));
        assertTrue(mf.isMetricsEnabled());
        mf.newMetrics().addDimension("key", "value").setNamespace("test").addMetric("count", 1).emit();
        verify(loggerSpy, times(1)).logMessage(eq(Level.ALL), any(), any(), any(), any(), any());

        assertEquals("test", message.getValue().getNamespace());
        assertEquals(1, message.getValue().getMetrics().size());
    }

    @Test
    void GIVEN_metricsFactory_WHEN_it_has_default_dimensions_THEN_metrics_contains_default_dimensions() {
        MetricsFactoryImpl mf = (MetricsFactoryImpl) MetricsFactoryImpl.getInstance();
        org.apache.logging.log4j.Logger loggerSpy = setupLoggerSpy(mf);

        mf.addDefaultDimension("d1", "v1");
        mf.newMetrics().addDimension("d2", "v2").addMetric("time", 1, Duration.UNIT).emit();
        verify(loggerSpy).logMessage(eq(Level.ALL), any(), any(), any(), any(), any());
        Map<String, String> dimensions = message.getValue().getDimensions();

        assertEquals(2, dimensions.size());
        assertEquals("v1", dimensions.get("d1"));
        assertEquals("v2", dimensions.get("d2"));
    }

    @Test
    void GIVEN_metricsFactory_WHEN_used_by_2_threads_THEN_both_threads_should_emit_metrics() {
        MetricsFactoryImpl mf = (MetricsFactoryImpl) MetricsFactoryImpl.getInstance();
        org.apache.logging.log4j.Logger loggerSpy = setupLoggerSpy(mf);

        mf.addDefaultDimension("d1", "v1");

        CyclicBarrier start = new CyclicBarrier(2);
        ExecutorService ses = Executors.newFixedThreadPool(2);
        Future future1 = ses.submit(() -> {
            try {
                start.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                fail("Error starting thread1 in sync", e);
            }
            mf.newMetrics().setNamespace("th1").addMetric("error", 1).emit();
            mf.newMetrics().setNamespace("th1").addDimension("d1", "override").addMetric("latency", 1, Duration.UNIT).emit();
        });

        Future future2 = ses.submit(() -> {
            try {
                start.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                fail("Error starting thread2 in sync", e);
            }
            mf.newMetrics().setNamespace("main").addDimension("d2", "v2").addMetric("time", 2, Duration.UNIT).emit();
        });

        try {
            future1.get(5, TimeUnit.SECONDS);
            future2.get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            fail("Error waiting for child threads to finish", e);
        }
        ses.shutdown();
        verify(loggerSpy, times(3)).logMessage(eq(Level.ALL), any(), any(), any(), any(), any());

        Map<String, String> defaultDimensionMap = Collections.singletonMap("d1", "v1");

        Map<String, String> dimensionMap = new HashMap<>(defaultDimensionMap);
        dimensionMap.put("d2", "v2");

        assertThat(message.getAllValues(), containsInAnyOrder(
            new EvergreenMetricsMessage("main",
                Collections.singletonList(new Metric<>("time", 2, Duration.UNIT)), dimensionMap),
            new EvergreenMetricsMessage("th1",
                Collections.singletonList(Metric.of("error", 1)), defaultDimensionMap),
            new EvergreenMetricsMessage("th1",
                Collections.singletonList(new Metric<>("latency", 1, Duration.UNIT)),
                Collections.singletonMap("d1", "override"))
        ));
    }

    private org.apache.logging.log4j.Logger setupLoggerSpy(MetricsFactoryImpl mf) {
        org.apache.logging.log4j.Logger loggerSpy = spy(mf.getLogger());
        mf.setLogger(loggerSpy);
        doCallRealMethod().when(loggerSpy).logMessage(any(), any(), any(), any(), message.capture(), any());
        return loggerSpy;
    }
}
