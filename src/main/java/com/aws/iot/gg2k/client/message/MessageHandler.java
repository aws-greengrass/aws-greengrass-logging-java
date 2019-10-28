package com.aws.iot.gg2k.client.message;



import com.aws.iot.gg2k.client.common.FrameReader;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.aws.iot.gg2k.client.common.FrameReader.*;


public class MessageHandler {

    private static final int DEFAULT_QUEUE_SIZE = 1;

    private final ConcurrentHashMap<Integer, List<Consumer<MessageFrame>>> opsListeners = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, BlockingQueue<MessageFrame>> responseMap;

    public MessageHandler() {
        responseMap = new ConcurrentHashMap<>();
    }

    public void registerListerener(int opcode, Consumer<MessageFrame> listerner) {
        List<Consumer<MessageFrame>> consumers = opsListeners.get(opcode);
        if (consumers == null) {
            consumers = new ArrayList<>();
            opsListeners.put(opcode, consumers);
        }
        consumers.add(listerner);
    }

    public void registerRequestId(String requestId) {

        responseMap.put(requestId, new ArrayBlockingQueue<>(DEFAULT_QUEUE_SIZE));
    }

    public Message waitForResponse(final String requestId,
                                               final long timeoutInSeconds, TimeUnit unit) throws InterruptedException {

        Message response = null;
        final BlockingQueue<MessageFrame> queue = responseMap.get(requestId);
        if (queue == null) {
            //   throw new Exception("Missing response registration");
        }

        final MessageFrame respFrame = queue.poll(timeoutInSeconds, unit);
        if (respFrame != null) {
            responseMap.remove(requestId);
            response = respFrame.message;
        }
        return response;
    }

    public void handleMessage(final MessageFrame response) {
        final BlockingQueue<MessageFrame> queue = responseMap.get(response.uuid.toString());
        if (queue != null) {
            final boolean success = queue.add(response);
            if (!success) {

            }
        } else {
            List<Consumer<MessageFrame>> consumers = opsListeners.get(response.message.getOpCode());
            if (consumers != null) {
                consumers.forEach(c -> c.accept(response));
            }
        }
    }
}

