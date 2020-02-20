package com.aws.iot.evergreen.ipc.services.lifecycle;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LifeCycleGenericResponse {

    private LifecycleResponseStatus status;

    private String errorMessage;

}
