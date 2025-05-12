package com.modforge.intellij.plugin.utils;

import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * Utility class for thread operations.
 * Provides support for Java 21 virtual threads.
 * Compatible with IntelliJ IDEA 2025.1.1.1
 */
public final class ThreadUtils {
    private static final Logger LOG = Logger.getInstance(ThreadUtils.class);
    
    // Thread pool for virtual threads
    private static final ExecutorService VIRTUAL_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();
    
    // Thread pool for regular threads
    private static final ExecutorService REGULAR_EXECUTOR = Executors.newCachedThreadPool();
    
    /**
     * Private constructor to prevent instantiation.
     */
    private ThreadUtils() {
        // Utility class
    }

    /**
     * Checks if virtual threads are supported.
     *
     * @return Whether virtual threads are supported.
     */
    public static boolean areVirtualThreadsSupported() {
        try {
            // Simple check for virtual thread API
            Class.forName("java.lang.Thread").getMethod("startVirtualThread", Runnable.class);
            return true;
        } catch (NoSuchMethodException | ClassNotFoundException e) {
            LOG.info("Virtual threads not supported: " + e.getMessage());
            return false;
        }
    }

    /**
     * Executes a runnable asynchronously using virtual threads if available.
     *
     * @param runnable The runnable to execute.
     * @return A CompletableFuture that completes when the runnable completes.
     */
    public static CompletableFuture<Void> runAsyncVirtual(@NotNull Runnable runnable) {
        if (areVirtualThreadsSupported()) {
            return CompletableFuture.runAsync(runnable, VIRTUAL_EXECUTOR);
        } else {
            return CompletableFuture.runAsync(runnable, REGULAR_EXECUTOR);
        }
    }

    /**
     * Supplies a value asynchronously using virtual threads if available.
     *
     * @param supplier The supplier to execute.
     * @param <T>      The return type.
     * @return A CompletableFuture that completes with the supplier's result.
     */
    public static <T> CompletableFuture<T> supplyAsyncVirtual(@NotNull Supplier<T> supplier) {
        if (areVirtualThreadsSupported()) {
            return CompletableFuture.supplyAsync(supplier, VIRTUAL_EXECUTOR);
        } else {
            return CompletableFuture.supplyAsync(supplier, REGULAR_EXECUTOR);
        }
    }

    /**
     * Executes a callable asynchronously using virtual threads if available.
     *
     * @param callable The callable to execute.
     * @param <T>      The return type.
     * @return A Future that completes with the callable's result.
     */
    public static <T> Future<T> submitVirtual(@NotNull Callable<T> callable) {
        if (areVirtualThreadsSupported()) {
            return VIRTUAL_EXECUTOR.submit(callable);
        } else {
            return REGULAR_EXECUTOR.submit(callable);
        }
    }

    /**
     * Executes a runnable asynchronously using virtual threads if available.
     *
     * @param runnable The runnable to execute.
     * @return A Future that completes when the runnable completes.
     */
    public static Future<?> submitVirtual(@NotNull Runnable runnable) {
        if (areVirtualThreadsSupported()) {
            return VIRTUAL_EXECUTOR.submit(runnable);
        } else {
            return REGULAR_EXECUTOR.submit(runnable);
        }
    }

    /**
     * Waits for a CompletableFuture to complete with a timeout.
     *
     * @param future   The future to wait for.
     * @param timeout  The timeout duration.
     * @param timeUnit The timeout unit.
     * @param <T>      The return type.
     * @return The result of the future.
     * @throws TimeoutException      If the future times out.
     * @throws InterruptedException  If the thread is interrupted.
     * @throws ExecutionException    If the future throws an exception.
     */
    public static <T> T waitFor(
            @NotNull CompletableFuture<T> future,
            long timeout,
            @NotNull TimeUnit timeUnit) 
            throws TimeoutException, InterruptedException, ExecutionException {
        return future.get(timeout, timeUnit);
    }

    /**
     * Waits for a CompletableFuture to complete with the default timeout (30 seconds).
     *
     * @param future The future to wait for.
     * @param <T>    The return type.
     * @return The result of the future.
     * @throws TimeoutException      If the future times out.
     * @throws InterruptedException  If the thread is interrupted.
     * @throws ExecutionException    If the future throws an exception.
     */
    public static <T> T waitFor(@NotNull CompletableFuture<T> future) 
            throws TimeoutException, InterruptedException, ExecutionException {
        return waitFor(future, 30, TimeUnit.SECONDS);
    }

    /**
     * Shuts down the thread pools.
     * Call this when the plugin is unloaded.
     */
    public static void shutdown() {
        VIRTUAL_EXECUTOR.shutdown();
        REGULAR_EXECUTOR.shutdown();
    }
}