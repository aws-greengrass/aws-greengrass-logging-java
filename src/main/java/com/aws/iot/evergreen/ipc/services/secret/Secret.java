package com.aws.iot.evergreen.ipc.services.secret;

import com.aws.iot.evergreen.ipc.services.secret.exception.SecretIPCException;

public interface Secret {
    GetSecretValueResult getSecretValue(GetSecretValueRequest request) throws SecretIPCException;
}
