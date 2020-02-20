package com.aws.iot.evergreen.ipc.services.servicediscovery;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterResourceResponse extends ServiceDiscoveryGenericResponse {

    /**
     * Resource that was registered.
     */
    private Resource resource;

    @Builder
    public RegisterResourceResponse(ServiceDiscoveryResponseStatus responseStatus,
                                    String errorMessage, Resource resource) {
        super(responseStatus, errorMessage);
        this.resource = resource;
    }
}
