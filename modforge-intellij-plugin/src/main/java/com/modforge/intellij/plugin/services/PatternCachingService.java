package com.modforge.intellij.plugin.services;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Service for caching patterns and AI responses to optimize API usage and improve performance.
 * This helps reduce the number of API calls to OpenAI by reusing results for similar requests.
 */
@Service(Service.Level.PROJECT)
public final class PatternCachingService {
    private static final Logger LOG = Logger.getInstance(PatternCachingService.class);
    
    // Cache expiration time (12 hours)
    private static final long CACHE_EXPIRATION_HOURS = 12;
    
    // Maximum cache size (to prevent memory issues)
    private static final int MAX_CACHE_SIZE = 1000;
    
    // Cache hit metrics
    private int cacheHits = 0;
    private int cacheMisses = 0;
    
    // The pattern cache
    private final Map<String, CacheEntry> patternCache = new ConcurrentHashMap<>();
    
    // Executor for cleanup tasks
    private final ScheduledExecutorService scheduler = AppExecutorUtil.getAppScheduledExecutorService();
    
    // Cache entry class
    private static class CacheEntry {
        final String result;
        final Instant timestamp;
        int useCount;
        
        CacheEntry(String result) {
            this.result = result;
            this.timestamp = Instant.now();
            this.useCount = 1;
        }
        
        boolean isExpired() {
            Duration age = Duration.between(timestamp, Instant.now());
            return age.toHours() >= CACHE_EXPIRATION_HOURS;
        }
    }
    
    /**
     * Constructor that starts the cleanup task.
     */
    public PatternCachingService() {
        // Schedule cache cleanup
        scheduler.scheduleAtFixedRate(
                this::cleanupExpiredEntries,
                1,
                6,
                TimeUnit.HOURS
        );
    }
    
    /**
     * Gets an instance of the service.
     * @param project The project
     * @return The service instance
     */
    public static PatternCachingService getInstance(@NotNull Project project) {
        return project.getService(PatternCachingService.class);
    }
    
    /**
     * Gets a cached result for a pattern.
     * @param patternKey The pattern key
     * @return The cached result, or null if not found
     */
    @Nullable
    public String getCachedResult(@NotNull String patternKey) {
        CacheEntry entry = patternCache.get(patternKey);
        
        if (entry == null) {
            cacheMisses++;
            return null;
        }
        
        if (entry.isExpired()) {
            patternCache.remove(patternKey);
            cacheMisses++;
            return null;
        }
        
        // Increment use count
        entry.useCount++;
        cacheHits++;
        
        LOG.debug("Pattern cache hit for key: " + patternKey);
        return entry.result;
    }
    
    /**
     * Stores a result in the cache.
     * @param patternKey The pattern key
     * @param result The result to cache
     */
    public void cacheResult(@NotNull String patternKey, @NotNull String result) {
        // Check if we need to clean up the cache
        if (patternCache.size() >= MAX_CACHE_SIZE) {
            cleanupLeastUsedEntries();
        }
        
        patternCache.put(patternKey, new CacheEntry(result));
        LOG.debug("Cached result for key: " + patternKey);
    }
    
    /**
     * Invalidates a cached result.
     * @param patternKey The pattern key
     */
    public void invalidateCache(@NotNull String patternKey) {
        patternCache.remove(patternKey);
        LOG.debug("Invalidated cache for key: " + patternKey);
    }
    
    /**
     * Clears the entire cache.
     */
    public void clearCache() {
        patternCache.clear();
        LOG.info("Cleared pattern cache");
    }
    
    /**
     * Gets the cache hit statistics.
     * @return The cache hit statistics
     */
    public Map<String, Integer> getCacheStatistics() {
        return Map.of(
                "cacheSize", patternCache.size(),
                "cacheHits", cacheHits,
                "cacheMisses", cacheMisses,
                "hitRatio", calculateHitRatio()
        );
    }
    
