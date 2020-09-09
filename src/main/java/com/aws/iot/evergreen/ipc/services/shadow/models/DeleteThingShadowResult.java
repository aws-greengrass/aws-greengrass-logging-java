package com.aws.iot.evergreen.ipc.services.shadow.models;

import java.nio.ByteBuffer;

/**
 * Result for deleting a thing shadow.
 */
public class DeleteThingShadowResult {
    /**
     * The payload of the request.
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
     * @param payload the payload byte
     */
    public void setPayload(final ByteBuffer payload) {
        this.payload = ByteBuffer.wrap(payload.array());
    }
}
