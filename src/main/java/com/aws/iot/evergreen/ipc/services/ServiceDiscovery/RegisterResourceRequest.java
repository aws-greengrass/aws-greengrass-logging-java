package com.aws.iot.evergreen.ipc.services.ServiceDiscovery;

import java.net.URI;
import java.util.Map;

public class RegisterResourceRequest {
    public String name; // In DNS-SD RFC this is "instance", but Avahi and mDNS call it "name"
    public String serviceType;
    public String serviceSubtype;
    public String domain = ".local";
    public URI uri;
    public Map<String, String> txtRecords;
    public boolean publishToDNSSD; // When true, our service discovery service will publish this record over DNS-SD using avahi or similar
}
