package com.aws.iot.evergreen.logging.impl;

import com.aws.iot.evergreen.logging.api.MetricsFactory;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.rolling.CompositeTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.DefaultRolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.SizeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.NullConfiguration;
import org.apache.logging.log4j.core.layout.JsonLayout;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Log4jLogManager implements com.aws.iot.evergreen.logging.api.LogManager {

    // key: name (String), value: a Log4jLoggerAdapter;
    ConcurrentMap<String, com.aws.iot.evergreen.logging.api.Logger> loggerMap;
    // ConcurrentMap<String, com.aws.iot.evergreen.logging.api.Logger> metricsFactoryMap;

    private Configuration configuration;
    private LoggerContext context;
    private LoggerConfig loggerConfig;

    public Log4jLogManager() {
        loggerMap = new ConcurrentHashMap<>();
        // metricsFactoryMap = new ConcurrentHashMap<>();

        context = (LoggerContext) LogManager.getContext(false);
        configuration = context.getConfiguration();

        JsonLayout layout = JsonLayout.newBuilder().setCompact(true).setComplete(false).setEventEol(true)
                .setLocationInfo(false).setObjectMessageAsJsonObject(true).setPropertiesAsList(false)
                .setProperties(false).build();

        Appender appender = RollingFileAppender.newBuilder().setName("RollingFile").setLayout(layout)
                .withFileName("demo.log").withFilePattern("demo.%d{MM-dd-yyyy-HH:mm:ss}.log").withAppend(true)
                .setConfiguration(new NullConfiguration())
                .withPolicy(CompositeTriggeringPolicy.createPolicy(SizeBasedTriggeringPolicy.createPolicy("3096")))
                .withStrategy(DefaultRolloverStrategy.newBuilder().withMax("5").build()).build();

        loggerConfig = new LoggerConfig("DEFAULT", Level.DEBUG, false);
        loggerConfig.addAppender(appender, null, null);

        configuration.addAppender(appender);

        LogManager.getRootLogger();
    }

    @Override
    public com.aws.iot.evergreen.logging.api.Logger getLogger(String name) {
        com.aws.iot.evergreen.logging.api.Logger evgLogger = loggerMap.get(name);
        if (evgLogger != null) {
            return evgLogger;
        } else {
            Logger log4jLogger;

            configuration.addLogger(name, loggerConfig);
            context.updateLoggers(configuration);

            log4jLogger = LogManager.getLogger(name);

            com.aws.iot.evergreen.logging.api.Logger newInstance = new Log4jLoggerAdapter(log4jLogger);
            com.aws.iot.evergreen.logging.api.Logger oldInstance = loggerMap.putIfAbsent(name, newInstance);
            return oldInstance == null ? newInstance : oldInstance;
        }
    }

    @Override
    public com.aws.iot.evergreen.logging.api.Logger getLogger(Class<?> clazz) {
        return getLogger(clazz.getName());
    }

    /**
     * TODO: implement metrics generation https://sim.amazon.com/issues/P32636549
     */
    @Override
    public MetricsFactory getMetricsFactory(String name) {

        return null;
    }

    @Override
    public MetricsFactory getMetricsFactory(Class<?> clazz) {
        return getMetricsFactory(clazz.getName());
    }
}
