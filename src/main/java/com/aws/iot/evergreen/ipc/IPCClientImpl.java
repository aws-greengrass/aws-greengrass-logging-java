package com.aws.iot.evergreen.ipc;

import com.aws.iot.evergreen.ipc.common.GenericErrorCodes;
import com.aws.iot.evergreen.ipc.config.KernelIPCClientConfig;
import com.aws.iot.evergreen.ipc.message.MessageHandler;
import com.aws.iot.evergreen.ipc.services.common.AuthRequestTypes;
import com.aws.iot.evergreen.ipc.services.common.GeneralRequest;
import com.aws.iot.evergreen.ipc.services.common.GeneralResponse;
import com.aws.iot.evergreen.ipc.services.common.SendAndReceiveIPCUtil;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.aws.iot.evergreen.ipc.common.Constants.AUTH_SERVICE;
import static com.aws.iot.evergreen.ipc.common.FrameReader.FrameType.REQUEST;
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
        this.clientSocket.setKeepAlive(true);
        this.reader = new ConnectionReader(clientSocket.getInputStream(), messageHandler);
        this.writer = new ConnectionWriter(clientSocket.getOutputStream());
        new Thread(reader).start();
        try {
            // Send Auth request and wait for response.
            GeneralResponse<Void, GenericErrorCodes> resp = SendAndReceiveIPCUtil.sendAndReceive(this,
                    AUTH_SERVICE,
                    GeneralRequest.builder()
                            .request(config.getToken() == null ? "" : config.getToken())
                            .type(AuthRequestTypes.Auth)
                            .build(),
                    new TypeReference<GeneralResponse<Void, GenericErrorCodes>>() {
                    }).get(); // TODO: Add timeout?
            if (!resp.getError().equals(GenericErrorCodes.Success)) {
                throw new IOException(resp.getErrorMessage());
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new IOException(e);
        }
    }

    public void disconnect() throws IOException {
        reader.close();
        clientSocket.close();
    }

    public CompletableFuture<Message> sendRequest(String destination, Message msg) {
        //TODO: implement timeout for listening to requests https://issues.amazon.com/issues/86453f7c-c94e-4a3c-b8ff-679767e7443c
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

    public static class ConnectionWriter {
        private final DataOutputStream dataOutputStream;

        public ConnectionWriter(OutputStream os) {
            this.dataOutputStream = new DataOutputStream(os);
        }

        public void write(MessageFrame f) throws IOException {
            writeFrame(f, dataOutputStream);
        }
    }

    public static class ConnectionReader implements Runnable {

        private final DataInputStream dataInputStream;
        AtomicBoolean isShutdown = new AtomicBoolean(false);
        private MessageHandler messageHandler;

        public ConnectionReader(InputStream is, MessageHandler messageHandler) {
            this.dataInputStream = new DataInputStream(is);
            this.messageHandler = messageHandler;
        }

        @Override
        public void run() {
            while (!isShutdown.get()) {
                try {
                    MessageFrame messageFrame = readFrame(dataInputStream);
                    messageHandler.handleMessage(messageFrame);
                } catch (Exception e) {
                    if (!isShutdown.get()) {
                        System.out.println("Connection error");
                        e.printStackTrace();
                        break;
                    }
                }
            }
        }

        public void close() {
            isShutdown.set(true);
        }
    }
}
