package com.aws.iot.evergreen.ipc;

import com.aws.iot.evergreen.ipc.common.FrameReader;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public interface IPCClient {

    boolean ping();
    void disconnect() throws IOException;
    void connect() throws IOException, InterruptedException;
    CompletableFuture<FrameReader.Message> sendRequest(String destination, FrameReader.Message msg);
}
