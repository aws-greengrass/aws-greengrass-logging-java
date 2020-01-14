package com.aws.iot.evergreen.ipc.services.ServiceDiscovery;

import java.net.URI;
import java.util.Map;

public class UpdateResourceRequest {
    public String name;
    public String serviceType;
    public String serviceSubtype;
    public String domain = ".local";
    public URI uri;
    public Map<String, String> txtRecords;
    public boolean publishToDNSSD;
}
