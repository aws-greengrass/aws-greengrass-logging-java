package com.aws.iot.evergreen.ipc.services.servicediscovery;

import com.aws.iot.evergreen.ipc.common.GenericErrors;

public enum ServiceDiscoveryResponseStatus implements GenericErrors {
    Success, InternalError, InvalidRequest, AlreadyRegistered, ResourceNotFound, ResourceNotOwned;
}
