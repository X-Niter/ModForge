package com.modforge.intellij.plugin.services;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for recognizing code patterns using machine learning and embeddings.
 * This helps identify similar code structures and reduce API calls by reusing
 * previously generated patterns.
 */
@Service(Service.Level.PROJECT)
public final class PatternRecognitionService {
    private static final Logger LOG = Logger.getInstance(PatternRecognitionService.class);
    
    // Similarity threshold for pattern matching (0-1)
    private static final double SIMILARITY_THRESHOLD = 0.85;
    
    // Maximum number of patterns to store
    private static final int MAX_PATTERNS = 5000;
    
    // Project reference
    private final Project project;
    
    // Services
    private final EmbeddingService embeddingService;
    private final PatternCachingService patternCachingService;
    
    // Pattern storage
    private final Map<String, PatternEntry> patterns = new ConcurrentHashMap<>();
    
    // Metrics
    private final AtomicInteger patternMatchCount = new AtomicInteger(0);
    private final AtomicInteger patternMissCount = new AtomicInteger(0);
    
    // Pattern categories
    public enum PatternCategory {
        CODE_GENERATION,
        ERROR_RESOLUTION,
        FEATURE_SUGGESTION,
        CLASS_STRUCTURE,
        METHOD_IMPLEMENTATION,
        ARCHITECTURY_LOADER
    }
    
    /**
     * Pattern entry containing metadata and result.
     */
    private static class PatternEntry {
        final String pattern;
        final String result;
        final PatternCategory category;
        final long timestamp;
        int useCount;
        float effectiveness;  // 0-1 scale of how effective the pattern is
        
        PatternEntry(String pattern, String result, PatternCategory category) {
            this.pattern = pattern;
            this.result = result;
            this.category = category;
            this.timestamp = System.currentTimeMillis();
            this.useCount = 1;
            this.effectiveness = 0.5f;  // Initial middle effectiveness
        }
        
        void incrementUseCount() {
            useCount++;
        }
        
        void updateEffectiveness(float delta) {
            effectiveness = Math.max(0, Math.min(1, effectiveness + delta));
        }
    }
    
    /**
     * Creates a new PatternRecognitionService.
     * @param project The project
     */
    public PatternRecognitionService(@NotNull Project project) {
        this.project = project;
        this.embeddingService = EmbeddingService.getInstance(project);
        this.patternCachingService = PatternCachingService.getInstance(project);
        
        // Load patterns from storage
        loadPatterns();
    }
    
    /**
     * Gets the PatternRecognitionService instance.
     * @param project The project
     * @return The PatternRecognitionService instance
     */
    public static PatternRecognitionService getInstance(@NotNull Project project) {
        return project.getService(PatternRecognitionService.class);
    }
    
    /**
     * Loads patterns from storage.
     */
    private void loadPatterns() {
        LOG.info("Loading patterns from storage...");
        
        try {
            // Get patterns from caching service
            Map<String, String> storedPatterns = patternCachingService.loadStoredPatterns();
            
            if (storedPatterns == null || storedPatterns.isEmpty()) {
                LOG.info("No stored patterns found");
                return;
            }
            
            int loadedCount = 0;
            
            // Process each stored pattern
            for (Map.Entry<String, String> entry : storedPatterns.entrySet()) {
                try {
                    String key = entry.getKey();
                    String serializedValue = entry.getValue();
                    
                    // Parse the key to get category
                    String[] keyParts = key.split(":", 2);
                    if (keyParts.length != 2) {
                        LOG.warn("Invalid pattern key format: " + key);
                        continue;
                    }
                    
                    // Get category from key
                    PatternCategory category;
                    try {
                        category = PatternCategory.valueOf(keyParts[0]);
                    } catch (IllegalArgumentException e) {
                        LOG.warn("Unknown pattern category: " + keyParts[0]);
                        continue;
                    }
                    
                    // Parse serialized pattern entry
                    String[] valueParts = serializedValue.split("\\|\\|\\|", 5);
                    if (valueParts.length < 3) {
                        LOG.warn("Invalid pattern value format: " + serializedValue);
                        continue;
                    }
                    
                    String pattern = valueParts[0];
                    String result = valueParts[1];
                    int useCount = Integer.parseInt(valueParts[2]);
                    float effectiveness = valueParts.length > 3 ? Float.parseFloat(valueParts[3]) : 0.5f;
                    long timestamp = valueParts.length > 4 ? Long.parseLong(valueParts[4]) : System.currentTimeMillis();
                    
                    // Create and store pattern entry
                    PatternEntry patternEntry = new PatternEntry(pattern, result, category);
                    patternEntry.useCount = useCount;
                    patternEntry.effectiveness = effectiveness;
                    
                    // Add to patterns map
                    patterns.put(key, patternEntry);
                    loadedCount++;
                    
                } catch (Exception e) {
                    LOG.warn("Error parsing stored pattern", e);
                }
            }
            
            LOG.info("Loaded " + loadedCount + " patterns from storage");
            
        } catch (Exception e) {
            LOG.error("Error loading patterns from storage", e);
        }
    }
    
