package com.modforge.intellij.plugin.utils;

import com.intellij.openapi.diagnostic.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Utility class for thread management, optimized for Java 21 virtual threads.
 * Compatible with IntelliJ IDEA 2025.1.1.1
 */
public final class ThreadUtils {
    private static final Logger LOG = Logger.getInstance(ThreadUtils.class);
    private static final int VIRTUAL_THREAD_POOL_SIZE = 250;
    private static final int PLATFORM_THREAD_POOL_SIZE = 16;
    
    private static final ExecutorService VIRTUAL_THREAD_POOL;
    private static final ExecutorService PLATFORM_THREAD_POOL;
    
    static {
        // Initialize virtual thread pool - optimized for Java 21
        VIRTUAL_THREAD_POOL = createVirtualThreadPool();
        
        // Initialize platform thread pool for tasks that need platform threads
        PLATFORM_THREAD_POOL = Executors.newFixedThreadPool(
                PLATFORM_THREAD_POOL_SIZE,
                new NamedThreadFactory("ModForge-Platform-Thread")
        );
        
        // Register shutdown hook to clean up threads
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            shutdownExecutor(VIRTUAL_THREAD_POOL, "Virtual Thread Pool");
            shutdownExecutor(PLATFORM_THREAD_POOL, "Platform Thread Pool");
        }));
    }
    
    private ThreadUtils() {
        // Private constructor to prevent instantiation
    }

    /**
     * Executes a task on a virtual thread.
     *
     * @param runnable The task to run.
     */
    public static void executeOnVirtualThread(Runnable runnable) {
        VIRTUAL_THREAD_POOL.execute(() -> {
            try {
                runnable.run();
            } catch (Throwable t) {
                LOG.error("Error executing task on virtual thread", t);
            }
        });
    }

    /**
     * Executes a task on a platform thread.
     *
     * @param runnable The task to run.
     */
    public static void executeOnPlatformThread(Runnable runnable) {
        PLATFORM_THREAD_POOL.execute(() -> {
            try {
                runnable.run();
            } catch (Throwable t) {
                LOG.error("Error executing task on platform thread", t);
            }
        });
    }

    /**
     * Creates a virtual thread pool using Java 21 virtual threads.
     * Falls back to a standard thread pool on older JDK versions.
     *
     * @return The executor service.
     */
    private static ExecutorService createVirtualThreadPool() {
        try {
            // Attempt to create a virtual thread executor (Java 21+)
            return Executors.newVirtualThreadPerTaskExecutor();
        } catch (Throwable t) {
            LOG.warn("Could not create virtual thread pool. Falling back to standard thread pool.", t);
            
            // Fall back to a standard thread pool
            return Executors.newFixedThreadPool(
                    VIRTUAL_THREAD_POOL_SIZE,
                    new NamedThreadFactory("ModForge-Thread")
            );
        }
    }

    /**
     * Shuts down an executor service.
     *
     * @param executorService The executor service.
     * @param name The name of the executor service.
     */
    private static void shutdownExecutor(ExecutorService executorService, String name) {
        try {
            LOG.info("Shutting down " + name);
            executorService.shutdown();
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                LOG.warn(name + " did not terminate in time, forcing shutdown");
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            LOG.warn("Interrupted while shutting down " + name, e);
            Thread.currentThread().interrupt();
        } catch (Throwable t) {
            LOG.error("Error shutting down " + name, t);
        }
    }

    /**
     * Thread factory that creates named threads.
     */
    private static class NamedThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;
        
        NamedThreadFactory(String namePrefix) {
            this.namePrefix = namePrefix;
        }
        
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, namePrefix + "-" + threadNumber.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }
}