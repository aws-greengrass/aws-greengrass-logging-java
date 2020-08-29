package com.aws.iot.evergreen.ipc.services.secret;

import com.aws.iot.evergreen.ipc.IPCClient;
import com.aws.iot.evergreen.ipc.services.common.IPCUtil;
import com.aws.iot.evergreen.ipc.services.secret.exception.SecretIPCException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static com.aws.iot.evergreen.ipc.common.BuiltInServiceDestinationCode.SECRET;

public class SecretImpl implements Secret {
    public static final int API_VERSION = 1;
    private final IPCClient ipc;

    public SecretImpl(IPCClient ipc) {
        this.ipc = ipc;
    }

    @Override
    public GetSecretValueResult getSecretValue(GetSecretValueRequest request) throws SecretIPCException {
        return sendAndReceive(SecretClientOpCodes.GET_SECRET, request, GetSecretValueResult.class);
    }

    private <T extends SecretGenericResponse> T sendAndReceive(SecretClientOpCodes opCode,
                                                               Object request,
                                                               final Class<T> returnTypeClass)
            throws SecretIPCException {
        try {
            CompletableFuture<T> responseFuture =
                    IPCUtil.sendAndReceive(ipc, SECRET.getValue(), API_VERSION, opCode.ordinal(), request,
                            returnTypeClass);
            SecretGenericResponse response = (SecretGenericResponse) responseFuture.get();
            if (!SecretResponseStatus.Success.equals(response.getStatus())) {
                throw new SecretIPCException(response.getErrorMessage());
            }
            return responseFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new SecretIPCException(e);
        }
    }
}
