package com.aws.iot.evergreen.ipc.services.cli.exceptions;

public class InvalidArtifactsDirectoryPathError extends GenericCliIpcServerException {

    public InvalidArtifactsDirectoryPathError(String message) {
        super(message);
    }

}
