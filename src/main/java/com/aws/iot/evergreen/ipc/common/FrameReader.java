package com.aws.iot.evergreen.ipc.common;

import lombok.ToString;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;


public class FrameReader {

    private static final int VERSION = 1;
    private static final int BYTE_MASK = 0xff;
    private static final int IS_RESPONSE_MASK = 0x01;
    /**
     * version and request type : 1 byte
     * destination name length  : 1 byte
     * sequence number          : 4 bytes
     * payload length           : 2 bytes
     *                 Total    : 8 bytes
     */
    private static final int HEADER_SIZE_IN_BYTES = 8;

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
     * @param dataInputStream input stream
     * @return
     * @throws Exception
     */
    public static MessageFrame readFrame(DataInputStream dataInputStream) throws Exception {
        synchronized (dataInputStream) {
            int firstByte =  dataInputStream.readByte() & BYTE_MASK;
            int version = firstByte >> 1;
            FrameType type = FrameType.fromOrdinal(firstByte & IS_RESPONSE_MASK);
            int destinationNameLength = dataInputStream.readByte();
            byte[] destinationNameByte = new byte[destinationNameLength];
            dataInputStream.read(destinationNameByte);
            int sequenceNumber = dataInputStream.readInt();
            int payloadLength = dataInputStream.readShort();
            byte[] payload = new byte[payloadLength];
            dataInputStream.read(payload);

            return new MessageFrame(sequenceNumber, version, new String(destinationNameByte, StandardCharsets.UTF_8),
                    new Message(payload), type);
        }
    }

    /**
     * Encodes the Message frame to bytes
     *
     * @param f
     * @param dataOutputStream
     * @throws IOException
     */
    public static void writeFrame(MessageFrame f, DataOutputStream dataOutputStream) throws IOException {
        synchronized (dataOutputStream) {
            if (f == null || f.message == null) {
                throw new IllegalArgumentException("Message is null ");
            }
            //TODO: perform range checks on payload numeric fields before writing
            Message m = f.message;
            byte[] destination = f.destination.getBytes(StandardCharsets.UTF_8);
            ByteBuffer buffer =  ByteBuffer.allocate(HEADER_SIZE_IN_BYTES + destination.length + m.payload.length);
            buffer.put((byte) ((f.version << 1) | (f.type.ordinal())));
            buffer.put((byte) destination.length);
            buffer.put(destination);
            buffer.putInt(f.sequenceNumber);
            buffer.putShort((short) m.payload.length);
            buffer.put(m.payload);
            dataOutputStream.write(buffer.array());
            dataOutputStream.flush();
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
    @ToString
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
