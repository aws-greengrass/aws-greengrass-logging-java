package com.aws.iot.evergreen.ipc.services.cli.exceptions;

public class InvalidRecipesDirectoryPathError extends GenericCliIpcServerException {

    public InvalidRecipesDirectoryPathError(String message) {
        super(message);
    }

}
