package com.aws.iot.evergreen.ipc.services.cli.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class GetLocalDeploymentStatusResponse extends CliGenericResponse {
    LocalDeployment deployment;
}
