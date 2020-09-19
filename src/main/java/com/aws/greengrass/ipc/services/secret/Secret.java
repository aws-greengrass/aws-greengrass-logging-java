package com.aws.greengrass.ipc.services.secret;

import com.aws.greengrass.ipc.services.secret.exception.SecretIPCException;

public interface Secret {
    GetSecretValueResult getSecretValue(GetSecretValueRequest request) throws SecretIPCException;
}
