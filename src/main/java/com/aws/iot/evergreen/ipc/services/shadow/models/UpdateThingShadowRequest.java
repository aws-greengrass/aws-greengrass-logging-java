package com.aws.iot.evergreen.ipc.services.shadow.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.nio.ByteBuffer;

/**
 * Request for updating a thing shadow.
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateThingShadowRequest {
    @Getter
    @Setter
    String thingName;

    /**
     * The payload byte.
     */
    private ByteBuffer payload;

    /**
     * Getter for the payload.
     * Warning: ByteBuffers returned by the SDK are mutable. Changes to the content of the byte buffer will
     * be seen by all objects that have a reference to this object. It is recommended to call
     * ByteBuffer.asReadOnlyBuffer() before using or reading from the buffer.
     *
     * @return the payload byte
     */
    public ByteBuffer getPayload() {
        return payload.duplicate();
    }

    /**
     * Setter for the payload.
     * Warning: ByteBuffers returned by the SDK are mutable. Changes to the content of the byte buffer will
     * be seen by all objects that have a reference to this object. It is recommended to call
     * ByteBuffer.asReadOnlyBuffer() before using or reading from the buffer.
     *
     * @param payload the payload byte in JSON
     */
    public void setPayload(final ByteBuffer payload) {
        this.payload = ByteBuffer.wrap(payload.array());
    }
}
