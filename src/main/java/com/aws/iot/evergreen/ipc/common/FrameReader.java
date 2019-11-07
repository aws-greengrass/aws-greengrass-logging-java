package com.aws.iot.evergreen.ipc.common;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;


public class FrameReader {

    private static final int VERSION = 1;
    private static final int BYTE_MASK = 0xff;
    private static final int BYTE_SHIFT = 8;

    //TODO: implement read frame with timeout
    public static MessageFrame readFrame(InputStream is, int timeoutInMilliSec) {
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
        int version = ((int) dis.readByte()) & BYTE_MASK;
        long mostSigBits = dis.readLong();
        long leastSigBits = dis.readLong();
        byte[] payload = new byte[totalLength];
        dis.readFully(payload);
        return new MessageFrame(new UUID(mostSigBits, leastSigBits), version, new Message(opCode, payload));
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
        dos.write(f.version);

        dos.writeLong(f.uuid.getMostSignificantBits());
        dos.writeLong(f.uuid.getLeastSignificantBits());

        dos.write(m.payload);
        dos.flush();
    }

    public static class Message {
        private int opCode;
        private byte[] payload;

        public Message(int opCode,byte[] payload) {
            this.opCode = opCode;
            this.payload = payload;
        }

        public int getOpCode() {
            return opCode;
        }

        public byte[] getPayload() {
            return payload;
        }

        public static Message errorMessage(String errorMsg) { return new Message(Constants.ERROR_OP_CODE, errorMsg.getBytes(StandardCharsets.UTF_8));}
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