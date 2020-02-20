package com.aws.iot.evergreen.ipc.services.servicediscovery;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LookupResourceResponse extends ServiceDiscoveryGenericResponse {

    private List<Resource> resources;

    @Builder
    public LookupResourceResponse(ServiceDiscoveryResponseStatus responseStatus, String errorMessage,
                                  List<Resource> resources) {
        super(responseStatus, errorMessage);
        this.resources = resources;
    }
}
