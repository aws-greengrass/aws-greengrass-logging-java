package com.aws.iot.evergreen.ipc.services.lifecycle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StateTransitionEvent {
    private String oldState;
    private String newState;
    private String service;
}