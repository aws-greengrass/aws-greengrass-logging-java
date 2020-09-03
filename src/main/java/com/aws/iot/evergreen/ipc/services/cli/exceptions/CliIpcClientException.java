package com.aws.iot.evergreen.ipc.services.cli.exceptions;

public class CliIpcClientException extends Exception {

    public CliIpcClientException(String message) {
        super(message);
    }

    public CliIpcClientException(Throwable e) {
        super(e);
    }
}
