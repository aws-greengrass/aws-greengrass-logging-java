package com.aws.iot.evergreen.ipc.services.cli.models;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetComponentDetailsRequest {
    String componentName;
}
