/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.aws.iot.evergreen.logging.impl;

import com.aws.iot.evergreen.logging.impl.config.EvergreenMetricsConfig;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
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
public class MetricsFactoryTest {
    @Captor
    ArgumentCaptor<EvergreenMetricsMessage> message;

    @Test
    public void GIVEN_metricsFactory_WHEN_metrics_are_enabled_THEN_metrics_should_be_logged() {
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
    public void GIVEN_metricsFactory_WHEN_it_has_default_dimensions_THEN_metrics_contains_default_dimensions() {
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
    public void GIVEN_metricsFactory_WHEN_used_by_2_threads_THEN_both_threads_should_emit_metrics() {
        MetricsFactoryImpl mf = (MetricsFactoryImpl) MetricsFactoryImpl.getInstance();
        org.apache.logging.log4j.Logger loggerSpy = setupLoggerSpy(mf);

        mf.addDefaultDimension("d1", "v1");
        Thread thread1 = new Thread(() -> {
            mf.newMetrics().setNamespace("th1").addMetric("error", 1).emit();
            mf.newMetrics().setNamespace("th1").addDimension("d1", "override").addMetric("latency", 1, Duration.UNIT).emit();
        });
        thread1.start();

        mf.newMetrics().setNamespace("main").addDimension("d2", "v2").addMetric("time", 2, Duration.UNIT).emit();
        try {
            thread1.join(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
            fail("Test thread timed out emitting metrics.");
        }

        verify(loggerSpy, times(3)).logMessage(eq(Level.ALL), any(), any(), any(), any(), any());

        Map<String, String> defaultDimensionMap = Collections.singletonMap("d1", "v1");

        Map<String, String> dimensionMap = new HashMap<>();
        dimensionMap.putAll(defaultDimensionMap);
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
