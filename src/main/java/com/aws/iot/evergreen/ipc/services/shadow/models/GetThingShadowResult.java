package com.aws.iot.evergreen.ipc.services.shadow.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

/**
 * Result for getting a thing shadow.
 */
@Builder
@Data
@AllArgsConstructor
public class GetThingShadowResult {
    /**
     * The payload bytes.
     */
    @NonNull
    private byte[] payload;
}
