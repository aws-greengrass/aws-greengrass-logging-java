package com.aws.iot.evergreen.ipc.services.lifecycle;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)

public class DeferComponentUpdateResponse extends LifecycleGenericResponse {
    @Builder
    public DeferComponentUpdateResponse(LifecycleResponseStatus responseStatus, String errorMessage) {
        super(responseStatus, errorMessage);
    }
}
