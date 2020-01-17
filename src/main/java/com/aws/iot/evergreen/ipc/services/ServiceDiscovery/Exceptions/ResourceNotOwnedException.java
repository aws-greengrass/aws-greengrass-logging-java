package com.aws.iot.evergreen.ipc.services.ServiceDiscovery.Exceptions;

public class ResourceNotOwnedException extends ServiceDiscoveryException {
    public ResourceNotOwnedException(Throwable e) {
        super(e);
    }

    public ResourceNotOwnedException(String errorMessage) {
        super(errorMessage);
    }
}
