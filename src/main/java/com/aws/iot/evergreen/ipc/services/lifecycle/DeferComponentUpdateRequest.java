package com.aws.iot.evergreen.ipc.services.lifecycle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeferComponentUpdateRequest {
    String componentName;
    /**
     Estimated time in milliseconds after which component will be willing to be disrupted.
     If the returned value is zero the handler is granting permission to be disrupted.
     Otherwise, it will be asked again later
     */
    long recheckTimeInMs;
}
