package com.aws.iot.gg2k.client.config;

import java.util.concurrent.TimeUnit;

public class KernelIPCClientConfig {

    private String hostAddress;
    private int port;
    private long requestTimeoutInMillSec;

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

    public static KernellPCClientConfigBuilder builder() { return new KernellPCClientConfigBuilder();}
    public static class KernellPCClientConfigBuilder{
        private String hostAddress;
        private int port;
        private long requestTimeoutInMillSec = TimeUnit.SECONDS.toMillis(30);

        public KernellPCClientConfigBuilder(){
        // get host and port information from env variables.
        }

        public KernellPCClientConfigBuilder hostAddress(final String hostAddress){
            this.hostAddress = hostAddress;
            return this;
        }

        public KernellPCClientConfigBuilder port(final int port){
            this.port = port;
            return this;
        }

        public KernellPCClientConfigBuilder requestTimeoutInMillSec(final long requestTimeoutInMillSec){
            this.requestTimeoutInMillSec = requestTimeoutInMillSec;
            return this;
        }

        public KernelIPCClientConfig build() {
            KernelIPCClientConfig config = new KernelIPCClientConfig();
            config.setHostAddress(hostAddress);
            config.setPort(port);
            config.setRequestTimeoutInMillSec(requestTimeoutInMillSec);
            return config;
        }
    }
}
