package com.aws.iot.evergreen.ipc.services.shadow.models;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

/**
 * Request for deleting a thing shadow.
 */
@Builder
@Data
public class DeleteThingShadowRequest {
    /**
     * The name of the thing.
     */
    @NonNull
    String thingName;

    private DeleteThingShadowRequest(@NonNull final String thingName) {
        if (thingName.isEmpty()) {
            throw new IllegalArgumentException("thingName cannot be empty");
        }
        setThingName(thingName);
    }
}
