package com.aws.iot.evergreen.ipc.services.servicediscovery;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Update resource request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateResourceRequest {
    /**
     * Updated resource definition
     */
    private Resource resource;

    /**
     * Updated publish to DNS-SD option
     */
    private boolean publishToDNSSD;
}
