package com.aws.iot.evergreen.ipc.services.cli;

import com.aws.iot.evergreen.ipc.IPCClient;
import com.aws.iot.evergreen.ipc.services.cli.exceptions.CliIpcClientException;
import com.aws.iot.evergreen.ipc.services.cli.exceptions.ComponentNotFoundError;
import com.aws.iot.evergreen.ipc.services.cli.exceptions.GenericCliIpcServerException;
import com.aws.iot.evergreen.ipc.services.cli.exceptions.InvalidArgumentsError;
import com.aws.iot.evergreen.ipc.services.cli.exceptions.InvalidArtifactsDirectoryPathError;
import com.aws.iot.evergreen.ipc.services.cli.exceptions.InvalidComponentConfigurationError;
import com.aws.iot.evergreen.ipc.services.cli.exceptions.InvalidRecipesDirectoryPathError;
import com.aws.iot.evergreen.ipc.services.cli.exceptions.ResourceNotFoundError;
import com.aws.iot.evergreen.ipc.services.cli.exceptions.ServiceError;
import com.aws.iot.evergreen.ipc.services.cli.models.CliGenericResponse;
import com.aws.iot.evergreen.ipc.services.cli.models.CreateLocalDeploymentRequest;
import com.aws.iot.evergreen.ipc.services.cli.models.CreateLocalDeploymentResponse;
import com.aws.iot.evergreen.ipc.services.cli.models.GetComponentDetailsRequest;
import com.aws.iot.evergreen.ipc.services.cli.models.GetComponentDetailsResponse;
import com.aws.iot.evergreen.ipc.services.cli.models.GetLocalDeploymentStatusRequest;
import com.aws.iot.evergreen.ipc.services.cli.models.GetLocalDeploymentStatusResponse;
import com.aws.iot.evergreen.ipc.services.cli.models.ListComponentsResponse;
import com.aws.iot.evergreen.ipc.services.cli.models.ListLocalDeploymentResponse;
import com.aws.iot.evergreen.ipc.services.cli.models.RestartComponentRequest;
import com.aws.iot.evergreen.ipc.services.cli.models.RestartComponentResponse;
import com.aws.iot.evergreen.ipc.services.cli.models.StopComponentRequest;
import com.aws.iot.evergreen.ipc.services.cli.models.StopComponentResponse;
import com.aws.iot.evergreen.ipc.services.cli.models.UpdateRecipesAndArtifactsRequest;
import com.aws.iot.evergreen.ipc.services.common.ApplicationMessage;
import com.aws.iot.evergreen.ipc.services.common.IPCUtil;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import static com.aws.iot.evergreen.ipc.common.BuiltInServiceDestinationCode.CLI;

public class CliImpl implements Cli {

    public static final int API_VERSION = 1;
    private static final String CLI_EXCEPTIONS_PACKAGE = "com.aws.iot.evergreen.ipc.services.cli.exceptions.";

    IPCClient ipcClient;

    public CliImpl(IPCClient ipcClient) {
        this.ipcClient = ipcClient;
    }

    @Override
    public GetComponentDetailsResponse getComponentDetails(GetComponentDetailsRequest request) throws ServiceError,
            ComponentNotFoundError, CliIpcClientException, InvalidArgumentsError {
        Object responseObject = sendAndReceive(CliClientOpCodes.GET_COMPONENT_DETAILS, request,
                GetComponentDetailsResponse.class);
        if (responseObject instanceof GetComponentDetailsResponse) {
            return (GetComponentDetailsResponse) responseObject;
        } else if (responseObject instanceof ComponentNotFoundError) {
            ComponentNotFoundError errorObject = (ComponentNotFoundError) responseObject;
            throw new ComponentNotFoundError(errorObject.getErrorMessage());
        } else if (responseObject instanceof InvalidArgumentsError) {
            InvalidArgumentsError errorObject = (InvalidArgumentsError) responseObject;
            throw new InvalidArgumentsError(errorObject.getErrorMessage());
        } else {
            ServiceError errorObject = (ServiceError) responseObject;
            throw new ServiceError(errorObject.getErrorMessage());
        }
    }

