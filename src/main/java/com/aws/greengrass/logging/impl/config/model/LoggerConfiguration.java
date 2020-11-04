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
    private Level level;
    private String fileName;
    @Builder.Default
    private long fileSizeKB = -1;
    @Builder.Default
    private long totalLogsSizeKB = -1;
    private LogFormat format;
    private LogStore outputType;
    private String outputDirectory;
}
