package com.aws.iot.evergreen.ipc.services.servicediscovery;

public class RegisterResourceRequest {
    public Resource resource;
    public boolean publishToDNSSD; // When true, our service discovery service will publish this record over DNS-SD using avahi or similar
}
