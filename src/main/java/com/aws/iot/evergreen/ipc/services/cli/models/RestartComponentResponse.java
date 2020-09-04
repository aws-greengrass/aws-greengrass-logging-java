package com.aws.iot.evergreen.ipc.services.cli.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class RestartComponentResponse extends CliGenericResponse {
        RequestStatus requestStatus;
        String message;
}
