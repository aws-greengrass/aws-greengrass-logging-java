package com.aws.iot.evergreen.ipc;

import com.aws.iot.evergreen.ipc.common.FrameReader;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public interface IPCClient {

    void connect() throws IOException, InterruptedException;

    void disconnect() throws IOException;

    CompletableFuture<FrameReader.Message> sendRequest(String destination, FrameReader.Message msg);
}
