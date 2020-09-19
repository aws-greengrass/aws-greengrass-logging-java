package com.aws.greengrass.ipc.services.configstore;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ConfigurationValidityReport {
    private ConfigurationValidityStatus status;
    private String message;
}
