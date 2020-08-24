/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.telemetry.impl;

import com.aws.iot.evergreen.telemetry.impl.config.TelemetryConfig;
import com.aws.iot.evergreen.telemetry.models.TelemetryAggregation;
import com.aws.iot.evergreen.telemetry.models.TelemetryMetricName;
import com.aws.iot.evergreen.telemetry.models.TelemetryNamespace;
import com.aws.iot.evergreen.telemetry.models.TelemetryUnit;
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
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
class MetricFactoryTest {

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
        MetricFactory mf = (MetricFactory) MetricFactory.getInstance();
        Logger loggerSpy = setupLoggerSpy(mf);
        Metric m= Metric.builder()
                .metricNamespace(TelemetryNamespace.Kernel)
                .metricName(TelemetryMetricName.SystemMetrics.CpuUsage)
                .metricUnit(TelemetryUnit.Percent)
                .metricAggregation(TelemetryAggregation.Average)
                .build();

        TelemetryConfig.getInstance().setEnabled(false);
        assertFalse(mf.isMetricsEnabled());

        mf.addMetric(m).putMetricData(80).emit();
        verify(loggerSpy, times(0)).info(any());

        TelemetryConfig.getInstance().setEnabled(true);
        assertTrue(mf.isMetricsEnabled());
        mf.addMetric(m).putMetricData(80).emit();
        verify(loggerSpy, times(1)).info(any());

        assertThat(message.getValue(), containsString("CpuUsage"));
    }

    @Test
    void GIVEN_metricsFactory_WHEN_used_by_2_threads_THEN_both_threads_should_emit_metrics() {
        MetricFactory mf = (MetricFactory) MetricFactory.getInstance();
        Logger loggerSpy = setupLoggerSpy(mf);
        Metric m1 = Metric.builder()
                .metricNamespace(TelemetryNamespace.Kernel)
                .metricName(TelemetryMetricName.SystemMetrics.CpuUsage)
                .metricUnit(TelemetryUnit.Percent)
                .metricAggregation(TelemetryAggregation.Average)
                .build();

        Metric m2 = Metric.builder()
                .metricNamespace(TelemetryNamespace.Kernel)
                .metricName(TelemetryMetricName.SystemMetrics.CpuUsage)
                .metricUnit(TelemetryUnit.Percent)
                .metricAggregation(TelemetryAggregation.Average)
                .build();


        CyclicBarrier start = new CyclicBarrier(2);
        ExecutorService ses = Executors.newFixedThreadPool(2);
        Future future1 = ses.submit(() -> {
            try {
                start.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                fail("Error starting thread1 in sync", e);
            }
            mf.addMetric(m1).putMetricData(100).emit();
            mf.addMetric(m1).putMetricData(120).emit();
        });

        Future future2 = ses.submit(() -> {
            try {
                start.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                fail("Error starting thread2 in sync", e);
            }
            mf.addMetric(m1).putMetricData(150).emit();
            mf.addMetric(m2).putMetricData(180).emit();
        });

        try {
            future1.get(5, TimeUnit.SECONDS);
            future2.get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            fail("Error waiting for child threads to finish", e);
        }
        ses.shutdown();
        verify(loggerSpy, times(4)).info(any());

        List<String> messages = message.getAllValues();
        assertThat(messages, hasSize(4));
        Collections.sort(messages);
        assertThat(messages.get(0), containsString("{\"M\":{\"NS\":\"Kernel\",\"N\":\"CpuUsage\",\"U\":\"Percent\",\"A\":\"Average\",\"D\":null},\"V\":100,\"TS"));
        assertThat(messages.get(1), containsString("{\"M\":{\"NS\":\"Kernel\",\"N\":\"CpuUsage\",\"U\":\"Percent\",\"A\":\"Average\",\"D\":null},\"V\":120,\"TS"));
        assertThat(messages.get(2), containsString("{\"M\":{\"NS\":\"Kernel\",\"N\":\"CpuUsage\",\"U\":\"Percent\",\"A\":\"Average\",\"D\":null},\"V\":150,\"TS"));
        assertThat(messages.get(3), containsString("{\"M\":{\"NS\":\"Kernel\",\"N\":\"CpuUsage\",\"U\":\"Percent\",\"A\":\"Average\",\"D\":null},\"V\":180,\"TS"));
    }
    private Logger setupLoggerSpy(MetricFactory mf) {
        Logger loggerSpy = spy(mf.getLogger());
        mf.setLogger(loggerSpy);
        doCallRealMethod().when(loggerSpy).info(message.capture());
        return loggerSpy;
    }
}
