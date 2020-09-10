/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.telemetry.impl;

import com.aws.iot.evergreen.logging.api.Logger;
import com.aws.iot.evergreen.telemetry.impl.config.TelemetryConfig;
import com.aws.iot.evergreen.telemetry.models.TelemetryAggregation;
import com.aws.iot.evergreen.telemetry.models.TelemetryMetricName;
import com.aws.iot.evergreen.telemetry.models.TelemetryNamespace;
import com.aws.iot.evergreen.telemetry.models.TelemetryUnit;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
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
    protected Path tempRootDir;
    @Captor
    ArgumentCaptor<String> message;

    @BeforeEach
    public void setup() {
        System.setProperty("root", tempRootDir.toAbsolutePath().toString());
    }

    @Test
    void GIVEN_metricsFactory_with_null_or_empty_storePath_THEN_generic_log_file_is_created() {
        new MetricFactory("");
        File logFile = new File(TelemetryConfig.getTelemetryDirectory() + "/generic.log");
        assertTrue(logFile.exists());

        new MetricFactory(null);
        logFile = new File(TelemetryConfig.getTelemetryDirectory() + "/generic.log");
        assertTrue(logFile.exists());
    }

    @Test
    void GIVEN_metricsFactory_with_storeName_argument_THEN_log_file_with_storeName_is_created() {
        new MetricFactory("storePathTest");
        File logFile = new File(TelemetryConfig.getTelemetryDirectory() + "/storePathTest.log");
        assertTrue(logFile.exists());
    }

    @Test
    void GIVEN_metricsFactory_with_no_argument_THEN_generic_log_file_is_created() {
        new MetricFactory();
        File logFile = new File(TelemetryConfig.getTelemetryDirectory() + "/generic.log");
        assertTrue(logFile.exists());
    }

    @Test
    void GIVEN_metricsFactory_with_some_storePath_THEN_metrics_logger_specific_to_the_storePath_is_created() {
        MetricFactory mf = new MetricFactory();
        Logger loggerSpy = setupLoggerSpy(mf);
        assertEquals(loggerSpy.getName(), "Metrics-generic");

        mf = new MetricFactory(null);
        loggerSpy = setupLoggerSpy(mf);
        assertEquals(loggerSpy.getName(), "Metrics-generic");

        mf = new MetricFactory("");
        loggerSpy = setupLoggerSpy(mf);
        assertEquals(loggerSpy.getName(), "Metrics-generic");

        mf = new MetricFactory("MetricLoggerTest");
        loggerSpy = setupLoggerSpy(mf);
        assertEquals(loggerSpy.getName(), "Metrics-MetricLoggerTest");
    }

    @Test
    void GIVEN_metricsFactory_WHEN_metrics_are_enabled_THEN_metrics_should_be_logged() {
        MetricFactory mf = new MetricFactory("EnableMetricsTests");
        Logger loggerSpy = setupLoggerSpy(mf);
        doCallRealMethod().when(loggerSpy).trace(message.capture());

        Metric m = Metric.builder()
                .namespace(TelemetryNamespace.SystemMetrics)
                .name(TelemetryMetricName.CpuUsage)
                .unit(TelemetryUnit.Percent)
                .aggregation(TelemetryAggregation.Average)
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
    void GIVEN_metricsFactory_WHEN_used_by_2_threads_THEN_both_threads_should_emit_metrics_AND_write_to_correct_files()
            throws InterruptedException, ExecutionException, TimeoutException, IOException {
        MetricFactory mf = new MetricFactory("EmitMetricsWithThreads");
        Logger loggerSpy = setupLoggerSpy(mf);
        doCallRealMethod().when(loggerSpy).trace(message.capture());

        Metric m1 = Metric.builder()
                .namespace(TelemetryNamespace.SystemMetrics)
                .name(TelemetryMetricName.CpuUsage)
                .unit(TelemetryUnit.Percent)
                .aggregation(TelemetryAggregation.Average)
                .build();

        Metric m2 = Metric.builder()
                .namespace(TelemetryNamespace.KernelComponents)
                .name(TelemetryMetricName.NumberOfComponentsInstalled)
                .unit(TelemetryUnit.Count)
                .aggregation(TelemetryAggregation.Average)
                .build();

        CyclicBarrier start = new CyclicBarrier(2);
        ExecutorService ses = Executors.newFixedThreadPool(2);
        Future future1 = ses.submit(() -> {
            try {
                start.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                fail("Error starting thread1 in sync", e);
            }
            mf.addMetric(m1).putMetricData(400).emit();
            mf.addMetric(m2).putMetricData(200).emit();
        });
        Future future2 = ses.submit(() -> {
            try {
                start.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                fail("Error starting thread2 in sync", e);
            }
            mf.addMetric(m1).putMetricData(300).emit();
            mf.addMetric(m2).putMetricData(100).emit();
        });
        future1.get(5, TimeUnit.SECONDS);
        future2.get(5, TimeUnit.SECONDS);
        ses.shutdown();

        // all the metric messages are logged as trace messages
        verify(loggerSpy, times(4)).trace(any());
        String path = TelemetryConfig.getTelemetryDirectory() + "/EmitMetricsWithThreads.log";
        File logFile = new File(path);
        // file exists
        assertTrue(logFile.exists());
        ObjectMapper mapper = new ObjectMapper();
        List<String> messages = new ArrayList<>();
        Files
                .lines(Paths.get(path))
                .forEach(s -> {
                    try {
                        //get only the message part of the log that contains metric info
                        messages.add(mapper.readTree(s).get("message").asText());
                    } catch (JsonProcessingException e) {
                        fail("Unable to parse the log.");
                    }
                });

        // file has four entries of the emitted metrics
        assertThat(messages, hasSize(4));
        Collections.sort(messages);
        List<MetricDataPoint> mdp = new ArrayList<>();
        for (String s : messages) {
            mdp.add(mapper.readValue(s, MetricDataPoint.class));
        }
        // assert the values of the metric
        assertEquals((Integer) mdp.get(0).getValue(), 100);
        assertEquals((Integer) mdp.get(1).getValue(), 200);
        assertEquals((Integer) mdp.get(2).getValue(), 300);
        assertEquals((Integer) mdp.get(3).getValue(), 400);
        // assert the metric attributes in order
        assertEquals(mdp.get(0).getMetric().getNamespace(), TelemetryNamespace.KernelComponents);
        assertEquals(mdp.get(1).getMetric().getAggregation(), TelemetryAggregation.Average);
        assertEquals(mdp.get(2).getMetric().getName(), TelemetryMetricName.CpuUsage);
        assertEquals(mdp.get(3).getMetric().getUnit(), TelemetryUnit.Percent);

        logFile = new File(TelemetryConfig.getTelemetryDirectory() + "/evergreen.log");
        // evergreen.log file does not exist
        assertFalse(logFile.exists());

    }

    private Logger setupLoggerSpy(MetricFactory mf) {
        Logger loggerSpy = spy(mf.getLogger());
        mf.setLogger(loggerSpy);
        return loggerSpy;
    }
}
