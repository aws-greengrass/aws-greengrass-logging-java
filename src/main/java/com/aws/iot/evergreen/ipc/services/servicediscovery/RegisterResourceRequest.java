package com.aws.iot.evergreen.ipc.services.servicediscovery;

/**
 * Request to register a resource with Service Discovery
 */
public class RegisterResourceRequest {
    /**
     * Resource to be registered
     */
    public Resource resource;

    /**
     * When true, our service discovery service will publish this record over DNS-SD using avahi or similar
     */
    public boolean publishToDNSSD;
}
