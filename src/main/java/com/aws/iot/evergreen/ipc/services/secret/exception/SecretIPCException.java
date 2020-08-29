package com.aws.iot.evergreen.ipc.services.secret.exception;

public class SecretIPCException extends Exception {
    public SecretIPCException(Throwable e) {
        super(e);
    }

    public SecretIPCException(String errorMessage) {
        super(errorMessage);
    }
}