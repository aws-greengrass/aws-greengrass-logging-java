package com.aws.iot.evergreen.ipc.authorization;

import com.aws.iot.evergreen.ipc.IPCClient;
import com.aws.iot.evergreen.ipc.services.common.IPCUtil;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static com.aws.iot.evergreen.ipc.common.BuiltInServiceDestinationCode.AUTHORIZATION;

public class AuthorizationClient {

    public static final int AUTHORIZATION_API_VERSION = 1;
    private final IPCClient ipc;

    public AuthorizationClient(IPCClient ipc) {
        this.ipc = ipc;
    }

    /**
     * Authorizes the requested operations.
     *
     * @param token token to validate server-side
     * @throws AuthorizationException for any exceptions
     */
    public AuthorizationResponse validateToken(String token) throws AuthorizationException {
        if (token == null) {
            throw new AuthorizationException("Provided auth token is null");
        } else if (token.isEmpty()) {
            throw new AuthorizationException("Provided auth token is empty");
        }
        AuthorizationRequest request = AuthorizationRequest.builder().token(token).build();
        return sendAndReceive(AuthorizationClientOpCodes.VALIDATE_TOKEN, request, AuthorizationResponse.class);
    }

    private <T> T sendAndReceive(AuthorizationClientOpCodes opCode, Object request, final Class<T> returnTypeClass)
            throws AuthorizationException {
        try {
            CompletableFuture<T> responseFuture  = IPCUtil.sendAndReceive(ipc, AUTHORIZATION.getValue(),
                    AUTHORIZATION_API_VERSION, opCode.ordinal(), request, returnTypeClass);
            AuthorizationResponse authorizationResponse = (AuthorizationResponse) responseFuture.get();

            if (!authorizationResponse.isAuthorized()) {
                throw new AuthorizationException(authorizationResponse.getErrorMessage());
            }
            return responseFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new AuthorizationException(String.format("Unable to authorize component due to error %s",
                    e.getMessage()));
        }
    }
}
