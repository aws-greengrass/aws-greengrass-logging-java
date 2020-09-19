package com.aws.greengrass.ipc.services.cli.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class ListLocalDeploymentResponse extends CliGenericResponse {
    List<LocalDeployment> localDeployments;
}
