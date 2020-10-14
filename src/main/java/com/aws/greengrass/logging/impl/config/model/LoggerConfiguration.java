/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.greengrass.logging.impl.config.model;

import com.aws.greengrass.logging.impl.config.LogFormat;
import com.aws.greengrass.logging.impl.config.LogStore;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.event.Level;

import java.nio.file.Path;

@Getter
@Setter
@Builder
public class LoggerConfiguration {
    private Level level;
    private String fileName;
    @Builder.Default
    private long fileSizeKB = -1;
    @Builder.Default
    private long totalLogStoreSizeKB = -1;
}
