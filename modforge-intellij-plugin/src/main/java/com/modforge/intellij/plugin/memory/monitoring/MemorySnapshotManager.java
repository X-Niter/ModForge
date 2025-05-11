package com.modforge.intellij.plugin.memory.monitoring;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.modforge.intellij.plugin.memory.MemoryManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Memory snapshot manager for tracking memory usage over time
 * Provides thread-safe access to memory snapshots with persistence capabilities
 */
@Service(Service.Level.APP)
public final class MemorySnapshotManager {
    private static final Logger LOG = Logger.getInstance(MemorySnapshotManager.class);
    private static final int MAX_SNAPSHOTS = 10000; // Maximum number of snapshots to keep in memory
    private static final int TRIM_THRESHOLD = 9000; // Threshold for trimming snapshots
    private static final int TRIM_TARGET = 8000; // Target number of snapshots after trimming
    private static final int AUTO_SAVE_THRESHOLD = 100; // Auto-save after this many new snapshots
    
    private final List<MemorySnapshot> snapshots = new CopyOnWriteArrayList<>();
    private final ReadWriteLock snapshotsLock = new ReentrantReadWriteLock();
    private final AtomicBoolean loading = new AtomicBoolean(false);
    private final AtomicBoolean saving = new AtomicBoolean(false);
    private int newSnapshotsCount = 0; // Count since last save
    
    /**
     * Get the singleton instance
     * 
     * @return The memory snapshot manager instance
     */
    public static MemorySnapshotManager getInstance() {
        return ApplicationManager.getApplication().getService(MemorySnapshotManager.class);
    }
    
    /**
     * Add a new memory snapshot
     * 
     * @param snapshot The snapshot to add
     */
    public void addSnapshot(@NotNull MemorySnapshot snapshot) {
        try {
            snapshotsLock.writeLock().lock();
            
            // Add the snapshot
            snapshots.add(snapshot);
            newSnapshotsCount++;
            
            // Trim if exceeding threshold
            if (snapshots.size() > MAX_SNAPSHOTS) {
                trimSnapshots();
            }
            
            // Auto-save periodically
            if (newSnapshotsCount >= AUTO_SAVE_THRESHOLD) {
                // Schedule async save to avoid blocking
                ApplicationManager.getApplication().executeOnPooledThread(() -> saveSnapshotsAsync());
                newSnapshotsCount = 0;
            }
        } catch (Exception ex) {
            LOG.error("Error adding memory snapshot", ex);
        } finally {
            snapshotsLock.writeLock().unlock();
        }
    }
    
    /**
     * Get all memory snapshots
     * 
     * @return A list of all memory snapshots
     */
    @NotNull
    public List<MemorySnapshot> getSnapshots() {
        try {
            snapshotsLock.readLock().lock();
            return new ArrayList<>(snapshots);
        } finally {
            snapshotsLock.readLock().unlock();
        }
    }
    
    /**
     * Get memory snapshots within a time range
     * 
     * @param startTime The start time (inclusive)
     * @param endTime The end time (inclusive)
     * @return A list of memory snapshots within the time range
     */
    @NotNull
    public List<MemorySnapshot> getSnapshotsInRange(@NotNull LocalDateTime startTime, @NotNull LocalDateTime endTime) {
        try {
            snapshotsLock.readLock().lock();
            
            return snapshots.stream()
                    .filter(snapshot -> snapshot != null && snapshot.getTimestamp() != null)
                    .filter(snapshot -> 
                        !snapshot.getTimestamp().isBefore(startTime) && 
                        !snapshot.getTimestamp().isAfter(endTime))
                    .toList();
        } catch (Exception ex) {
            LOG.error("Error getting snapshots in range", ex);
            return Collections.emptyList();
        } finally {
            snapshotsLock.readLock().unlock();
        }
    }
    
    /**
     * Get the most recent memory snapshot
     * 
     * @return The most recent memory snapshot, or null if none exists
     */
    @Nullable
    public MemorySnapshot getMostRecentSnapshot() {
        try {
            snapshotsLock.readLock().lock();
            
            if (snapshots.isEmpty()) {
                return null;
            }
            
            return snapshots.get(snapshots.size() - 1);
        } catch (Exception ex) {
            LOG.error("Error getting most recent snapshot", ex);
            return null;
        } finally {
            snapshotsLock.readLock().unlock();
        }
    }
    
