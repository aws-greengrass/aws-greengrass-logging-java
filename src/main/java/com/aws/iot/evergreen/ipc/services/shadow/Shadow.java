package com.aws.iot.evergreen.ipc.services.shadow;

import com.aws.iot.evergreen.ipc.services.shadow.exception.ShadowIPCException;
import com.aws.iot.evergreen.ipc.services.shadow.models.DeleteThingShadowRequest;
import com.aws.iot.evergreen.ipc.services.shadow.models.DeleteThingShadowResult;
import com.aws.iot.evergreen.ipc.services.shadow.models.GetThingShadowRequest;
import com.aws.iot.evergreen.ipc.services.shadow.models.GetThingShadowResult;
import com.aws.iot.evergreen.ipc.services.shadow.models.UpdateThingShadowRequest;
import com.aws.iot.evergreen.ipc.services.shadow.models.UpdateThingShadowResult;

public interface Shadow {
    GetThingShadowResult getThingShadow(GetThingShadowRequest request) throws ShadowIPCException;

    DeleteThingShadowResult deleteThingShadow(DeleteThingShadowRequest request) throws ShadowIPCException;

    UpdateThingShadowResult updateThingShadow(UpdateThingShadowRequest request) throws ShadowIPCException;
}
