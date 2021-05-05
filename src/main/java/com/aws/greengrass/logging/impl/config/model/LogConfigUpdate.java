/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.greengrass.logging.impl.config.model;

import com.aws.greengrass.logging.impl.config.LogFormat;
import com.aws.greengrass.logging.impl.config.LogStore;
import com.aws.greengrass.logging.impl.config.PersistenceConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.slf4j.event.Level;


/**
 * Data transfer object for passing around log config parameters.
 */
@Builder
@Data
@AllArgsConstructor
public class LogConfigUpdate {
    private Level level;
    private String fileName;
    private Long fileSizeKB;
    private Long totalLogsSizeKB;
    private LogFormat format;
    private String outputDirectory;
    private LogStore outputType;

    /**
     * Construct from a PersistenceConfig by reading the config values from it.
     * @param persistenceConfig a PersistenceConfig object
     */
    public LogConfigUpdate(PersistenceConfig persistenceConfig) {
        level = persistenceConfig.getLevel();
        fileName = persistenceConfig.getFileName();
        fileSizeKB = persistenceConfig.getFileSizeKB();
        totalLogsSizeKB = persistenceConfig.getTotalLogStoreSizeKB();
        format = persistenceConfig.getFormat();
        outputDirectory = persistenceConfig.getStoreDirectory().toString();
        outputType = persistenceConfig.getStore();
    }
}
