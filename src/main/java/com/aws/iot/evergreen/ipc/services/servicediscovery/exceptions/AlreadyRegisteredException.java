/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.ipc.services.servicediscovery.exceptions;

public class AlreadyRegisteredException extends ServiceDiscoveryException {
    public AlreadyRegisteredException(Throwable e) {
        super(e);
    }

    public AlreadyRegisteredException(String errorMessage) {
        super(errorMessage);
    }
}
