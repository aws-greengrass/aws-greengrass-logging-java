package com.aws.iot.evergreen.ipc.services.shadow.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

/**
 * Request for getting a thing shadow.
 */
@Builder
@Data
@AllArgsConstructor
public class GetThingShadowRequest {
    /**
     * The name of the thing.
     */
    @NonNull
    String thingName;
}
