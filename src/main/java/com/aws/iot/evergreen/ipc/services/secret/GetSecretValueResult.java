package com.aws.iot.evergreen.ipc.services.secret;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class GetSecretValueResult extends SecretGenericResponse {
    private String secretId;
    private String versionId;
    private List<String> versionStages;
    private String secretString;
    private byte[] secretBinary;

    /**
     * Builder.
     * @param responseStatus response status
     * @param errorMessage   error message
     * @param secretId       secret identifier, arn in aws case
     * @param versionId      version Id of the secret
     * @param versionStages  version stages attached with this version of secret
     * @param secretString   secret value in string
     * @param secretBinary   secret value as bytes
     */
    @Builder
    public GetSecretValueResult(SecretResponseStatus responseStatus,
                                String errorMessage,
                                String secretId,
                                String versionId,
                                List<String> versionStages,
                                String secretString,
                                byte[] secretBinary) {
        super(responseStatus, errorMessage);
        this.secretId = secretId;
        this.versionId = versionId;
        this.versionStages = versionStages;
        this.secretString = secretString;
        this.secretBinary = secretBinary;
    }
}
