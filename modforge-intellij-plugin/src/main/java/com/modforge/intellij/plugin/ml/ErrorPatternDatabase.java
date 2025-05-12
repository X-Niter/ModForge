package com.modforge.intellij.plugin.ml;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.modforge.intellij.plugin.models.ErrorFix;
import com.modforge.intellij.plugin.models.Pattern;
import com.modforge.intellij.plugin.services.AIServiceManager;
import com.modforge.intellij.plugin.services.ModForgeProjectService;
import com.modforge.intellij.plugin.services.ModForgeSettingsService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * Database for storing and retrieving error patterns.
 * Uses a combination of local and remote patterns.
 */
public class ErrorPatternDatabase {
    private static final Logger LOG = Logger.getInstance(ErrorPatternDatabase.class);
    
    private final Project project;
    private final Path databaseFile;
    private final Gson gson;
    private final ExecutorService executorService;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    
    // In-memory pattern cache
    private final Map<String, Pattern> patterns = new ConcurrentHashMap<>();
    
    // Stats
    private int patternCount = 0;
    private int successfulMatches = 0;
    private int failedMatches = 0;
    private int totalQueries = 0;
    
    /**
     * Creates a new ErrorPatternDatabase for the specified project.
     * @param project The project
     */
    public ErrorPatternDatabase(@NotNull Project project) {
        this.project = project;
        this.databaseFile = Paths.get(project.getBasePath(), ".modforge", "error_patterns.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.executorService = AppExecutorUtil.getAppExecutorService();
    }
    
    /**
     * Initializes the database.
     */
    public void initialize() {
        try {
            lock.writeLock().lock();
            try {
                LOG.info("Initializing error pattern database");
                
                // Create the database directory if it doesn't exist
                Files.createDirectories(databaseFile.getParent());
                
                // Load existing patterns
                loadPatterns();
                
                // Schedule sync with remote database
                if (ModForgeSettingsService.getInstance().isSyncEnabled()) {
                    executorService.submit(this::syncWithRemoteDatabase);
                }
                
                LOG.info("Initialized error pattern database with " + patterns.size() + " patterns");
            } finally {
                lock.writeLock().unlock();
            }
        } catch (Exception e) {
            LOG.error("Error initializing error pattern database", e);
        }
    }
    
    /**
     * Loads patterns from the database file.
     */
    private void loadPatterns() {
        if (!Files.exists(databaseFile)) {
            LOG.info("No error pattern database file found, starting with empty database");
            return;
        }
        
        try (Reader reader = new InputStreamReader(Files.newInputStream(databaseFile), StandardCharsets.UTF_8)) {
            Type patternListType = new TypeToken<List<Pattern>>(){}.getType();
            List<Pattern> loadedPatterns = gson.fromJson(reader, patternListType);
            
            if (loadedPatterns != null) {
                for (Pattern pattern : loadedPatterns) {
                    patterns.put(pattern.getId(), pattern);
                }
                
                patternCount = patterns.size();
                LOG.info("Loaded " + patternCount + " error patterns from database file");
            }
        } catch (Exception e) {
            LOG.error("Error loading error patterns from database file", e);
        }
    }
    
    /**
     * Saves patterns to the database file.
     */
    private void savePatterns() {
        try {
            lock.readLock().lock();
            try {
                LOG.info("Saving " + patterns.size() + " error patterns to database file");
                
                Files.createDirectories(databaseFile.getParent());
                
                try (Writer writer = new OutputStreamWriter(Files.newOutputStream(databaseFile), StandardCharsets.UTF_8)) {
                    List<Pattern> patternList = new ArrayList<>(patterns.values());
                    gson.toJson(patternList, writer);
                }
                
                LOG.info("Saved error patterns to database file");
            } finally {
                lock.readLock().unlock();
            }
        } catch (Exception e) {
            LOG.error("Error saving error patterns to database file", e);
        }
    }
    
    /**
     * Syncs with the remote database.
     */
    public void syncWithRemoteDatabase() {
        try {
            LOG.info("Syncing error patterns with remote database");
            
            AIServiceManager aiServiceManager = project.getService(AIServiceManager.class);
            
            // Get modified patterns for upload
            List<Pattern> modifiedPatterns = patterns.values().stream()
                    .filter(Pattern::isDirty)
                    .filter(p -> p.getType() == Pattern.PatternType.ERROR_RESOLUTION)
                    .collect(Collectors.toList());
            
            // Upload modified patterns
            if (!modifiedPatterns.isEmpty()) {
                LOG.info("Uploading " + modifiedPatterns.size() + " modified error patterns to remote database");
                aiServiceManager.uploadPatterns(modifiedPatterns);
                
                // Mark patterns as clean
                for (Pattern pattern : modifiedPatterns) {
                    pattern.markClean();
                }
                
                // Save patterns to disk
                savePatterns();
            }
            
            // Download new patterns
            long lastSyncTimestamp = patterns.values().stream()
                    .mapToLong(Pattern::getLastModifiedTimestamp)
                    .max()
                    .orElse(0);
            
            List<Pattern> newPatterns = aiServiceManager.downloadLatestPatterns(lastSyncTimestamp);
            
            if (!newPatterns.isEmpty()) {
                lock.writeLock().lock();
                try {
                    LOG.info("Downloaded " + newPatterns.size() + " new error patterns from remote database");
                    
                    // Add new patterns to local database
                    for (Pattern pattern : newPatterns) {
                        if (pattern.getType() == Pattern.PatternType.ERROR_RESOLUTION) {
                            patterns.put(pattern.getId(), pattern);
                        }
                    }
                    
                    patternCount = patterns.size();
                    
                    // Save patterns to disk
                    savePatterns();
                } finally {
                    lock.writeLock().unlock();
                }
            }
            
            LOG.info("Error pattern sync complete. Total patterns: " + patternCount);
        } catch (Exception e) {
            LOG.error("Error syncing error patterns with remote database", e);
        }
    }
    
    /**
     * Finds a matching fix for the specified error signature.
     * @param errorSignature The error signature
     * @return The matching fix, or null if no match was found
     */
    @Nullable
    public ErrorFix findMatchingFix(@NotNull ErrorSignature errorSignature) {
        try {
            lock.readLock().lock();
            try {
                LOG.info("Finding matching fix for error: " + errorSignature.getErrorMessage());
                
                totalQueries++;
                
                // Find patterns matching this error type
                List<PatternMatch> matches = new ArrayList<>();
                
                for (Pattern pattern : patterns.values()) {
                    if (pattern.getType() != Pattern.PatternType.ERROR_RESOLUTION) {
                        continue;
                    }
                    
                    try {
                        // Deserialize the pattern context to get the error signature
                        ErrorSignature patternSignature = ErrorSignature.deserialize(pattern.getContext());
                        
                        // Calculate similarity
                        double similarity = patternSignature.calculateSimilarity(errorSignature);
                        
                        // Add to matches if similarity is above threshold
                        if (similarity >= ModForgeSettingsService.getInstance().getPatternMatchingThreshold() / 100.0) {
                            matches.add(new PatternMatch(pattern, similarity));
                        }
                    } catch (Exception e) {
                        LOG.warn("Error matching pattern: " + e.getMessage());
                    }
                }
                
                // Sort matches by similarity and confidence
                Collections.sort(matches);
                
                if (!matches.isEmpty()) {
                    // Use the best match
                    PatternMatch bestMatch = matches.get(0);
                    LOG.info("Found matching pattern with similarity: " + bestMatch.similarity);
                    
                    // Increment success counter for this pattern
                    bestMatch.pattern.recordSuccess();
                    
                    // Save patterns to update the success count
                    savePatterns();
                    
                    // Create fix from pattern
                    ErrorFix fix = new ErrorFix(
                            bestMatch.pattern.getSolution(),
                            "Fix generated from similar error pattern with " + 
                                    String.format("%.1f%%", bestMatch.similarity * 100) + " similarity.",
                            errorSignature.getFilePath(),
                            errorSignature.getLine(),
                            errorSignature.getColumn()
                    );
                    
                    // Update stats
                    successfulMatches++;
                    
                    return fix;
                }
                
                // Update stats
                failedMatches++;
                
                LOG.info("No matching pattern found for error");
                return null;
            } finally {
                lock.readLock().unlock();
            }
        } catch (Exception e) {
            LOG.error("Error finding matching fix", e);
            return null;
        }
    }
    
    /**
     * Stores a pattern in the database.
     * @param pattern The pattern to store
     */
    public void storePattern(@NotNull Pattern pattern) {
        try {
            lock.writeLock().lock();
            try {
                LOG.info("Storing error pattern: " + pattern.getId());
                
                // Add to in-memory cache
                patterns.put(pattern.getId(), pattern);
                patternCount = patterns.size();
                
                // Save to disk
                savePatterns();
                
                // Schedule upload to remote database if sync is enabled
                if (ModForgeSettingsService.getInstance().isSyncEnabled()) {
                    executorService.submit(() -> {
                        try {
                            ModForgeProjectService projectService = project.getService(ModForgeProjectService.class);
                            projectService.syncWithWebPlatform();
                        } catch (Exception e) {
                            LOG.error("Error uploading error pattern to remote database", e);
                        }
                    });
                }
            } finally {
                lock.writeLock().unlock();
            }
        } catch (Exception e) {
            LOG.error("Error storing error pattern", e);
        }
    }
    
    /**
     * Updates a pattern in the database.
     * @param pattern The pattern to update
     * @return Whether the pattern was updated
     */
    public boolean updatePattern(@NotNull Pattern pattern) {
        try {
            lock.writeLock().lock();
            try {
                LOG.info("Updating error pattern: " + pattern.getId());
                
                // Check if the pattern exists
                if (!patterns.containsKey(pattern.getId())) {
                    LOG.warn("Pattern not found: " + pattern.getId());
                    return false;
                }
                
                // Update the pattern
                patterns.put(pattern.getId(), pattern);
                
                // Save to disk
                savePatterns();
                
                return true;
            } finally {
                lock.writeLock().unlock();
            }
        } catch (Exception e) {
            LOG.error("Error updating error pattern", e);
            return false;
        }
    }
    
    /**
     * Gets a pattern from the database.
     * @param patternId The pattern ID
     * @return The pattern, or null if not found
     */
    @Nullable
    public Pattern getPattern(@NotNull String patternId) {
        try {
            lock.readLock().lock();
            try {
                return patterns.get(patternId);
            } finally {
                lock.readLock().unlock();
            }
        } catch (Exception e) {
            LOG.error("Error getting error pattern", e);
            return null;
        }
    }
    
    /**
     * Gets all patterns from the database.
     * @return All patterns
     */
    @NotNull
    public List<Pattern> getAllPatterns() {
        try {
            lock.readLock().lock();
            try {
                return new ArrayList<>(patterns.values());
            } finally {
                lock.readLock().unlock();
            }
        } catch (Exception e) {
            LOG.error("Error getting all error patterns", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Gets the pattern count.
     * @return The pattern count
     */
    public int getPatternCount() {
        return patternCount;
    }
    
    /**
     * Gets statistics about the database.
     * @return Statistics about the database
     */
    @NotNull
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("patternCount", patternCount);
        stats.put("successfulMatches", successfulMatches);
        stats.put("failedMatches", failedMatches);
        stats.put("totalQueries", totalQueries);
        
        // Calculate success rate
        double successRate = 0;
        if (totalQueries > 0) {
            successRate = (double) successfulMatches / totalQueries;
        }
        stats.put("successRate", successRate);
        
        // Get error type distribution
        Map<String, Integer> errorTypeDistribution = new HashMap<>();
        for (Pattern pattern : patterns.values()) {
            if (pattern.getType() == Pattern.PatternType.ERROR_RESOLUTION) {
                try {
                    ErrorSignature signature = ErrorSignature.deserialize(pattern.getContext());
                    String errorType = signature.getErrorType();
                    errorTypeDistribution.put(errorType, errorTypeDistribution.getOrDefault(errorType, 0) + 1);
                } catch (Exception e) {
                    LOG.warn("Error getting error type for pattern: " + e.getMessage());
                }
            }
        }
        stats.put("errorTypeDistribution", errorTypeDistribution);
        
        return stats;
    }
    
    /**
     * Class representing a pattern match.
     */
    private static class PatternMatch implements Comparable<PatternMatch> {
        private final Pattern pattern;
        private final double similarity;
        
        public PatternMatch(Pattern pattern, double similarity) {
            this.pattern = pattern;
            this.similarity = similarity;
        }
        
        @Override
        public int compareTo(PatternMatch other) {
            // First compare by similarity
            int similarityCompare = Double.compare(other.similarity, this.similarity);
            if (similarityCompare != 0) {
                return similarityCompare;
            }
            
            // Then by confidence (success rate)
            return Double.compare(other.pattern.getConfidence(), this.pattern.getConfidence());
        }
    }
}