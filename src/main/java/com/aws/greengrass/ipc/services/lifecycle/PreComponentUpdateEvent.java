package com.aws.greengrass.ipc.services.lifecycle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreComponentUpdateEvent extends ComponentUpdateEvent {
    boolean isGgcRestarting;
}
