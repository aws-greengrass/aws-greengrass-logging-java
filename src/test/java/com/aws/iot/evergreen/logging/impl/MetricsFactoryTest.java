/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.logging.impl;

import com.aws.iot.evergreen.logging.impl.config.EvergreenMetricsConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
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
    ArgumentCaptor<String> message;

    @Test
    void GIVEN_metricsFactory_WHEN_metrics_are_enabled_THEN_metrics_should_be_logged() {
        MetricsFactoryImpl mf = (MetricsFactoryImpl) MetricsFactoryImpl.getInstance();
        Logger loggerSpy = setupLoggerSpy(mf);

        EvergreenMetricsConfig.getInstance().setEnabled(false);
        assertFalse(mf.isMetricsEnabled());
        mf.newMetrics().addDimension("key", "value").setNamespace("test").addMetric("count", 1).emit();
        verify(loggerSpy, times(0)).trace(any());

        EvergreenMetricsConfig.getInstance().setEnabled(true);
        assertTrue(mf.isMetricsEnabled());
        mf.newMetrics().addDimension("key", "value").setNamespace("test").addMetric("count", 1).emit();
        verify(loggerSpy, times(1)).trace(any());

        assertThat(message.getValue(), containsString("test"));
    }

    @Test
    void GIVEN_metricsFactory_WHEN_it_has_default_dimensions_THEN_metrics_contains_default_dimensions() {
        MetricsFactoryImpl mf = (MetricsFactoryImpl) MetricsFactoryImpl.getInstance();
        Logger loggerSpy = setupLoggerSpy(mf);

        mf.addDefaultDimension("d1", "v1");
        mf.newMetrics().addDimension("d2", "v2").addMetric("time", 1, Duration.UNIT).emit();
        verify(loggerSpy).trace(any());

        assertThat(message.getValue(), containsString("{\"d1\":\"v1\",\"d2\":\"v2\"}"));
    }

    @Test
    void GIVEN_metricsFactory_WHEN_used_by_2_threads_THEN_both_threads_should_emit_metrics() {
        MetricsFactoryImpl mf = (MetricsFactoryImpl) MetricsFactoryImpl.getInstance();
        Logger loggerSpy = setupLoggerSpy(mf);

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
            mf.newMetrics().setNamespace("th1").addDimension("d1", "override").addMetric("latency", 1, Duration.UNIT)
                    .emit();
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
        verify(loggerSpy, times(3)).trace(any());

        Map<String, String> defaultDimensionMap = Collections.singletonMap("d1", "v1");

        Map<String, String> dimensionMap = new HashMap<>(defaultDimensionMap);
        dimensionMap.put("d2", "v2");

        List<String> messages = message.getAllValues();
        assertThat(messages, hasSize(3));
        Collections.sort(messages);
        assertThat(messages.get(0), containsString("{\"namespace\":\"main\",\"metrics\":[{\"name\":\"time\",\"value\":2,\"unit\":\"s\"}],\"dimensions\":{\"d1\":\"v1\",\"d2\":\"v2\"},\"timestamp\":"));
        assertThat(messages.get(1), containsString("{\"namespace\":\"th1\",\"metrics\":[{\"name\":\"error\",\"value\":1,\"unit\":\"\"}],\"dimensions\":{\"d1\":\"v1\"},\"timestamp\":"));
        assertThat(messages.get(2), containsString("{\"namespace\":\"th1\",\"metrics\":[{\"name\":\"latency\",\"value\":1,\"unit\":\"s\"}],\"dimensions\":{\"d1\":\"override\"},\"timestamp\":"));
    }

    private Logger setupLoggerSpy(MetricsFactoryImpl mf) {
        Logger loggerSpy = spy(mf.getLogger());
        mf.setLogger(loggerSpy);
        doCallRealMethod().when(loggerSpy).trace(message.capture());
        return loggerSpy;
    }
}
