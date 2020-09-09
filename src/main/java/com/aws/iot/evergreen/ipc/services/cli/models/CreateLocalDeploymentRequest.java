package com.aws.iot.evergreen.ipc.services.cli.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateLocalDeploymentRequest {
    String groupName;
    Map<String, String> rootComponentVersionsToAdd;
    List<String> rootComponentsToRemove;
    Map<String, Map<String, Object>> componentToConfiguration;
}
