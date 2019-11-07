package com.aws.iot.evergreen.ipc;

import com.aws.iot.evergreen.ipc.config.KernelIPCClientConfig;
import com.aws.iot.evergreen.ipc.message.MessageHandler;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.aws.iot.evergreen.ipc.common.Constants.*;
import static com.aws.iot.evergreen.ipc.common.FrameReader.*;


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

    public void connect() throws IOException, InterruptedException {
        this.clientSocket = new Socket(config.getHostAddress(), config.getPort());
        this.clientSocket.setKeepAlive(true);
        this.reader = new ConnectionReader(clientSocket.getInputStream(), messageHandler);
        this.writer = new ConnectionWriter(clientSocket.getOutputStream());
        new Thread(reader).start();
        sendRequest(new Message(AUTH_OP_CODE, config.getToken().getBytes()));
    }

    public boolean ping() {
        Message msg = new Message(PING_OP_CODE, "ping".getBytes());
        try {
            Message resp = sendRequest(msg);
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

    private Message sendRequest(Message msg) throws InterruptedException {
        MessageFrame frame = new MessageFrame(msg);

        messageHandler.registerRequestId(frame.uuid.toString());
        writer.write(frame);

        Message response = messageHandler.waitForResponse(frame.uuid.toString(), config.getRequestTimeoutInMillSec(), TimeUnit.MILLISECONDS);
        // TODO: throw client specific exception
        if (response == null) {
            throw new RuntimeException("Request timed out");
        } else if (response.getOpCode() == ERROR_OP_CODE) {
            throw new RuntimeException(new String(response.getPayload(), StandardCharsets.UTF_8));
        }

        return response;
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
