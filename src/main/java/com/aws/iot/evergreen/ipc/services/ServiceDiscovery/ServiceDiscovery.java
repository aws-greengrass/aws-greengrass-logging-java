package com.aws.iot.evergreen.ipc.services.ServiceDiscovery;

import com.aws.iot.evergreen.ipc.services.ServiceDiscovery.Exceptions.ServiceDiscoveryException;

import java.util.List;

public interface ServiceDiscovery {
    public final static String SERVICE_DISCOVERY_NAME = "SERV_DISCO";

    Resource registerResource(RegisterResourceRequest request) throws ServiceDiscoveryException;
    void updateResource(UpdateResourceRequest request) throws ServiceDiscoveryException;
    void removeResource(RemoveResourceRequest request) throws ServiceDiscoveryException;
    List<Resource> lookupResources(LookupResourceRequest request) throws ServiceDiscoveryException;
}
