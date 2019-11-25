package com.aws.iot.evergreen.ipc.config;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class KernelIPCClientConfig {

    private String hostAddress;
    private int port;
    private long requestTimeoutInMillSec;
    private String token;

    private KernelIPCClientConfig(){

    }
    public String getHostAddress() {
        return hostAddress;
    }

    public void setHostAddress(String hostAddress) {
        this.hostAddress = hostAddress;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public long getRequestTimeoutInMillSec() {
        return requestTimeoutInMillSec;
    }

    public void setRequestTimeoutInMillSec(long requestTimeoutInMillSec) {
        this.requestTimeoutInMillSec = requestTimeoutInMillSec;
    }

    public String getToken() { return token; }

    public void setToken(String token) { this.token = token; }

    public static KernelIPCClientConfigBuilder builder() { return new KernelIPCClientConfigBuilder();}

    public static class KernelIPCClientConfigBuilder {
        private String hostAddress;
        private int port;
        private long requestTimeoutInMillSec = TimeUnit.SECONDS.toMillis(30);
        //TODO: read token from env variable
        private String token = UUID.randomUUID().toString();

        public KernelIPCClientConfigBuilder(){
            // default host and port information should come from env variables.
        }

        public KernelIPCClientConfigBuilder hostAddress(final String hostAddress){
            this.hostAddress = hostAddress;
            return this;
        }

        public KernelIPCClientConfigBuilder port(final int port){
            this.port = port;
            return this;
        }

        public KernelIPCClientConfigBuilder token(final String token){
            this.token = token;
            return this;
        }

        public KernelIPCClientConfigBuilder requestTimeoutInMillSec(final long requestTimeoutInMillSec){
            this.requestTimeoutInMillSec = requestTimeoutInMillSec;
            return this;
        }

        public KernelIPCClientConfig build() {
            KernelIPCClientConfig config = new KernelIPCClientConfig();
            config.setHostAddress(hostAddress);
            config.setPort(port);
            config.setRequestTimeoutInMillSec(requestTimeoutInMillSec);
            config.setToken(token);
            return config;
        }
    }
}
