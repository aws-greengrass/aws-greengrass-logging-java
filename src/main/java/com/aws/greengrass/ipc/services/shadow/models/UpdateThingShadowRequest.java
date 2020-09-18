package com.aws.greengrass.ipc.services.shadow.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * Request for updating a thing shadow.
 */
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateThingShadowRequest {
    /**
     * The name of the thing.
     */
    @NonNull
    String thingName;

    /**
     * The new shadow bytes.
     */
    @NonNull
    byte[] payload;
}
