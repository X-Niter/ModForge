package com.modforge.intellij.plugin.memory.monitoring;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.modforge.intellij.plugin.memory.MemoryManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Manager for taking and storing memory snapshots over time.
 * This is implemented as a singleton service.
 */
public class MemorySnapshotManager {
    private static final Logger LOG = Logger.getInstance(MemorySnapshotManager.class);
    
    // Singleton instance
    private static MemorySnapshotManager instance;
    
    // Maximum number of snapshots to keep in memory
    private static final int MAX_SNAPSHOTS = 1000;
    
    // Snapshot collection interval in seconds
    private static final int DEFAULT_SNAPSHOT_INTERVAL_SECONDS = 10;
    
    // Collection of memory snapshots
    private final List<MemorySnapshot> snapshots = new CopyOnWriteArrayList<>();
    
    // Scheduled executor for collecting snapshots
    private final ScheduledExecutorService executor;
    private ScheduledFuture<?> scheduledTask;
    
    // Current snapshot interval in seconds
    private int snapshotIntervalSeconds = DEFAULT_SNAPSHOT_INTERVAL_SECONDS;
    
    /**
     * Private constructor for singleton pattern
     */
    private MemorySnapshotManager() {
        executor = AppExecutorUtil.createBoundedScheduledExecutorService(
                "MemorySnapshotManager", 1);
        
        // Start collecting snapshots
        startCollecting();
        
        // Register for disposal when application is shutting down
        Disposer.register(ApplicationManager.getApplication(), () -> {
            stopCollecting();
            executor.shutdown();
        });
    }
    
    /**
     * Get the singleton instance
     *
     * @return The singleton instance
     */
    public static synchronized MemorySnapshotManager getInstance() {
        if (instance == null) {
            instance = new MemorySnapshotManager();
        }
        return instance;
    }
    
    /**
     * Start collecting memory snapshots at regular intervals
     */
    public void startCollecting() {
        if (scheduledTask != null && !scheduledTask.isDone()) {
            // Already collecting
            return;
        }
        
        // Schedule snapshot collection
        scheduledTask = executor.scheduleAtFixedRate(
                this::collectSnapshot,
                0,
                snapshotIntervalSeconds,
                TimeUnit.SECONDS
        );
        
        LOG.info("Started collecting memory snapshots every " + snapshotIntervalSeconds + " seconds");
    }
    
    /**
     * Stop collecting memory snapshots
     */
    public void stopCollecting() {
        if (scheduledTask != null && !scheduledTask.isDone()) {
            scheduledTask.cancel(false);
            LOG.info("Stopped collecting memory snapshots");
        }
    }
    
    /**
     * Set the interval for collecting snapshots
     *
     * @param seconds The interval in seconds
     */
    public void setSnapshotInterval(int seconds) {
        if (seconds < 1) {
            throw new IllegalArgumentException("Interval must be at least 1 second");
        }
        
        // Update interval and restart collection
        this.snapshotIntervalSeconds = seconds;
        
        if (scheduledTask != null && !scheduledTask.isDone()) {
            stopCollecting();
            startCollecting();
        }
    }
    
    /**
     * Collect a memory snapshot and add it to the list
     */
    private void collectSnapshot() {
        try {
            // Get memory pressure level from memory manager
            MemoryManager memoryManager = MemoryManager.getInstance();
            String pressureLevel = memoryManager != null
                    ? memoryManager.getPressureLevel().toString()
                    : "UNKNOWN";
            
            // Create snapshot
            MemorySnapshot snapshot = MemorySnapshot.createCurrentSnapshot(pressureLevel);
            
            // Add to list and trim if necessary
            synchronized (snapshots) {
                snapshots.add(snapshot);
                
                // Trim list if it exceeds maximum size
                if (snapshots.size() > MAX_SNAPSHOTS) {
                    snapshots.remove(0);
                }
            }
        } catch (Exception e) {
            LOG.error("Error collecting memory snapshot", e);
        }
    }
    
    /**
     * Get a copy of the current snapshots list
     *
     * @return A copy of the snapshots list
     */
    @NotNull
    public List<MemorySnapshot> getSnapshots() {
        synchronized (snapshots) {
            return new ArrayList<>(snapshots);
        }
    }
    
    /**
     * Clear all snapshots
     */
    public void clearSnapshots() {
        synchronized (snapshots) {
            snapshots.clear();
        }
    }
    
    /**
     * Get the latest memory snapshot
     *
     * @return The latest memory snapshot, or null if no snapshots are available
     */
    @Nullable
    public MemorySnapshot getLatestSnapshot() {
        synchronized (snapshots) {
            if (snapshots.isEmpty()) {
                return null;
            }
            return snapshots.get(snapshots.size() - 1);
        }
    }
    
    /**
     * Export the memory snapshots to a CSV file
     *
     * @param file The file to export to
     * @return True if the export was successful, false otherwise
     */
    public boolean exportSnapshotsToCSV(@NotNull File file) {
        synchronized (snapshots) {
            if (snapshots.isEmpty()) {
                LOG.warn("No snapshots to export");
                return false;
            }
            
            try (FileOutputStream fos = new FileOutputStream(file);
                 OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
                 BufferedWriter writer = new BufferedWriter(osw)) {
                
                // Write header
                writer.write("Timestamp,UsedMemory,FreeMemory,TotalMemory,MaxMemory,PressureLevel,UsagePercentage,AllocationPercentage");
                writer.newLine();
                
                // Write each snapshot
                for (MemorySnapshot snapshot : snapshots) {
                    writer.write(snapshot.toCsvLine());
                    writer.newLine();
                }
                
                LOG.info("Exported " + snapshots.size() + " memory snapshots to: " + file.getAbsolutePath());
                return true;
            } catch (IOException e) {
                LOG.error("Error exporting memory snapshots to CSV", e);
                return false;
            }
        }
    }
    
