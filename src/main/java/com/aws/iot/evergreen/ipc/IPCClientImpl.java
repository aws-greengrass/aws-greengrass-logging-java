package com.aws.iot.evergreen.ipc;

import com.aws.iot.evergreen.ipc.config.KernelIPCClientConfig;
import com.aws.iot.evergreen.ipc.message.MessageHandler;


import java.io.*;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.aws.iot.evergreen.ipc.common.Constants.*;
import static com.aws.iot.evergreen.ipc.common.FrameReader.*;
import static com.aws.iot.evergreen.ipc.common.FrameReader.FrameType.REQUEST;


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
        this.clientSocket.setKeepAlive(true);
        this.reader = new ConnectionReader(clientSocket.getInputStream(), messageHandler);
        this.writer = new ConnectionWriter(clientSocket.getOutputStream());
        new Thread(reader).start();
        sendRequest(AUTH_SERVICE, new Message(config.getToken().getBytes()));
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

    private CompletableFuture<Message> sendRequest(String destination, Message msg) {
        //TODO: implement timeout for listening to requests
        MessageFrame frame = new MessageFrame(destination, msg, REQUEST);
        CompletableFuture<Message> future = new CompletableFuture<>();
        messageHandler.registerRequestId(frame.sequenceNumber, future);
        writer.write(frame);
        return future;
    }

    public static class ConnectionWriter {
        private final DataOutputStream dos;

        public ConnectionWriter(OutputStream os) {
            this.dos = new DataOutputStream(os);
        }

        public void write(MessageFrame f) {
            synchronized (dos) {
                try {
                    writeFrame(f, dos);
                } catch (IOException e) {
                    e.printStackTrace();
                }
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
