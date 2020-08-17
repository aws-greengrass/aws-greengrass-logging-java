package com.aws.iot.evergreen.ipc.services.cli.exceptions;

import com.aws.iot.evergreen.ipc.services.cli.exceptions.GenericCliIpcServerException;

public class InvalidArgumentsError extends GenericCliIpcServerException {

    public InvalidArgumentsError(String message) {
        super(message);
    }
}
