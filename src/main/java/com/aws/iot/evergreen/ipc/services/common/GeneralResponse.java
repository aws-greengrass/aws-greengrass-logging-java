package com.aws.iot.evergreen.ipc.services.common;

import com.aws.iot.evergreen.ipc.common.GenericErrors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneralResponse<T, E extends Enum<?> & GenericErrors> {
    private E error;
    private String errorMessage;
    private T response;
}
