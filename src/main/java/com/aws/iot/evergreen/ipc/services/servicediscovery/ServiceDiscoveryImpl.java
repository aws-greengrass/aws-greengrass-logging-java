package com.aws.iot.evergreen.ipc.services.servicediscovery;

import com.aws.iot.evergreen.ipc.IPCClient;
import com.aws.iot.evergreen.ipc.services.servicediscovery.Exceptions.AlreadyRegisteredException;
import com.aws.iot.evergreen.ipc.services.servicediscovery.Exceptions.ResourceNotFoundException;
import com.aws.iot.evergreen.ipc.services.servicediscovery.Exceptions.ResourceNotOwnedException;
import com.aws.iot.evergreen.ipc.services.servicediscovery.Exceptions.ServiceDiscoveryException;
import com.aws.iot.evergreen.ipc.services.common.GeneralResponse;
import com.aws.iot.evergreen.ipc.services.common.GeneralRequest;
import com.aws.iot.evergreen.ipc.services.common.SendAndReceiveIPCUtil;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class ServiceDiscoveryImpl implements ServiceDiscovery {
    private final IPCClient ipc;

    public ServiceDiscoveryImpl(IPCClient ipc) throws Exception {
//        handler.registerListener(SERVICE_DISCOVERY_NAME, listener); -- Register if we need to receive push from server
        this.ipc = ipc;
    }

    // TODO: Needs input validations for all operations

    @Override
    public Resource registerResource(RegisterResourceRequest request) throws ServiceDiscoveryException {
        GeneralRequest<Object, ServiceDiscoveryRequestTypes> req = new GeneralRequest<>();
        req.request = request;
        req.type = ServiceDiscoveryRequestTypes.register;
        return sendAndReceive(req, new TypeReference<GeneralResponse<Resource, ServiceDiscoveryResponseStatus>>() {});
    }

    @Override
    public void updateResource(UpdateResourceRequest request) throws ServiceDiscoveryException {
        GeneralRequest<Object, ServiceDiscoveryRequestTypes> req = new GeneralRequest<>();
        req.request = request;
        req.type = ServiceDiscoveryRequestTypes.update;
        sendAndReceive(req, new TypeReference<GeneralResponse<Void, ServiceDiscoveryResponseStatus>>() {});
    }

    @Override
    public void removeResource(RemoveResourceRequest request) throws ServiceDiscoveryException {
        GeneralRequest<Object, ServiceDiscoveryRequestTypes> req = new GeneralRequest<>();
        req.request = request;
        req.type = ServiceDiscoveryRequestTypes.remove;
        sendAndReceive(req, new TypeReference<GeneralResponse<Void, ServiceDiscoveryResponseStatus>>() {});
    }

    @Override
    public List<Resource> lookupResources(LookupResourceRequest request) throws ServiceDiscoveryException {
        GeneralRequest<Object, ServiceDiscoveryRequestTypes> req = new GeneralRequest<>();
        req.request = request;
        req.type = ServiceDiscoveryRequestTypes.lookup;
        return sendAndReceive(req, new TypeReference<GeneralResponse<List<Resource>, ServiceDiscoveryResponseStatus>>() {});
    }

    private <T> T sendAndReceive(GeneralRequest<Object, ServiceDiscoveryRequestTypes> data, TypeReference<GeneralResponse<T, ServiceDiscoveryResponseStatus>> clazz) throws ServiceDiscoveryException {
        try {
            GeneralResponse<T, ServiceDiscoveryResponseStatus> req = SendAndReceiveIPCUtil.sendAndReceive(ipc, SERVICE_DISCOVERY_NAME, data, clazz).get();
            if (!ServiceDiscoveryResponseStatus.Success.equals(req.error)) {
                throwOnError(req);
            }

            return req.response;
        } catch (InterruptedException | ExecutionException e) {
            throw new ServiceDiscoveryException(e);
        }
    }

    private void throwOnError(GeneralResponse<?, ServiceDiscoveryResponseStatus> req) throws ServiceDiscoveryException {
        switch (req.error)  {
            case ResourceNotFound:
                throw new ResourceNotFoundException(req.errorMessage);
            case ResourceNotOwned:
                throw new ResourceNotOwnedException(req.errorMessage);
            case AlreadyRegistered:
                throw new AlreadyRegisteredException(req.errorMessage);
            default:
                throw new ServiceDiscoveryException(req.errorMessage);
        }
    }
}
