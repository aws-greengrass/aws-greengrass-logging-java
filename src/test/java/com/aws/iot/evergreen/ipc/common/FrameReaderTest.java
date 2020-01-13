package com.aws.iot.evergreen.ipc.common;

import com.aws.iot.evergreen.ipc.common.FrameReader.Message;
import org.junit.jupiter.api.Test;

import java.io.*;

import static com.aws.iot.evergreen.ipc.common.FrameReader.*;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class FrameReaderTest {

    @Test
    public void basicSanityCheck() throws Exception {
        Message msg = new Message( "Test Payload".getBytes());
        MessageFrame inputFrame = new MessageFrame(1234,"10", msg, FrameType.REQUEST);
        MessageFrame outputFrame = serialiseAndRead(inputFrame);
        validate(inputFrame,outputFrame);

        inputFrame = new MessageFrame(1234,"10", msg, FrameType.RESPONSE);
        outputFrame = serialiseAndRead(inputFrame);
        validate(inputFrame,outputFrame);
    }

    private static  MessageFrame serialiseAndRead(MessageFrame inputFrame) throws Exception {
        ByteArrayOutputStream bb = new ByteArrayOutputStream();
        writeFrame(inputFrame, new DataOutputStream(bb));
        return readFrame(new DataInputStream(new ByteArrayInputStream(bb.toByteArray())));
    }

    private static void validate(MessageFrame inputFrame, MessageFrame outputFrame)  {
        assertEquals(inputFrame.sequenceNumber,outputFrame.sequenceNumber);
        assertEquals(inputFrame.type,outputFrame.type);
        assertEquals(inputFrame.version,outputFrame.version);
        assertEquals(inputFrame.destination,outputFrame.destination);
        assertArrayEquals(inputFrame.message.getPayload(), outputFrame.message.getPayload());
    }
}
