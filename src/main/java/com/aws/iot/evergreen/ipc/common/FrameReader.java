package com.aws.iot.evergreen.ipc.common;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.ToString;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;


public class FrameReader {
    public static final int VERSION = 1;
    private static final int BYTE_MASK = 0xff;
    private static final int IS_RESPONSE_MASK = 0x01;

    /**
     * Header size in bytes.
     *
     * <p>version and request type : 1 byte
     * destination                 : 1 byte
     * request id                  : 4 bytes
     * payload length              : 4 bytes
     * Total                       : 10 bytes
     */
    private static final int HEADER_SIZE_IN_BYTES = 10;

    /**
     * Constructs MessageFrame from bits reads from the input stream.
     *
     * <p>+------------------+---------------+---------------+------------------+----------+
     *    | Version + Type   |  Destination  |  Request Id   |  Payload Length  |  Payload |
     *    |     1 byte       |    1 byte     |    4 bytes    |      4 bytes     |  x bytes |
     *    +------------------+---------------+---------------+------------------+----------+
     *
     * @param dataInputStream input stream
     * @return frame from the stream
     * @throws Exception if anything goes wrong
     */
    @SuppressFBWarnings(value = "RR_NOT_CHECKED", justification = "No need to check return from stream.read()")
    public static MessageFrame readFrame(DataInputStream dataInputStream) throws Exception {
        synchronized (dataInputStream) {
            int firstByte = dataInputStream.readByte() & BYTE_MASK;
            int version = firstByte >> 1;
            FrameType type = FrameType.fromOrdinal(firstByte & IS_RESPONSE_MASK);
            int destination = dataInputStream.readByte() & BYTE_MASK;
            int requestId = dataInputStream.readInt();
            int payloadLength = dataInputStream.readInt();
            byte[] payload = new byte[payloadLength];
            dataInputStream.read(payload);

            return new MessageFrame(requestId, version, destination, new Message(payload), type);
        }
    }

    /**
     * Encodes the Message frame to bytes.
     *
     * @param f                frame to write into the stream
     * @param dataOutputStream stream to write into
     * @throws IOException if writing goes wrong
     */
    public static void writeFrame(MessageFrame f, DataOutputStream dataOutputStream) throws IOException {
        synchronized (dataOutputStream) {
            if (f == null || f.message == null) {
                throw new IllegalArgumentException("Message is null ");
            }
            if (f.version > 127) {
                throw new IllegalArgumentException("Frame version is too high. Must be less than 128");
            }
            if (f.destination > 255) {
                throw new IllegalArgumentException("Frame destination is too high. Must be less than 255");
            }

            Message m = f.message;
            ByteBuffer buffer = ByteBuffer.allocate(HEADER_SIZE_IN_BYTES + m.payload.length);
            buffer.put((byte) ((f.version << 1) | (f.type.ordinal())));
            buffer.put((byte) f.destination);
            buffer.putInt(f.requestId);
            buffer.putInt(m.payload.length);
            buffer.put(m.payload);
            dataOutputStream.write(buffer.array());
            dataOutputStream.flush();
        }
    }

    public enum FrameType {
        REQUEST, RESPONSE;
        private static FrameType[] allValues = values();

        public static FrameType fromOrdinal(int n) {
            return allValues[n];
        }
    }

    /**
     * Represents our protocol packet.
     */
    @ToString
    public static class MessageFrame {
        private static final AtomicInteger requestIdGenerator = new AtomicInteger();
        public final int requestId;
        public final int version;
        public final FrameType type;
        public final Message message;
        public final int destination;

        /**
         * Construct a frame.
         */
        public MessageFrame(int requestId, int version, int destination, Message message, FrameType type) {
            this.requestId = requestId;
            this.version = version;
            this.message = message;
            this.type = type;
            this.destination = destination;
        }

        /**
         * Construct a frame.
         */
        public MessageFrame(int requestId, int destination, Message message, FrameType type) {
            this.requestId = requestId;
            this.message = message;
            this.version = VERSION;
            this.type = type;
            this.destination = destination;
        }

        public MessageFrame(int destination, Message message, FrameType type) {
            this(requestIdGenerator.incrementAndGet(), VERSION, destination, message, type);
        }
    }

    public static class Message {
        private final byte[] payload;

        public Message(byte[] payload) {
            this.payload = payload;
        }

        public byte[] getPayload() {
            return payload;
        }
    }
}
