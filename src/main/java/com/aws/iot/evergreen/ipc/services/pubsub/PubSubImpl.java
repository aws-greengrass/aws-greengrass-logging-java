/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.ipc.services.pubsub;

import com.aws.iot.evergreen.ipc.IPCClient;
import com.aws.iot.evergreen.ipc.IPCClientImpl;
import com.aws.iot.evergreen.ipc.common.FrameReader;
import com.aws.iot.evergreen.ipc.services.common.ApplicationMessage;
import com.aws.iot.evergreen.ipc.services.common.IPCUtil;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import static com.aws.iot.evergreen.ipc.common.BuiltInServiceDestinationCode.PUBSUB;

public class PubSubImpl implements PubSub {
    public static final int API_VERSION = 1;
    private final IPCClient ipc;
    private final Map<String, Consumer<byte[]>> callbacks = new ConcurrentHashMap<>();


    public PubSubImpl(IPCClient ipc) {
        this.ipc = ipc;
        ipc.registerMessageHandler(PUBSUB.getValue(), this::onMessage);
    }

    private FrameReader.Message onMessage(FrameReader.Message message) {
        try {
            ApplicationMessage request = ApplicationMessage.fromBytes(message.getPayload());
            PubSubResponseStatus resp = PubSubResponseStatus.Success;
            if (PubSubServiceOpCodes.PUBLISHED.equals(PubSubServiceOpCodes.values()[request.getOpCode()])) {
                MessagePublishedEvent changedEvent = IPCUtil.decode(request.getPayload(), MessagePublishedEvent.class);

                IPCClientImpl.EXECUTOR.execute(() -> {
                    Consumer<byte[]> cb = callbacks.get(changedEvent.getTopic());
                    // Received message without a callback, try to unsubscribe so the server
                    // stops sending us stuff.
                    if (cb == null) {
                        try {
                            unsubscribeFromTopic(changedEvent.getTopic());
                        } catch (PubSubException ignore) {
                            // TODO: Log exception or something else.
                            //  https://issues.amazon.com/issues/86453f7c-c94e-4a3c-b8ff-679767e7443c
                        }
                    } else {
                        cb.accept(changedEvent.getPayload());
                    }
                });
            } else {
                resp = PubSubResponseStatus.InvalidRequest;
            }
            ApplicationMessage responseMessage =
                    ApplicationMessage.builder().version(request.getVersion()).payload(IPCUtil.encode(resp)).build();

            return new FrameReader.Message(responseMessage.toByteArray());
        } catch (IOException ex) {
            // TODO: Log exception or something else.
            //  https://issues.amazon.com/issues/86453f7c-c94e-4a3c-b8ff-679767e7443c
        }
        return new FrameReader.Message(new byte[0]);
    }

    @Override
    public void publishToTopic(String topic, byte[] payload) throws PubSubException {
        sendAndReceive(PubSubClientOpCodes.PUBLISH, new PubSubPublishRequest(topic, payload),
                PubSubGenericResponse.class);
    }

    @Override
    public synchronized void subscribeToTopic(String topic, Consumer<byte[]> callback) throws PubSubException {
        // Register with IPC to re-register the listener when the client reconnects
        ipc.onReconnect(() -> {
            try {
                // If we haven't unsubscribed, then re-register the subscription
                if (callbacks.containsKey(topic)) {
                    registerSubscription(topic);
                }
            } catch (PubSubException e) {
                // TODO: Log exception / retry
            }
        });

        callbacks.put(topic, callback);
        registerSubscription(topic);
    }

    private void registerSubscription(String topic) throws PubSubException {
        sendAndReceive(PubSubClientOpCodes.SUBSCRIBE, new PubSubSubscribeRequest(topic), PubSubGenericResponse.class);
    }

    @Override
    public void unsubscribeFromTopic(String topic) throws PubSubException {
        callbacks.remove(topic);
        sendAndReceive(PubSubClientOpCodes.UNSUBSCRIBE, new PubSubUnsubscribeRequest(topic),
                PubSubGenericResponse.class);
    }

    private <T> T sendAndReceive(PubSubClientOpCodes opCode, Object request, final Class<T> returnTypeClass)
            throws PubSubException {
        try {
            CompletableFuture<T> responseFuture =
                    IPCUtil.sendAndReceive(ipc, PUBSUB.getValue(), API_VERSION, opCode.ordinal(), request,
                            returnTypeClass);
            PubSubGenericResponse response = (PubSubGenericResponse) responseFuture.get();
            if (!PubSubResponseStatus.Success.equals(response.getStatus())) {
                throwOnError(response);
            }
            return responseFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new PubSubException(e);
        }
    }

    private void throwOnError(PubSubGenericResponse response) throws PubSubException {
        switch (response.getStatus()) {
            default:
                throw new PubSubException(response.getErrorMessage());
        }
    }
}
