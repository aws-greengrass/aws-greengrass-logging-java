/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package ch.qos.logback.core.util;

import ch.qos.logback.core.CoreConstants;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Static utility methods for manipulating an {@link ExecutorService}.
 *
 * @author Carl Harris
 * @author Mikhail Mazursky
 * @author Modified by AWS to reuse the same scheduled executor see {@link #newExecutorService()}
 */
public class ExecutorServiceUtil {

    static private final String NEW_VIRTUAL_TPT_METHOD_NAME = "newVirtualThreadPerTaskExecutor";

    private static final ThreadFactory THREAD_FACTORY_FOR_SCHEDULED_EXECUTION_SERVICE = new ThreadFactory() {

        private final AtomicInteger threadNumber = new AtomicInteger(1);


        private final ThreadFactory defaultFactory = makeThreadFactory();

        /**
         * A thread factory which may be a virtual thread factory the JDK supports it.
         *
         * @return
         */
        private ThreadFactory makeThreadFactory() {
            if (EnvUtil.isJDK21OrHigher()) {
                try {
                    Method ofVirtualMethod = Thread.class.getMethod("ofVirtual");
                    Object threadBuilderOfVirtual = ofVirtualMethod.invoke(null);
                    Method factoryMethod = threadBuilderOfVirtual.getClass().getMethod("factory");
                    return (ThreadFactory) factoryMethod.invoke(threadBuilderOfVirtual);
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                    return Executors.defaultThreadFactory();
                }

            } else {
                return Executors.defaultThreadFactory();
            }
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = defaultFactory.newThread(r);
            if (!thread.isDaemon()) {
                thread.setDaemon(true);
            }
            thread.setName("logback-" + threadNumber.getAndIncrement());
            return thread;
        }
    };

    private static ScheduledThreadPoolExecutor executor;

    /**
     * Creates a scheduled executor service suitable for use by logback components.
     *
     * @return scheduled executor service
     */
    public static synchronized ScheduledExecutorService newScheduledExecutorService() {
        // This method has been modified from the original source in order to do two things.
        // 1) Set the scheduled threadpool size to be 1. The original size was 8.
        // 2) Cache and return the same executor so that there's a single threadpool in use to decrease
        //  the number of threads which Logback creates.
        if (executor == null || executor.isShutdown() || executor.isTerminated() || executor.isTerminating()) {
            executor = new ScheduledThreadPoolExecutor(1, THREAD_FACTORY_FOR_SCHEDULED_EXECUTION_SERVICE);
        }
        return executor;
    }

    /**
     * Creates threadpool executor suitable for use by logback components.
     *
     * @return threadpool executor
     */
    static public ThreadPoolExecutor newThreadPoolExecutor() {
        return new ThreadPoolExecutor(CoreConstants.CORE_POOL_SIZE, CoreConstants.MAX_POOL_SIZE, 0L,
                TimeUnit.MILLISECONDS, new SynchronousQueue<>(), THREAD_FACTORY_FOR_SCHEDULED_EXECUTION_SERVICE);
    }


    /**
     * Creates an executor service suitable for use by logback components.
     *
     * @deprecated replaced by {@link #newThreadPoolExecutor()}
     */
    static public ExecutorService newExecutorService() {
        return newThreadPoolExecutor();
    }

    /**
     * Shuts down an executor service.
     * <p>
     *
     * @param executorService the executor service to shut down
     */
    public static void shutdown(ExecutorService executorService) {
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    /**
     * An alternate implementation of {@link #newThreadPoolExecutor} which returns a virtual thread per task executor
     * when available.
     *
     * @since 1.3.12/1.4.12
     */
    static public ExecutorService newAlternateThreadPoolExecutor() {
        if (EnvUtil.isJDK21OrHigher()) {
            try {
                Method newVirtualTPTMethod = Executors.class.getMethod(NEW_VIRTUAL_TPT_METHOD_NAME);
                return (ExecutorService) newVirtualTPTMethod.invoke(null);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                return newThreadPoolExecutor();
            }
        } else {
            return newThreadPoolExecutor();
        }
    }
}
