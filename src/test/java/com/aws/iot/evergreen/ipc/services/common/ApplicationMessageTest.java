/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.ipc.services.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ApplicationMessageTest {

    @Test
    public void WHEN_build_application_message_with_invalid_version_THEN_builder_fails() {
        assertThrows(IllegalArgumentException.class, () -> ApplicationMessage.builder()
                .version(ApplicationMessage.MAX_VERSION_VALUE + 1).payload(new byte[1]).build());

        assertThrows(IllegalArgumentException.class, () -> ApplicationMessage.builder()
                .version(ApplicationMessage.MIN_VERSION_VALUE - 1).payload(new byte[1]).build());
    }

    @Test
    public void WHEN_build_application_message_with_valid_version_THEN_builder_succeeds() {
        ApplicationMessage applicationMessage = ApplicationMessage.builder()
                .version(1).payload(new byte[1]).build();
        assertEquals(1, applicationMessage.getVersion());
    }

}