    /**
     * Saves patterns to storage.
     */
    private void savePatterns() {
        LOG.info("Saving patterns to storage...");
        
        try {
            // Create storage map
            Map<String, String> storageMap = new HashMap<>();
            
            // Serialize each pattern
            for (Map.Entry<String, PatternEntry> entry : patterns.entrySet()) {
                String key = entry.getKey();
                PatternEntry patternEntry = entry.getValue();
                
                // Format: pattern|||result|||useCount|||effectiveness|||timestamp
                String serializedValue = String.format("%s|||%s|||%d|||%f|||%d",
                        patternEntry.pattern,
                        patternEntry.result,
                        patternEntry.useCount,
                        patternEntry.effectiveness,
                        patternEntry.timestamp);
                
                storageMap.put(key, serializedValue);
            }
            
            // Save to storage
            patternCachingService.saveStoredPatterns(storageMap);
            
            LOG.info("Saved " + storageMap.size() + " patterns to storage");
            
        } catch (Exception e) {
            LOG.error("Error saving patterns to storage", e);
        }
    }
    
    /**
     * Registers a new pattern.
     * @param pattern The pattern
     * @param result The result
     * @param category The pattern category
     */
    public void registerPattern(@NotNull String pattern, @NotNull String result, @NotNull PatternCategory category) {
        // Check if we need to clean up patterns
        if (patterns.size() >= MAX_PATTERNS) {
            cleanupLeastUsedPatterns();
        }
        
        // Add the pattern
        PatternEntry entry = new PatternEntry(pattern, result, category);
        patterns.put(generatePatternKey(pattern, category), entry);
        
        // Generate embedding asynchronously
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Generating Pattern Embedding", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                embeddingService.getEmbedding(pattern, false);
            }
        });
        
        LOG.info("Registered new pattern in category: " + category);
        
        // Save patterns to persistent storage
        savePatterns();
    }
    
    /**
     * Finds a matching pattern for the given input.
     * @param input The input to match
     * @param category The pattern category
     * @return The matching pattern result, or null if no match
     */
    @Nullable
    public String findMatchingPattern(@NotNull String input, @NotNull PatternCategory category) {
        // Check for exact match first
        String patternKey = generatePatternKey(input, category);
        PatternEntry entry = patterns.get(patternKey);
        
        if (entry != null) {
            entry.incrementUseCount();
            patternMatchCount.incrementAndGet();
            LOG.info("Found exact pattern match in category: " + category);
            return entry.result;
        }
        
        // No exact match, try semantic search
        List<String> candidatePatterns = new ArrayList<>();
        Map<String, PatternEntry> candidateEntries = new HashMap<>();
        
        // Collect patterns in the same category
        for (Map.Entry<String, PatternEntry> patternEntry : patterns.entrySet()) {
            if (patternEntry.getValue().category == category) {
                candidatePatterns.add(patternEntry.getValue().pattern);
                candidateEntries.put(patternEntry.getValue().pattern, patternEntry.getValue());
            }
        }
        
        // Find most similar pattern
        Map.Entry<String, Double> mostSimilar = embeddingService.findMostSimilarText(
                input, candidatePatterns, SIMILARITY_THRESHOLD);
        
        if (mostSimilar != null) {
            String mostSimilarPattern = mostSimilar.getKey();
            double similarity = mostSimilar.getValue();
            
            PatternEntry similarEntry = candidateEntries.get(mostSimilarPattern);
            if (similarEntry != null) {
                similarEntry.incrementUseCount();
                patternMatchCount.incrementAndGet();
                LOG.info("Found similar pattern match in category: " + category + 
                         " with similarity: " + similarity);
                return similarEntry.result;
            }
        }
        
        patternMissCount.incrementAndGet();
        LOG.info("No pattern match found in category: " + category);
        return null;
    }
    
    /**
     * Cleans up the least used patterns.
     */
    private void cleanupLeastUsedPatterns() {
        // Remove 20% of the least used patterns
        int toRemove = MAX_PATTERNS / 5;
        
        List<Map.Entry<String, PatternEntry>> entries = new ArrayList<>(patterns.entrySet());
        entries.sort(Comparator.comparingInt(e -> e.getValue().useCount));
        
        for (int i = 0; i < toRemove && i < entries.size(); i++) {
            patterns.remove(entries.get(i).getKey());
        }
        
        LOG.info("Cleaned up " + toRemove + " least used patterns");
    }
    
    /**
     * Updates the effectiveness of a pattern.
     * @param pattern The pattern
     * @param category The pattern category
     * @param successful Whether the pattern was successful
     */
    public void updatePatternEffectiveness(
            @NotNull String pattern,
            @NotNull PatternCategory category,
            boolean successful
    ) {
        String patternKey = generatePatternKey(pattern, category);
        PatternEntry entry = patterns.get(patternKey);
        
        if (entry != null) {
            // Update effectiveness based on success
            float delta = successful ? 0.1f : -0.1f;
            entry.updateEffectiveness(delta);
            LOG.info("Updated pattern effectiveness: " + entry.effectiveness + 
                     " for category: " + category);
        }
    }
    
    /**
     * Analyzes a Java class to identify patterns.
     * @param psiClass The PsiClass to analyze
     */
    public void analyzeJavaClass(@NotNull PsiClass psiClass) {
        LOG.info("Analyzing class: " + psiClass.getQualifiedName());
        
        // Extract class structure pattern
        String classPattern = extractClassPattern(psiClass);
        if (classPattern != null && !classPattern.isEmpty()) {
            // Register the pattern - the result would be the class structure itself
            registerPattern(classPattern, classPattern, PatternCategory.CLASS_STRUCTURE);
        }
        
        // Analyze methods
        for (PsiMethod method : psiClass.getMethods()) {
            analyzeJavaMethod(method);
        }
    }
    
    /**
     * Analyzes a Java method to identify patterns.
     * @param psiMethod The PsiMethod to analyze
     */
    public void analyzeJavaMethod(@NotNull PsiMethod psiMethod) {
        LOG.info("Analyzing method: " + psiMethod.getName());
        
        // Extract method implementation pattern
        String methodPattern = extractMethodPattern(psiMethod);
        if (methodPattern != null && !methodPattern.isEmpty()) {
            // Register the pattern - the result would be the method implementation itself
            registerPattern(methodPattern, methodPattern, PatternCategory.METHOD_IMPLEMENTATION);
        }
    }
    
    /**
     * Extracts a pattern from a Java class.
     * @param psiClass The PsiClass
     * @return The pattern
     */
    @Nullable
    private String extractClassPattern(@NotNull PsiClass psiClass) {
        try {
            StringBuilder pattern = new StringBuilder();
            
            // Add class signature
            pattern.append("class ").append(psiClass.getName());
            
            if (psiClass.getExtendsList() != null && psiClass.getExtendsList().getReferenceElements().length > 0) {
                pattern.append(" extends ");
                pattern.append(psiClass.getExtendsList().getReferenceElements()[0].getQualifiedName());
            }
            
            if (psiClass.getImplementsList() != null && psiClass.getImplementsList().getReferenceElements().length > 0) {
                pattern.append(" implements ");
                pattern.append(psiClass.getImplementsList().getReferenceElements()[0].getQualifiedName());
            }
            
            // Add method signatures (not implementations)
            pattern.append(" { ");
            for (PsiMethod method : psiClass.getMethods()) {
                pattern.append(method.getName()).append("(); ");
            }
            pattern.append(" }");
            
            return pattern.toString();
        } catch (Exception e) {
            LOG.error("Error extracting class pattern", e);
            return null;
        }
    }
    
    /**
     * Extracts a pattern from a Java method.
     * @param psiMethod The PsiMethod
     * @return The pattern
     */
    @Nullable
    private String extractMethodPattern(@NotNull PsiMethod psiMethod) {
        try {
            return psiMethod.getText();
        } catch (Exception e) {
            LOG.error("Error extracting method pattern", e);
            return null;
        }
    }
    
    /**
     * Generates a key for a pattern.
     * @param pattern The pattern
     * @param category The category
     * @return The key
     */
    @NotNull
    private String generatePatternKey(@NotNull String pattern, @NotNull PatternCategory category) {
        return category.name() + ":" + Integer.toHexString(pattern.hashCode());
    }
    
    /**
     * Gets pattern metrics.
     * @return The metrics
     */
    @NotNull
    public Map<String, Integer> getMetrics() {
        Map<String, Integer> metrics = new HashMap<>();
        metrics.put("patternCount", patterns.size());
        metrics.put("patternMatchCount", patternMatchCount.get());
        metrics.put("patternMissCount", patternMissCount.get());
        
        // Count patterns by category
        Map<PatternCategory, Integer> categoryCounts = new EnumMap<>(PatternCategory.class);
        for (PatternEntry entry : patterns.values()) {
            categoryCounts.merge(entry.category, 1, Integer::sum);
        }
        
        // Add category counts to metrics
        for (Map.Entry<PatternCategory, Integer> entry : categoryCounts.entrySet()) {
            metrics.put("category_" + entry.getKey().name(), entry.getValue());
        }
        
        return metrics;
    }
}