    @Override
    public ListComponentsResponse listComponents() throws ServiceError, CliIpcClientException {

        Object responseObject = sendAndReceive(CliClientOpCodes.LIST_COMPONENTS, "",
                ListComponentsResponse.class);
        if (responseObject instanceof ListComponentsResponse) {
            return (ListComponentsResponse) responseObject;
        } else {
            ServiceError errorObject = (ServiceError) responseObject;
            throw new ServiceError(errorObject.getErrorMessage());
        }
    }

    @Override
    public RestartComponentResponse restartComponent(RestartComponentRequest request) throws ServiceError,
            CliIpcClientException, ComponentNotFoundError, InvalidArgumentsError {
        Object responseObject = sendAndReceive(CliClientOpCodes.RESTART_COMPONENT, request,
                RestartComponentResponse.class);
        if (responseObject instanceof RestartComponentResponse) {
            return (RestartComponentResponse) responseObject;
        } else if (responseObject instanceof ComponentNotFoundError) {
            ComponentNotFoundError errorObject = (ComponentNotFoundError) responseObject;
            throw new ComponentNotFoundError(errorObject.getErrorMessage());
        } else if (responseObject instanceof InvalidArgumentsError) {
            InvalidArgumentsError errorObject = (InvalidArgumentsError) responseObject;
            throw new InvalidArgumentsError(errorObject.getErrorMessage());
        } else {
            ServiceError errorObject = (ServiceError) responseObject;
            throw new ServiceError(errorObject.getErrorMessage());
        }
    }

    @Override
    public StopComponentResponse stopComponent(StopComponentRequest request) throws ServiceError,
            ComponentNotFoundError, CliIpcClientException, InvalidArgumentsError {
        Object responseObject = sendAndReceive(CliClientOpCodes.STOP_COMPONENT, request, StopComponentResponse.class);
        if (responseObject instanceof StopComponentResponse) {
            return (StopComponentResponse) responseObject;
        } else if (responseObject instanceof ComponentNotFoundError) {
            ComponentNotFoundError errorObject = (ComponentNotFoundError) responseObject;
            throw new ComponentNotFoundError(errorObject.getErrorMessage());
        } else if (responseObject instanceof InvalidArgumentsError) {
            InvalidArgumentsError errorObject = (InvalidArgumentsError) responseObject;
            throw new InvalidArgumentsError(errorObject.getErrorMessage());
        } else {
            ServiceError errorObject = (ServiceError) responseObject;
            throw new ServiceError(errorObject.getErrorMessage());
        }
    }

    @Override
    public void updateRecipesAndArtifacts(UpdateRecipesAndArtifactsRequest request) throws ServiceError,
            InvalidArtifactsDirectoryPathError, InvalidRecipesDirectoryPathError, InvalidArgumentsError,
            CliIpcClientException {
        Object responseObject = sendAndReceive(CliClientOpCodes.UPDATE_RECIPES_AND_ARTIFACTS, request,
                CliGenericResponse.class);
        if (responseObject instanceof CliGenericResponse) {
            return;
        } else if (responseObject instanceof InvalidArtifactsDirectoryPathError) {
            InvalidArtifactsDirectoryPathError errorObject = (InvalidArtifactsDirectoryPathError) responseObject;
            throw new InvalidArtifactsDirectoryPathError(errorObject.getErrorMessage());
        } else if (responseObject instanceof InvalidRecipesDirectoryPathError) {
            InvalidRecipesDirectoryPathError errorObject = (InvalidRecipesDirectoryPathError) responseObject;
            throw new InvalidRecipesDirectoryPathError(errorObject.getErrorMessage());
        } else if (responseObject instanceof InvalidArgumentsError) {
            InvalidArgumentsError errorObject = (InvalidArgumentsError) responseObject;
            throw new InvalidArgumentsError(errorObject.getErrorMessage());
        } else {
            ServiceError errorObject = (ServiceError) responseObject;
            throw new ServiceError(errorObject.getErrorMessage());
        }
    }

