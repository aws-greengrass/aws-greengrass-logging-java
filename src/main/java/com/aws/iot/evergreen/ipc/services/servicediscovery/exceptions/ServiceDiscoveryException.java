package com.aws.iot.evergreen.ipc.services.servicediscovery.exceptions;

public class ServiceDiscoveryException extends Exception {
    public ServiceDiscoveryException(Throwable e) {
        super(e);
    }

    public ServiceDiscoveryException(String errorMessage) {
        super(errorMessage);
    }
}