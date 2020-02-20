package com.aws.iot.evergreen.ipc.services.servicediscovery;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceDiscoveryGenericResponse {

    private ServiceDiscoveryResponseStatus responseStatus;

    private String errorMessage;
}
