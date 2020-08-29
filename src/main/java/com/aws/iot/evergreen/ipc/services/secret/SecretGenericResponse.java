package com.aws.iot.evergreen.ipc.services.secret;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SecretGenericResponse {
    private SecretResponseStatus status;

    private String errorMessage;
}
