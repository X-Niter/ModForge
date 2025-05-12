package com.modforge.intellij.plugin.utils;

import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * Utility class for working with threads and asynchronous operations.
 * Compatible with IntelliJ IDEA 2025.1.1.1
 * Uses Java 21 virtual threads when available.
 */
public final class ThreadUtils {
    private static final Logger LOG = Logger.getInstance(ThreadUtils.class);
    
    private static final ExecutorService VIRTUAL_THREAD_EXECUTOR;
    private static final ExecutorService PLATFORM_THREAD_EXECUTOR;
    
    static {
        // Initialize executors with Java 21 Virtual Threads when available
        VIRTUAL_THREAD_EXECUTOR = isVirtualThreadSupported() 
                ? Executors.newVirtualThreadPerTaskExecutor()
                : ThreadUtils.newCachedThreadPoolExecutor("ModForge-Virtual");
        
        PLATFORM_THREAD_EXECUTOR = ThreadUtils.newCachedThreadPoolExecutor("ModForge-Platform");
    }
    
    /**
     * Private constructor to prevent instantiation.
     */
    private ThreadUtils() {
        // Utility class
    }

    /**
     * Checks if virtual threads are supported on this Java version.
     * Virtual threads were introduced in Java 21.
     *
     * @return Whether virtual threads are supported.
     */
    public static boolean isVirtualThreadSupported() {
        try {
            // Try to access the newVirtualThreadPerTaskExecutor method
            // which was introduced in Java 21
            Executors.class.getMethod("newVirtualThreadPerTaskExecutor");
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    /**
     * Creates a cached thread pool executor.
     *
     * @param threadNamePrefix The thread name prefix.
     * @return The executor.
     */
    @NotNull
    public static ExecutorService newCachedThreadPoolExecutor(@NotNull String threadNamePrefix) {
        return new ThreadPoolExecutor(
                0, Integer.MAX_VALUE,
                60L, TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                r -> {
                    Thread thread = Executors.defaultThreadFactory().newThread(r);
                    thread.setName(threadNamePrefix + "-" + thread.getId());
                    thread.setDaemon(true);
                    return thread;
                }
        );
    }

    /**
     * Executes a task asynchronously using virtual threads.
     *
     * @param task The task to execute.
     * @return A future representing the completion of the task.
     */
    @NotNull
    public static CompletableFuture<Void> runAsyncVirtual(@NotNull Runnable task) {
        return CompletableFuture.runAsync(task, VIRTUAL_THREAD_EXECUTOR);
    }

    /**
     * Executes a task asynchronously using platform threads.
     *
     * @param task The task to execute.
     * @return A future representing the completion of the task.
     */
    @NotNull
    public static CompletableFuture<Void> runAsyncPlatform(@NotNull Runnable task) {
        return CompletableFuture.runAsync(task, PLATFORM_THREAD_EXECUTOR);
    }

    /**
     * Executes a task asynchronously using virtual threads.
     *
     * @param task The task to execute.
     * @param <T>  The return type.
     * @return A future representing the completion of the task.
     */
    @NotNull
    public static <T> CompletableFuture<T> supplyAsyncVirtual(@NotNull Supplier<T> task) {
        return CompletableFuture.supplyAsync(task, VIRTUAL_THREAD_EXECUTOR);
    }

    /**
     * Executes a task asynchronously using platform threads.
     *
     * @param task The task to execute.
     * @param <T>  The return type.
     * @return A future representing the completion of the task.
     */
    @NotNull
    public static <T> CompletableFuture<T> supplyAsyncPlatform(@NotNull Supplier<T> task) {
        return CompletableFuture.supplyAsync(task, PLATFORM_THREAD_EXECUTOR);
    }

    /**
     * Waits for all futures to complete, logging any exceptions.
     *
     * @param futures The futures to wait for.
     */
    @SafeVarargs
    public static void joinAll(@NotNull CompletableFuture<?>... futures) {
        try {
            CompletableFuture.allOf(futures).join();
        } catch (CompletionException e) {
            LOG.error("Exception while joining futures", e.getCause());
        }
    }

    /**
     * Sleeps for the given amount of time.
     *
     * @param millis The time to sleep in milliseconds.
     */
    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Runs a task with a timeout.
     *
     * @param task    The task to run.
     * @param timeout The timeout.
     * @param unit    The timeout unit.
     * @param <T>     The return type.
     * @return The result of the task, or null if timed out.
     */
    public static <T> T runWithTimeout(@NotNull Callable<T> task, long timeout, @NotNull TimeUnit unit) {
        try {
            Future<T> future = VIRTUAL_THREAD_EXECUTOR.submit(task);
            return future.get(timeout, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (ExecutionException | TimeoutException e) {
            LOG.warn("Task timed out or failed", e);
            return null;
        }
    }

    /**
     * Shuts down the executors.
     */
    public static void shutdownExecutors() {
        VIRTUAL_THREAD_EXECUTOR.shutdown();
        PLATFORM_THREAD_EXECUTOR.shutdown();
    }
}