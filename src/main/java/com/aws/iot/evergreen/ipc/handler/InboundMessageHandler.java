package com.aws.iot.evergreen.ipc.handler;

import com.aws.iot.evergreen.ipc.common.FrameReader;
import com.aws.iot.evergreen.ipc.message.MessageHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class InboundMessageHandler extends SimpleChannelInboundHandler<FrameReader.MessageFrame> {

    private final MessageHandler messageHandler;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FrameReader.MessageFrame msg) {
        messageHandler.handleMessage(msg);
    }
}
