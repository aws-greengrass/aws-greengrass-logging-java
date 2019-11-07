package com.aws.iot.evergreen.ipc;

import java.io.IOException;

public interface IPCClient {

    boolean ping();
    void disconnect() throws IOException;
    void connect() throws IOException, InterruptedException;
}
