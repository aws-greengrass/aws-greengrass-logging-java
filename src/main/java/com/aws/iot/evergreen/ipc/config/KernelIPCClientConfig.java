package com.aws.iot.evergreen.ipc.config;

import java.net.URI;
import java.net.URISyntaxException;
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
        private String hostAddress = "127.0.0.1";
        private int port;
        private long requestTimeoutInMillSec = TimeUnit.SECONDS.toMillis(30);
        private String token = System.getenv("SVCUID");

        public KernelIPCClientConfigBuilder(){
            // default host and port information should come from env variables.
            String kernelStuff = System.getenv("AWS_GG_KERNEL_URI");
            if (kernelStuff == null || kernelStuff.isEmpty()) {
                return;
            }
            try {
                URI uri = new URI(kernelStuff);
                this.hostAddress = uri.getHost();
                this.port = uri.getPort();
            } catch (URISyntaxException ignored) {
            }
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
