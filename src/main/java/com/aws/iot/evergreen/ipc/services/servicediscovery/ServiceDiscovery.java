package com.aws.iot.evergreen.ipc.services.servicediscovery;

import com.aws.iot.evergreen.ipc.services.servicediscovery.exceptions.ServiceDiscoveryException;

import java.util.List;

/**
 * Service Discovery client interface. Used to register and query resources registered by other services.
 */
public interface ServiceDiscovery {
    /**
     * Register a resource. Previously registered resources cannot be updated: use updateResource instead.
     *
     * @param request request
     * @return The registered Resource if successful
     * @throws ServiceDiscoveryException for any errors
     */
    Resource registerResource(RegisterResourceRequest request) throws ServiceDiscoveryException;

    /**
     * Update a resource which is already registered. Only the service that originally registered
     * a resource is allowed to update it.
     *
     * @param request request
     * @throws ServiceDiscoveryException for any errors
     */
    void updateResource(UpdateResourceRequest request) throws ServiceDiscoveryException;

    /**
     * Remove a resource from the registry. Only the service that originally registered the resource
     * is allowed to remove it.
     *
     * @param request request
     * @throws ServiceDiscoveryException for any errors
     */
    void removeResource(RemoveResourceRequest request) throws ServiceDiscoveryException;

    /**
     * Lookup a resource or resources. Any fields which are null in the request will be used for a fuzzy
     * match. ie. If you provide only the name, then you may get results with that name with any other
     * service type and subtype.
     *
     * @param request request
     * @return list of resource which matched. May be empty.
     * @throws ServiceDiscoveryException for any errors
     */
    List<Resource> lookupResources(LookupResourceRequest request) throws ServiceDiscoveryException;
}
