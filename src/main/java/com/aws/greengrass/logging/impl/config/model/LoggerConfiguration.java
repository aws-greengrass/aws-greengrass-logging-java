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

import java.util.Objects;


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
    @Builder.Default
    private LogFormat format = LogFormat.TEXT;
    private LogStore outputType;
    private String outputDirectory;

    @Override
    public boolean equals(Object o) {
        // If the object is compared with itself then return true
        if (o == this) {
            return true;
        }
        /* Check if o is an instance of LoggerConfiguration or not "null instanceof [type]" also returns false */
        if (!(o instanceof LoggerConfiguration)) {
            return false;
        }

        // typecast o to LoggerConfiguration so that we can compare data members
        LoggerConfiguration newConfiguration = (LoggerConfiguration) o;

        // Compare the data members and return accordingly
        return Objects.equals(this.level, newConfiguration.level)
                && Objects.equals(this.format, newConfiguration.format)
                && Objects.equals(this.outputType, newConfiguration.outputType)
                && this.fileSizeKB == newConfiguration.fileSizeKB
                && this.totalLogsSizeKB == newConfiguration.totalLogsSizeKB
                && Objects.equals(this.outputDirectory, newConfiguration.outputDirectory);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
