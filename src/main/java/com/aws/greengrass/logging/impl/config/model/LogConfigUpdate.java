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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.slf4j.event.Level;


/**
 * Data transfer object for passing around log config parameters.
 */
@Builder(toBuilder = true)
@ToString
@EqualsAndHashCode
@Getter
@AllArgsConstructor
public class LogConfigUpdate {
    private final Level level;
    private final String fileName;
    private final Long fileSizeKB;
    private final Long totalLogsSizeKB;
    private final LogFormat format;
    private final String outputDirectory;
    private final LogStore outputType;

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
