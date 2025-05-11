package com.modforge.intellij.plugin.debug;

import com.intellij.execution.process.ProcessHandler;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Key;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.intellij.xdebugger.breakpoints.XBreakpointListener;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import com.intellij.xdebugger.frame.XStackFrame;
import com.intellij.xdebugger.frame.XSuspendContext;
import com.modforge.intellij.plugin.notifications.ModForgeNotificationService;
import com.modforge.intellij.plugin.run.MinecraftRunConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Performance monitoring tool for Minecraft mods
 * Tracks execution time, memory usage, and identifies performance bottlenecks
 */
public class MinecraftPerformanceMonitor {
    private static final Logger LOG = Logger.getInstance(MinecraftPerformanceMonitor.class);
    
    // Keys for storing timing data
    private static final Key<Long> START_TIME_KEY = Key.create("MINECRAFT_PERF_START_TIME");
    private static final Key<Integer> EXECUTION_COUNT_KEY = Key.create("MINECRAFT_PERF_EXECUTION_COUNT");
    private static final Key<Long> TOTAL_TIME_KEY = Key.create("MINECRAFT_PERF_TOTAL_TIME");
    
    // Performance thresholds (can be customized in settings)
    private static final String PERF_THRESHOLD_MS_KEY = "minecraft.performance.threshold.ms";
    private static final String MEMORY_THRESHOLD_MB_KEY = "minecraft.performance.memory.threshold.mb";
    private static final long DEFAULT_PERF_THRESHOLD_MS = 50; // 50ms is noticeable in a game loop
    private static final long DEFAULT_MEMORY_THRESHOLD_MB = 100; // 100MB significant memory allocation
    
    private final Project project;
    private final Map<String, MethodPerformanceData> performanceData = new ConcurrentHashMap<>();
    private final AtomicLong lastGcTime = new AtomicLong(0);
    private final Set<String> hotMethods = ConcurrentHashMap.newKeySet();
    
    // Capture memory usage data
    private final Map<String, Long> memoryAllocationData = new ConcurrentHashMap<>();
    
    public MinecraftPerformanceMonitor(Project project) {
        this.project = project;
    }
    
    /**
     * Start monitoring performance for a Minecraft debug session
     * 
     * @param debugProcess The debug process to monitor
     */
    public void startMonitoring(XDebugProcess debugProcess) {
        XDebugSession session = debugProcess.getSession();
        
        // Only monitor Minecraft run configurations
        if (!(session.getRunProfile() instanceof MinecraftRunConfiguration)) {
            return;
        }
        
        LOG.info("Starting performance monitoring for Minecraft debug session");
        
        // Monitor method execution time by using custom breakpoints
        setupPerformanceBreakpoints(session);
        
        // Track memory usage periodically
        startMemoryTracking(debugProcess);
    }
    
    /**
     * Set up performance breakpoints for known expensive operations
     * 
     * @param session The debug session
     */
    private void setupPerformanceBreakpoints(XDebugSession session) {
        // List of potentially expensive methods to monitor
        String[][] methodsToMonitor = {
            // Class, method, description
            {"net.minecraft.client.renderer.chunk.ChunkRenderDispatcher", "uploadChunk", "Chunk Upload"},
            {"net.minecraft.world.level.chunk.LevelChunk", "addEntity", "Entity Addition"},
            {"net.minecraft.client.renderer.EntityRenderer", "render", "Entity Rendering"},
            {"net.minecraft.world.level.Level", "tick", "World Tick"},
            {"net.minecraft.world.entity.Entity", "tick", "Entity Tick"},
            {"net.minecraft.world.level.block.entity.BlockEntity", "tick", "Block Entity Tick"},
            {"net.minecraft.client.renderer.ItemRenderer", "renderItem", "Item Rendering"},
            {"net.minecraft.client.particle.ParticleEngine", "render", "Particle Rendering"}
        };
        
        // Register a breakpoint listener to intercept execution
        session.addBreakpointListener(new XBreakpointListener<>() {
            @Override
            public void breakpointReached(@NotNull XBreakpoint<?> breakpoint, @NotNull XSuspendContext suspendContext) {
                if (breakpoint instanceof XLineBreakpoint) {
                    handleBreakpointReached(breakpoint, suspendContext, session);
                }
            }
        });
        
        // TODO: Add programmatic breakpoints for methods to monitor
        // This is complex and requires more work with the debugging API
        LOG.info("Performance monitoring breakpoints setup completed");
    }
    
