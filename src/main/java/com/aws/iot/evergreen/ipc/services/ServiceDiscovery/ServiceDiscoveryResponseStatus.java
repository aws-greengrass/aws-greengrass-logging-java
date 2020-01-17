package com.aws.iot.evergreen.ipc.services.ServiceDiscovery;

public enum ServiceDiscoveryResponseStatus {
    Success,
    Unknown,
    AlreadyRegistered,
    ResourceNotFound,
    ResourceNotOwned;
}
