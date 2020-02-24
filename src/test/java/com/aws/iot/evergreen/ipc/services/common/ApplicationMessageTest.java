package com.aws.iot.evergreen.ipc.services.common;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ApplicationMessageTest {

    @Test
    public void WHEN_build_application_message_with_invalid_version_THEN_builder_fails() {
        assertThrows(IllegalArgumentException.class, () -> ApplicationMessage.builder()
                .version(ApplicationMessage.MAX_VERSION_VALUE + 1).payload(new byte[1]).build());

        assertThrows(IllegalArgumentException.class, () -> ApplicationMessage.builder()
                .version(ApplicationMessage.MIN_VERSION_VALUE - 1).payload(new byte[1]).build());
    }

}
