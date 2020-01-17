package com.aws.iot.evergreen.ipc.services.servicediscovery;

public enum ServiceDiscoveryResponseStatus {
    Success,
    Unknown,
    AlreadyRegistered,
    ResourceNotFound,
    ResourceNotOwned;
}
