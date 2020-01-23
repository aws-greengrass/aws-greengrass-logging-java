package com.aws.iot.evergreen.ipc.services.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneralResponse<T, E extends Enum<?>> {
    private E error;
    private String errorMessage;
    private T response;
}
