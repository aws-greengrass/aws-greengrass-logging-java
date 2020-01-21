package com.aws.iot.evergreen.ipc;

import com.aws.iot.evergreen.ipc.common.FrameReader;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public interface IPCClient {

    void disconnect() throws IOException;

    CompletableFuture<FrameReader.Message> sendRequest(String destination, FrameReader.Message msg);

    void registerDestination(String destination, Function<FrameReader.Message, FrameReader.Message> callback);
}