    /**
     * Get the latest memory snapshot, creating a new one if necessary
     * 
     * @return The latest memory snapshot
     */
    @NotNull
    public MemorySnapshot getLatestSnapshot() {
        MemorySnapshot recent = getMostRecentSnapshot();
        
        // If no snapshot exists or it's too old, create a new one
        if (recent == null || 
                recent.getTimestamp().plusSeconds(5).isBefore(LocalDateTime.now())) {
            
            MemoryManager memoryManager = MemoryManager.getInstance();
            if (memoryManager != null) {
                MemorySnapshot newSnapshot = memoryManager.getCurrentMemorySnapshot();
                
                // Store the new snapshot
                if (newSnapshot != null) {
                    addSnapshot(newSnapshot);
                    return newSnapshot;
                }
            }
            
            // If we can't create a new snapshot, return the most recent or a default one
            if (recent != null) {
                return recent;
            } else {
                // Create a default snapshot as last resort
                return new MemorySnapshot(
                        LocalDateTime.now(),
                        Runtime.getRuntime().totalMemory(),
                        Runtime.getRuntime().freeMemory()
                );
            }
        }
        
        return recent;
    }
    
    /**
     * Clear all memory snapshots
     */
    public void clearSnapshots() {
        try {
            snapshotsLock.writeLock().lock();
            snapshots.clear();
            newSnapshotsCount = 0;
            LOG.info("Memory snapshots cleared");
        } catch (Exception ex) {
            LOG.error("Error clearing memory snapshots", ex);
        } finally {
            snapshotsLock.writeLock().unlock();
        }
    }
    
