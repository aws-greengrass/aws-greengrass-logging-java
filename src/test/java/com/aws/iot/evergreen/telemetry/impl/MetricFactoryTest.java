/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.telemetry.impl;

import com.aws.iot.evergreen.logging.impl.Slf4jLogAdapter;
import com.aws.iot.evergreen.telemetry.impl.config.TelemetryConfig;
import com.aws.iot.evergreen.telemetry.models.TelemetryMetricName;
import com.aws.iot.evergreen.telemetry.models.TelemetryNamespace;
import com.aws.iot.evergreen.telemetry.models.TelemetryUnit;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void GIVEN_metricsFactory_with_null_or_empty_storePath_THEN_generic_log_file_is_created() {
        new MetricFactory("");
        File logFile = new File(tempDir+ "/Telemetry/generic.log");
        assertTrue(logFile.exists());

        new MetricFactory(null);
        logFile = new File(tempDir+ "/Telemetry/generic.log");
        assertTrue(logFile.exists());
    }

    @Test
    void GIVEN_metricsFactory_with_storeName_argument_THEN_log_file_with_storeName_is_created() {
        new MetricFactory("storePathTest");
        File logFile = new File(tempDir + "/Telemetry/storePathTest.log");
        assertTrue(logFile.exists());
    }

    @Test
    void GIVEN_metricsFactory_with_no_argument_THEN_generic_log_file_is_created() {
        new MetricFactory();
        File logFile = new File(tempDir + "/Telemetry/generic.log");
        assertTrue(logFile.exists());
    }

    @Test
    void GIVEN_metricsFactory_with_some_storePath_THEN_metrics_logger_specific_to_the_storePath_is_created() {
        MetricFactory mf = new MetricFactory();
        Slf4jLogAdapter loggerSpy = setupLoggerSpy(mf);
        assertEquals(loggerSpy.getName(),"Metrics-generic");

        mf = new MetricFactory(null);
        loggerSpy = setupLoggerSpy(mf);
        assertEquals(loggerSpy.getName(),"Metrics-generic");


        mf = new MetricFactory("");
        loggerSpy = setupLoggerSpy(mf);
        assertEquals(loggerSpy.getName(),"Metrics-generic");

        mf = new MetricFactory("MetricLoggerTest");
        loggerSpy = setupLoggerSpy(mf);
        assertEquals(loggerSpy.getName(),"Metrics-MetricLoggerTest");
    }

    @Test
    void GIVEN_metricsFactory_WHEN_metrics_are_enabled_THEN_metrics_should_be_logged() {
        MetricFactory mf = new MetricFactory("EnableMetricsTests");
        Slf4jLogAdapter loggerSpy = setupLoggerSpy(mf);
        doCallRealMethod().when(loggerSpy).trace(message.capture());

        Metric m= Metric.builder()
                .metricNamespace(TelemetryNamespace.SystemMetrics)
                .metricName(TelemetryMetricName.CpuUsage)
                .metricUnit(TelemetryUnit.Percent)
                .build();

        TelemetryConfig.getInstance().setMetricsEnabled(false);
        mf.addMetric(m).putMetricData(80).emit();
        verify(loggerSpy, times(0)).trace(any());

        TelemetryConfig.getInstance().setMetricsEnabled(true);
        mf.addMetric(m).putMetricData(80).emit();
        verify(loggerSpy, times(1)).trace(any());
        assertThat(message.getValue(), containsString("CpuUsage"));
    }

    @Test
    void GIVEN_metricsFactory_WHEN_used_by_2_threads_THEN_both_threads_should_emit_metrics() {
        MetricFactory mf = new MetricFactory("EmitMetricsWithThreads");
        Slf4jLogAdapter loggerSpy = setupLoggerSpy(mf);
        doCallRealMethod().when(loggerSpy).trace(message.capture());

        Metric m1 = Metric.builder()
                .metricNamespace(TelemetryNamespace.SystemMetrics)
                .metricName(TelemetryMetricName.CpuUsage)
                .metricUnit(TelemetryUnit.Percent)
                .build();

        Metric m2 = Metric.builder()
                .metricNamespace(TelemetryNamespace.SystemMetrics)
                .metricName(TelemetryMetricName.CpuUsage)
                .metricUnit(TelemetryUnit.Percent)
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
        verify(loggerSpy, times(4)).trace(any());

        List<String> messages = message.getAllValues();
        assertThat(messages, hasSize(4));
        Collections.sort(messages);

        assertThat(messages.get(0), containsString("{\"M\":{\"NS\":\"SystemMetrics\",\"N\":\"CpuUsage\",\"U\":\"Percent\"},\"V\":100,\"TS"));
        assertThat(messages.get(1), containsString("{\"M\":{\"NS\":\"SystemMetrics\",\"N\":\"CpuUsage\",\"U\":\"Percent\"},\"V\":120,\"TS"));
        assertThat(messages.get(2), containsString("{\"M\":{\"NS\":\"SystemMetrics\",\"N\":\"CpuUsage\",\"U\":\"Percent\"},\"V\":150,\"TS"));
        assertThat(messages.get(3), containsString("{\"M\":{\"NS\":\"SystemMetrics\",\"N\":\"CpuUsage\",\"U\":\"Percent\"},\"V\":180,\"TS"));
    }

    private Slf4jLogAdapter setupLoggerSpy(MetricFactory mf) {
        Slf4jLogAdapter loggerSpy = spy(mf.getLogger());
        mf.setLogger(loggerSpy);
        return loggerSpy;
    }
}
