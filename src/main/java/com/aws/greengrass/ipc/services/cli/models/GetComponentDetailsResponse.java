package com.aws.greengrass.ipc.services.cli.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class GetComponentDetailsResponse extends CliGenericResponse {
    ComponentDetails componentDetails;
}
