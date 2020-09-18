/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.greengrass.ipc.handler;

import com.aws.greengrass.ipc.common.FrameReader;
import com.aws.greengrass.ipc.message.MessageHandler;
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
