/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.iot.evergreen.logging.impl.plugins;

import com.aws.iot.evergreen.logging.impl.config.EvergreenLogConfig;
import com.aws.iot.evergreen.logging.impl.config.EvergreenMetricsConfig;
import com.aws.iot.evergreen.logging.impl.config.PersistenceConfig;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.LayoutComponentBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.core.config.plugins.Plugin;

import java.net.URI;
import java.nio.charset.StandardCharsets;

@Plugin(name = "Log4jConfigurationFactory", category = ConfigurationFactory.CATEGORY)
public class Log4jConfigurationFactory extends ConfigurationFactory {
    public static final String METRICS_LOGGER_NAME = "metrics";

    static Configuration createConfiguration(final String name, ConfigurationBuilder<BuiltConfiguration> builder) {

        // Set package of plugins
        builder.setConfigurationName(name).setPackages(
                "com.aws.iot.evergreen.logging.impl.plugins,com.aws.iot.evergreen.logging.impl.plugins.layouts");
        builder.setStatusLevel(Level.ERROR);

        // Configure root logger
        EvergreenLogConfig logConfig = new EvergreenLogConfig();
        String logAppenderName = name.concat(logConfig.CONFIG_PREFIX);
        configureLoggerAppender(logConfig, logAppenderName, builder);
        builder.add(builder.newRootLogger(logConfig.getLevel()).add(builder.newAppenderRef(logAppenderName)));
        builder.setShutdownHook("disable"); // Disable Log4J shutdown hook

        // Configure metrics logger
        EvergreenMetricsConfig metricsConfig = new EvergreenMetricsConfig();
        String metricsAppenderName = name.concat(metricsConfig.CONFIG_PREFIX);
        configureLoggerAppender(metricsConfig, metricsAppenderName, builder);
        builder.add(builder.newLogger(METRICS_LOGGER_NAME, Level.ALL).add(builder.newAppenderRef(metricsAppenderName))
                .addAttribute("additivity", false));

        return builder.build();
    }

    private static void configureLoggerAppender(PersistenceConfig config, final String appenderName,
                                        ConfigurationBuilder<BuiltConfiguration> builder) {

        // Configure log output format
        LayoutComponentBuilder layoutBuilder = builder.newLayout("StructuredLayout")
            .addAttribute("format", config.getFormat())
            .addAttribute("pattern", config.getPattern())
            .addAttribute("charset", StandardCharsets.UTF_8);

        switch (config.getStore()) {
            case CONSOLE:
                addConsoleAppenderToBuilder(builder, appenderName, layoutBuilder);
                break;
            case FILE:
            default:
                String fileName = config.getStoreName();
                addFileAppenderToBuilder(builder, appenderName, fileName, layoutBuilder,
                        config.getFileSizeKB(), config.getNumRollingFiles());
                break;
        }
    }

    private static void addFileAppenderToBuilder(ConfigurationBuilder<BuiltConfiguration> builder, String appenderName,
                                                 String fileName, LayoutComponentBuilder layoutBuilder,
                                                 String fileSizeKB, int numRollingFiles) {
        AppenderComponentBuilder appenderBuilder =
                builder.newAppender(appenderName, "RollingFile").addAttribute("fileName", fileName)
                        .addAttribute("filePattern", "%d{MM-dd-yyyy-HH:mm:ss}-".concat(fileName));

        ComponentBuilder triggeringPolicies = builder.newComponent("Policies").addComponent(
                builder.newComponent("SizeBasedTriggeringPolicy")
                        .addAttribute("size", fileSizeKB + "K"));
        appenderBuilder.addComponent(triggeringPolicies);

        ComponentBuilder rollover = builder.newComponent("DefaultRolloverStrategy").addAttribute("max",
                numRollingFiles);
        appenderBuilder.addComponent(rollover);
        appenderBuilder.add(layoutBuilder);
        builder.add(appenderBuilder);
    }

    private static void addConsoleAppenderToBuilder(ConfigurationBuilder<BuiltConfiguration> builder,
                                                    String appenderName, LayoutComponentBuilder layoutBuilder) {
        AppenderComponentBuilder appenderBuilder = builder.newAppender(appenderName, "Console");
        appenderBuilder.add(layoutBuilder);
        builder.add(appenderBuilder);
    }

    /**
     * Get supported configuration file types. By specifying a supported type of "*",
     * it will override any configuration files provided.
     *
     * @return supported configuration file types
     */
    @Override
    protected String[] getSupportedTypes() {
        return new String[]{"*"};
    }

    @Override
    public Configuration getConfiguration(LoggerContext loggerContext, ConfigurationSource source) {
        return getConfiguration(loggerContext, source.toString(), null);
    }

    @Override
    public Configuration getConfiguration(final LoggerContext loggerContext, final String name,
                                          final URI configLocation) {
        ConfigurationBuilder<BuiltConfiguration> builder = newConfigurationBuilder();
        return createConfiguration(name, builder);
    }

}
