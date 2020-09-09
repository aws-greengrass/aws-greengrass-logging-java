package com.aws.iot.evergreen.ipc.services.shadow.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.nio.ByteBuffer;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShadowGenericResponse {
    private ShadowResponseStatus status;

    private String errorMessage;

    private ByteBuffer payload;
}
