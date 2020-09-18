package com.aws.greengrass.ipc.services.shadow.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

/**
 * Result for getting a thing shadow.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class GetThingShadowResult extends ShadowGenericResponse {
    /**
     * The payload bytes.
     */
    @NonNull
    private byte[] payload;

    /**
     * Builder.
     * @param responseStatus response status
     * @param errorMessage   error message
     * @param payload        the response payload
     */
    @Builder
    public GetThingShadowResult(ShadowResponseStatus responseStatus,
                                   String errorMessage,
                                   byte[] payload) {
        super(responseStatus, errorMessage);
        this.payload = payload;
    }
}
