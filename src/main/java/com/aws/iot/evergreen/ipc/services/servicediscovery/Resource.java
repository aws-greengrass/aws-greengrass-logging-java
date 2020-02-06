package com.aws.iot.evergreen.ipc.services.servicediscovery;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.net.URI;
import java.util.Map;

/**
 * A Resource
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Resource {
    /**
     * Name of the service. Maps to instance name in DNS-SD
     */
    @Size(max=63)
    private String name;

    /**
     * Type of the service. Maps to service name in DNS-SD
     */
    @Size(max=15)
    private String serviceType;

    /**
     * Resource's protocol. Only TCP or UDP.
     */
    @NotNull
    @Builder.Default
    private final ServiceProtocol serviceProtocol = ServiceProtocol.TCP;

    /**
     * Subtype of the service
     */
    @Size(max=63)
    private String serviceSubtype;

    /**
     * Domain of the service. Default is "local".
     */
    @Size(max=255)
    @Builder.Default
    private String domain = "local";

    /**
     * URI to connect to the resource
     */
    private URI uri;

    /**
     * Generic key-value pairs for extra information about the resource
     */
    private Map<String, String> txtRecords;
}
