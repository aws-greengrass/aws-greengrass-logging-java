package com.aws.greengrass.ipc.services.cli.exceptions;

import com.aws.greengrass.ipc.services.cli.exceptions.GenericCliIpcServerException;

public class InvalidArgumentsError extends GenericCliIpcServerException {

    public InvalidArgumentsError(String message) {
        super(message);
    }
}
