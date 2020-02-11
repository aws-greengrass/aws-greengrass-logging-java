package com.aws.iot.evergreen.ipc.exceptions;

public class IPCClientException extends Exception {
    public IPCClientException() {
        super();
    }

    public IPCClientException(String message) {
        super(message);
    }

    public IPCClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public IPCClientException(Throwable cause) {
        super(cause);
    }
}
