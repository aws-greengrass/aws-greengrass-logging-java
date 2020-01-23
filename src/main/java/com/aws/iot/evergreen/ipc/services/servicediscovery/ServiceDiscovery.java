package com.aws.iot.evergreen.ipc.services.servicediscovery;

import com.aws.iot.evergreen.ipc.services.servicediscovery.exceptions.ServiceDiscoveryException;

import java.util.List;

/**
 * Service Discovery client interface. Used to register and query resources registered by other services.
 */
public interface ServiceDiscovery {
    String SERVICE_DISCOVERY_NAME = "SERV_DISCO";

    /**
     * Register a resource. Previously registered resources cannot be updated: use updateResource instead.
     *
     * @param request
     * @return The registered Resource if successful
     * @throws ServiceDiscoveryException
     */
    Resource registerResource(RegisterResourceRequest request) throws ServiceDiscoveryException;

    /**
     * Update a resource which is already registered. Only the service that originally registered
     * a resource is allowed to update it.
     *
     * @param request
     * @throws ServiceDiscoveryException
     */
    void updateResource(UpdateResourceRequest request) throws ServiceDiscoveryException;

    /**
     * Remove a resource from the registry. Only the service that originally registered the resource
     * is allowed to remove it.
     *
     * @param request
     * @throws ServiceDiscoveryException
     */
    void removeResource(RemoveResourceRequest request) throws ServiceDiscoveryException;

    /**
     * Lookup a resource or resources. Any fields which are null in the request will be used for a fuzzy
     * match. ie. If you provide only the name, then you may get results with that name with any other
     * service type and subtype.
     *
     * @param request
     * @return
     * @throws ServiceDiscoveryException
     */
    List<Resource> lookupResources(LookupResourceRequest request) throws ServiceDiscoveryException;
}
