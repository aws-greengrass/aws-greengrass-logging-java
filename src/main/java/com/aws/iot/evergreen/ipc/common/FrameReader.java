package com.aws.iot.evergreen.ipc.common;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;


public class FrameReader {

    private static final int VERSION = 1;
    private static final int BYTE_MASK = 0xff;
    private static final int IS_RESPONSE_MASK = 0x01;
    private static final int BYTE_SHIFT = 8;

    //TODO: implement read frame with timeout
    public static MessageFrame readFrame(DataInputStream dis, int timeoutInMilliSec) {
        return null;
    }

    /**
     * Constructs MessageFrame from bits reads from the input stream
     * The first 2 bytes represent the length of the payload
     * 3rd byte contains the opcode
     * 4th byte, first 7 bits represent the version number and the last bit represent the type
     * Rest of the bytes capture the payload
     *
     * @param dis input stream
     * @return
     * @throws Exception
     */
    public static MessageFrame readFrame(DataInputStream dis) throws Exception {
        synchronized (dis) {
            int payloadLength = dis.readShort();
            int opCode = ((int) dis.readByte()) & BYTE_MASK;
            int thirdByte = ((int) dis.readByte()) & BYTE_MASK;
            int version = thirdByte >> 1;
            FrameType type = FrameType.fromOrdinal(thirdByte & IS_RESPONSE_MASK);
            int sequenceNumber = dis.readInt();
            byte[] payload = new byte[payloadLength];
            dis.readFully(payload);
            return new MessageFrame(sequenceNumber, version, new Message(opCode, payload), type);
        }
    }

    /**
     * Encodes the Message frame to bytes
     *
     * @param f
     * @param dos
     * @throws IOException
     */
    public static void writeFrame(MessageFrame f, DataOutputStream dos) throws IOException {
        synchronized (dos) {
            if (f == null || f.message == null) {
                throw new IllegalArgumentException("Message is null ");
            }
            //TODO: perform range checks on payload numeric fields before writing
            Message m = f.message;
            int payloadLength = m.payload.length;
            dos.write(payloadLength >> (BYTE_SHIFT) & BYTE_MASK);
            dos.write(payloadLength & BYTE_MASK);

            dos.write(m.opCode);
            dos.write((f.version << 1) | (f.type.ordinal()));
            dos.writeInt(f.sequenceNumber);

            dos.write(m.payload);
            dos.flush();
        }
    }

    public enum FrameType {
        REQUEST,
        RESPONSE;

        private static FrameType[] allValues = values();

        public static FrameType fromOrdinal(int n) {
            return allValues[n];
        }
    }

    /**
     *
     */
    public static class MessageFrame {
        private final static AtomicInteger sequenceNumberGenerator = new AtomicInteger();
        public final int sequenceNumber;
        public final int version;
        public final FrameType type;
        public final Message message;

        public MessageFrame(int sequenceNumber, int version, Message message, FrameType type) {
            this.sequenceNumber = sequenceNumber;
            this.version = version;
            this.message = message;
            this.type = type;
        }

        public MessageFrame(int sequenceNumber, Message message, FrameType type) {
            this.sequenceNumber = sequenceNumber;
            this.message = message;
            this.version = VERSION;
            this.type = type;
        }

        public MessageFrame(Message message, FrameType type) {
            this(sequenceNumberGenerator.incrementAndGet(), VERSION, message, type);
        }
    }

    public static class Message {
        private final int opCode;
        private final byte[] payload;

        public Message(int opCode, byte[] payload) {
            this.opCode = opCode;
            this.payload = payload;
        }

        public static Message errorMessage(String errorMsg) {
            return new Message(Constants.ERROR_OP_CODE, errorMsg.getBytes(StandardCharsets.UTF_8));
        }

        public static Message emptyResponse(int opCode) {
            return new Message(opCode, new byte[0]);
        }

        public int getOpCode() {
            return opCode;
        }

        public byte[] getPayload() {
            return payload;
        }
    }
}