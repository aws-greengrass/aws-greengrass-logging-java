package com.aws.iot.evergreen.ipc.services.common;


import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.nio.ByteBuffer;

/**
 * Represents application layer packet.
 */
@Data
@Builder
public class ApplicationMessage {
    public static final int MIN_VERSION_VALUE = 1;
    public static final int MAX_VERSION_VALUE = 255;
    private static final int BYTE_MASK = 0xff;
    private int version;
    private int opCode;
    @NonNull
    private byte[] payload;


    /**
     * Constructs application message from bytes.
     *
     *   <p>+------------------+---------------+
     *      | Version    |  OpCode   | Payload |
     *      | 1 byte     |  1 byte   | x bytes |
     *      +------------------+---------------+
     *
     * @param bytes encoded application message object
     */
    public static ApplicationMessage fromBytes(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        int version = buffer.get() & BYTE_MASK;
        int opCode = buffer.get() & BYTE_MASK;
        byte[] payload = new byte[bytes.length - 2];
        //TODO: refactor to avoid copying the bytes
        buffer.get(payload);
        return new ApplicationMessage(version, opCode, payload);
    }


    /**
     * Encodes  ApplicationMessage to bytes.
     */
    public byte[] toByteArray() {
        ByteBuffer buffer = ByteBuffer.allocate(payload.length + 2);
        buffer.put((byte) this.version);
        buffer.put((byte) this.opCode);
        buffer.put(this.payload);
        return buffer.array();
    }

    /**
     All args constructor for ApplicationMessage.
     */
    public ApplicationMessage(int version, int opCode, @NonNull byte[] payload) {
        this.setVersion(version);
        this.opCode = opCode;
        this.payload = payload;
    }

    /**
     * set the version of the ApplicationMessage.
     */
    public void setVersion(int version) {
        if (!(version >= MIN_VERSION_VALUE && version <= MAX_VERSION_VALUE)) {
            throw new IllegalArgumentException("Invalid version " + version);
        }
        this.version = version;
    }
}
