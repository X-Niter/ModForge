package com.modforge.intellij.plugin.utils;

import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * Utilities for working with threads, including Java 21 virtual threads.
 * Compatible with IntelliJ IDEA 2025.1.1.1
 */
public final class ThreadUtils {
    private static final Logger LOG = Logger.getInstance(ThreadUtils.class);
    
    // Thread pool sizes
    private static final int CORE_POOL_SIZE = Runtime.getRuntime().availableProcessors();
    private static final int MAX_POOL_SIZE = Math.max(CORE_POOL_SIZE * 2, 8);
    
    // Thread pools
    private static final ExecutorService PLATFORM_EXECUTOR = Executors.newCachedThreadPool(
            r -> {
                Thread thread = new Thread(r);
                thread.setDaemon(true);
                return thread;
            }
    );
    
    private static final ExecutorService VIRTUAL_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();
    
    private static final ScheduledExecutorService SCHEDULED_EXECUTOR = Executors.newScheduledThreadPool(
            CORE_POOL_SIZE,
            r -> {
                Thread thread = new Thread(r);
                thread.setDaemon(true);
                return thread;
            }
    );

    private ThreadUtils() {
        // Private constructor to prevent instantiation
    }

    /**
     * Executes a task asynchronously using platform threads.
     *
     * @param task The task to execute.
     */
    public static void runAsync(@NotNull Runnable task) {
        PLATFORM_EXECUTOR.execute(() -> {
            try {
                task.run();
            } catch (Exception e) {
                LOG.error("Error executing task asynchronously", e);
            }
        });
    }

    /**
     * Executes a task asynchronously using Java 21 virtual threads.
     *
     * @param task The task to execute.
     */
    public static void runAsyncVirtual(@NotNull Runnable task) {
        VIRTUAL_EXECUTOR.execute(() -> {
            try {
                task.run();
            } catch (Exception e) {
                LOG.error("Error executing task asynchronously on virtual thread", e);
            }
        });
    }

    /**
     * Executes a task that returns a result asynchronously using platform threads.
     *
     * @param <T>      The result type.
     * @param supplier The task to execute.
     * @return A CompletableFuture with the result.
     */
    public static <T> CompletableFuture<T> supplyAsync(@NotNull Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return supplier.get();
            } catch (Exception e) {
                LOG.error("Error executing supplier asynchronously", e);
                throw e;
            }
        }, PLATFORM_EXECUTOR);
    }

    /**
     * Executes a task that returns a result asynchronously using Java 21 virtual threads.
     *
     * @param <T>      The result type.
     * @param supplier The task to execute.
     * @return A CompletableFuture with the result.
     */
    public static <T> CompletableFuture<T> supplyAsyncVirtual(@NotNull Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return supplier.get();
            } catch (Exception e) {
                LOG.error("Error executing supplier asynchronously on virtual thread", e);
                throw e;
            }
        }, VIRTUAL_EXECUTOR);
    }

    /**
     * Schedules a task to run after a delay using platform threads.
     *
     * @param task  The task to execute.
     * @param delay The delay before executing the task.
     * @return A ScheduledFuture representing the scheduled task.
     */
    public static ScheduledFuture<?> schedule(@NotNull Runnable task, @NotNull Duration delay) {
        return SCHEDULED_EXECUTOR.schedule(task, delay.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * Schedules a task to run periodically using platform threads.
     *
     * @param task     The task to execute.
     * @param initialDelay The initial delay before executing the task.
     * @param period   The period between successive executions.
     * @return A ScheduledFuture representing the scheduled task.
     */
    public static ScheduledFuture<?> scheduleAtFixedRate(@NotNull Runnable task, @NotNull Duration initialDelay, @NotNull Duration period) {
        return SCHEDULED_EXECUTOR.scheduleAtFixedRate(
                task,
                initialDelay.toMillis(),
                period.toMillis(),
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * Schedules a task to run periodically with a fixed delay between executions using platform threads.
     *
     * @param task     The task to execute.
     * @param initialDelay The initial delay before executing the task.
     * @param delay    The delay between the termination of one execution and the commencement of the next.
     * @return A ScheduledFuture representing the scheduled task.
     */
    public static ScheduledFuture<?> scheduleWithFixedDelay(@NotNull Runnable task, @NotNull Duration initialDelay, @NotNull Duration delay) {
        return SCHEDULED_EXECUTOR.scheduleWithFixedDelay(
                task,
                initialDelay.toMillis(),
                delay.toMillis(),
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * Creates a new thread builder for a virtual thread.
     *
     * @return A thread builder for a virtual thread.
     */
    public static Thread.Builder.OfVirtual virtualThreadBuilder() {
        return Thread.ofVirtual();
    }

    /**
     * Creates a new thread builder for a platform thread.
     *
     * @return A thread builder for a platform thread.
     */
    public static Thread.Builder.OfPlatform platformThreadBuilder() {
        return Thread.ofPlatform();
    }

    /**
     * Checks if the current thread is a virtual thread.
     *
     * @return Whether the current thread is a virtual thread.
     */
    public static boolean isVirtualThread() {
        return Thread.currentThread().isVirtual();
    }

    /**
     * Executes a task with a timeout using platform threads.
     *
     * @param <T>      The result type.
     * @param supplier The task to execute.
     * @param timeout  The timeout.
     * @return The result of the task.
     * @throws TimeoutException If the task times out.
     * @throws ExecutionException If the task throws an exception.
     */
    public static <T> T executeWithTimeout(@NotNull Supplier<T> supplier, @NotNull Duration timeout) throws TimeoutException, ExecutionException {
        try {
            return CompletableFuture.supplyAsync(supplier, PLATFORM_EXECUTOR)
                    .orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                    .get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExecutionException("Task was interrupted", e);
        }
    }

    /**
     * Executes a task with a timeout using Java 21 virtual threads.
     *
     * @param <T>      The result type.
     * @param supplier The task to execute.
     * @param timeout  The timeout.
     * @return The result of the task.
     * @throws TimeoutException If the task times out.
     * @throws ExecutionException If the task throws an exception.
     */
    public static <T> T executeWithTimeoutVirtual(@NotNull Supplier<T> supplier, @NotNull Duration timeout) throws TimeoutException, ExecutionException {
        try {
            return CompletableFuture.supplyAsync(supplier, VIRTUAL_EXECUTOR)
                    .orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                    .get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExecutionException("Task was interrupted", e);
        }
    }

    /**
     * Executes a task asynchronously and returns a result with a default value if the task fails.
     *
     * @param <T>          The result type.
     * @param supplier     The task to execute.
     * @param defaultValue The default value to return if the task fails.
     * @return The result of the task or the default value if the task fails.
     */
    public static <T> T executeWithDefault(@NotNull Supplier<T> supplier, T defaultValue) {
        try {
            return supplier.get();
        } catch (Exception e) {
            LOG.error("Error executing task, returning default value", e);
            return defaultValue;
        }
    }

    /**
     * Shuts down all executors.
     */
    public static void shutdownAll() {
        PLATFORM_EXECUTOR.shutdown();
        VIRTUAL_EXECUTOR.shutdown();
        SCHEDULED_EXECUTOR.shutdown();
    }
}