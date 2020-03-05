/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.ipc.codec;

import com.aws.iot.evergreen.ipc.common.FrameReader;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.io.DataInputStream;
import java.util.List;

public class MessageFrameDecoder extends ByteToMessageDecoder {
    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list)
            throws Exception {
        try (DataInputStream dataInputStream = new DataInputStream(new ByteBufInputStream(byteBuf))) {
            list.add(FrameReader.readFrame(dataInputStream));
        }
    }
}
