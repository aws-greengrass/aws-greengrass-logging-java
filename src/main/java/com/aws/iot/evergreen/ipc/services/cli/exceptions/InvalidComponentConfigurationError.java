package com.aws.iot.evergreen.ipc.services.cli.exceptions;

public class InvalidComponentConfigurationError extends GenericCliIpcServerException {

    public InvalidComponentConfigurationError(String message) {
        super(message);
    }

}
