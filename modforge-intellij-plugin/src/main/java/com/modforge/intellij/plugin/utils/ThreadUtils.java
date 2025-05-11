package com.modforge.intellij.plugin.utils;

import com.intellij.openapi.diagnostic.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Utility class for thread management, optimized for Java 21 virtual threads.
 * Compatible with IntelliJ IDEA 2025.1.1.1
 */
public final class ThreadUtils {
    private static final Logger LOG = Logger.getInstance(ThreadUtils.class);
    
    /**
     * Creates a virtual thread executor service.
     * Uses Java 21 virtual threads for optimal performance with network-bound operations.
     *
     * @return The executor service.
     */
    public static ExecutorService createVirtualThreadExecutor() {
        try {
            // Java 21 virtual threads
            return Executors.newVirtualThreadPerTaskExecutor();
        } catch (Exception e) {
            // Fall back to a standard thread pool if virtual threads are not available
            LOG.warn("Virtual threads not available, falling back to standard thread pool", e);
            return Executors.newCachedThreadPool(createNamedThreadFactory("ModForge-Pool"));
        }
    }
    
    /**
     * Creates a scheduled executor service optimized for the environment.
     *
     * @param corePoolSize The number of threads to keep in the pool.
     * @return The scheduled executor service.
     */
    public static ScheduledExecutorService createScheduledExecutor(int corePoolSize) {
        return Executors.newScheduledThreadPool(corePoolSize, createNamedThreadFactory("ModForge-Scheduled"));
    }
    
    /**
     * Creates a named thread factory.
     *
     * @param prefix The prefix for thread names.
     * @return The thread factory.
     */
    public static ThreadFactory createNamedThreadFactory(String prefix) {
        return new NamedThreadFactory(prefix);
    }
    
    /**
     * Thread factory that creates named threads.
     */
    private static class NamedThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;
        
        /**
         * Creates a new named thread factory.
         *
         * @param prefix The prefix for thread names.
         */
        public NamedThreadFactory(String prefix) {
            this.namePrefix = prefix + "-";
        }
        
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, namePrefix + threadNumber.getAndIncrement());
            
            // Set to daemon to not prevent JVM shutdown
            thread.setDaemon(true);
            
            return thread;
        }
    }
    
    /**
     * Private constructor to prevent instantiation.
     */
    private ThreadUtils() {
        throw new AssertionError("Utility class should not be instantiated");
    }
}