    /**
     * Calculates the cache hit ratio.
     * @return The cache hit ratio (0-100)
     */
    private int calculateHitRatio() {
        int total = cacheHits + cacheMisses;
        if (total == 0) {
            return 0;
        }
        return (int) ((cacheHits * 100.0) / total);
    }
    
    /**
     * Cleans up expired entries from the cache.
     */
    private void cleanupExpiredEntries() {
        LOG.debug("Running cache cleanup task");
        int beforeSize = patternCache.size();
        
        patternCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        
        int afterSize = patternCache.size();
        int removedEntries = beforeSize - afterSize;
        
        if (removedEntries > 0) {
            LOG.info("Cleaned up " + removedEntries + " expired cache entries");
        }
    }
    
    /**
     * Cleans up the least used entries from the cache.
     */
    private void cleanupLeastUsedEntries() {
        // Remove 25% of the least used entries
        int toRemove = MAX_CACHE_SIZE / 4;
        
        patternCache.entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(e1.getValue().useCount, e2.getValue().useCount))
                .limit(toRemove)
                .forEach(entry -> patternCache.remove(entry.getKey()));
        
        LOG.info("Cleaned up " + toRemove + " least used cache entries");
    }
    
    /**
     * Loads stored patterns from disk.
     * @return Map of stored patterns
     */
    @NotNull
    public Map<String, String> loadStoredPatterns() {
        LOG.info("Loading stored patterns from disk...");
        Map<String, String> result = new HashMap<>();
        
        try {
            // Get storage directory
            java.nio.file.Path storageDir = getStorageDirectory();
            java.nio.file.Path patternFile = storageDir.resolve("patterns.dat");
            
            if (!java.nio.file.Files.exists(patternFile)) {
                LOG.info("No pattern file found at: " + patternFile);
                return result;
            }
            
            // Load data
            java.util.List<String> lines = java.nio.file.Files.readAllLines(patternFile);
            
            for (String line : lines) {
                // Format: key=value
                int separatorIndex = line.indexOf('=');
                if (separatorIndex > 0) {
                    String key = line.substring(0, separatorIndex);
                    String value = line.substring(separatorIndex + 1);
                    result.put(key, value);
                }
            }
            
            LOG.info("Loaded " + result.size() + " patterns from disk");
            
        } catch (Exception e) {
            LOG.error("Error loading patterns from disk", e);
        }
        
        return result;
    }
    
    /**
     * Saves stored patterns to disk.
     * @param patterns The patterns to save
     */
    public void saveStoredPatterns(@NotNull Map<String, String> patterns) {
        LOG.info("Saving " + patterns.size() + " patterns to disk...");
        
        try {
            // Get storage directory
            java.nio.file.Path storageDir = getStorageDirectory();
            java.nio.file.Files.createDirectories(storageDir);
            
            java.nio.file.Path patternFile = storageDir.resolve("patterns.dat");
            
            // Build content
            StringBuilder content = new StringBuilder();
            for (Map.Entry<String, String> entry : patterns.entrySet()) {
                content.append(entry.getKey())
                       .append('=')
                       .append(entry.getValue())
                       .append(System.lineSeparator());
            }
            
            // Write to file
            java.nio.file.Files.writeString(patternFile, content.toString());
            
            LOG.info("Successfully saved patterns to: " + patternFile);
            
        } catch (Exception e) {
            LOG.error("Error saving patterns to disk", e);
        }
    }
    
    /**
     * Gets the storage directory path.
     * @return The storage directory path
     */
    @NotNull
    private java.nio.file.Path getStorageDirectory() throws java.io.IOException {
        // Use system property for user home
        String userHome = System.getProperty("user.home");
        
        // Create .modforge directory if it doesn't exist
        java.nio.file.Path modforgeDir = java.nio.file.Paths.get(userHome, ".modforge");
        java.nio.file.Files.createDirectories(modforgeDir);
        
        // Create patterns directory
        java.nio.file.Path patternsDir = modforgeDir.resolve("patterns");
        java.nio.file.Files.createDirectories(patternsDir);
        
        return patternsDir;
    }
}