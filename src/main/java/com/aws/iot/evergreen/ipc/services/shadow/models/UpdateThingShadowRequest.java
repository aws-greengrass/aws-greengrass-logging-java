package com.aws.iot.evergreen.ipc.services.shadow.models;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.nio.ByteBuffer;

/**
 * Request for updating a thing shadow.
 */
@Builder
public class UpdateThingShadowRequest {
    @Getter
    @Setter
    @NonNull
    String thingName;

    private UpdateThingShadowRequest(@NonNull final String thingName, @NonNull final ByteBuffer payload) {
        if (thingName.isEmpty()) {
            throw new IllegalArgumentException("thingName cannot be empty");
        }
        setThingName(thingName);
        setPayload(payload);
    }

    /**
     * The payload byte.
     */
    @NonNull
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
    public void setPayload(@NonNull final ByteBuffer payload) {
        this.payload = ByteBuffer.wrap(payload.array());
    }
}
