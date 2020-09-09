package com.aws.iot.evergreen.ipc.services.cli.exceptions;

import com.aws.iot.evergreen.ipc.services.cli.models.CliGenericResponse;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GenericCliIpcServerException extends Exception {
    CliGenericResponse.MessageType messageType;
    String errorType;
    String errorMessage;

    public GenericCliIpcServerException(String message) {
        super(message);
        this.errorMessage = message;
    }
}
