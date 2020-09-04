package com.aws.iot.evergreen.ipc.services.cli.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode (callSuper = true)
public class ListComponentsResponse extends CliGenericResponse {
    List<ComponentDetails> components;
}
