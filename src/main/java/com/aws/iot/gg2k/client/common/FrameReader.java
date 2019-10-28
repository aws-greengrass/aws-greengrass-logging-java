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
        is.available();
        return null;
    }

    public static MessageFrame readFrame(DataInputStream dis) throws Exception {
        int totalLength = dis.readShort();
        int b = ((int) dis.readByte()) & BYTE_MASK;
      //  int nextByte = dis.readByte();
        int opCode = b>>2; //Byte.toUnsignedInt(b) >> 2;
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

    public static void writeFrame(MessageFrame f,  DataOutputStream dos) throws IOException {
        Message m = f.message;
        int payloadLength = m.payload.length;
        dos.write(payloadLength >> (BYTE_SHIFT * 1) & BYTE_MASK);
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

    public static void main(String args[]) throws Exception {


        Map m = new HashMap<String, String>();
        m.put("asdas", "asdasd");

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(m);
        byte[] payload = bos.toByteArray();

        MessageFrame test = new MessageFrame(UUID.randomUUID(), new Message(0, REQUEST_RESPONSE,payload));
        ByteArrayOutputStream bb = new ByteArrayOutputStream();
           FrameReader.writeFrame(test, new DataOutputStream(bb));
        byte[] b = bb.toByteArray();
        MessageFrame gotMessageFrame = FrameReader.readFrame(new DataInputStream(new ByteArrayInputStream(b)));

        System.out.println(gotMessageFrame.message.opCode);
        System.out.println(test.uuid.equals(gotMessageFrame.uuid));
        HashMap payloadAsMap = (HashMap) new ObjectInputStream(new ByteArrayInputStream(payload)).readObject();

        System.out.println(m.equals(payloadAsMap));
    }

}