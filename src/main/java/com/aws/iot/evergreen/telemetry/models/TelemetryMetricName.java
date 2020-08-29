/*
 * Copyright Amazon.com Inc. or its affiliates.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.telemetry.models;

import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public enum TelemetryMetricName {
    NumberOfComponentsStateless(TelemetryNamespace.KernelComponents),
    NumberOfComponentsNew(TelemetryNamespace.KernelComponents),
    NumberOfComponentsInstalled(TelemetryNamespace.KernelComponents),
    NumberOfComponentsStarting(TelemetryNamespace.KernelComponents),
    NumberOfComponentsRunning(TelemetryNamespace.KernelComponents),
    NumberOfComponentsStopping(TelemetryNamespace.KernelComponents),
    NumberOfComponentsErrored(TelemetryNamespace.KernelComponents),
    NumberOfComponentsBroken(TelemetryNamespace.KernelComponents),
    NumberOfComponentsFinished(TelemetryNamespace.KernelComponents),

    NumberOfDeploymentsSuccess(TelemetryNamespace.KernelDeployment),
    NumberOfDeploymentsFailed(TelemetryNamespace.KernelDeployment),

    CpuUsage(TelemetryNamespace.SystemMetrics),
    SystemMemUsage(TelemetryNamespace.SystemMetrics),
    TotalNumberOfFDs(TelemetryNamespace.SystemMetrics),

    NumberOfSubscriptions(TelemetryNamespace.Mqtt),
    NumberOfConnections(TelemetryNamespace.Mqtt);

    private TelemetryNamespace telemetryNamespace;

    TelemetryMetricName(TelemetryNamespace telemetryNamespace) {
        this.telemetryNamespace = telemetryNamespace;
    }

    public boolean belongsTo(TelemetryNamespace telemetryNamespace) {
        return this.telemetryNamespace == telemetryNamespace;
    }

    /**
     * This returns the list of metric names that belong to the given namespace.
     * @param telemetryNamespace TelemetryNamespace enum
     * @return List of enums
     */
    public static List<TelemetryMetricName> getMetricNamesOf(TelemetryNamespace telemetryNamespace) {
        List<TelemetryMetricName> ns = new ArrayList<>();
        for (TelemetryMetricName n : TelemetryMetricName.values()) {
            if (n.telemetryNamespace == telemetryNamespace) {
                ns.add(n);
            }
        }
        return ns;
    }



}