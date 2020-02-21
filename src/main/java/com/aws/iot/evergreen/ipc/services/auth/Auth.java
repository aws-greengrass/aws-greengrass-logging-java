package com.aws.iot.evergreen.ipc.services.auth;

import com.aws.iot.evergreen.ipc.IPCClient;
import com.aws.iot.evergreen.ipc.exceptions.IPCClientException;
import com.aws.iot.evergreen.ipc.services.common.IPCUtil;
import com.aws.iot.evergreen.logging.api.Logger;
import com.aws.iot.evergreen.logging.impl.LogManager;

import java.io.IOException;

import static com.aws.iot.evergreen.ipc.common.BuiltInServiceDestinationCode.AUTH;

/**
 * Authenticates the connection with IPC Service.
 */
public class Auth {
    private static final int AUTH_API_VERSION = 1;
    private static final int AUTH_OP_CODE = 1;
    private final Logger log = LogManager.getLogger(Auth.class);
    private final IPCClient ipc;

    public Auth(IPCClient ipc) {
        this.ipc = ipc;
    }

    // TODO: Needs input validations for all operations
    /**
     * Authenticates the connection. Auth request should contain auth token
     *
     * @param request authentication request which contains auth token
     * @throws IPCClientException for any exceptions
     */
    public AuthResponse doAuth(AuthRequest request) throws IPCClientException {

        // TODO: Add timeout waiting for auth to come back?
        // https://issues.amazon.com/issues/86453f7c-c94e-4a3c-b8ff-679767e7443c
        try {
            AuthResponse authResponse = IPCUtil.sendAndReceive(ipc, AUTH.getValue(),
                    AUTH_API_VERSION, AUTH_OP_CODE, request, AuthResponse.class).get();

            if (authResponse.getErrorMessage() != null) {
                throw new IOException(authResponse.getErrorMessage());
            }
            if (authResponse.getServiceName() == null) {
                throw new IOException("Service name was null");
            }
            log.info("Connected as serviceName %s , clientId %s", authResponse.getServiceName(),
                    authResponse.getClientId());
            return authResponse;
        } catch (Exception e) {
            throw new IPCClientException("Unable to authenticate ", e);
        }
    }
}