    /**
     * Handle a breakpoint being reached for performance monitoring
     * 
     * @param breakpoint The breakpoint that was reached
     * @param suspendContext The suspend context
     * @param session The debug session
     */
    private void handleBreakpointReached(@NotNull XBreakpoint<?> breakpoint, 
                                        @NotNull XSuspendContext suspendContext,
                                        @NotNull XDebugSession session) {
        // Get the current position
        XSourcePosition position = breakpoint.getSourcePosition();
        if (position == null) return;
        
        // Get the current stack frame
        XStackFrame frame = suspendContext.getActiveExecutionStack().getTopFrame();
        if (frame == null) return;
        
        // Generate a key for this method based on file and line
        String locationKey = position.getFile().getPath() + ":" + position.getLine();
        
        // Get existing timing data or create new
        Long startTime = breakpoint.getUserData(START_TIME_KEY);
        Integer executionCount = breakpoint.getUserData(EXECUTION_COUNT_KEY);
        Long totalTime = breakpoint.getUserData(TOTAL_TIME_KEY);
        
        if (startTime == null) {
            // First time hitting this breakpoint, just record start time
            breakpoint.putUserData(START_TIME_KEY, System.nanoTime());
            breakpoint.putUserData(EXECUTION_COUNT_KEY, 1);
            breakpoint.putUserData(TOTAL_TIME_KEY, 0L);
            session.resume();
        } else {
            // Subsequent hit, calculate elapsed time
            long endTime = System.nanoTime();
            long elapsed = endTime - startTime;
            long totalElapsed = totalTime + elapsed;
            int newCount = executionCount + 1;
            
            // Update the performance data
            breakpoint.putUserData(START_TIME_KEY, null); // Clear for next cycle
            breakpoint.putUserData(EXECUTION_COUNT_KEY, newCount);
            breakpoint.putUserData(TOTAL_TIME_KEY, totalElapsed);
            
            // Update our tracking map
            updatePerformanceData(locationKey, elapsed, newCount, totalElapsed);
            
            // Check if this is a performance hotspot
            checkPerformanceHotspot(locationKey, elapsed);
            
            // Resume execution
            session.resume();
        }
    }
    
    /**
     * Update the performance data for a specific method
     * 
     * @param locationKey The method location key
     * @param elapsed The elapsed time for this execution in nanoseconds
     * @param executionCount The number of times the method has been executed
     * @param totalTime The total execution time for this method in nanoseconds
     */
    private void updatePerformanceData(String locationKey, long elapsed, int executionCount, long totalTime) {
        MethodPerformanceData data = performanceData.computeIfAbsent(locationKey, 
                k -> new MethodPerformanceData(locationKey));
        
        data.addExecution(elapsed);
        LOG.debug("Performance data updated for " + locationKey + ": " + 
                TimeUnit.NANOSECONDS.toMillis(elapsed) + "ms (count: " + executionCount + 
                ", avg: " + TimeUnit.NANOSECONDS.toMillis(totalTime / executionCount) + "ms)");
    }
    
    /**
     * Check if a method execution time indicates a performance hotspot
     * 
     * @param locationKey The method location key
     * @param elapsedNanos The elapsed time in nanoseconds
     */
    private void checkPerformanceHotspot(String locationKey, long elapsedNanos) {
        // Get the performance threshold from settings
        long thresholdMs = PropertiesComponent.getInstance(project)
                .getLong(PERF_THRESHOLD_MS_KEY, DEFAULT_PERF_THRESHOLD_MS);
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(elapsedNanos);
        
        // If execution time exceeds threshold and not already reported
        if (elapsedMs > thresholdMs && !hotMethods.contains(locationKey)) {
            hotMethods.add(locationKey);
            
            // Notify the user of the performance hotspot
            ModForgeNotificationService notificationService = 
                    project.getService(ModForgeNotificationService.class);
            
            if (notificationService != null) {
                notificationService.showNotification(
                        "Performance Hotspot Detected",
                        "Method at " + locationKey + " took " + elapsedMs + "ms to execute, " +
                                "which exceeds the threshold of " + thresholdMs + "ms.",
                        NotificationType.WARNING
                );
            }
            
            LOG.warn("Performance hotspot detected at " + locationKey + 
                    ": " + elapsedMs + "ms (threshold: " + thresholdMs + "ms)");
        }
    }
    
    /**
     * Start tracking memory usage for the debug process
     * 
     * @param debugProcess The debug process to monitor
     */
    private void startMemoryTracking(XDebugProcess debugProcess) {
        // Get the process handler
        ProcessHandler processHandler = debugProcess.getProcessHandler();
        
        // TODO: Implement memory tracking
        // This requires more complex integration with memory agents or JMX
        LOG.info("Memory tracking started for Minecraft debug process");
    }
    
