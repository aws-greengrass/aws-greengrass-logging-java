package com.aws.iot.evergreen.ipc.services.shadow.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request for deleting a thing shadow.
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeleteThingShadowRequest {
    /**
     * The name of the thing.
     */
    String thingName;
}
