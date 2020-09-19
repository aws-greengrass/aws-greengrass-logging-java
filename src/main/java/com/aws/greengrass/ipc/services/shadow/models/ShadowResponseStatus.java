package com.aws.greengrass.ipc.services.shadow.models;

import com.aws.greengrass.ipc.common.GenericErrors;

public enum ShadowResponseStatus  implements GenericErrors {
    Success,  InternalError, InvalidRequest, Unauthorized,
     ConflictError, ResourceNotFoundError;
}
