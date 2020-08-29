package com.aws.iot.evergreen.ipc.authorization;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorizationResponse {

    @NonNull private boolean isAuthorized;
    private String errorMessage;
}
