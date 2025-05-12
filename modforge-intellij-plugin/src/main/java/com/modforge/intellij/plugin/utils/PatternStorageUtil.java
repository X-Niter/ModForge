package com.modforge.intellij.plugin.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.modforge.intellij.plugin.models.ModLoaderType;
import com.modforge.intellij.plugin.models.Pattern;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Utility class for storing and retrieving patterns.
 * Manages local pattern database and sync status.
 */
public class PatternStorageUtil {
    private static final Logger LOG = Logger.getInstance(PatternStorageUtil.class);
    
    private final Project project;
    private final File storageDir;
    private final File patternsFile;
    private final Gson gson;
    
    // In-memory cache of patterns
    private final Map<String, Pattern> patterns = new ConcurrentHashMap<>();
    
    /**
     * Creates a new PatternStorageUtil for the specified project.
     * @param project The project to store patterns for
     */
    public PatternStorageUtil(@NotNull Project project) {
        this.project = project;
        this.storageDir = new File(project.getBasePath(), ".modforge");
        this.patternsFile = new File(storageDir, "patterns.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }
    
    /**
     * Initializes the pattern storage by creating directories and loading patterns.
     */
    public void initialize() {
        try {
            // Create storage directory if it doesn't exist
            if (!storageDir.exists() && !storageDir.mkdirs()) {
                LOG.warn("Failed to create ModForge storage directory: " + storageDir.getAbsolutePath());
            }
            
            // Load patterns from file
            loadPatterns();
            
            LOG.info("Initialized pattern storage with " + patterns.size() + " patterns");
        } catch (Exception e) {
            LOG.error("Error initializing pattern storage", e);
        }
    }
    
    /**
     * Loads patterns from the patterns file.
     */
    private void loadPatterns() {
        if (!patternsFile.exists()) {
            LOG.info("Patterns file does not exist, starting with empty patterns");
            return;
        }
        
        try (Reader reader = new InputStreamReader(new FileInputStream(patternsFile), StandardCharsets.UTF_8)) {
            Type patternListType = new TypeToken<List<Pattern>>(){}.getType();
            List<Pattern> loadedPatterns = gson.fromJson(reader, patternListType);
            
            if (loadedPatterns != null) {
                // Add patterns to the cache
                for (Pattern pattern : loadedPatterns) {
                    patterns.put(pattern.getId(), pattern);
                }
                LOG.info("Loaded " + patterns.size() + " patterns from disk");
            }
        } catch (IOException e) {
            LOG.error("Error loading patterns from file", e);
        }
    }
    
    /**
     * Saves patterns to the patterns file.
     */
    public void savePatterns() {
        try {
            if (!storageDir.exists() && !storageDir.mkdirs()) {
                LOG.warn("Failed to create ModForge storage directory: " + storageDir.getAbsolutePath());
                return;
            }
            
            // Write patterns to file
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(patternsFile), StandardCharsets.UTF_8)) {
                List<Pattern> patternList = new ArrayList<>(patterns.values());
                gson.toJson(patternList, writer);
                LOG.info("Saved " + patternList.size() + " patterns to disk");
            }
        } catch (IOException e) {
            LOG.error("Error saving patterns to file", e);
        }
    }
    
    /**
     * Adds a pattern to the storage.
     * @param pattern The pattern to add
     */
    public void addPattern(Pattern pattern) {
        patterns.put(pattern.getId(), pattern);
        // Auto-save after adding a pattern
        savePatterns();
    }
    
    /**
     * Updates an existing pattern in the storage.
     * @param pattern The pattern to update
     * @return True if the pattern was updated, false if it wasn't found
     */
    public boolean updatePattern(Pattern pattern) {
        if (patterns.containsKey(pattern.getId())) {
            patterns.put(pattern.getId(), pattern);
            // Auto-save after updating a pattern
            savePatterns();
            return true;
        }
        return false;
    }
    
    /**
     * Gets a pattern by ID.
     * @param patternId The ID of the pattern to get
     * @return The pattern, or null if it wasn't found
     */
    public Pattern getPattern(String patternId) {
        return patterns.get(patternId);
    }
    
    /**
     * Gets all patterns in the storage.
     * @return All patterns
     */
    public List<Pattern> getAllPatterns() {
        return new ArrayList<>(patterns.values());
    }
    
    /**
     * Gets patterns of the specified type.
     * @param type The type of patterns to get
     * @return Patterns of the specified type
     */
    public List<Pattern> getPatternsByType(Pattern.PatternType type) {
        return patterns.values().stream()
                .filter(p -> p.getType() == type)
                .collect(Collectors.toList());
    }
    
    /**
     * Gets patterns for the specified mod loader.
     * @param modLoader The mod loader to get patterns for
     * @return Patterns for the specified mod loader
     */
    public List<Pattern> getPatternsByModLoader(ModLoaderType modLoader) {
        return patterns.values().stream()
                .filter(p -> p.getModLoader() == modLoader)
                .collect(Collectors.toList());
    }
    
    /**
     * Gets patterns that have been modified since the last sync.
     * @return Modified patterns
     */
    public List<Pattern> getModifiedPatterns() {
        return patterns.values().stream()
                .filter(Pattern::isDirty)
                .collect(Collectors.toList());
    }
    
    /**
     * Merges the specified patterns into the storage.
     * If a pattern with the same ID already exists, it will be updated if the incoming pattern
     * has a more recent modification timestamp.
     * @param incomingPatterns The patterns to merge
     */
    public void mergePatterns(List<Pattern> incomingPatterns) {
        if (incomingPatterns == null || incomingPatterns.isEmpty()) {
            return;
        }
        
        int updateCount = 0;
        int newCount = 0;
        
        for (Pattern incoming : incomingPatterns) {
            Pattern existing = patterns.get(incoming.getId());
            
            if (existing == null) {
                // New pattern
                patterns.put(incoming.getId(), incoming);
                newCount++;
            } else if (incoming.getLastModifiedTimestamp() > existing.getLastModifiedTimestamp()) {
                // Updated pattern
                patterns.put(incoming.getId(), incoming);
                updateCount++;
            }
        }
        
        LOG.info("Merged patterns: " + newCount + " new, " + updateCount + " updated");
        
        // Save after merging
        savePatterns();
    }
    
    /**
     * Marks all modified patterns as clean, indicating they have been synced.
     */
    public void markAllPatternsClean() {
        for (Pattern pattern : getModifiedPatterns()) {
            pattern.markClean();
        }
        savePatterns();
    }
    
    /**
     * Records a successful use of the specified pattern.
     * @param patternId The ID of the pattern
     */
    public void recordPatternSuccess(String patternId) {
        Pattern pattern = patterns.get(patternId);
        if (pattern != null) {
            pattern.recordSuccess();
            savePatterns();
        }
    }
    
    /**
     * Records a failed use of the specified pattern.
     * @param patternId The ID of the pattern
     */
    public void recordPatternFailure(String patternId) {
        Pattern pattern = patterns.get(patternId);
        if (pattern != null) {
            pattern.recordFailure();
            savePatterns();
        }
    }
}