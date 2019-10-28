package com.aws.iot.gg2k.client;

import com.aws.iot.gg2k.client.common.Contants;
import com.aws.iot.gg2k.client.common.FrameReader.*;
import com.aws.iot.gg2k.client.config.KernellPCClientConfig;
import com.aws.iot.gg2k.client.message.MessageHandler;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

import static com.aws.iot.gg2k.client.common.FrameReader.*;
import static com.aws.iot.gg2k.client.common.FrameReader.RequestType.REQUEST_RESPONSE;

public class KernelIPCClientImpl implements KernelIPCClient {

    private final Socket clientSocket;
    private final MessageHandler messageHandler;
    private final ConnectionWriter writer;
    private final ConnectionReader reader;
    private final KernellPCClientConfig config;

    public KernelIPCClientImpl(KernellPCClientConfig config) throws IOException {
        this.messageHandler = new MessageHandler();
        this.config = config;
        this.clientSocket = new Socket(config.getHostAddress(), config.getPort());
        this.clientSocket.setKeepAlive(true);
        this.reader = new ConnectionReader(clientSocket.getInputStream(), messageHandler);
        this.writer = new ConnectionWriter(clientSocket.getOutputStream());
        new Thread(reader).start();
    }

    public boolean ping() {
        Message msg = new Message(60, REQUEST_RESPONSE, "ping".getBytes());
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

    private Message sendRequest(Message msg) throws Exception {
        MessageFrame frame = new MessageFrame(msg);
        if (msg.getType().equals(REQUEST_RESPONSE))
            messageHandler.registerRequestId(frame.uuid.toString());
        writer.write(frame);

        Message response = null;
        if (msg.getType().equals(REQUEST_RESPONSE)) {

            response = messageHandler.waitForResponse(frame.uuid.toString(), config.getRequestTimeoutInMillSec(), TimeUnit.SECONDS);
            if (response.getOpCode() == Contants.errOpCode) {
                throw new Exception(new String(response.getPayload(), "UTF8"));
            }
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
        boolean running = true;
        private MessageHandler messageHandler;

        public ConnectionReader(InputStream is, MessageHandler messageHandler) {
            this.dis = new DataInputStream(is);
            this.messageHandler = messageHandler;
        }

        @Override
        public void run() {
            while (running) {
                try {
                    MessageFrame messageFrame = readFrame(dis);
                    messageHandler.handleMessage(messageFrame);
                } catch (Exception e) {
                    //log
                    running = false;
                }
            }
        }
    }
}
