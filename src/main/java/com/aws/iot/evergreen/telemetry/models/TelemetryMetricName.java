package com.aws.iot.evergreen.telemetry.models;

public interface TelemetryMetricName {

     enum KernelComponents implements TelemetryMetricName {
        NUM_COMPONENTS_STATELESS,
        NUM_COMPONENTS_NEW,
        NUM_COMPONENTS_INSTALLED,
        NUM_COMPONENTS_STARTING,
        NUM_COMPONENTS_RUNNING,
        NUM_COMPONENTS_STOPPING,
        NUM_COMPONENTS_ERRORED,
        NUM_COMPONENTS_BROKEN,
        NUM_COMPONENTS_FINISHED
    }

    enum KernelDeployments implements TelemetryMetricName {
        NUM_DEPLOYMENTS_SUCCESS,
        NUM_DEPLOYMENTS_FAILED
    }

    enum SystemMetrics implements TelemetryMetricName {
        CPU_UTILIZATION,
        RSS,
        FS
    }

    enum Mqtt implements TelemetryMetricName {
        NUM_SUBSCRIPTIONS,
        NUM_CONNECTIONS,
    }
}