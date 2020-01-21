package com.aws.iot.evergreen.ipc.services.lifecycle;

import com.aws.iot.evergreen.ipc.services.lifecycle.exceptions.LifecycleIPCException;

import java.util.function.BiConsumer;

public interface Lifecycle {
    String LIFECYCLE_SERVICE_NAME = "LIFECYCLE";

    void requestStateChange(String newState) throws LifecycleIPCException;
    void listenToStateChanges(String service, BiConsumer<String, String> onTransition) throws LifecycleIPCException;
}
