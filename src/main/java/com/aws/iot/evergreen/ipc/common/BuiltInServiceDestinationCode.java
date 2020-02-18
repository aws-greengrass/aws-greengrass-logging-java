package com.aws.iot.evergreen.ipc.common;

public enum BuiltInServiceDestinationCode {
    AUTH(0), LIFECYCLE(1), SERVICE_DISCOVERY(2), ERROR(255);

    private final int value;

    BuiltInServiceDestinationCode(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
