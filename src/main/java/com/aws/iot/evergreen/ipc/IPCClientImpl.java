package com.aws.iot.evergreen.ipc;

import com.aws.iot.evergreen.ipc.codec.MessageFrameDecoder;
import com.aws.iot.evergreen.ipc.codec.MessageFrameEncoder;
import com.aws.iot.evergreen.ipc.common.GenericErrorCodes;
import com.aws.iot.evergreen.ipc.config.KernelIPCClientConfig;
import com.aws.iot.evergreen.ipc.handler.InboundMessageHandler;
import com.aws.iot.evergreen.ipc.message.MessageHandler;
import com.aws.iot.evergreen.ipc.services.common.AuthRequestTypes;
import com.aws.iot.evergreen.ipc.services.common.GeneralRequest;
import com.aws.iot.evergreen.ipc.services.common.GeneralResponse;
import com.aws.iot.evergreen.ipc.services.common.IPCUtil;
import com.aws.iot.evergreen.logging.api.Logger;
import com.aws.iot.evergreen.logging.impl.Log4jLogManager;
import com.fasterxml.jackson.core.type.TypeReference;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.aws.iot.evergreen.ipc.common.Constants.AUTH_SERVICE;
import static com.aws.iot.evergreen.ipc.common.FrameReader.FrameType.REQUEST;
import static com.aws.iot.evergreen.ipc.common.FrameReader.FrameType.RESPONSE;
import static com.aws.iot.evergreen.ipc.common.FrameReader.Message;
import static com.aws.iot.evergreen.ipc.common.FrameReader.MessageFrame;


//TODO: implement logging
//TODO: throw ipc client specific runtime exceptions
public class IPCClientImpl implements IPCClient {
    private final MessageHandler messageHandler;
    private final EventLoopGroup eventLoopGroup;
    private final Bootstrap clientBootstrap;
    private Channel channel;
    private volatile boolean shutdownRequested;
    private final Object connectionLock = new Object();
    private String serviceName = null;
    private Set<Runnable> registeredListeners = new CopyOnWriteArraySet<>();

    private Logger log = new Log4jLogManager().getLogger(IPCClient.class);

    /**
     * Construct a client and immediately connect to the server.
     *
     * @param config configuration used to connect to the server
     * @throws IOException          if connection fails
     * @throws InterruptedException if connection times out
     */
    public IPCClientImpl(KernelIPCClientConfig config) throws IOException, InterruptedException {
        this.messageHandler = new MessageHandler();
        eventLoopGroup = new NioEventLoopGroup();
        // Help bootstrapping a channel
        clientBootstrap = new Bootstrap();
        clientBootstrap.group(eventLoopGroup) // associate event loop to channel
                .channel(NioSocketChannel.class) // create a NIO socket channel
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new MessageFrameDecoder());
                        ch.pipeline().addLast(new MessageFrameEncoder());
                        ch.pipeline().addLast(new InboundMessageHandler(messageHandler));
                    }
                }).option(ChannelOption.SO_KEEPALIVE, true).option(ChannelOption.TCP_NODELAY, true);

        connect(config);
    }

    private void connect(KernelIPCClientConfig config) throws InterruptedException, IOException {
        synchronized (connectionLock) {
            if (isConnected()) {
                // Already connected, so don't connect again
                return;
            }

            // Connect to listening server
            ChannelFuture channelFuture = clientBootstrap.connect(config.getHostAddress(), config.getPort()).sync();

            this.channel = channelFuture.channel();

            // If the channel gets closed, then reconnect automatically
            channel.closeFuture().addListener((ChannelFutureListener) future -> {
                // Keep retrying to connect (forever)
                while (true) {
                    try {
                        connect(config);
                        break;
                    } catch (Throwable t) {
                        log.atError().setCause(t).log("Error while reconnecting IPC client. Will continue to retry...");
                    }
                }
            });

            try {
                // Send Auth request and wait for response.
                GeneralResponse<String, GenericErrorCodes> resp = IPCUtil.sendAndReceive(this, AUTH_SERVICE,
                        GeneralRequest.builder().request(config.getToken() == null ? "" : config.getToken())
                                .type(AuthRequestTypes.Auth).build(),
                        new TypeReference<GeneralResponse<String, GenericErrorCodes>>() {
                        }).get(); // TODO: Add timeout waiting for auth to come back?
                // https://issues.amazon.com/issues/86453f7c-c94e-4a3c-b8ff-679767e7443c
                if (!resp.getError().equals(GenericErrorCodes.Success)) {
                    throw new IOException(resp.getErrorMessage());
                }
                serviceName = resp.getResponse();
            } catch (InterruptedException | ExecutionException e) {
                throw new IOException(e);
            }

            // Re-run our listeners before acknowledging that we're connected
            registeredListeners.forEach(Runnable::run);

            // Unlock anyone waiting for an active connection
            connectionLock.notifyAll();
            log.debug("Successfully connected to {}:{}", config.getHostAddress(), config.getPort());
        }
    }

    private boolean isConnected() {
        return channel != null && channel.isActive() || shutdownRequested;
    }

    @Override
    public void disconnect() {
        shutdownRequested = true;
        channel.close();
        eventLoopGroup.shutdownGracefully();
    }

    @Override
    public CompletableFuture<Message> sendRequest(String destination, Message msg) {
        waitForConnected();

        log.debug("Sending message to destination {} on server", destination);
        //TODO: implement timeout for listening to requests
        // https://issues.amazon.com/issues/86453f7c-c94e-4a3c-b8ff-679767e7443c
        MessageFrame frame = new MessageFrame(destination, msg, REQUEST);
        CompletableFuture<Message> future = new CompletableFuture<>();
        messageHandler.registerRequestId(frame.sequenceNumber, future);

        channel.writeAndFlush(frame);
        return future;
    }

    @SuppressWarnings({"checkstyle:emptycatchblock"})
    @SuppressFBWarnings(value = {"UW_UNCOND_WAIT"})
    private void waitForConnected() {
        while (!isConnected()) {
            synchronized (connectionLock) {
                try {
                    log.trace("Waiting to be connected to server");
                    connectionLock.wait();
                    log.trace("Connection done, unlocking");
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    private CompletableFuture<Void> sendResponse(String destination, int sequenceNumber, Message msg) {
        log.debug("Sending response message to destination {} with request id {} on server", destination,
                sequenceNumber);

        //TODO: implement timeout for listening to requests
        MessageFrame frame = new MessageFrame(sequenceNumber, destination, msg, RESPONSE);
        CompletableFuture<Void> future = new CompletableFuture<>();

        channel.writeAndFlush(frame);
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

    @Override
    public String getServiceName() {
        return serviceName;
    }

    @Override
    public void onReconnect(Runnable r) {
        registeredListeners.add(r);
    }
}
