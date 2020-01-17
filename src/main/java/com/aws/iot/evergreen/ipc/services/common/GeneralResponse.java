package com.aws.iot.evergreen.ipc.services.common;

public class GeneralResponse<T, E extends Enum<?>> {
    public E error;
    public String errorMessage;
    public T response;
}
