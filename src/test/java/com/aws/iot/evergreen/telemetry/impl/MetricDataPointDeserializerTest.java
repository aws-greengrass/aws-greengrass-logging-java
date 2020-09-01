/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.telemetry.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class MetricDataPointDeserializerTest {

    @Test
    public void GIVEN_telemetry_log_WHEN_deserialize_THEN_convert_into_MetricDataPoint_Object() throws Exception{
        String s = "{\"M\":{\"NS\":\"KernelComponents\",\"N\":\"NumberOfComponentsStopping\",\"U\":\"Count\"},\"V\":0.0,\"TS\":1598598501520}";
        MetricDataPoint mdp = new ObjectMapper().readValue(s, MetricDataPoint.class);
        assertEquals(mdp.getValue().toString(),"0.0");
        assertEquals(mdp.getMetric().getMetricName().toString(),"NumberOfComponentsStopping");
    }
}
