package com.aws.iot.evergreen.ipc.services.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private int version;
    private int opCode;
    private byte[] payload;

    /**
     * Constructs application message from bytes.
     *
     * @param bytes encoded application message object
     */
    public ApplicationMessage(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        this.version = buffer.get() & BYTE_MASK;
        this.opCode = buffer.get() & BYTE_MASK;
        this.payload = new byte[bytes.length - 2];
        buffer.get(payload);
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
