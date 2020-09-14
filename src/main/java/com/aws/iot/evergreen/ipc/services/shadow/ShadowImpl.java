package com.aws.iot.evergreen.ipc.services.shadow;

import com.aws.iot.evergreen.ipc.IPCClient;
import com.aws.iot.evergreen.ipc.services.common.IPCUtil;
import com.aws.iot.evergreen.ipc.services.shadow.exception.ShadowIPCException;
import com.aws.iot.evergreen.ipc.services.shadow.models.DeleteThingShadowRequest;
import com.aws.iot.evergreen.ipc.services.shadow.models.DeleteThingShadowResult;
import com.aws.iot.evergreen.ipc.services.shadow.models.GetThingShadowRequest;
import com.aws.iot.evergreen.ipc.services.shadow.models.GetThingShadowResult;
import com.aws.iot.evergreen.ipc.services.shadow.models.ShadowGenericResponse;
import com.aws.iot.evergreen.ipc.services.shadow.models.ShadowResponseStatus;
import com.aws.iot.evergreen.ipc.services.shadow.models.UpdateThingShadowRequest;
import com.aws.iot.evergreen.ipc.services.shadow.models.UpdateThingShadowResult;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static com.aws.iot.evergreen.ipc.common.BuiltInServiceDestinationCode.SHADOW;

public class ShadowImpl implements  Shadow {
    public static final int API_VERSION = 1;
    private final IPCClient ipc;

    public ShadowImpl(IPCClient ipc) {
        this.ipc = ipc;
    }

    @Override
    public GetThingShadowResult getThingShadow(GetThingShadowRequest request) throws ShadowIPCException {
        final ShadowGenericResponse response = sendAndReceive(ShadowClientOpCodes.GET_THING_SHADOW, request,
                ShadowGenericResponse.class);

        return new GetThingShadowResult(response.getPayload());
    }

    @Override
    public UpdateThingShadowResult updateThingShadow(UpdateThingShadowRequest request) throws ShadowIPCException {
        final ShadowGenericResponse response = sendAndReceive(ShadowClientOpCodes.UPDATE_THING_SHADOW, request,
                ShadowGenericResponse.class);

        return new UpdateThingShadowResult(response.getPayload());
    }

    @Override
    public DeleteThingShadowResult deleteThingShadow(DeleteThingShadowRequest request) throws ShadowIPCException {
        final ShadowGenericResponse response = sendAndReceive(ShadowClientOpCodes.DELETE_THING_SHADOW, request,
                ShadowGenericResponse.class);

        return new DeleteThingShadowResult(response.getPayload());
    }

    private <T extends ShadowGenericResponse> T sendAndReceive(ShadowClientOpCodes opCode,
                                                               Object request,
                                                               final Class<T> returnTypeClass)
            throws ShadowIPCException {
        try {
            CompletableFuture<T> responseFuture =
                    IPCUtil.sendAndReceive(ipc, SHADOW.getValue(), API_VERSION, opCode.ordinal(), request,
                            returnTypeClass);
            ShadowGenericResponse response = responseFuture.get();
            if (!ShadowResponseStatus.Success.equals(response.getStatus())) {
                throw new ShadowIPCException(response.getErrorMessage());
            }
            return responseFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new ShadowIPCException(e);
        }
    }

}
