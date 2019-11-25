package com.aws.iot.evergreen.ipc.message;

import com.aws.iot.evergreen.ipc.common.Constants;
import com.aws.iot.evergreen.ipc.common.FrameReader;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static com.aws.iot.evergreen.ipc.common.Constants.ERROR_OP_CODE;
import static com.aws.iot.evergreen.ipc.common.FrameReader.*;
import static com.aws.iot.evergreen.ipc.common.FrameReader.Message;
import static com.aws.iot.evergreen.ipc.common.FrameReader.MessageFrame;

public class MessageHandler {


    private final ConcurrentHashMap<Integer, Consumer<MessageFrame>> opsListeners = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, CompletableFuture<Message>> responseMap;

    public MessageHandler() {
        responseMap = new ConcurrentHashMap<>();
    }

    public void registerListener(int opcode, Consumer<MessageFrame> listener) throws Exception {
       Consumer<MessageFrame> consumers = opsListeners.putIfAbsent(opcode, listener);
        if (consumers != null) {
            throw new Exception("blah");
        }
    }

    public void registerRequestId(int requestId, CompletableFuture future) {
        responseMap.put(requestId, future);
    }


    public void handleMessage(final MessageFrame incomingMessageFrame) {
        Message msg = incomingMessageFrame.message;
        if(FrameType.RESPONSE == incomingMessageFrame.type){
            CompletableFuture<Message> future = responseMap.remove(incomingMessageFrame.sequenceNumber);
            if(msg == null){
                future.completeExceptionally(new RuntimeException("Request timed out"));
            }else if(msg.getOpCode() == ERROR_OP_CODE){
                future.completeExceptionally(new RuntimeException(new String(msg.getPayload(), StandardCharsets.UTF_8)));
            }

            if (future == null || !future.complete(msg)) {
                // log "Unable to handle response with sequence number  " + incomingMessageFrame.sequenceNumber
            }
        } else {
            Consumer<MessageFrame> consumer = opsListeners.computeIfAbsent(msg.getOpCode(), (opCode) -> ((m) -> System.out.println("Dropping message with sequence number" + m.sequenceNumber + "opcode " + msg.getOpCode())));
            try {
                consumer.accept(incomingMessageFrame);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

