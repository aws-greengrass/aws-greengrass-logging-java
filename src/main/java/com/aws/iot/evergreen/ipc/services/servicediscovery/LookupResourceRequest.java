package com.aws.iot.evergreen.ipc.services.servicediscovery;

/**
 * Request to lookup a resource
 */
public class LookupResourceRequest {
    /**
     * Resource to look up. If any field in this resource is null, then a fuzzy search will be performed,
     * accepting any value for the null field.
     */
    public Resource resource;
}