    /**
     * Import memory snapshots from a CSV file
     *
     * @param file The file to import from
     * @return True if the import was successful, false otherwise
     */
    public boolean importSnapshotsFromCSV(@NotNull File file) {
        if (!file.exists() || !file.isFile()) {
            LOG.warn("Import file does not exist: " + file.getAbsolutePath());
            return false;
        }
        
        List<MemorySnapshot> importedSnapshots = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        
        try (FileInputStream fis = new FileInputStream(file);
             InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(isr)) {
            
            // Skip header
            String line = reader.readLine();
            
            // Read each line
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                
                if (parts.length >= 6) {
                    try {
                        LocalDateTime timestamp = LocalDateTime.parse(parts[0], formatter);
                        long usedMemory = Long.parseLong(parts[1]);
                        long freeMemory = Long.parseLong(parts[2]);
                        long totalMemory = Long.parseLong(parts[3]);
                        long maxMemory = Long.parseLong(parts[4]);
                        String pressureLevel = parts[5];
                        
                        MemorySnapshot snapshot = new MemorySnapshot(
                                timestamp, usedMemory, freeMemory, totalMemory, maxMemory, pressureLevel);
                        
                        importedSnapshots.add(snapshot);
                    } catch (Exception e) {
                        LOG.warn("Error parsing CSV line: " + line, e);
                    }
                }
            }
            
            if (!importedSnapshots.isEmpty()) {
                // Sort by timestamp
                Collections.sort(importedSnapshots, 
                        (s1, s2) -> s1.getTimestamp().compareTo(s2.getTimestamp()));
                
                // Replace current snapshots with imported ones
                synchronized (snapshots) {
                    snapshots.clear();
                    snapshots.addAll(importedSnapshots);
                    
                    // Trim if necessary
                    if (snapshots.size() > MAX_SNAPSHOTS) {
                        int toRemove = snapshots.size() - MAX_SNAPSHOTS;
                        snapshots.subList(0, toRemove).clear();
                    }
                }
                
                LOG.info("Imported " + importedSnapshots.size() + " memory snapshots from: " + file.getAbsolutePath());
                return true;
            } else {
                LOG.warn("No valid snapshots found in import file: " + file.getAbsolutePath());
                return false;
            }
        } catch (IOException e) {
            LOG.error("Error importing memory snapshots from CSV", e);
            return false;
        }
    }
    
    /**
     * Save the memory snapshots to a persistence file
     *
     * @param file The file to save to
     * @return True if the save was successful, false otherwise
     */
    public boolean saveSnapshots(@NotNull File file) {
        return exportSnapshotsToCSV(file);
    }
    
    /**
     * Load memory snapshots from a persistence file
     *
     * @param file The file to load from
     * @return True if the load was successful, false otherwise
     */
    public boolean loadSnapshots(@NotNull File file) {
        return importSnapshotsFromCSV(file);
    }
    
    /**
     * Get the maximum number of snapshots to keep in memory
     *
     * @return The maximum number of snapshots
     */
    public int getMaxSnapshots() {
        return MAX_SNAPSHOTS;
    }
    
    /**
     * Get the current snapshot interval in seconds
     *
     * @return The snapshot interval in seconds
     */
    public int getSnapshotIntervalSeconds() {
        return snapshotIntervalSeconds;
    }
    
    /**
     * Force a snapshot to be taken immediately
     *
     * @return The collected snapshot
     */
    @NotNull
    public MemorySnapshot takeSnapshotNow() {
        collectSnapshot();
        return getLatestSnapshot();
    }
    
    /**
     * Save memory snapshots to the default persistence file
     *
     * @return True if the save was successful, false otherwise
     */
    public boolean saveToPersistence() {
        try {
            // Get system temp directory
            String tmpDir = System.getProperty("java.io.tmpdir");
            File persistenceDir = new File(tmpDir, "modforge/memory");
            FileUtil.createDirectory(persistenceDir);
            
            File persistenceFile = new File(persistenceDir, "memory_snapshots.csv");
            return saveSnapshots(persistenceFile);
        } catch (Exception e) {
            LOG.error("Error saving memory snapshots to persistence", e);
            return false;
        }
    }
    
    /**
     * Load memory snapshots from the default persistence file
     *
     * @return True if the load was successful, false otherwise
     */
    public boolean loadFromPersistence() {
        try {
            // Get system temp directory
            String tmpDir = System.getProperty("java.io.tmpdir");
            File persistenceDir = new File(tmpDir, "modforge/memory");
            File persistenceFile = new File(persistenceDir, "memory_snapshots.csv");
            
            if (persistenceFile.exists() && persistenceFile.isFile()) {
                return loadSnapshots(persistenceFile);
            }
            
            return false;
        } catch (Exception e) {
            LOG.error("Error loading memory snapshots from persistence", e);
            return false;
        }
    }
    
    /**
     * Reset the snapshot manager, clearing all snapshots and resetting the interval
     */
    public void reset() {
        stopCollecting();
        clearSnapshots();
        snapshotIntervalSeconds = DEFAULT_SNAPSHOT_INTERVAL_SECONDS;
        startCollecting();
    }
}