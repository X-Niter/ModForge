package com.modforge.intellij.plugin.utils;

import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * Utility class for thread operations, with Java 21 virtual thread support.
 * Compatible with IntelliJ IDEA 2025.1.1.1
 */
public final class ThreadUtils {
    private static final Logger LOG = Logger.getInstance(ThreadUtils.class);
    
    // Thread pool for virtual threads
    private static final ExecutorService VIRTUAL_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();
    
    // Scheduled executor for periodic tasks
    private static final ScheduledExecutorService SCHEDULED_EXECUTOR = Executors.newScheduledThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors() / 2),
            r -> {
                Thread t = new Thread(r, "ModForge-Scheduler");
                t.setDaemon(true);
                return t;
            }
    );
    
    /**
     * Private constructor to prevent instantiation.
     */
    private ThreadUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Runs a task asynchronously using virtual threads.
     *
     * @param runnable The task to run.
     * @return A CompletableFuture representing the completion of the task.
     */
    public static CompletableFuture<Void> runAsyncVirtual(@NotNull Runnable runnable) {
        return CompletableFuture.runAsync(runnable, VIRTUAL_EXECUTOR);
    }

    /**
     * Supplies a value asynchronously using virtual threads.
     *
     * @param supplier The supplier to run.
     * @param <T>      The type of the value.
     * @return A CompletableFuture representing the completion of the supplier.
     */
    public static <T> CompletableFuture<T> supplyAsyncVirtual(@NotNull Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, VIRTUAL_EXECUTOR);
    }

    /**
     * Runs a task after a delay.
     *
     * @param runnable The task to run.
     * @param delay    The delay.
     * @return A ScheduledFuture representing the scheduled task.
     */
    public static ScheduledFuture<?> schedule(@NotNull Runnable runnable, @NotNull Duration delay) {
        return SCHEDULED_EXECUTOR.schedule(runnable, delay.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * Runs a task periodically.
     *
     * @param runnable      The task to run.
     * @param initialDelay  The initial delay.
     * @param period        The period.
     * @return A ScheduledFuture representing the scheduled task.
     */
    public static ScheduledFuture<?> scheduleAtFixedRate(
            @NotNull Runnable runnable,
            @NotNull Duration initialDelay,
            @NotNull Duration period) {
        return SCHEDULED_EXECUTOR.scheduleAtFixedRate(
                runnable,
                initialDelay.toMillis(),
                period.toMillis(),
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * Runs a task periodically with a fixed delay between the end of one execution and the start of the next.
     *
     * @param runnable      The task to run.
     * @param initialDelay  The initial delay.
     * @param delay         The delay between executions.
     * @return A ScheduledFuture representing the scheduled task.
     */
    public static ScheduledFuture<?> scheduleWithFixedDelay(
            @NotNull Runnable runnable,
            @NotNull Duration initialDelay,
            @NotNull Duration delay) {
        return SCHEDULED_EXECUTOR.scheduleWithFixedDelay(
                runnable,
                initialDelay.toMillis(),
                delay.toMillis(),
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * Shuts down the executor services.
     */
    public static void shutdown() {
        VIRTUAL_EXECUTOR.shutdown();
        SCHEDULED_EXECUTOR.shutdown();
    }

    /**
     * Gets the virtual executor.
     *
     * @return The virtual executor.
     */
    public static ExecutorService getVirtualExecutor() {
        return VIRTUAL_EXECUTOR;
    }

    /**
     * Gets the scheduled executor.
     *
     * @return The scheduled executor.
     */
    public static ScheduledExecutorService getScheduledExecutor() {
        return SCHEDULED_EXECUTOR;
    }
}