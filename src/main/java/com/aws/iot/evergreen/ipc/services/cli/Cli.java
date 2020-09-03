package com.aws.iot.evergreen.ipc.services.cli;

import com.aws.iot.evergreen.ipc.services.cli.exceptions.CliIpcClientException;
import com.aws.iot.evergreen.ipc.services.cli.exceptions.ComponentNotFoundError;
import com.aws.iot.evergreen.ipc.services.cli.exceptions.GenericCliIpcServerException;
import com.aws.iot.evergreen.ipc.services.cli.exceptions.InvalidArgumentsError;
import com.aws.iot.evergreen.ipc.services.cli.exceptions.InvalidArtifactsDirectoryPathError;
import com.aws.iot.evergreen.ipc.services.cli.exceptions.InvalidComponentConfigurationError;
import com.aws.iot.evergreen.ipc.services.cli.exceptions.InvalidRecipesDirectoryPathError;
import com.aws.iot.evergreen.ipc.services.cli.exceptions.ResourceNotFoundError;
import com.aws.iot.evergreen.ipc.services.cli.exceptions.ServiceError;
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

/**
 * Interface for CLI operations.
 */
public interface Cli {
    /**
     * Get details of a component with given name.
     * @return
     */
    public GetComponentDetailsResponse getComponentDetails(GetComponentDetailsRequest request)
            throws ServiceError, ComponentNotFoundError, GenericCliIpcServerException, CliIpcClientException;

    /**
     * List all the currently running components in the kernel.
     * @return
     */
    public ListComponentsResponse listComponents() throws ServiceError, GenericCliIpcServerException,
            CliIpcClientException;

    /**
     * Restart component with the given name.
     * @param request TBD
     * @return
     */
    public RestartComponentResponse restartComponent(RestartComponentRequest request) throws ServiceError,
            ComponentNotFoundError, GenericCliIpcServerException, CliIpcClientException, InvalidArgumentsError;

    /**
     * Stop component with the given name.
     */
    public StopComponentResponse stopComponent(StopComponentRequest request) throws ServiceError,
            ComponentNotFoundError, CliIpcClientException, InvalidArgumentsError;

    /**
     * Update the recipes and artifacts from given directory to the kernel's local store.
     * @param request TBD
     * @throws ServiceError TBD
     * @throws InvalidArtifactsDirectoryPathError TBD
     * @throws InvalidRecipesDirectoryPathError TBD
     * @throws InvalidArgumentsError TBD
     */
    public void updateRecipesAndArtifacts(UpdateRecipesAndArtifactsRequest request) throws ServiceError,
            InvalidArtifactsDirectoryPathError, InvalidRecipesDirectoryPathError, InvalidArgumentsError,
            CliIpcClientException;

    /**
     * Create a local deployment for a thing group. Add/Remove root components.
     * @param request TBD
     * @throws ServiceError TBD
     * @throws InvalidArgumentsError TBD
     * @throws InvalidComponentConfigurationError TBD
     */
    public CreateLocalDeploymentResponse createLocalDeployment(CreateLocalDeploymentRequest request)
            throws ServiceError, InvalidComponentConfigurationError, CliIpcClientException;

    /**
     * Gets status of local deployment with given Id.
     * @param request TBD
     * @throws ServiceError TBD
     * @throws ResourceNotFoundError TBD
     */
    public GetLocalDeploymentStatusResponse getLocalDeploymentStatus(GetLocalDeploymentStatusRequest request)
            throws ServiceError, ResourceNotFoundError, CliIpcClientException;

    /**
     * Lists the last 5 local deployments with their statuses.
     * @throws ServiceError TBD
     */
    public ListLocalDeploymentResponse listLocalDeployments() throws ServiceError, CliIpcClientException;

}
