package com.aws.iot.evergreen.ipc.services.lifecycle;

import com.aws.iot.evergreen.ipc.common.GenericErrors;

public enum LifecycleResponseStatus implements GenericErrors {
    Success, InternalError, InvalidRequest, Unauthorized, StateTransitionCallbackNotFound;
}
