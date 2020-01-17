package com.aws.iot.evergreen.ipc.services.ServiceDiscovery;

import java.net.URI;
import java.util.Map;

public class Resource {
    public String name;
    public String serviceType;
    public String serviceSubtype;
    public URI uri;
    public Map<String, String> txtRecords;
}
