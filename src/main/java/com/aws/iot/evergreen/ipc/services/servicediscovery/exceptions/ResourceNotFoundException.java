package com.aws.iot.evergreen.ipc.services.servicediscovery.exceptions;

public class ResourceNotFoundException extends ServiceDiscoveryException {
    public ResourceNotFoundException(Throwable e) {
        super(e);
    }

    public ResourceNotFoundException(String errorMessage) {
        super(errorMessage);
    }
}
