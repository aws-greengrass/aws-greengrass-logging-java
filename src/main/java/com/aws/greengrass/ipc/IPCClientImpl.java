/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.greengrass.ipc;

import com.aws.greengrass.ipc.codec.MessageFrameDecoder;
import com.aws.greengrass.ipc.codec.MessageFrameEncoder;
import com.aws.greengrass.ipc.config.KernelIPCClientConfig;
import com.aws.greengrass.ipc.exceptions.IPCClientException;
import com.aws.greengrass.ipc.handler.InboundMessageHandler;
import com.aws.greengrass.ipc.message.MessageHandler;
import com.aws.greengrass.ipc.services.authentication.Authentication;
import com.aws.greengrass.ipc.services.authentication.AuthenticationRequest;
import com.aws.greengrass.ipc.services.authentication.AuthenticationResponse;
import com.aws.greengrass.logging.api.Logger;
import com.aws.greengrass.logging.impl.LogManager;
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
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.aws.greengrass.ipc.codec.MessageFrameEncoder.LENGTH_FIELD_LENGTH;
import static com.aws.greengrass.ipc.codec.MessageFrameEncoder.LENGTH_FIELD_OFFSET;
import static com.aws.greengrass.ipc.codec.MessageFrameEncoder.MAX_PAYLOAD_SIZE;
import static com.aws.greengrass.ipc.common.FrameReader.FrameType.REQUEST;
import static com.aws.greengrass.ipc.common.FrameReader.FrameType.RESPONSE;
import static com.aws.greengrass.ipc.common.FrameReader.Message;
import static com.aws.greengrass.ipc.common.FrameReader.MessageFrame;


//TODO: implement logging
//TODO: throw ipc client specific runtime exceptions
public class IPCClientImpl implements IPCClient {

    public static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();
    private final MessageHandler messageHandler;
    private final EventLoopGroup eventLoopGroup;
    private final Bootstrap clientBootstrap;
    // Lock used so that only 1 thread is performing the connection
    private final ReentrantLock connectionLock = new ReentrantLock(true);
    private final Set<Runnable> onConnectTasks = new CopyOnWriteArraySet<>();
    private final Logger log = LogManager.getLogger(IPCClient.class);
    private Channel channel;
    private volatile boolean shutdownRequested;
    private String serviceName = null;
    private String clientId = null;
    private volatile boolean authenticated;
    private final Authentication authentication;


    /**
     * Construct a client and immediately connect to the server.
     *
     * @param config configuration used to connect to the server
     * @throws IOException          if connection fails
     * @throws InterruptedException if connection times out
     */
    public IPCClientImpl(KernelIPCClientConfig config) throws IPCClientException, InterruptedException {
        this.messageHandler = new MessageHandler();
        eventLoopGroup = new NioEventLoopGroup();
        // Help bootstrapping a channel
        clientBootstrap = new Bootstrap();
        clientBootstrap.group(eventLoopGroup) // associate event loop to channel
                .channel(NioSocketChannel.class) // create a NIO socket channel
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(MAX_PAYLOAD_SIZE, LENGTH_FIELD_OFFSET,
                                LENGTH_FIELD_LENGTH));
                        ch.pipeline().addLast(new MessageFrameDecoder());
                        ch.pipeline().addLast(new MessageFrameEncoder());
                        ch.pipeline().addLast(new InboundMessageHandler(messageHandler));
                    }
                }).option(ChannelOption.SO_KEEPALIVE, true).option(ChannelOption.TCP_NODELAY, true);
        authentication = new Authentication(this);
        connect(config);
    }

    private void connect(KernelIPCClientConfig config) throws IPCClientException, InterruptedException {
        try {
            connectionLock.lock();

            if (isConnectedAndAuthenticated()) {
                // Already connected, so don't connect again
                return;
            }
            authenticated = false;

            // Connect to listening server
            ChannelFuture channelFuture = clientBootstrap.connect(config.getHostAddress(), config.getPort());
            channelFuture.sync();

            this.channel = channelFuture.channel();

            // If the channel gets closed, then reconnect automatically

            channel.closeFuture().addListener((ChannelFutureListener) future -> {
                log.atError().log("Client disconnected");
            });

            AuthenticationRequest request = new AuthenticationRequest(config.getToken());
            AuthenticationResponse authenticationResponse = authentication.doAuthentication(request);
            // Send Authentication request and wait for response.
            serviceName = authenticationResponse.getServiceName();
            clientId = authenticationResponse.getClientId();
            authenticated = true;

            log.debug("Successfully connected to {}:{}", config.getHostAddress(), config.getPort());
        } finally {
            connectionLock.unlock();
        }

        log.debug("Running reconnection tasks");
        onConnectTasks.forEach(Runnable::run);
        log.debug("Done running reconnection tasks");
    }

    public boolean isConnectedAndAuthenticated() {
        return shutdownRequested || channel != null && channel.isActive() && authenticated;
    }

    @Override
    public void disconnect() {
        shutdownRequested = true;
        channel.close();
        eventLoopGroup.shutdownGracefully();
    }

    @Override
    public CompletableFuture<Message> sendRequest(int destination, Message msg) {
        CompletableFuture<Message> future = new CompletableFuture<>();

        // Check if we're connected and authenticated
        // if not, but we're running in the thread which is performing the connection, then we should continue
        // with the request.
        if (!isConnectedAndAuthenticated() && (!connectionLock.isHeldByCurrentThread() && connectionLock.isLocked())) {
            future.completeExceptionally(new IPCClientException("Client is not connected"));
        }

        log.debug("Sending message to destination {} on server", destination);
        //TODO: implement timeout for listening to requests
        // https://issues.amazon.com/issues/86453f7c-c94e-4a3c-b8ff-679767e7443c
        MessageFrame frame = new MessageFrame(destination, msg, REQUEST);
        messageHandler.registerRequestId(frame.requestId, future);

        // Try writing to the channel, but add a listener for if it fails
        channel.writeAndFlush(frame).addListener(channelFut -> {
            if (!channelFut.isSuccess()) {
                future.completeExceptionally(channelFut.cause());
            }
        });
        return future;
    }

    private void sendResponse(int destination, int sequenceNumber, Message msg) {
        log.debug("Sending response message to destination {} with request id {} on server", destination,
                sequenceNumber);
        MessageFrame frame = new MessageFrame(sequenceNumber, destination, msg, RESPONSE);
        channel.writeAndFlush(frame);
    }

    @Override
    public void registerMessageHandler(int destination, Function<Message, Message> handler) {
        Consumer<MessageFrame> cb = (MessageFrame mf) -> {
            Message toSend = handler.apply(mf.message);
            sendResponse(destination, mf.requestId, toSend);
        };
        messageHandler.registerListener(destination, cb);
    }

    @Override
    public String getServiceName() {
        return serviceName;
    }

    @Override
    public String getClientId() {
        return clientId;
    }

    @Override
    public void onReconnect(Runnable r) {
        onConnectTasks.add(r);
    }
}