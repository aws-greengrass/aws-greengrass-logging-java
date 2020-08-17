/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.ipc.services.lifecycle;

import com.aws.iot.evergreen.ipc.services.lifecycle.exceptions.LifecycleIPCException;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Interface for Lifecycle operations.
 */
public interface Lifecycle {


    /**
     * Report that the service is in some state.
     *
     * @param newState the state to transition into
     * @throws LifecycleIPCException for any error
     */
    void updateState(String newState) throws LifecycleIPCException;


    /**
     * Subscribe to events PreComponentUpdateEvent and PostComponentUpdateEvent.
     * PreComponentUpdateEvent is send when kernel has pending component updates.
     * PostComponentUpdateEvent is send after performing component updates.
     *
     * @param componentUpdateEventConsumer call back invoked when a component update event is received
     * @throws LifecycleIPCException for any error
     */
    void subscribeToComponentUpdate(Consumer<ComponentUpdateEvent> componentUpdateEventConsumer)
            throws LifecycleIPCException;


    /**
     * Used to acknowledge a PreComponentUpdateEvent.
     * if the update needs to be delayed, respond back with none zero value for recheckTimeInMs. The kernel will wait
     * for recheckTimeInMs seconds and check back with another PreComponentUpdateEvent.
     * if the update can proceed, respond with recheckTimeInMs equal to zero.
     *
     * @param componentName   component that cannot be disrupted now
     * @param recheckTimeInMs time kernel will wait before sending the next PreComponentUpdateEvent
     * @throws LifecycleIPCException for any error
     */
    void deferComponentUpdate(String componentName, long recheckTimeInMs)
            throws LifecycleIPCException;


}