    /**
     * Initialize and load previously saved snapshots, if available
     */
    public void initialize() {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            loadSnapshotsAsync();
        });
    }
    
    /**
     * Save snapshots to disk asynchronously
     * 
     * @return True if the save operation was successful, false otherwise
     */
    public boolean saveSnapshotsAsync() {
        // Avoid concurrent saves
        if (!saving.compareAndSet(false, true)) {
            LOG.debug("Save already in progress, skipping");
            return false;
        }
        
        try {
            snapshotsLock.readLock().lock();
            
            if (snapshots.isEmpty()) {
                LOG.debug("No snapshots to save");
                return true;
            }
            
            // Copy for safe serialization outside lock
            List<MemorySnapshot> snapshotsCopy = new ArrayList<>(snapshots);
            
            // Release lock before disk operations
            snapshotsLock.readLock().unlock();
            
            File file = getSnapshotFile();
            file.getParentFile().mkdirs();
            
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
                oos.writeObject(snapshotsCopy);
                LOG.info("Saved " + snapshotsCopy.size() + " memory snapshots to " + file.getAbsolutePath());
                return true;
            } catch (Exception ex) {
                LOG.error("Error saving memory snapshots", ex);
                return false;
            }
        } catch (Exception ex) {
            LOG.error("Error during memory snapshot save operation", ex);
            return false;
        } finally {
            // Release lock if still held
            if (snapshotsLock.readLock().isHeldByCurrentThread()) {
                snapshotsLock.readLock().unlock();
            }
            saving.set(false);
        }
    }
    
    /**
     * Load snapshots from disk asynchronously
     * 
     * @return True if the load operation was successful, false otherwise
     */
    public boolean loadSnapshotsAsync() {
        // Avoid concurrent loads
        if (!loading.compareAndSet(false, true)) {
            LOG.debug("Load already in progress, skipping");
            return false;
        }
        
        try {
            File file = getSnapshotFile();
            
            if (!file.exists() || !file.isFile()) {
                LOG.info("No snapshot file found at " + file.getAbsolutePath());
                return false;
            }
            
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                // Safely handle deserialization with proper validation
                Object obj = ois.readObject();
                
                if (!(obj instanceof List)) {
                    LOG.warn("Invalid snapshot file format: expected List, got " + 
                             (obj != null ? obj.getClass().getName() : "null"));
                    return false;
                }
                
                @SuppressWarnings("unchecked")
                List<?> rawList = (List<?>) obj;
                
                // Validate list contents before casting
                List<MemorySnapshot> loadedSnapshots = new ArrayList<>();
                for (Object item : rawList) {
                    if (item instanceof MemorySnapshot) {
                        loadedSnapshots.add((MemorySnapshot) item);
                    } else {
                        LOG.warn("Invalid item in snapshot list: expected MemorySnapshot, got " + 
                                (item != null ? item.getClass().getName() : "null"));
                    }
                }
                
                if (!loadedSnapshots.isEmpty()) {
                    try {
                        snapshotsLock.writeLock().lock();
                        
                        // Clear existing snapshots and add loaded ones
                        snapshots.clear();
                        
                        // Filter out invalid snapshots
                        for (MemorySnapshot snapshot : loadedSnapshots) {
                            if (snapshot != null && snapshot.getTimestamp() != null) {
                                snapshots.add(snapshot);
                            }
                        }
                        
                        // Reset counter
                        newSnapshotsCount = 0;
                        
                        LOG.info("Loaded " + snapshots.size() + " memory snapshots from " + file.getAbsolutePath());
                        return true;
                    } finally {
                        snapshotsLock.writeLock().unlock();
                    }
                } else {
                    LOG.info("No valid snapshots found in file " + file.getAbsolutePath());
                    return false;
                }
            } catch (Exception ex) {
                LOG.error("Error loading memory snapshots", ex);
                return false;
            }
        } catch (Exception ex) {
            LOG.error("Error during memory snapshot load operation", ex);
            return false;
        } finally {
            loading.set(false);
        }
    }
    
    /**
     * Get the snapshot file
     * 
     * @return The snapshot file
     */
    private File getSnapshotFile() {
        File snapshotDir = new File(System.getProperty("user.home"), ".modforge/snapshots");
        
        // Ensure directory exists
        try {
            if (!snapshotDir.exists()) {
                boolean created = snapshotDir.mkdirs();
                if (created) {
                    LOG.info("Created snapshot directory: " + snapshotDir.getAbsolutePath());
                } else {
                    LOG.warn("Failed to create snapshot directory: " + snapshotDir.getAbsolutePath());
                }
            }
        } catch (Exception ex) {
            LOG.error("Error creating snapshot directory", ex);
        }
        
        return new File(snapshotDir, "memory_snapshots.dat");
    }
    
    /**
     * Trim snapshots to prevent excessive memory usage
     */
    private void trimSnapshots() {
        if (snapshots.size() <= TRIM_THRESHOLD) {
            return;
        }
        
        LOG.info("Trimming memory snapshots from " + snapshots.size() + " to " + TRIM_TARGET);
        
        // Keep the most recent snapshots
        int toRemove = snapshots.size() - TRIM_TARGET;
        if (toRemove > 0) {
            snapshots.subList(0, toRemove).clear();
        }
    }
    
    /**
     * Reset the memory snapshot manager
     * Clears all snapshots, saves current state, and reinitializes
     * 
     * @return True if reset was successful
     */
    public boolean reset() {
        LOG.info("Resetting memory snapshot manager");
        
        try {
            // Save current snapshots first in case they're valuable
            boolean savedOk = false;
            try {
                savedOk = saveSnapshotsAsync();
                if (savedOk) {
                    LOG.info("Successfully saved snapshots before reset");
                }
            } catch (Exception e) {
                LOG.warn("Failed to save snapshots before reset", e);
                // Continue with reset even if save fails
            }
            
            // Clear all snapshots
            clearSnapshots();
            
            // Create a single current snapshot to start fresh
            MemoryManager memoryManager = MemoryManager.getInstance();
            if (memoryManager != null) {
                MemorySnapshot currentSnapshot = memoryManager.getCurrentMemorySnapshot();
                if (currentSnapshot != null) {
                    addSnapshot(currentSnapshot);
                    LOG.info("Added current memory snapshot after reset");
                }
            }
            
            LOG.info("Memory snapshot manager reset completed successfully");
            return true;
        } catch (Exception e) {
            LOG.error("Failed to reset memory snapshot manager", e);
            return false;
        }
    }
    
    /**
     * Export snapshots to CSV
     * 
     * @param file The file to export to
     * @return True if the export was successful
     */
    public boolean exportSnapshotsToCSV(File file) {
        try {
            snapshotsLock.readLock().lock();
            
            if (snapshots.isEmpty()) {
                LOG.info("No snapshots to export");
                return false;
            }
            
            // Create a copy for thread-safety
            List<MemorySnapshot> snapshotsCopy = new ArrayList<>(snapshots);
            
            // Release lock before file operations
            snapshotsLock.readLock().unlock();
            
            try (PrintWriter writer = new PrintWriter(file)) {
                // Write header
                writer.println("Timestamp,Used Memory (%),Used Memory (MB),Available Memory (MB),Total Memory (MB)");
                
                // Create date-time formatter for consistent output
                DateTimeFormatter dtf = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
                
                // Write data
                for (MemorySnapshot snapshot : snapshotsCopy) {
                    if (snapshot != null && snapshot.getTimestamp() != null) {
                        try {
                            writer.println(
                                    snapshot.getTimestamp().format(dtf) + "," +
                                    String.format("%.2f", snapshot.getUsedMemoryPercent()) + "," +
                                    String.format("%.2f", snapshot.getUsedMemoryMB()) + "," +
                                    String.format("%.2f", snapshot.getAvailableMemoryMB()) + "," +
                                    String.format("%.2f", snapshot.getTotalMemoryMB())
                            );
                        } catch (Exception ex) {
                            LOG.warn("Error writing snapshot to CSV: " + ex.getMessage());
                            // Continue with next snapshot
                        }
                    }
                }
            }
            
            LOG.info("Exported " + snapshotsCopy.size() + " memory snapshots to " + file.getAbsolutePath());
            return true;
            
        } catch (Exception ex) {
            LOG.error("Error exporting memory snapshots to CSV", ex);
            return false;
        } finally {
            // Release lock if still held
            if (snapshotsLock.readLock().isHeldByCurrentThread()) {
                snapshotsLock.readLock().unlock();
            }
        }
    }
}