package com.aws.greengrass.ipc.services.secret;

import com.aws.greengrass.ipc.common.GenericErrors;

public enum SecretResponseStatus implements GenericErrors {
    Success, InternalError, InvalidRequest, Unauthorized;
}
