package com.aws.greengrass.ipc.services.shadow.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShadowGenericResponse {
    private ShadowResponseStatus status;

    private String errorMessage;
}
