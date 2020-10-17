/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.greengrass.ipc.services.common;

import com.aws.greengrass.ipc.IPCClient;
import com.aws.greengrass.ipc.common.FrameReader;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Future;

public class IPCUtil {
    private static final ObjectMapper mapper = new CBORMapper().disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

    /**
     * Constructs an application packet and embeds that in a protocol packet before sending it to IPC client.
     * @param ipc Core IPC client used to send the message
     * @param destination Application level destination on the kernel side
     * @param version Application level API Protocol
     * @param opCode Application level request type
     * @param data actual data that need to be exchanges
     * @param returnType return type
     * @return response returned by the kernel cast into type returnType
     */
    public static <T> CompletableFuture<T> sendAndReceive(IPCClient ipc, int destination, int version, int opCode,
                                                          Object data, final Class<T> returnType) {
        byte[] payload;
        try {
            payload = encode(data);
        } catch (IOException e) {
            CompletableFuture<T> f = new CompletableFuture<>();
            f.completeExceptionally(e);
            return f;
        }
        ApplicationMessage request = new ApplicationMessage(version, opCode, payload);
        CompletableFuture<FrameReader.Message> fut = ipc.sendRequest(destination,
                new FrameReader.Message(request.toByteArray()));
        return fut.thenApply((message) -> {
            try {
                ApplicationMessage response = ApplicationMessage.fromBytes(message.getPayload());
                if (response.getVersion() != version) {
                    throw new IllegalArgumentException(String.format(
                            "Protocol version not supported requested %d, returned %d",
                            version, response.getVersion()));
                }
                return decode(response.getPayload(), returnType);
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        });
    }

    /**
     * Constructs an application packet and embeds that in a protocol packet before sending it to IPC client.
     * @param ipc Core IPC client used to send the message
     * @param destination Application level destination on the kernel side
     * @param version Application level API Protocol
     * @param opCode Application level request type
     * @param data actual data that need to be exchanges
     * @return response returned by the kernel cast into type returnType
     */
    public static CompletableFuture<ApplicationMessage> sendAndReceive(IPCClient ipc, int destination, int version,
                                                                       int opCode, Object data) {
        byte[] payload;
        try {
            payload = encode(data);
        } catch (IOException e) {
            CompletableFuture<ApplicationMessage> f = new CompletableFuture<>();
            f.completeExceptionally(e);
            return f;
        }
        ApplicationMessage request = new ApplicationMessage(version, opCode, payload);
        CompletableFuture<FrameReader.Message> fut = ipc.sendRequest(destination,
                new FrameReader.Message(request.toByteArray()));
        return fut.thenApply((message) -> {
                ApplicationMessage response = ApplicationMessage.fromBytes(message.getPayload());
                if (response.getVersion() != version) {
                    throw new IllegalArgumentException(String.format(
                            "Protocol version not supported requested %d, returned %d",
                            version, response.getVersion()));
                }
                return response;
        });
    }

    public static byte[] encode(Object o) throws IOException {
        return mapper.writeValueAsBytes(o);
    }

    public static <T> T decode(byte[] data, final Class<T> returnTypeClass) throws IOException {
        return mapper.readValue(data, returnTypeClass);
    }
}