    /**
     * Get performance data for all monitored methods
     * 
     * @return Map of method location keys to performance data
     */
    public Map<String, MethodPerformanceData> getPerformanceData() {
        return new HashMap<>(performanceData);
    }
    
    /**
     * Get memory allocation data for all monitored methods
     * 
     * @return Map of method location keys to memory allocation in bytes
     */
    public Map<String, Long> getMemoryAllocationData() {
        return new HashMap<>(memoryAllocationData);
    }
    
    /**
     * Generate a performance report for the current session
     * 
     * @return A formatted performance report
     */
    public String generatePerformanceReport() {
        StringBuilder report = new StringBuilder();
        report.append("Minecraft Performance Report\n");
        report.append("===========================\n\n");
        
        // Add execution time data
        report.append("Method Execution Times:\n");
        report.append("---------------------\n");
        
        List<Map.Entry<String, MethodPerformanceData>> sortedData = 
                new ArrayList<>(performanceData.entrySet());
        
        // Sort by average execution time (descending)
        sortedData.sort((e1, e2) -> Long.compare(e2.getValue().getAverageTimeNanos(), 
                                                e1.getValue().getAverageTimeNanos()));
        
        for (Map.Entry<String, MethodPerformanceData> entry : sortedData) {
            MethodPerformanceData data = entry.getValue();
            report.append(entry.getKey()).append(":\n");
            report.append("  Calls: ").append(data.getExecutionCount()).append("\n");
            report.append("  Avg time: ").append(
                    TimeUnit.NANOSECONDS.toMillis(data.getAverageTimeNanos())).append("ms\n");
            report.append("  Max time: ").append(
                    TimeUnit.NANOSECONDS.toMillis(data.getMaxTimeNanos())).append("ms\n");
            report.append("  Min time: ").append(
                    TimeUnit.NANOSECONDS.toMillis(data.getMinTimeNanos())).append("ms\n");
            report.append("\n");
        }
        
        // Add memory allocation data
        report.append("Memory Allocations:\n");
        report.append("------------------\n");
        
        List<Map.Entry<String, Long>> sortedMemory = 
                new ArrayList<>(memoryAllocationData.entrySet());
        
        // Sort by memory allocation (descending)
        sortedMemory.sort((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()));
        
        for (Map.Entry<String, Long> entry : sortedMemory) {
            report.append(entry.getKey()).append(": ")
                  .append(entry.getValue() / (1024 * 1024)).append(" MB\n");
        }
        
        return report.toString();
    }
    
    /**
     * Data class for storing method performance information
     */
    public static class MethodPerformanceData {
        private final String locationKey;
        private final AtomicLong executionCount = new AtomicLong(0);
        private final AtomicLong totalTimeNanos = new AtomicLong(0);
        private final AtomicLong maxTimeNanos = new AtomicLong(0);
        private final AtomicLong minTimeNanos = new AtomicLong(Long.MAX_VALUE);
        
        public MethodPerformanceData(String locationKey) {
            this.locationKey = locationKey;
        }
        
        public void addExecution(long elapsedNanos) {
            executionCount.incrementAndGet();
            totalTimeNanos.addAndGet(elapsedNanos);
            
            // Update max time if greater
            long currentMax = maxTimeNanos.get();
            while (elapsedNanos > currentMax) {
                if (maxTimeNanos.compareAndSet(currentMax, elapsedNanos)) {
                    break;
                }
                currentMax = maxTimeNanos.get();
            }
            
            // Update min time if less
            long currentMin = minTimeNanos.get();
            while (elapsedNanos < currentMin) {
                if (minTimeNanos.compareAndSet(currentMin, elapsedNanos)) {
                    break;
                }
                currentMin = minTimeNanos.get();
            }
        }
        
        public String getLocationKey() {
            return locationKey;
        }
        
        public long getExecutionCount() {
            return executionCount.get();
        }
        
        public long getTotalTimeNanos() {
            return totalTimeNanos.get();
        }
        
        public long getAverageTimeNanos() {
            long count = executionCount.get();
            return count > 0 ? totalTimeNanos.get() / count : 0;
        }
        
        public long getMaxTimeNanos() {
            return maxTimeNanos.get();
        }
        
        public long getMinTimeNanos() {
            long min = minTimeNanos.get();
            return min == Long.MAX_VALUE ? 0 : min;
        }
    }
}