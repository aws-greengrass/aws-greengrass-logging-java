package ch.qos.logback.core.util;

import ch.qos.logback.core.CoreConstants;

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

    private static final ThreadFactory THREAD_FACTORY = new ThreadFactory() {

        private final ThreadFactory defaultFactory = Executors.defaultThreadFactory();
        private final AtomicInteger threadNumber = new AtomicInteger(1);

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
     * @return scheduled executor service
     */
    public static synchronized ScheduledExecutorService newScheduledExecutorService() {
        // This method has been modified from the original source in order to do two things.
        // 1) Set the scheduled threadpool size to be 1. The original size was 8.
        // 2) Cache and return the same executor so that there's a single threadpool in use to decrease
        //  the number of threads which Logback creates.
        if (executor == null || executor.isShutdown() || executor.isTerminated() || executor.isTerminating()) {
            executor = new ScheduledThreadPoolExecutor(1, THREAD_FACTORY);
        }
        return executor;
    }


    /**
     * Creates an executor service suitable for use by logback components.
     * @return executor service
     */
    public static ExecutorService newExecutorService() {
        return new ThreadPoolExecutor(CoreConstants.CORE_POOL_SIZE, CoreConstants.MAX_POOL_SIZE, 0L,
                TimeUnit.MILLISECONDS, new SynchronousQueue<>(), THREAD_FACTORY);
    }

    /**
     * Shuts down an executor service.
     * @param executorService the executor service to shut down
     */
    public static void shutdown(ExecutorService executorService) {
        executorService.shutdownNow();
    }
}
