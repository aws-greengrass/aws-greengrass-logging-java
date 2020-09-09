package com.aws.iot.evergreen.ipc.services.shadow.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request for getting a thing shadow.
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetThingShadowRequest {
    /**
     * The name of the thing.
     */
    String thingName;
}
