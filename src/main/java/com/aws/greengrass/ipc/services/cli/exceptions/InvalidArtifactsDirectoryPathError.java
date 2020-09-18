package com.aws.greengrass.ipc.services.cli.exceptions;

public class InvalidArtifactsDirectoryPathError extends GenericCliIpcServerException {

    public InvalidArtifactsDirectoryPathError(String message) {
        super(message);
    }

}
