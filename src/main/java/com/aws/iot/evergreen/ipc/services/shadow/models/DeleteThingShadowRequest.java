package com.aws.iot.evergreen.ipc.services.shadow.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * Request for deleting a thing shadow.
 */
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeleteThingShadowRequest {
    /**
     * The name of the thing.
     */
    @NonNull
    String thingName;
}
