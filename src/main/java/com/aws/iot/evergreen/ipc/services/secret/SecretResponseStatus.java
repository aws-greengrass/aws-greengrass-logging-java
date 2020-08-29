package com.aws.iot.evergreen.ipc.services.secret;

import com.aws.iot.evergreen.ipc.common.GenericErrors;

public enum SecretResponseStatus implements GenericErrors {
    Success, InternalError, InvalidRequest, Unauthorized;
}
