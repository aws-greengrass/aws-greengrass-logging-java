/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.ipc.message;

import com.aws.iot.evergreen.ipc.common.BuiltInServiceDestinationCode;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static com.aws.iot.evergreen.ipc.common.FrameReader.FrameType;
import static com.aws.iot.evergreen.ipc.common.FrameReader.Message;
import static com.aws.iot.evergreen.ipc.common.FrameReader.MessageFrame;

public class MessageHandler {
    private final ConcurrentHashMap<Integer, Consumer<MessageFrame>> destinationListener = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, CompletableFuture<Message>> responseMap;

    public MessageHandler() {
        responseMap = new ConcurrentHashMap<>();
    }

    public boolean registerListener(int destination, Consumer<MessageFrame> listener) {
        Consumer<MessageFrame> consumers = destinationListener.putIfAbsent(destination, listener);
        return consumers == null;
    }

    public void registerRequestId(int requestId, CompletableFuture<Message> future) {
        responseMap.put(requestId, future);
    }

    /**
     * Handle an incoming message by completing the saved future.
     *
     * @param incomingMessageFrame the incoming frame
     */
    @SuppressFBWarnings(value = "UCF_USELESS_CONTROL_FLOW", justification = "TODO")
    public void handleMessage(final MessageFrame incomingMessageFrame) {
        Message msg = incomingMessageFrame.message;
        if (FrameType.RESPONSE == incomingMessageFrame.type) {
            CompletableFuture<Message> future = responseMap.remove(incomingMessageFrame.requestId);
            if (msg == null) {
                future.completeExceptionally(new RuntimeException("Request timed out"));
            } else if (incomingMessageFrame.destination == BuiltInServiceDestinationCode.ERROR.getValue()) {
                future.completeExceptionally(
                        new RuntimeException(new String(msg.getPayload(), StandardCharsets.UTF_8)));
            }

            if (future == null || !future.complete(msg)) {
                // TODO: handle the error correctly
                //  https://issues.amazon.com/issues/86453f7c-c94e-4a3c-b8ff-679767e7443c
                // log "Unable to handle response with sequence number  " + incomingMessageFrame.sequenceNumber
            }
        } else {
            Consumer<MessageFrame> consumer = destinationListener.computeIfAbsent(incomingMessageFrame.destination,
                    (destination) -> ((m) -> System.out.println(
                            "Dropping message with request id " + m.requestId + " and destination "
                                    + incomingMessageFrame.destination)));
            try {
                consumer.accept(incomingMessageFrame);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

