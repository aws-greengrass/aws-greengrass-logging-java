package com.aws.iot.evergreen.ipc.services.lifecycle.exceptions;

public class LifecycleIPCException extends Exception {
    public LifecycleIPCException(Throwable e) {
        super(e);
    }

    public LifecycleIPCException(String errorMessage) {
        super(errorMessage);
    }
}
