package com.aws.iot.evergreen.ipc.services.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.nio.ByteBuffer;

/**
 * Represents application layer packet.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationMessage {

    private static final int BYTE_MASK = 0xff;
    @NonNull
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

}
