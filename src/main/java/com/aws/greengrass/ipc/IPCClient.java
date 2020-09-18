/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.greengrass.ipc;

import com.aws.greengrass.ipc.common.FrameReader;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public interface IPCClient {

    /**
     * Disconnect the client from the server.
     */
    void disconnect() throws IOException;

    /**
     * Send a request to the server to the given destination.
     *
     * @param destination what service should receive the message
     * @param msg         message to send
     * @return future containing the response (if any)
     */
    CompletableFuture<FrameReader.Message> sendRequest(int destination, FrameReader.Message msg);

    /**
     * Register a destination to receive incoming requests for a given destination.
     *
     * @param destination destination to register
     * @param handler     what to call when a message comes in
     */
    void registerMessageHandler(int destination, Function<FrameReader.Message, FrameReader.Message> handler);

    /**
     * Get this service's name.
     *
     * @return service name
     */
    String getServiceName();


    /**
     * Get this clients's unique id .
     *
     * @return client id
     */
    String getClientId();


    /**
     * Register a listener which needs to be re-run every time we disconnect and reconnect.
     *
     * @param r something to run on reconnect
     */
    void onReconnect(Runnable r);
}
