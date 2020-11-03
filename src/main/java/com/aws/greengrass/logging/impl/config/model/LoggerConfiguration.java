/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.greengrass.logging.impl.config.model;

import com.aws.greengrass.logging.impl.config.LogFormat;
import com.aws.greengrass.logging.impl.config.LogStore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.slf4j.event.Level;


@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoggerConfiguration {
    @Builder.Default
    private Level level = Level.INFO;
    private String fileName;
    @Builder.Default
    private long fileSizeKB = 1024L;
    @Builder.Default
    private long totalLogsSizeKB = 10240L;
    @Builder.Default
    private LogFormat format = LogFormat.JSON;
    @Builder.Default
    private LogStore outputType = LogStore.CONSOLE;
    private String outputDirectory;
}
