package com.aws.iot.evergreen.ipc.services.ServiceDiscovery.Exceptions;

public class ServiceDiscoveryException extends Exception {
    public ServiceDiscoveryException(Throwable e) {
        super(e);
    }

    public ServiceDiscoveryException(String errorMessage) {
        super(errorMessage);
    }
}
