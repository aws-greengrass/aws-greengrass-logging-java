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

    //TODO: implement read frame with timeout
    public static MessageFrame readFrame(DataInputStream dis, int timeoutInMilliSec) {
        return null;
    }

    /**
     * Constructs MessageFrame from bits reads from the input stream
     * 1st byte, first 7 bits represent the version number and the last bit represent the type
     * 2-3 byte, length of destination UTF-8 string as a short
     * 4+ byte, destination string
     * next 2 bytes, length of payload as a short
     * Rest of the bytes capture the payload
     *
     * @param dis input stream
     * @return
     * @throws Exception
     */
    public static MessageFrame readFrame(DataInputStream dis) throws Exception {
        synchronized (dis) {
            int firstByte = ((int) dis.readByte()) & BYTE_MASK;
            int version = firstByte >> 1;
            FrameType type = FrameType.fromOrdinal(firstByte & IS_RESPONSE_MASK);

            int destinationNameLength = dis.readUnsignedShort();
            byte[] destinationNameByte = new byte[destinationNameLength];
            assert dis.read(destinationNameByte) == destinationNameLength;

            int sequenceNumber = dis.readInt();

            int payloadLength = dis.readUnsignedShort();
            byte[] payload = new byte[payloadLength];
            assert dis.read(payload) == payloadLength;

            return new MessageFrame(sequenceNumber, version, new String(destinationNameByte, StandardCharsets.UTF_8),
                    new Message(payload), type);
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

            dos.write((f.version << 1) | (f.type.ordinal()));

            dos.writeShort(f.destination.length());
            dos.write(f.destination.getBytes(StandardCharsets.UTF_8));

            dos.writeInt(f.sequenceNumber);

            int payloadLength = m.payload.length;
            dos.writeShort(payloadLength);

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
        public final String destination;

        public MessageFrame(int sequenceNumber, int version, String destination, Message message, FrameType type) {
            this.sequenceNumber = sequenceNumber;
            this.version = version;
            this.message = message;
            this.type = type;
            this.destination = destination;
        }

        public MessageFrame(int sequenceNumber, String destination, Message message, FrameType type) {
            this.sequenceNumber = sequenceNumber;
            this.message = message;
            this.version = VERSION;
            this.type = type;
            this.destination = destination;
        }

        public MessageFrame(String destination, Message message, FrameType type) {
            this(sequenceNumberGenerator.incrementAndGet(), VERSION, destination, message, type);
        }
    }

    public static class Message {
        private final byte[] payload;

        public Message(byte[] payload) {
            this.payload = payload;
        }

        public static Message errorMessage(String errorMsg) {
            return new Message(errorMsg.getBytes(StandardCharsets.UTF_8));
        }

        public byte[] getPayload() {
            return payload;
        }
    }
}
