package com.aws.iot.evergreen.ipc.services.servicediscovery;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.net.URI;
import java.util.Map;
import java.util.Objects;

/**
 * A Resource
 */
public class Resource implements Cloneable {
    /**
     * Name of the service. Maps to instance name in DNS-SD
     */
    @Size(max=63)
    public String name;

    /**
     * Type of the service. Maps to service name in DNS-SD
     */
    @Size(max=15)
    public String serviceType;

    /**
     * Resource's protocol. Only TCP or UDP.
     */
    @NotNull
    public ServiceProtocol serviceProtocol = ServiceProtocol.TCP;

    /**
     * Subtype of the service
     */
    @Size(max=63)
    public String serviceSubtype;

    /**
     * Domain of the service. Default is "local".
     */
    @Size(max=255)
    public String domain = "local";

    /**
     * URI to connect to the resource
     */
    public URI uri;

    /**
     * Generic key-value pairs for extra information about the resource
     */
    public Map<String, String> txtRecords;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Resource resource = (Resource) o;
        return Objects.equals(name, resource.name) &&
                Objects.equals(serviceType, resource.serviceType) &&
                serviceProtocol == resource.serviceProtocol &&
                Objects.equals(serviceSubtype, resource.serviceSubtype) &&
                Objects.equals(domain, resource.domain) &&
                Objects.equals(uri, resource.uri) &&
                Objects.equals(txtRecords, resource.txtRecords);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, serviceType, serviceProtocol, serviceSubtype, domain, uri, txtRecords);
    }

    @Override
    public Resource clone() throws CloneNotSupportedException {
        return (Resource) super.clone();
    }
}
