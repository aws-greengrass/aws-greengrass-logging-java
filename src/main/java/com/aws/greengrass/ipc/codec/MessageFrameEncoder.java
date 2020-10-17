/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.greengrass.ipc.codec;

import com.aws.greengrass.ipc.common.FrameReader;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.io.DataOutputStream;

public class MessageFrameEncoder extends MessageToByteEncoder<FrameReader.MessageFrame> {
    public static final int MAX_PAYLOAD_SIZE = (1 << 31) - 1;
    public static final int LENGTH_FIELD_OFFSET = 6;
    public static final int LENGTH_FIELD_LENGTH = 4;

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, FrameReader.MessageFrame messageFrame,
                          ByteBuf byteBuf) throws Exception {
        FrameReader.writeFrame(messageFrame, new DataOutputStream(new ByteBufOutputStream(byteBuf)));
    }
}