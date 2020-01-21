package com.aws.iot.evergreen.ipc.services.servicediscovery;

/**
 * Update resource request
 */
public class UpdateResourceRequest {
    /**
     * Updated resource definition
     */
    public Resource resource;

    /**
     * Updated publish to DNS-SD option
     */
    public boolean publishToDNSSD;
}
