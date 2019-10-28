package com.aws.iot.gg2k.client.common;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.aws.iot.gg2k.client.common.FrameReader.RequestType.REQUEST_RESPONSE;
import static com.aws.iot.gg2k.client.common.FrameReader.RequestType.fromOrdinal;


public class FrameReader {
    private static final int TYPE_MASK = 0x03;
    private static final int BYTE_MASK = 0xff;
    private static final int BYTE_SHIFT = 8;

    public static MessageFrame readFrame(InputStream is, int timeoutInMilliSec) throws IOException {
        return null;
    }


    /**
     * Reads a MessageFrame from input steam.
     * @param dis input stream
     * @return
     * @throws Exception
     */
    public static MessageFrame readFrame(DataInputStream dis) throws Exception {
        int totalLength = dis.readShort();
        int b = ((int) dis.readByte()) & BYTE_MASK;
        int opCode = b>>2;
        RequestType type = fromOrdinal(b & TYPE_MASK);
        long mostSigBits = 0, leastSigBits = 0;
        if (type.equals(REQUEST_RESPONSE)) {
            mostSigBits = dis.readLong();
            leastSigBits = dis.readLong();
        }
        byte[] payload = new byte[totalLength];
        dis.readFully(payload);
        return new MessageFrame(new UUID(mostSigBits, leastSigBits), new Message(opCode, type, payload));
    }


    /** Encodes the Message frame to bytes
     *
     * The first 2 bytes represent the length of the payload
     * In the 3rd byte, first 6 bits represent opcode values and last 2 bits capture the type
     * The next 16 bytes represent the UUID if the type is {#RequestType.REQUEST_RESPONSE}
     * Rest of the bytes capture the payload
     * @param f
     * @param dos
     * @throws IOException
     */
    public static void writeFrame(MessageFrame f,  DataOutputStream dos) throws IOException {
        Message m = f.message;
        int payloadLength = m.payload.length;
        dos.write(payloadLength >> (BYTE_SHIFT) & BYTE_MASK);
        dos.write(payloadLength & BYTE_MASK);

        dos.write((m.opCode << 2) | m.type.ordinal());
        if (m.type.equals(REQUEST_RESPONSE)) {
            dos.writeLong(f.uuid.getMostSignificantBits());
            dos.writeLong(f.uuid.getLeastSignificantBits());
        }
        dos.write(m.payload);
        dos.flush();
    }

    public enum RequestType {
        FIRE_AND_FORGET,
        REQUEST_RESPONSE;

        private static RequestType[] allValues = values();
        public static RequestType fromOrdinal(int n) {
            return allValues[n];
        }
    }

    public static class Message {
        private int opCode;
        private RequestType type;
        private byte[] payload;

        public Message(int opCode, RequestType type, byte[] payload) {
            this.opCode = opCode;
            this.type = type;
            this.payload = payload;
        }

        public int getOpCode() {
            return opCode;
        }

        public RequestType getType() {
            return type;
        }

        public byte[] getPayload() {
            return payload;
        }
    }

    /**
     * 16 byte UUID is used instead of 4 byte sequence number to avoid collisions as
     * both the kernel and client can initiate an IPC call.
     */
    public static class MessageFrame {
        public UUID uuid;
        public Message message;

        public MessageFrame(UUID uuid, Message message) {
            this.uuid = uuid;
            this.message = message;
        }

        public MessageFrame(Message message) {
            this(UUID.randomUUID(), message);
        }
    }
}