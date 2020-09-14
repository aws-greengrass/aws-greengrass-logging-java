package com.aws.iot.evergreen.ipc.services.shadow.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

/**
 * Result for deleting a thing shadow.
 */
@Builder
@Data
@AllArgsConstructor
public class DeleteThingShadowResult {
    /**
     * The payload bytes.
     */
    @NonNull
    private byte[] payload;
}
