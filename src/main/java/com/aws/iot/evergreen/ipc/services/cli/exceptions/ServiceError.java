package com.aws.iot.evergreen.ipc.services.cli.exceptions;

public class ServiceError extends GenericCliIpcServerException {
    public ServiceError(String message) {
        super(message);
    }
}
