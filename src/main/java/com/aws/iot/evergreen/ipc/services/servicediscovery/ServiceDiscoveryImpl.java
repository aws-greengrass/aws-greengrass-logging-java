package com.aws.iot.evergreen.ipc.services.servicediscovery;

import com.aws.iot.evergreen.ipc.IPCClient;
import com.aws.iot.evergreen.ipc.services.common.IPCUtil;
import com.aws.iot.evergreen.ipc.services.servicediscovery.exceptions.AlreadyRegisteredException;
import com.aws.iot.evergreen.ipc.services.servicediscovery.exceptions.ResourceNotFoundException;
import com.aws.iot.evergreen.ipc.services.servicediscovery.exceptions.ResourceNotOwnedException;
import com.aws.iot.evergreen.ipc.services.servicediscovery.exceptions.ServiceDiscoveryException;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static com.aws.iot.evergreen.ipc.common.BuiltInServiceDestinationCode.SERVICE_DISCOVERY;

public class ServiceDiscoveryImpl implements ServiceDiscovery {
    public static final int API_VERSION = 1;
    private final IPCClient ipc;


    public ServiceDiscoveryImpl(IPCClient ipc) {
        this.ipc = ipc;
    }

    // TODO: Needs input validations for all operations

    @Override
    public Resource registerResource(RegisterResourceRequest request) throws ServiceDiscoveryException {
        RegisterResourceResponse registerResourceResponse =
                sendAndReceive(ServiceDiscoveryOpCodes.RegisterResource, request, RegisterResourceResponse.class);
        return registerResourceResponse.getResource();
    }

    @Override
    public void updateResource(UpdateResourceRequest request) throws ServiceDiscoveryException {
        sendAndReceive(ServiceDiscoveryOpCodes.UpdateResource, request, ServiceDiscoveryGenericResponse.class);
    }

    @Override
    public void removeResource(RemoveResourceRequest request) throws ServiceDiscoveryException {
        sendAndReceive(ServiceDiscoveryOpCodes.RemoveResource, request, ServiceDiscoveryGenericResponse.class);
    }

    @Override
    public List<Resource> lookupResources(LookupResourceRequest request) throws ServiceDiscoveryException {
        LookupResourceResponse lookupResourceResponse =
                sendAndReceive(ServiceDiscoveryOpCodes.LookupResources, request, LookupResourceResponse.class);
        return lookupResourceResponse.getResources();
    }

    private <T extends ServiceDiscoveryGenericResponse> T sendAndReceive(
            ServiceDiscoveryOpCodes opCode, Object request, final Class<T> returnTypeClass)
            throws ServiceDiscoveryException {

        try {
            CompletableFuture<T> responseFuture = IPCUtil.sendAndReceive(ipc, SERVICE_DISCOVERY.getValue(),
                    opCode.ordinal(), request, API_VERSION, returnTypeClass);
            ServiceDiscoveryGenericResponse response = responseFuture.get();
            if (!ServiceDiscoveryResponseStatus.Success.equals(response.getResponseStatus())) {
                throwOnError(response);
            }
            return responseFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new ServiceDiscoveryException(e);
        }
    }

    private void throwOnError(ServiceDiscoveryGenericResponse response) throws ServiceDiscoveryException {
        switch (response.getResponseStatus()) {
            case ResourceNotFound:
                throw new ResourceNotFoundException(response.getErrorMessage());
            case ResourceNotOwned:
                throw new ResourceNotOwnedException(response.getErrorMessage());
            case AlreadyRegistered:
                throw new AlreadyRegisteredException(response.getErrorMessage());
            default:
                throw new ServiceDiscoveryException(response.getErrorMessage());
        }
    }
}
