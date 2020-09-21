/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.greengrass.telemetry.impl;

import com.aws.greengrass.logging.api.Logger;
import com.aws.greengrass.telemetry.impl.config.TelemetryConfig;
import com.aws.greengrass.telemetry.models.TelemetryAggregation;
import com.aws.greengrass.telemetry.models.TelemetryUnit;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @AfterEach
    public void cleanup() {
        TelemetryConfig.getInstance().close();
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
                .namespace("SystemMetrics")
                .name("CpuUsage")
                .unit(TelemetryUnit.Percent)
                .aggregation(TelemetryAggregation.Average)
                .build();

        TelemetryConfig.getInstance().setMetricsEnabled(false);
        mf.putMetricData(m, 80);
        verify(loggerSpy, times(0)).trace(any());

        TelemetryConfig.getInstance().setMetricsEnabled(true);
        mf.putMetricData(m, 80);
        verify(loggerSpy, times(1)).trace(any());
        assertThat(message.getValue(), containsString("CpuUsage"));
    }

    @Test
    void GIVEN_metricsFactory_WHEN_used_by_2_threads_THEN_both_threads_should_emit_metrics_AND_write_to_correct_files()
            throws InterruptedException, ExecutionException, TimeoutException, IOException {
        MetricFactory mf = new MetricFactory("EmitMetricsWithThreads");
        Logger loggerSpy = setupLoggerSpy(mf);
        doCallRealMethod().when(loggerSpy).trace(message.capture());
        ExecutorService ses = Executors.newFixedThreadPool(2);
        Future future1 = ses.submit(() -> {
            Metric m1 = new Metric("SystemMetrics", "CpuUsage", TelemetryUnit.Percent, TelemetryAggregation.Average);
            Metric m2 = new Metric("KernelComponents", "NumberOfComponentsInstalled", TelemetryUnit.Count,
                    TelemetryAggregation.Average);
            mf.putMetricData(m1, 400);
            mf.putMetricData(m2, 200);

        });
        Future future2 = ses.submit(() -> {
            Metric m1 = new Metric("SystemMetrics", "CpuUsage", TelemetryUnit.Percent, TelemetryAggregation.Average);
            Metric m2 = new Metric("KernelComponents", "NumberOfComponentsInstalled", TelemetryUnit.Count,
                    TelemetryAggregation.Average);
            mf.putMetricData(m1, 300);
            mf.putMetricData(m2, 100);
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
        List<Metric> mdp = new ArrayList<>();
        for (String s : messages) {
            mdp.add(mapper.readValue(s, Metric.class));
        }
        // assert the values of the metric
        assertEquals((Integer) mdp.get(0).getValue(), 100);
        assertEquals((Integer) mdp.get(1).getValue(), 200);
        assertEquals((Integer) mdp.get(2).getValue(), 300);
        assertEquals((Integer) mdp.get(3).getValue(), 400);
        // assert the metric attributes in order
        assertEquals(mdp.get(0).getNamespace(), "KernelComponents");
        assertEquals(mdp.get(1).getAggregation(), TelemetryAggregation.Average);
        assertEquals(mdp.get(2).getName(), "CpuUsage");
        assertEquals(mdp.get(3).getUnit(), TelemetryUnit.Percent);

        logFile = new File(TelemetryConfig.getTelemetryDirectory() + "/greengrass.log");
        // greengrass.log file does not exist
        assertFalse(logFile.exists());
    }

    @Test
    void GIVEN_Metrics_Factory_WHEN_invalid_metric_is_passed_THEN_throw_exception() {
        MetricFactory mf = new MetricFactory("EnableMetricsTests");

        assertThrows(IllegalArgumentException.class, () -> {
            Metric m = Metric.builder().namespace("").name("CpuUsage").unit(TelemetryUnit.Percent)
                    .aggregation(TelemetryAggregation.Average).build();
            mf.putMetricData(m, 80);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            Metric m = Metric.builder().namespace("SystemMetrics").name("").unit(TelemetryUnit.Percent)
                    .aggregation(TelemetryAggregation.Average).build();
            mf.putMetricData(m, 80);
        });
    }

    @Test
    void GIVEN_metricsFactory_WHEN_store_root_path_changes_THEN_metrics_are_logged_at_new_path() throws IOException {
        MetricFactory mf1 = new MetricFactory("someFile");
        Metric m = Metric.builder().namespace("A").name("B").unit(TelemetryUnit.Percent).value(10)
                .aggregation(TelemetryAggregation.Average).timestamp((long) 10).build();

        Path path1 = TelemetryConfig.getTelemetryDirectory();
        mf1.putMetricData(m);

        assertTrue(new File(path1 + "/someFile.log").exists());  // file exists

        // case 1: New root directory does not exist
        Path path2 = tempRootDir.resolve("someNewRootDir");
        TelemetryConfig.getInstance().setRoot(path2);
        mf1.putMetricData(m);

        // path1 is renamed to path2. So file not exists at path1
        assertFalse(new File(path1 + "/someFile.log").exists());

        // telemetry root directory changed to new path
        path2 = path2.resolve(TelemetryConfig.TELEMETRY_DIRECTORY);
        assertEquals(path2, TelemetryConfig.getTelemetryDirectory());

        // file exists at new path
        assertTrue(new File(path2 + "/someFile.log").exists());

        // case 2: New root directory exists and is not empty

        //create path with a file in it to make it non empty
        path1 = tempRootDir.resolve("nonEmptyDir");
        Files.createDirectory(path1);

        Path file = Files.createFile(path1.resolve("newFile"));
        assertTrue(Files.exists(file));

        TelemetryConfig.getInstance().setRoot(path1);
        // telemetry root directory changed to new path
        path1 = path1.resolve(TelemetryConfig.TELEMETRY_DIRECTORY);
        assertEquals(path1, TelemetryConfig.getTelemetryDirectory());
        mf1.putMetricData(m);
        assertTrue(new File(path1 + "/someFile.log").exists());  // File exists at new path.
    }

    private Logger setupLoggerSpy(MetricFactory mf) {
        Logger loggerSpy = spy(mf.getLogger());
        mf.setLogger(loggerSpy);
        return loggerSpy;
    }
}
