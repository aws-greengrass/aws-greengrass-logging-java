package com.aws.greengrass.ipc.services.lifecycle;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor

public class SubscribeToComponentUpdatesResponse extends LifecycleGenericResponse {
    @Builder
    public SubscribeToComponentUpdatesResponse(LifecycleResponseStatus responseStatus, String errorMessage) {
        super(responseStatus, errorMessage);
    }
}
