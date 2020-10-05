package com.aws.greengrass.ipc.services.cli.models;

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
    @Deprecated
    Map<String, Map<String, Object>> componentToConfiguration;

    // config update for each component, in the format of <componentName, <MERGE/RESET, <key>>>
    Map<String, Map<String, Object>> configurationUpdate;
}
