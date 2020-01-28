package com.aws.iot.evergreen.ipc.services.servicediscovery;

import com.aws.iot.evergreen.ipc.IPCClient;
import com.aws.iot.evergreen.ipc.services.servicediscovery.exceptions.AlreadyRegisteredException;
import com.aws.iot.evergreen.ipc.services.servicediscovery.exceptions.ResourceNotFoundException;
import com.aws.iot.evergreen.ipc.services.servicediscovery.exceptions.ResourceNotOwnedException;
import com.aws.iot.evergreen.ipc.services.servicediscovery.exceptions.ServiceDiscoveryException;
import com.aws.iot.evergreen.ipc.services.common.GeneralResponse;
import com.aws.iot.evergreen.ipc.services.common.GeneralRequest;
import com.aws.iot.evergreen.ipc.services.common.SendAndReceiveIPCUtil;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.aws.iot.evergreen.ipc.services.servicediscovery.ServiceDiscoveryResponseStatus.*;

public class ServiceDiscoveryImpl implements ServiceDiscovery {
    private final IPCClient ipc;

    public ServiceDiscoveryImpl(IPCClient ipc) {
        this.ipc = ipc;
    }

    // TODO: Needs input validations for all operations

    @Override
    public Resource registerResource(RegisterResourceRequest request) throws ServiceDiscoveryException {
        GeneralRequest<Object, ServiceDiscoveryRequestTypes> req = GeneralRequest.<Object, ServiceDiscoveryRequestTypes>builder()
                .request(request).type(ServiceDiscoveryRequestTypes.register).build();
        return sendAndReceive(req, new TypeReference<GeneralResponse<Resource, ServiceDiscoveryResponseStatus>>() {});
    }

    @Override
    public void updateResource(UpdateResourceRequest request) throws ServiceDiscoveryException {
        GeneralRequest<Object, ServiceDiscoveryRequestTypes> req = GeneralRequest.<Object, ServiceDiscoveryRequestTypes>builder()
                .request(request).type(ServiceDiscoveryRequestTypes.update).build();
        sendAndReceive(req, new TypeReference<GeneralResponse<Void, ServiceDiscoveryResponseStatus>>() {});
    }

    @Override
    public void removeResource(RemoveResourceRequest request) throws ServiceDiscoveryException {
        GeneralRequest<Object, ServiceDiscoveryRequestTypes> req = GeneralRequest.<Object, ServiceDiscoveryRequestTypes>builder()
                .request(request).type(ServiceDiscoveryRequestTypes.remove).build();
        sendAndReceive(req, new TypeReference<GeneralResponse<Void, ServiceDiscoveryResponseStatus>>() {});
    }

    @Override
    public List<Resource> lookupResources(LookupResourceRequest request) throws ServiceDiscoveryException {
        GeneralRequest<Object, ServiceDiscoveryRequestTypes> req = GeneralRequest.<Object, ServiceDiscoveryRequestTypes>builder()
                .request(request).type(ServiceDiscoveryRequestTypes.lookup).build();
        return sendAndReceive(req, new TypeReference<GeneralResponse<List<Resource>, ServiceDiscoveryResponseStatus>>() {});
    }

    private <T> T sendAndReceive(GeneralRequest<Object, ServiceDiscoveryRequestTypes> data, TypeReference<GeneralResponse<T, ServiceDiscoveryResponseStatus>> clazz) throws ServiceDiscoveryException {
        try {
            GeneralResponse<T, ServiceDiscoveryResponseStatus> req = SendAndReceiveIPCUtil.sendAndReceive(ipc, SERVICE_DISCOVERY_NAME, data, clazz).get();
            if (!ServiceDiscoveryResponseStatus.Success.equals(req.getError())) {
                throwOnError(req);
            }

            return req.getResponse();
        } catch (InterruptedException | ExecutionException e) {
            throw new ServiceDiscoveryException(e);
        }
    }

    private void throwOnError(GeneralResponse<?, ServiceDiscoveryResponseStatus> req) throws ServiceDiscoveryException {
        switch (req.getError())  {
            case ResourceNotFound:
                throw new ResourceNotFoundException(req.getErrorMessage());
            case ResourceNotOwned:
                throw new ResourceNotOwnedException(req.getErrorMessage());
            case AlreadyRegistered:
                throw new AlreadyRegisteredException(req.getErrorMessage());
            default:
                throw new ServiceDiscoveryException(req.getErrorMessage());
        }
    }
}
