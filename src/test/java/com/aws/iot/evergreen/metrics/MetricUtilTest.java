/*
 * Copyright Amazon.com Inc. or its affiliates.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.metrics;

import com.aws.iot.evergreen.logging.impl.EvergreenMetricsMessage;
import com.aws.iot.evergreen.logging.impl.Metric;
import com.aws.iot.evergreen.logging.impl.MetricsFactoryImpl;
import com.aws.iot.evergreen.metrics.MetricUtil.Timer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.spy;

@ExtendWith(MockitoExtension.class)
public class MetricUtilTest {
    @Captor
    private ArgumentCaptor<EvergreenMetricsMessage> message;

    @Test
    void GIVEN_timer_WHEN_time_function_THEN_correct_time_is_emitted() throws Exception {
        MetricsFactoryImpl mf = (MetricsFactoryImpl) MetricsFactoryImpl.getInstance();
        org.apache.logging.log4j.Logger loggerSpy = setupLoggerSpy(mf);
        try (Timer timer = new Timer("test", mf.newMetrics())) {
            Thread.sleep(500);
        }

        assertThat(message.getValue().getMetrics(), hasSize(1));
        Metric m = message.getValue().getMetrics().get(0);

        assertThat(m.getName(), equalTo("test"));
        assertThat(m.getUnit().toString(), equalTo("ms"));
        assertThat((long) m.getValue(), is(both(greaterThanOrEqualTo(500L)).and(lessThan(520L))));
    }

    private org.apache.logging.log4j.Logger setupLoggerSpy(MetricsFactoryImpl mf) {
        org.apache.logging.log4j.Logger loggerSpy = spy(mf.getLogger());
        mf.setLogger(loggerSpy);
        doCallRealMethod().when(loggerSpy).logMessage(any(), any(), any(), any(), message.capture(), any());
        return loggerSpy;
    }
}
