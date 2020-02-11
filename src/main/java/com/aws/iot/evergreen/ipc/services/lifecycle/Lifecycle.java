package com.aws.iot.evergreen.ipc.services.lifecycle;

import com.aws.iot.evergreen.ipc.services.lifecycle.exceptions.LifecycleIPCException;

import java.util.function.BiConsumer;

/**
 * Interface for Lifecycle operations.
 */
public interface Lifecycle {
    /**
     * Register a handler function to run upon the service moving to "shutdown" state.
     *
     * @param handler handler to call
     * @throws LifecycleIPCException if any error occurs while registering the handler
     */
    void onStopping(Runnable handler) throws LifecycleIPCException;

    /**
     * Report that the service is in some state.
     *
     * @param newState the state to transition into
     * @throws LifecycleIPCException for any error
     */
    void reportState(String newState) throws LifecycleIPCException;

    /**
     * Receive all state updates about a given service with a callback function.
     *
     * @param service      the service to listen to the state of
     * @param onTransition a callback function called with the old state and the new state
     * @throws LifecycleIPCException for any error
     */
    void listenToStateChanges(String service, BiConsumer<String, String> onTransition) throws LifecycleIPCException;
}
