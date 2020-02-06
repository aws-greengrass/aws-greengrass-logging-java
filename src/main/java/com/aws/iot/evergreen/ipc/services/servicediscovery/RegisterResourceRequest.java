package com.aws.iot.evergreen.ipc.services.servicediscovery;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to register a resource with Service Discovery.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterResourceRequest {
    /**
     * Resource to be registered.
     */
    private Resource resource;

    /**
     * When true, our service discovery service will publish this record over DNS-SD using avahi or similar.
     */
    @SuppressWarnings({"checkstyle:abbreviationaswordinname"})
    private boolean publishToDNSSD;
}
