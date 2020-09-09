package com.aws.iot.evergreen.ipc.services.cli.exceptions;

public class ComponentNotFoundError extends GenericCliIpcServerException {

    public ComponentNotFoundError(String message) {
        super(message);
    }
}
