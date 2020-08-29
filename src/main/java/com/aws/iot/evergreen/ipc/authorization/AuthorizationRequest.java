package com.aws.iot.evergreen.ipc.authorization;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Value;

@Builder
@Value
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class AuthorizationRequest {

    @NonNull
    private String token;
}
