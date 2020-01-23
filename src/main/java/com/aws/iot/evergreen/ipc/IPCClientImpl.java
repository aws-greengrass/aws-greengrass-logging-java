package com.aws.iot.evergreen.ipc;

import com.aws.iot.evergreen.ipc.config.KernelIPCClientConfig;
import com.aws.iot.evergreen.ipc.message.MessageHandler;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.aws.iot.evergreen.ipc.common.Constants.AUTH_SERVICE;
import static com.aws.iot.evergreen.ipc.common.Constants.PING_SERVICE;
import static com.aws.iot.evergreen.ipc.common.FrameReader.FrameType.REQUEST;
import static com.aws.iot.evergreen.ipc.common.FrameReader.FrameType.RESPONSE;
import static com.aws.iot.evergreen.ipc.common.FrameReader.Message;
import static com.aws.iot.evergreen.ipc.common.FrameReader.MessageFrame;
import static com.aws.iot.evergreen.ipc.common.FrameReader.readFrame;
import static com.aws.iot.evergreen.ipc.common.FrameReader.writeFrame;


//TODO: implement logging
//TODO: throw ipc client specific runtime exceptions
public class IPCClientImpl implements IPCClient {

    private final MessageHandler messageHandler;
    private final KernelIPCClientConfig config;
    private Socket clientSocket;
    private ConnectionWriter writer;
    private ConnectionReader reader;

    public IPCClientImpl(KernelIPCClientConfig config) {
        this.messageHandler = new MessageHandler();
        this.config = config;
    }

    public void connect() throws IOException {
        this.clientSocket = new Socket(config.getHostAddress(), config.getPort());
        this.clientSocket.setTcpNoDelay(true);
        this.clientSocket.setKeepAlive(true);
        this.reader = new ConnectionReader(clientSocket.getInputStream(), messageHandler);
        this.writer = new ConnectionWriter(clientSocket.getOutputStream());
        new Thread(reader).start();
        try {
            // Send Auth request and wait for response.
            Message m = sendRequest(AUTH_SERVICE, new Message(config.getToken() == null ? new byte[0] : config.getToken().getBytes(StandardCharsets.UTF_8))).get();
            // If the response is empty, then we are authenticated successfully
            if (m.getPayload().length > 0) {
                throw new IOException(new String(m.getPayload(), StandardCharsets.UTF_8));
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new IOException(e);
        }
    }

    public boolean ping() {
        Message msg = new Message("ping".getBytes());
        try {
            Message resp = sendRequest(PING_SERVICE, msg).get();
            if (new String(resp.getPayload()).equals("pong")) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public void disconnect() throws IOException {
        reader.close();
        clientSocket.close();
    }

    public CompletableFuture<Message> sendRequest(String destination, Message msg) {
        //TODO: implement timeout for listening to requests
        MessageFrame frame = new MessageFrame(destination, msg, REQUEST);
        CompletableFuture<Message> future = new CompletableFuture<>();
        messageHandler.registerRequestId(frame.sequenceNumber, future);
        try {
            writer.write(frame);
        } catch (IOException e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    private CompletableFuture<Void> sendResponse(String destination, int sequenceNumber, Message msg) {
        //TODO: implement timeout for listening to requests
        MessageFrame frame = new MessageFrame(sequenceNumber, destination, msg, RESPONSE);
        CompletableFuture<Void> future = new CompletableFuture<>();
        try {
            writer.write(frame);
        } catch (IOException e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    @Override
    public void registerDestination(String destination, Function<Message, Message> callback) {
        Consumer<MessageFrame> cb = (MessageFrame mf) -> {
            Message toSend = callback.apply(mf.message);
            sendResponse(destination, mf.sequenceNumber, toSend);
        };
        messageHandler.registerListener(destination, cb);
    }

    public static class ConnectionWriter {
        private final DataOutputStream dos;

        public ConnectionWriter(OutputStream os) {
            this.dos = new DataOutputStream(os);
        }

        public void write(MessageFrame f) throws IOException {
            synchronized (dos) {
                writeFrame(f, dos);
            }
        }
    }

    public static class ConnectionReader implements Runnable {

        private final DataInputStream dis;
        AtomicBoolean running = new AtomicBoolean(true);
        private MessageHandler messageHandler;

        public ConnectionReader(InputStream is, MessageHandler messageHandler) {
            this.dis = new DataInputStream(is);
            this.messageHandler = messageHandler;
        }

        @Override
        public void run() {
            while (running.get()) {
                try {
                    MessageFrame messageFrame = readFrame(dis);
                    messageHandler.handleMessage(messageFrame);
                } catch (Exception e) {
                    if (running.get()) {
                        System.out.println("Connection error");
                        e.printStackTrace();
                        running.set(false);
                    }
                }
            }
        }

        public void close() {
            running.set(false);
        }
    }
}
