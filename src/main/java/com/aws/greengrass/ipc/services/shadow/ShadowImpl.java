package com.aws.greengrass.ipc.services.shadow;

import com.aws.greengrass.ipc.IPCClient;
import com.aws.greengrass.ipc.services.common.IPCUtil;
import com.aws.greengrass.ipc.services.shadow.exception.ShadowIPCException;
import com.aws.greengrass.ipc.services.shadow.models.DeleteThingShadowRequest;
import com.aws.greengrass.ipc.services.shadow.models.DeleteThingShadowResult;
import com.aws.greengrass.ipc.services.shadow.models.GetThingShadowRequest;
import com.aws.greengrass.ipc.services.shadow.models.GetThingShadowResult;
import com.aws.greengrass.ipc.services.shadow.models.ShadowGenericResponse;
import com.aws.greengrass.ipc.services.shadow.models.ShadowResponseStatus;
import com.aws.greengrass.ipc.services.shadow.models.UpdateThingShadowRequest;
import com.aws.greengrass.ipc.services.shadow.models.UpdateThingShadowResult;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static com.aws.greengrass.ipc.common.BuiltInServiceDestinationCode.SHADOW;

public class ShadowImpl implements  Shadow {
    public static final int API_VERSION = 1;
    private final IPCClient ipc;

    public ShadowImpl(IPCClient ipc) {
        this.ipc = ipc;
    }

    @Override
    public GetThingShadowResult getThingShadow(GetThingShadowRequest request) throws ShadowIPCException {
        return sendAndReceive(ShadowClientOpCodes.GET_THING_SHADOW, request, GetThingShadowResult.class);
    }

    @Override
    public UpdateThingShadowResult updateThingShadow(UpdateThingShadowRequest request) throws ShadowIPCException {
        return sendAndReceive(ShadowClientOpCodes.UPDATE_THING_SHADOW, request, UpdateThingShadowResult.class);
    }

    @Override
    public DeleteThingShadowResult deleteThingShadow(DeleteThingShadowRequest request) throws ShadowIPCException {
        return sendAndReceive(ShadowClientOpCodes.DELETE_THING_SHADOW, request, DeleteThingShadowResult.class);
    }

    private <T extends ShadowGenericResponse> T sendAndReceive(ShadowClientOpCodes opCode,
                                                               Object request,
                                                               final Class<T> returnTypeClass)
            throws ShadowIPCException {
        try {
            CompletableFuture<T> responseFuture =
                    IPCUtil.sendAndReceive(ipc, SHADOW.getValue(), API_VERSION, opCode.ordinal(), request,
                            returnTypeClass);
            ShadowGenericResponse response = (ShadowGenericResponse) responseFuture.get();
            if (!ShadowResponseStatus.Success.equals(response.getStatus())) {
                throw new ShadowIPCException(response.getErrorMessage());
            }
            return responseFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new ShadowIPCException(e);
        }
    }

}