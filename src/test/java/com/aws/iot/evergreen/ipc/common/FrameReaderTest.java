package com.aws.iot.evergreen.ipc.common;

import com.aws.iot.evergreen.ipc.codec.MessageFrameDecoder;
import com.aws.iot.evergreen.ipc.codec.MessageFrameEncoder;
import com.aws.iot.evergreen.ipc.common.FrameReader.Message;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;

import static com.aws.iot.evergreen.ipc.common.FrameReader.FrameType;
import static com.aws.iot.evergreen.ipc.common.FrameReader.MessageFrame;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class FrameReaderTest {

    @Test
    public void basicSanityCheck() {
        Message msg = new Message( "Test Payload".getBytes());
        MessageFrame inputFrame = new MessageFrame(1234,"10", msg, FrameType.REQUEST);
        MessageFrame outputFrame = serialiseAndRead(inputFrame);
        validate(inputFrame,outputFrame);

        inputFrame = new MessageFrame(1234,"10", msg, FrameType.RESPONSE);
        outputFrame = serialiseAndRead(inputFrame);
        validate(inputFrame,outputFrame);
    }

    private static MessageFrame serialiseAndRead(MessageFrame inputFrame) {
        EmbeddedChannel channel = new EmbeddedChannel(new MessageFrameEncoder(), new MessageFrameDecoder());
        channel.writeOutbound(inputFrame);
        Object outbound = channel.readOutbound();
        channel.writeInbound(outbound);
        return (MessageFrame) channel.readInbound();
    }

    private static void validate(MessageFrame inputFrame, MessageFrame outputFrame)  {
        assertEquals(inputFrame.sequenceNumber,outputFrame.sequenceNumber);
        assertEquals(inputFrame.type,outputFrame.type);
        assertEquals(inputFrame.version,outputFrame.version);
        assertEquals(inputFrame.destination,outputFrame.destination);
        assertArrayEquals(inputFrame.message.getPayload(), outputFrame.message.getPayload());
    }
}
