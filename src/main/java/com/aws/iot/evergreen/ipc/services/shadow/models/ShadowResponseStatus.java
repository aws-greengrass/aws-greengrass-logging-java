package com.aws.iot.evergreen.ipc.services.shadow.models;

import com.aws.iot.evergreen.ipc.common.GenericErrors;

public enum ShadowResponseStatus  implements GenericErrors {
    Success,  InternalError, InvalidRequest, Unauthorized,
    InvalidArgumentError, ConflictError, ResourceNotFoundError, ServiceError;
}
