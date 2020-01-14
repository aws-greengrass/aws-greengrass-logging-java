package com.aws.iot.evergreen.ipc.services.common;

public class GeneralRequest<T, E extends Enum<?>> {
    public E type;
    public T request;
}
