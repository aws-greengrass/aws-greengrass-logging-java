package com.aws.iot.evergreen.ipc.services.servicediscovery;

import java.net.URI;
import java.util.Map;
import java.util.Objects;

public class Resource implements Cloneable {
    public String name;
    public String serviceType;
    public String serviceSubtype;
    public String domain;
    public URI uri;
    public Map<String, String> txtRecords;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Resource resource = (Resource) o;
        return Objects.equals(name, resource.name) &&
                Objects.equals(serviceType, resource.serviceType) &&
                Objects.equals(serviceSubtype, resource.serviceSubtype) &&
                Objects.equals(domain, resource.domain) &&
                Objects.equals(uri, resource.uri) &&
                Objects.equals(txtRecords, resource.txtRecords);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, serviceType, serviceSubtype, domain, uri, txtRecords);
    }

    @Override
    public Resource clone() throws CloneNotSupportedException {
        return (Resource) super.clone();
    }
}
