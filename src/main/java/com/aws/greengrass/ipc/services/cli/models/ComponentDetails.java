package com.aws.greengrass.ipc.services.cli.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComponentDetails {
    String componentName;
    String version;
    LifecycleState state;
    @Deprecated
    Map<String, Object> configuration;

    Map<String, Object> nestedConfiguration;
}