    @Override
    public CreateLocalDeploymentResponse createLocalDeployment(CreateLocalDeploymentRequest request)
            throws ServiceError, InvalidComponentConfigurationError, CliIpcClientException {
        Object responseObject = sendAndReceive(CliClientOpCodes.CREATE_LOCAL_DEPLOYMENT, request,
                CreateLocalDeploymentResponse.class);
        if (responseObject instanceof CreateLocalDeploymentResponse) {
            return (CreateLocalDeploymentResponse) responseObject;
        } else if (responseObject instanceof InvalidComponentConfigurationError) {
            InvalidComponentConfigurationError errorObject = (InvalidComponentConfigurationError) responseObject;
            throw new InvalidComponentConfigurationError(errorObject.getErrorMessage());
        } else {
            ServiceError errorObject = (ServiceError) responseObject;
            throw new ServiceError(errorObject.getErrorMessage());
        }
    }

    @Override
    public GetLocalDeploymentStatusResponse getLocalDeploymentStatus(GetLocalDeploymentStatusRequest request)
            throws ServiceError, ResourceNotFoundError, CliIpcClientException {
        Object responseObject = sendAndReceive(CliClientOpCodes.GET_LOCAL_DEPLOYMENT_STATUS, request,
                GetLocalDeploymentStatusResponse.class);
        if (responseObject instanceof GetLocalDeploymentStatusResponse) {
            return (GetLocalDeploymentStatusResponse) responseObject;
        } else if (responseObject instanceof ResourceNotFoundError) {
            ResourceNotFoundError errorObject = (ResourceNotFoundError) responseObject;
            throw new ResourceNotFoundError(errorObject.getErrorMessage(), errorObject.getResourceType(), errorObject
                    .getResourceName());
        } else {
            ServiceError errorObject = (ServiceError) responseObject;
            throw new ServiceError(errorObject.getErrorMessage());
        }
    }

    @Override
    public ListLocalDeploymentResponse listLocalDeployments() throws ServiceError, CliIpcClientException {
        Object responseObject = sendAndReceive(CliClientOpCodes.LIST_LOCAL_DEPLOYMENTS, "",
                ListLocalDeploymentResponse.class);
        if (responseObject instanceof ListLocalDeploymentResponse) {
            return (ListLocalDeploymentResponse) responseObject;
        } else {
            ServiceError errorObject = (ServiceError) responseObject;
            throw new ServiceError(errorObject.getErrorMessage());
        }
    }

    private <T> Object sendAndReceive(CliClientOpCodes opCode, Object request, final Class<T> returnTypeClass)
            throws CliIpcClientException {

        String errorType = null;
        CompletableFuture<ApplicationMessage> responseFuture = IPCUtil
                .sendAndReceive(ipcClient, CLI.getValue(), API_VERSION, opCode.ordinal(), request);
        try {
            ApplicationMessage applicationMessage = responseFuture.get();
            CliGenericResponse genericResponse = IPCUtil
                    .decode(applicationMessage.getPayload(), CliGenericResponse.class);
            if (genericResponse.getMessageType() == CliGenericResponse.MessageType.APPLICATION_ERROR) {
                errorType = genericResponse.getErrorType();
                String completeErrorClassPath = CLI_EXCEPTIONS_PACKAGE + errorType;
                return IPCUtil.decode(applicationMessage.getPayload(), Class.forName(completeErrorClassPath, true,
                        this.getClass().getClassLoader()));
            }
            return IPCUtil.decode(applicationMessage.getPayload(), returnTypeClass);
        } catch (InterruptedException | ExecutionException e) {
            throw new CliIpcClientException(e.getMessage());
        } catch (ClassNotFoundException e) {
            throw new CliIpcClientException("Received unknown exception from server: " + errorType);
        } catch (IOException e) {
            throw new CompletionException(e);
        }
    }
}
