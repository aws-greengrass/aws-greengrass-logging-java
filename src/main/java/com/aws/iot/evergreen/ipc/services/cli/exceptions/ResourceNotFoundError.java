package com.aws.iot.evergreen.ipc.services.cli.exceptions;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode (callSuper = true)
@NoArgsConstructor
@Data
public class ResourceNotFoundError extends GenericCliIpcServerException {
    String message;
    String resourceType;
    String resourceName;

    /**
     * Thrown if a resources is not found on the server.
     * @param message TBD
     * @param resourceType TBD
     * @param resourceName TBD
     */
    public ResourceNotFoundError(String message,  String resourceType, String resourceName) {
        super(message);
        this.message = message;
        this.resourceType = resourceType;
        this.resourceName = resourceName;
    }
}
