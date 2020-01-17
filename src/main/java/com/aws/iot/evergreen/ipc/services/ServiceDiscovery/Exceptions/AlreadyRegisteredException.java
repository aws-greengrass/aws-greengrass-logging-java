package com.aws.iot.evergreen.ipc.services.ServiceDiscovery.Exceptions;

public class AlreadyRegisteredException extends ServiceDiscoveryException {
    public AlreadyRegisteredException(Throwable e) {
        super(e);
    }

    public AlreadyRegisteredException(String errorMessage) {
        super(errorMessage);
    }
}
