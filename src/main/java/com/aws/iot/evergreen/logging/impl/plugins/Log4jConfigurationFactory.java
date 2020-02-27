/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.aws.iot.evergreen.logging.impl.plugins;

import com.aws.iot.evergreen.logging.impl.config.EvergreenLogConfig;
import com.aws.iot.evergreen.logging.impl.config.LogStore;
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

    static Configuration createConfiguration(final String name, ConfigurationBuilder<BuiltConfiguration> builder) {
        EvergreenLogConfig startupConfig = EvergreenLogConfig.loadDefaultConfig();

        // Set package of plugins
        builder.setConfigurationName(name).setPackages(
                "com.aws.iot.evergreen.logging.impl.plugins," + "com.aws.iot.evergreen.logging.impl.plugins.layouts");
        builder.setStatusLevel(Level.ERROR);


        // Configure log output format
        LayoutComponentBuilder layoutBuilder =
                builder.newLayout("StructuredLayout")
                        .addAttribute("format", startupConfig.getFormat())
                        .addAttribute("pattern", startupConfig.getPattern())
                        .addAttribute("charset", StandardCharsets.UTF_8);


        // Configure log appenders
        LogStore store = startupConfig.getStore();
        String appenderName = name.concat(store.toString());

        switch (startupConfig.getStore()) {
            case CONSOLE:
                addConsoleAppenderToBuilder(builder, appenderName, layoutBuilder);
                break;
            case FILE:
            default:
                String fileName = startupConfig.getStoreName();
                addFileAppenderToBuilder(builder, appenderName, fileName, layoutBuilder, startupConfig);
                break;
        }

        // Configure root logger with default settings
        builder.add(builder.newRootLogger(startupConfig.getLevel()).add(builder.newAppenderRef(appenderName)));
        return builder.build();
    }

    private static void addFileAppenderToBuilder(ConfigurationBuilder<BuiltConfiguration> builder, String appenderName,
                                                 String fileName, LayoutComponentBuilder layoutBuilder,
                                                 EvergreenLogConfig startupConfig) {
        AppenderComponentBuilder appenderBuilder =
                builder.newAppender(appenderName, "RollingFile").addAttribute("fileName", fileName)
                        .addAttribute("filePattern", "%d{MM-dd-yyyy-HH:mm:ss}-".concat(fileName));

        ComponentBuilder triggeringPolicies = builder.newComponent("Policies").addComponent(
                builder.newComponent("SizeBasedTriggeringPolicy")
                        .addAttribute("size", startupConfig.getFileSize() + "K"));
        appenderBuilder.addComponent(triggeringPolicies);

        ComponentBuilder rollover =
                builder.newComponent("DefaultRolloverStrategy").addAttribute("max", startupConfig.getNumRollingFiles());
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
