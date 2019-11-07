package com.aws.iot.evergreen.ipc.message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.aws.iot.evergreen.ipc.common.FrameReader.Message;
import static com.aws.iot.evergreen.ipc.common.FrameReader.MessageFrame;

public class MessageHandler {

    private static final int DEFAULT_QUEUE_SIZE = 1;

    private final ConcurrentHashMap<Integer, List<Consumer<MessageFrame>>> opsListeners = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, BlockingQueue<MessageFrame>> responseMap;

    public MessageHandler() {
        responseMap = new ConcurrentHashMap<>();
    }

    public void registerListener(int opcode, Consumer<MessageFrame> listener) {

        List<Consumer<MessageFrame>> consumers = opsListeners.computeIfAbsent(opcode, (c) -> new ArrayList<>());
        consumers.add(listener);
    }

    public void registerRequestId(String requestId) {
        responseMap.put(requestId, new ArrayBlockingQueue<>(DEFAULT_QUEUE_SIZE));
    }

    public Message waitForResponse(final String requestId,
                                               final long requestTimeoutInMillSec, TimeUnit unit) throws InterruptedException {

        Message response = null;
        final BlockingQueue<MessageFrame> queue = responseMap.get(requestId);
        if (queue == null) {
               throw new IllegalArgumentException("requestId not found");
        }

        final MessageFrame respFrame = queue.poll(requestTimeoutInMillSec, unit);
        if (respFrame != null) {
            response = respFrame.message;
        }
        responseMap.remove(requestId);
        return response;
    }

    public void handleMessage(final MessageFrame response) {
        final BlockingQueue<MessageFrame> queue = responseMap.get(response.uuid.toString());
        if (queue == null || !queue.add(response)) {
                throw new IllegalStateException("Unable to handle response with UUID " + response.uuid.toString());
        } else {
            List<Consumer<MessageFrame>> consumers = opsListeners.computeIfAbsent(response.message.getOpCode(), (c) -> Collections.EMPTY_LIST);
            consumers.forEach(c -> {
                try {
                    c.accept(response);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }
}

