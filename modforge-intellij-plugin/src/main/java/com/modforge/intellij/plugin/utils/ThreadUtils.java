package com.modforge.intellij.plugin.utils;

import com.intellij.openapi.diagnostic.Logger;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Utility class for thread management.
 * Compatible with IntelliJ IDEA 2025.1.1.1 and Java 21 virtual threads.
 */
public final class ThreadUtils {
    private static final Logger LOG = Logger.getInstance(ThreadUtils.class);
    
    private static final int CORE_POOL_SIZE = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);
    private static final int MAX_POOL_SIZE = Runtime.getRuntime().availableProcessors() * 2;
    private static final long KEEP_ALIVE_TIME = 60L;
    
    private static final ScheduledExecutorService SCHEDULER;
    private static final ExecutorService EXECUTOR;
    private static final AtomicInteger THREAD_COUNT = new AtomicInteger(0);
    
    static {
        // Initialize thread pool with a custom thread factory
        ThreadFactory factory = r -> {
            Thread thread = Thread.ofVirtual()
                    .name("ModForge-Worker-" + THREAD_COUNT.incrementAndGet())
                    .uncaughtExceptionHandler((t, e) -> LOG.error("Uncaught exception in thread " + t.getName(), e))
                    .factory()
                    .newThread(r);
            return thread;
        };
        
        // Create executor services
        SCHEDULER = Executors.newScheduledThreadPool(CORE_POOL_SIZE, factory);
        EXECUTOR = Executors.newThreadPerTaskExecutor(factory);
        
        LOG.info("ThreadUtils initialized with " + CORE_POOL_SIZE + " core threads and " + MAX_POOL_SIZE + " max threads");
    }
    
    private ThreadUtils() {
        // Private constructor to prevent instantiation
    }

    /**
     * Runs a task asynchronously.
     *
     * @param task The task to run.
     * @return A CompletableFuture representing the result of the task.
     */
    public static CompletableFuture<Void> runAsync(Runnable task) {
        return CompletableFuture.runAsync(task, EXECUTOR);
    }

    /**
     * Runs a task asynchronously with a result.
     *
     * @param task The task to run.
     * @param <T> The type of the result.
     * @return A CompletableFuture representing the result of the task.
     */
    public static <T> CompletableFuture<T> supplyAsync(Callable<T> task) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return task.call();
            } catch (Exception e) {
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                } else {
                    throw new CompletionException(e);
                }
            }
        }, EXECUTOR);
    }

    /**
     * Runs a task after a delay.
     *
     * @param task The task to run.
     * @param delay The delay.
     * @param unit The time unit of the delay.
     * @return A ScheduledFuture representing the scheduled task.
     */
    public static ScheduledFuture<?> runWithDelay(Runnable task, long delay, TimeUnit unit) {
        return SCHEDULER.schedule(task, delay, unit);
    }

    /**
     * Runs a task after a delay.
     *
     * @param task The task to run.
     * @param delay The delay.
     * @return A ScheduledFuture representing the scheduled task.
     */
    public static ScheduledFuture<?> runWithDelay(Runnable task, Duration delay) {
        return SCHEDULER.schedule(task, delay.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * Runs a task periodically.
     *
     * @param task The task to run.
     * @param initialDelay The initial delay.
     * @param period The period.
     * @param unit The time unit of the delay and period.
     * @return A ScheduledFuture representing the scheduled task.
     */
    public static ScheduledFuture<?> runPeriodically(Runnable task, long initialDelay, long period, TimeUnit unit) {
        return SCHEDULER.scheduleAtFixedRate(task, initialDelay, period, unit);
    }

    /**
     * Runs a task periodically.
     *
     * @param task The task to run.
     * @param initialDelay The initial delay.
     * @param period The period.
     * @return A ScheduledFuture representing the scheduled task.
     */
    public static ScheduledFuture<?> runPeriodically(Runnable task, Duration initialDelay, Duration period) {
        return SCHEDULER.scheduleAtFixedRate(
                task,
                initialDelay.toMillis(),
                period.toMillis(),
                TimeUnit.MILLISECONDS);
    }

    /**
     * Creates a new virtual thread.
     *
     * @param name The thread name.
     * @param task The task to run.
     * @return The thread.
     */
    public static Thread newVirtualThread(String name, Runnable task) {
        return Thread.ofVirtual()
                .name(name)
                .uncaughtExceptionHandler((t, e) -> LOG.error("Uncaught exception in thread " + t.getName(), e))
                .start(task);
    }

    /**
     * Shuts down all thread pools.
     */
    public static void shutdown() {
        LOG.info("Shutting down thread pools");
        SCHEDULER.shutdown();
        EXECUTOR.shutdown();
    }

    /**
     * Checks if virtual threads are supported.
     *
     * @return Whether virtual threads are supported.
     */
    public static boolean isVirtualThreadSupported() {
        try {
            Thread.ofVirtual().name("test-virtual-thread").start(() -> {}).join();
            return true;
        } catch (UnsupportedOperationException e) {
            LOG.warn("Virtual threads are not supported", e);
            return false;
        }
    }
}