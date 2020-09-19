/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.greengrass.ipc.services.authentication;

import com.aws.greengrass.ipc.IPCClient;
import com.aws.greengrass.ipc.exceptions.IPCClientException;
import com.aws.greengrass.ipc.services.common.IPCUtil;
import com.aws.greengrass.logging.api.Logger;
import com.aws.greengrass.logging.impl.LogManager;

import java.util.concurrent.ExecutionException;

import static com.aws.greengrass.ipc.common.BuiltInServiceDestinationCode.AUTHENTICATION;

/**
 * Authenticates the connection with IPC Service.
 */
public class Authentication {
    private static final int AUTHENTICATION_API_VERSION = 1;
    private static final int AUTHENTICATION_OP_CODE = 1;
    private final Logger log = LogManager.getLogger(Authentication.class);
    private final IPCClient ipc;

    public Authentication(IPCClient ipc) {
        this.ipc = ipc;
    }

    // TODO: Needs input validations for all operations
    /**
     * Authenticates the connection. Authentication request should contain authentication token
     *
     * @param request authentication request which contains authentication token
     * @throws IPCClientException for any exceptions
     */
    public AuthenticationResponse doAuthentication(AuthenticationRequest request) throws IPCClientException {
        // TODO: Add timeout waiting for authentication to come back?
        // https://issues.amazon.com/issues/86453f7c-c94e-4a3c-b8ff-679767e7443c
        try {
            AuthenticationResponse authenticationResponse = IPCUtil.sendAndReceive(ipc, AUTHENTICATION.getValue(),
                    AUTHENTICATION_API_VERSION, AUTHENTICATION_OP_CODE, request, AuthenticationResponse.class).get();

            if (authenticationResponse.getErrorMessage() != null) {
                throw new IPCClientException(authenticationResponse.getErrorMessage());
            }
            if (authenticationResponse.getServiceName() == null) {
                throw new IPCClientException("Service name was null");
            }
            log.info("Connected as serviceName {}, clientId {}", authenticationResponse.getServiceName(),
                    authenticationResponse.getClientId());
            return authenticationResponse;
        } catch (ExecutionException | InterruptedException e) {
            throw new IPCClientException("Unable to authenticate ", e);
        }
    }
}
