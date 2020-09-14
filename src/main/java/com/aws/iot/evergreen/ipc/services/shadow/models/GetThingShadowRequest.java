package com.aws.iot.evergreen.ipc.services.shadow.models;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

/**
 * Request for getting a thing shadow.
 */
@Builder
@Data
public class GetThingShadowRequest {
    /**
     * The name of the thing.
     */
    @NonNull
    String thingName;

    private GetThingShadowRequest(@NonNull final String thingName) {
        if (thingName.isEmpty()) {
            throw new IllegalArgumentException("thingName cannot be empty");
        }
        setThingName(thingName);
    }
}
