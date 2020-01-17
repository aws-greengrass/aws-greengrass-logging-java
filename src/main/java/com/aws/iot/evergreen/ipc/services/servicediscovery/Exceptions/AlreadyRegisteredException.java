package com.aws.iot.evergreen.ipc.services.servicediscovery.Exceptions;

public class AlreadyRegisteredException extends ServiceDiscoveryException {
    public AlreadyRegisteredException(Throwable e) {
        super(e);
    }

    public AlreadyRegisteredException(String errorMessage) {
        super(errorMessage);
    }
}
