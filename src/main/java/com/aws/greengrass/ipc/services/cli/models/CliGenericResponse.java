package com.aws.greengrass.ipc.services.cli.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * This class represents the information used to identify the message-type and error-type.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CliGenericResponse {
    MessageType messageType;
    String errorType;

    public enum MessageType {
        APPLICATION_MESSAGE,
        APPLICATION_ERROR
    }
}
