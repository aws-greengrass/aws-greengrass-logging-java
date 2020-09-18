package com.aws.greengrass.ipc.services.secret;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetSecretValueRequest {
    String secretId;
    String versionId;
    String versionStage;
}
