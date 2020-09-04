package com.aws.iot.evergreen.ipc.services.cli.models;

public enum LifecycleState {
    NEW,
    INSTALLED,
    STARTING,
    RUNNING,
    STOPPING,
    FINISHED,
    ERRORED,
    BROKEN
}
