package com.aws.iot.gg2k.client.common;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.aws.iot.gg2k.client.common.FrameReader.RequestType.REQUEST_RESPONSE;
import static com.aws.iot.gg2k.client.common.FrameReader.RequestType.fromOrdinal;


public class FrameReader {

    private static final int VERSION = 1;
    private static final int VERSION_MASK = 0x0f;
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
        int opCode = ((int) dis.readByte()) & BYTE_MASK;
        int fourthByte = ((int) dis.readByte()) & BYTE_MASK;

        RequestType type = fromOrdinal(fourthByte >> 4);
        int version = fourthByte & VERSION_MASK;
        long mostSigBits = 0, leastSigBits = 0;
        if (type.equals(REQUEST_RESPONSE)) {
            mostSigBits = dis.readLong();
            leastSigBits = dis.readLong();
        }
        byte[] payload = new byte[totalLength];
        dis.readFully(payload);
        return new MessageFrame(new UUID(mostSigBits, leastSigBits),version, new Message(opCode, type, payload));
    }


    /** Encodes the Message frame to bytes
     *
     * The first 2 bytes represent the length of the payload
     * 3rd byte contains the opcode
     * 4th byte, first 4 bits represent the RequestType and last 4 bits represent the version number.
     * The next 16 bytes represent the UUID if the type is {#RequestType.REQUEST_RESPONSE}
     * Rest of the bytes capture the payload
     * @param f
     * @param dos
     * @throws IOException
     */
    public static void writeFrame(MessageFrame f,  DataOutputStream dos) throws IOException {
        if(f == null || f.message == null){
            throw new IllegalArgumentException("Message is null ");
        }
        Message m = f.message;
        int payloadLength = m.payload.length;
        dos.write(payloadLength >> (BYTE_SHIFT) & BYTE_MASK);
        dos.write(payloadLength & BYTE_MASK);

        dos.write(m.opCode);
        dos.write((m.type.ordinal() << 4) | f.version);
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
        public int version;
        public Message message;

        public MessageFrame(UUID uuid, int version, Message message) {
            this.uuid = uuid;
            this.version = version;
            this.message = message;
        }

        public MessageFrame(UUID uuid, Message message) {
            this.uuid = uuid;
            this.message = message;
            this.version = VERSION;
        }

        public MessageFrame(Message message) {
            this(UUID.randomUUID(), message);
        }
    }
}