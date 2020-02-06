package com.aws.iot.evergreen.ipc.services.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneralRequest<T, E extends Enum<?>> {
    private E type;
    private T request;
}
