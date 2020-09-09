package com.aws.iot.evergreen.ipc.services.shadow.exception;

public class ShadowIPCException extends Exception {
    public ShadowIPCException(Throwable e) {
        super(e);
    }

    public ShadowIPCException(String errorMessage) {
        super(errorMessage);
    }
}
