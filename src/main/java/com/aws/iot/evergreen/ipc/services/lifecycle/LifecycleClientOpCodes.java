package com.aws.iot.evergreen.ipc.services.lifecycle;

public enum LifecycleRequestTypes {
    listen(0), setState(1), transition(2);

    private final int value;

    LifecycleRequestTypes(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
