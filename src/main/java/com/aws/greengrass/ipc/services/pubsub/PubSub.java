/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.greengrass.ipc.services.pubsub;

import java.util.function.Consumer;

/**
 * PubSub client interface. Used to publish, subscribe, and unsubscribe to Greengrass topics.
 */
public interface PubSub {
    /**
     * Publish a message to a Greengrass topic.
     *
     * @param topic topic to publish to
     * @param payload message payload
     */
    void publishToTopic(String topic, byte[] payload) throws PubSubException;

    /**
     * Subscribe to messages from a topic.
     * Only 1 subscription can exist per-topic.
     *
     * @param topic topic to subscribe to
     * @param callback function to be called when a message is published to the topic
     */
    void subscribeToTopic(String topic, Consumer<byte[]> callback) throws PubSubException;

    /**
     * Unsubscribe from a topic.
     *
     * @param topic topic to unsubscribe from
     */
    void unsubscribeFromTopic(String topic) throws PubSubException;
}
