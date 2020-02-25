package com.aws.iot.evergreen.ipc.services.auth;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String serviceName;
    private String clientId;
    private String errorMessage;
}
