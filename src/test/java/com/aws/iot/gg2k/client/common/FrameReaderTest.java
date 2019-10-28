package com.aws.iot.gg2k.client.common;

import com.aws.iot.gg2k.client.common.FrameReader.Message;
import org.junit.Test;

import java.io.*;
import java.util.Arrays;
import java.util.UUID;

import static com.aws.iot.gg2k.client.common.FrameReader.*;
import static com.aws.iot.gg2k.client.common.FrameReader.RequestType.REQUEST_RESPONSE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FrameReaderTest {

    @Test
    public void basicSanityCheck() throws Exception {

        Message msg = new Message(60, REQUEST_RESPONSE, "Test Payload".getBytes());
        MessageFrame inputFrame = new MessageFrame(msg);
        MessageFrame outputFrame = serialiseAndRead(inputFrame);
        validate(inputFrame,outputFrame);
    }

    private static  MessageFrame serialiseAndRead(MessageFrame inputFrame) throws Exception {
        ByteArrayOutputStream bb = new ByteArrayOutputStream();
        writeFrame(inputFrame, new DataOutputStream(bb));
        return readFrame(new DataInputStream(new ByteArrayInputStream(bb.toByteArray())));
    }

    private static void validate(MessageFrame inputFrame, MessageFrame outputFrame)  {
        assertEquals(inputFrame.uuid,outputFrame.uuid);
        assertEquals(inputFrame.message.getOpCode(),outputFrame.message.getOpCode());
        assertEquals(inputFrame.message.getType(),outputFrame.message.getType());
        assertTrue(Arrays.equals(inputFrame.message.getPayload(),outputFrame.message.getPayload()));
    }
}
