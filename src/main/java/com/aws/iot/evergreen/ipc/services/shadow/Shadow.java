package com.aws.iot.evergreen.ipc.services.shadow;

import com.aws.iot.evergreen.ipc.services.shadow.exception.ShadowIPCException;
import com.aws.iot.evergreen.ipc.services.shadow.models.GetThingShadowRequest;
import com.aws.iot.evergreen.ipc.services.shadow.models.GetThingShadowResult;

public interface Shadow {
    GetThingShadowResult getThingShadow(GetThingShadowRequest request) throws ShadowIPCException;
}
