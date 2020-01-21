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
import com.fasterxml.jackson.core.type.TypeReference;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
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
    private Channel channel;

    /**
     * Construct a client and immediately connect to the server.
     *
     * @param config configuration used to connect to the server
     * @throws IOException if connection fails
     * @throws InterruptedException if connection times out
     */
    public IPCClientImpl(KernelIPCClientConfig config) throws IOException, InterruptedException {
        this.messageHandler = new MessageHandler();

        eventLoopGroup = new NioEventLoopGroup();

        // Help boot strapping a channel
        Bootstrap clientBootstrap = new Bootstrap();
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

        // Connect to listening server
        ChannelFuture channelFuture = clientBootstrap.connect(config.getHostAddress(), config.getPort()).sync();
        if (!channelFuture.isSuccess()) {
            throw new IOException("Unable to connect");
        }

        this.channel = channelFuture.channel();

        try {
            // Send Auth request and wait for response.
            GeneralResponse<Void, GenericErrorCodes> resp = IPCUtil.sendAndReceive(this, AUTH_SERVICE,
                    GeneralRequest.builder().request(config.getToken() == null ? "" : config.getToken())
                            .type(AuthRequestTypes.Auth).build(),
                    new TypeReference<GeneralResponse<Void, GenericErrorCodes>>() {
                    }).get(); // TODO: Add timeout waiting for auth to come back?
            // https://issues.amazon.com/issues/86453f7c-c94e-4a3c-b8ff-679767e7443c
            if (!resp.getError().equals(GenericErrorCodes.Success)) {
                throw new IOException(resp.getErrorMessage());
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new IOException(e);
        }
    }

    public void disconnect() {
        eventLoopGroup.shutdownGracefully();
    }

    /**
     * Send a request to the server to the given destination.
     *
     * @param destination what service should receive the message
     * @param msg message to send
     * @return future containing the response (if any)
     */
    public CompletableFuture<Message> sendRequest(String destination, Message msg) {
        //TODO: implement timeout for listening to requests
        // https://issues.amazon.com/issues/86453f7c-c94e-4a3c-b8ff-679767e7443c
        MessageFrame frame = new MessageFrame(destination, msg, REQUEST);
        CompletableFuture<Message> future = new CompletableFuture<>();
        messageHandler.registerRequestId(frame.sequenceNumber, future);

        channel.writeAndFlush(frame);
        return future;
    }

    private CompletableFuture<Void> sendResponse(String destination, int sequenceNumber, Message msg) {
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
}
