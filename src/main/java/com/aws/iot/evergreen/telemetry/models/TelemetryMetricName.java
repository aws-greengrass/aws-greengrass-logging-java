/*
 * Copyright Amazon.com Inc. or its affiliates.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.telemetry.models;

public interface TelemetryMetricName {

     enum KernelComponents implements TelemetryMetricName {
         NumberOfComponentsStateless,
         NumberOfComponentsNew,
         NumberOfComponentsInstalled,
         NumberOfComponentsStarting,
         NumberOfComponentsRunning,
         NumberOfComponentsStopping,
         NumberOfComponentsErrored,
         NumberOfComponentsBroken,
         NumberOfComponentsFinished
    }

    enum KernelDeployments implements TelemetryMetricName {
        NumberOfDeploymentsSuccess,
        NumberOfDeploymentsFailed
    }

    enum SystemMetrics implements TelemetryMetricName {
         CpuUsage,
         SystemMemUsage,
         TotalNumberOfFDs
    }

    enum Mqtt implements TelemetryMetricName {
        NumberOfSubscriptions,
        NumberOfConnections,
    }
}