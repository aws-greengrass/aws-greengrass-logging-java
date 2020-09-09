package com.aws.iot.evergreen.ipc.services.shadow.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
/**
 * Request for getting a thing shadow.
 */
public class GetThingShadowRequest {
    /**
     * The name of the thing.
     */
    String thingName;
}
