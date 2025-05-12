package com.modforge.intellij.plugin.designers.advancement;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.modforge.intellij.plugin.designers.advancement.models.AdvancementCriterion;
import com.modforge.intellij.plugin.designers.advancement.models.AdvancementModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manager for working with Minecraft advancements
 * Handles loading, saving, and editing advancements
 */
@Service
public final class AdvancementManager {
    private static final Logger LOG = Logger.getInstance(AdvancementManager.class);
    
    private final Project project;
    private final Map<String, AdvancementModel> advancements = new ConcurrentHashMap<>();
    private final List<AdvancementModel> rootAdvancements = new ArrayList<>();
    
    public AdvancementManager(Project project) {
        this.project = project;
    }
    
    /**
     * Load advancements from the project
     * 
     * @param basePath The base path to load from
     * @return Number of advancements loaded
     */
    public int loadAdvancements(@NotNull String basePath) {
        advancements.clear();
        rootAdvancements.clear();
        
        try {
            VirtualFile baseDir = LocalFileSystem.getInstance().findFileByPath(basePath);
            if (baseDir == null || !baseDir.isDirectory()) {
                LOG.warn("Invalid base path for advancements: " + basePath);
                return 0;
            }
            
            // Find all advancement JSON files
            List<VirtualFile> advancementFiles = findAdvancementFiles(baseDir);
            LOG.info("Found " + advancementFiles.size() + " advancement files");
            
            // Load each advancement
            for (VirtualFile file : advancementFiles) {
                AdvancementModel advancement = loadAdvancement(file);
                if (advancement != null) {
                    advancements.put(advancement.getId(), advancement);
                }
            }
            
            // Set up parent-child relationships
            for (AdvancementModel advancement : advancements.values()) {
                String parentId = advancement.getParentId();
                if (parentId != null && !parentId.isEmpty()) {
                    AdvancementModel parent = advancements.get(parentId);
                    if (parent != null) {
                        parent.addChild(advancement);
                    } else {
                        // Parent not found, add as root
                        rootAdvancements.add(advancement);
                    }
                } else {
                    // No parent, add as root
                    rootAdvancements.add(advancement);
                }
            }
            
            return advancements.size();
            
        } catch (Exception e) {
            LOG.error("Error loading advancements", e);
            return 0;
        }
    }
    
    /**
     * Save advancements to the project
     * 
     * @param outputPath The output path to save to
     * @return Number of advancements saved
     */
    public int saveAdvancements(@NotNull String outputPath) {
        int savedCount = 0;
        
        try {
            // Create output directory if it doesn't exist
            Path dirPath = Paths.get(outputPath);
            Files.createDirectories(dirPath);
            
            // Save each advancement
            for (AdvancementModel advancement : advancements.values()) {
                String filename = advancement.getId().replace(':', '_') + ".json";
                Path filePath = dirPath.resolve(filename);
                
                try {
                    String json = advancement.toJson();
                    Files.writeString(filePath, json);
                    savedCount++;
                } catch (IOException e) {
                    LOG.error("Error saving advancement " + advancement.getId(), e);
                }
            }
            
            // Refresh the virtual file system
            LocalFileSystem.getInstance().refreshAndFindFileByPath(outputPath);
            
            return savedCount;
            
        } catch (Exception e) {
            LOG.error("Error saving advancements", e);
            return savedCount;
        }
    }
    
    /**
     * Find all advancement JSON files in a directory
     * 
     * @param dir The directory to search
     * @return List of advancement files
     */
    private List<VirtualFile> findAdvancementFiles(@NotNull VirtualFile dir) {
        List<VirtualFile> results = new ArrayList<>();
        findAdvancementFilesRecursive(dir, results);
        return results;
    }
    
    /**
     * Recursively find advancement JSON files
     * 
     * @param dir The directory to search
     * @param results List to add results to
     */
    private void findAdvancementFilesRecursive(@NotNull VirtualFile dir, @NotNull List<VirtualFile> results) {
        for (VirtualFile child : dir.getChildren()) {
            if (child.isDirectory()) {
                findAdvancementFilesRecursive(child, results);
            } else if (child.getName().endsWith(".json")) {
                // Check if it's an advancement file
                if (isAdvancementFile(child)) {
                    results.add(child);
                }
            }
        }
    }
    
    /**
     * Check if a file is an advancement file
     * 
     * @param file The file to check
     * @return True if it's an advancement file
     */
    private boolean isAdvancementFile(@NotNull VirtualFile file) {
        try {
            String content = new String(file.contentsToByteArray());
            // Simple check for advancement-specific JSON keys
            return content.contains("\"display\"") && 
                   content.contains("\"criteria\"") && 
                   (content.contains("\"title\"") || content.contains("\"frame\""));
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * Load an advancement from a file
     * 
     * @param file The file to load from
     * @return The loaded advancement model, or null if loading failed
     */
    @Nullable
    private AdvancementModel loadAdvancement(@NotNull VirtualFile file) {
        try {
            // In a real implementation, this would parse the JSON using a library
            // For now, we'll create a minimal advancement for demonstration
            String id = file.getNameWithoutExtension().replace('_', ':');
            AdvancementModel advancement = new AdvancementModel(id);
            
            // Extract basic info from JSON
            String content = new String(file.contentsToByteArray());
            
            // This is a very naive parser - in reality, use a JSON library
            Map<String, String> extracted = extractBasicInfo(content);
            
            advancement.setName(extracted.getOrDefault("title", id));
            advancement.setDescription(extracted.getOrDefault("description", ""));
            advancement.setIconItem(extracted.getOrDefault("icon", "minecraft:stone"));
            advancement.setParentId(extracted.getOrDefault("parent", ""));
            
            // Parse frame type
            String frameType = extracted.getOrDefault("frame", "task");
            if (frameType.equalsIgnoreCase("challenge")) {
                advancement.setFrameType(AdvancementModel.AdvancementFrameType.CHALLENGE);
            } else if (frameType.equalsIgnoreCase("goal")) {
                advancement.setFrameType(AdvancementModel.AdvancementFrameType.GOAL);
            } else {
                advancement.setFrameType(AdvancementModel.AdvancementFrameType.TASK);
            }
            
            // Parse criteria (simplified)
            List<String> criteriaIds = extractCriteriaIds(content);
            for (String criterionId : criteriaIds) {
                AdvancementCriterion criterion = new AdvancementCriterion(criterionId, "minecraft:dummy");
                advancement.addCriterion(criterion);
            }
            
            return advancement;
            
        } catch (Exception e) {
            LOG.error("Error loading advancement from " + file.getPath(), e);
            return null;
        }
    }
    
    /**
     * Extract basic info from advancement JSON
     * 
     * @param json The JSON string
     * @return Map of extracted values
     */
    private Map<String, String> extractBasicInfo(String json) {
        Map<String, String> result = new HashMap<>();
        
        // Very naive extraction - in reality, use a JSON parser
        
        // Extract title
        extractStringValue(json, "\"title\"", result, "title");
        
        // Extract description
        extractStringValue(json, "\"description\"", result, "description");
        
        // Extract icon
        extractStringValue(json, "\"item\"", result, "icon");
        
        // Extract parent
        extractStringValue(json, "\"parent\"", result, "parent");
        
        // Extract frame
        extractStringValue(json, "\"frame\"", result, "frame");
        
        return result;
    }
    
    /**
     * Extract a string value from JSON
     * 
     * @param json The JSON string
     * @param key The key to look for
     * @param result The result map to update
     * @param resultKey The key to use in the result map
     */
    private void extractStringValue(String json, String key, Map<String, String> result, String resultKey) {
        int keyIndex = json.indexOf(key);
        if (keyIndex >= 0) {
            int valueStart = json.indexOf(":", keyIndex) + 1;
            
            // Skip whitespace
            while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
                valueStart++;
            }
            
            if (valueStart < json.length() && json.charAt(valueStart) == '"') {
                // String value
                valueStart++; // Skip opening quote
                int valueEnd = json.indexOf("\"", valueStart);
                if (valueEnd > valueStart) {
                    result.put(resultKey, json.substring(valueStart, valueEnd));
                }
            } else if (valueStart < json.length() && json.charAt(valueStart) == '{') {
                // Object value with text field (for title/description)
                int textIndex = json.indexOf("\"text\"", valueStart);
                if (textIndex > valueStart) {
                    extractStringValue(json.substring(valueStart), "\"text\"", result, resultKey);
                }
            }
        }
    }
    
    /**
     * Extract criteria IDs from advancement JSON
     * 
     * @param json The JSON string
     * @return List of criteria IDs
     */
    private List<String> extractCriteriaIds(String json) {
        List<String> criteria = new ArrayList<>();
        
        int criteriaStart = json.indexOf("\"criteria\"");
        if (criteriaStart >= 0) {
            int objectStart = json.indexOf("{", criteriaStart);
            if (objectStart > criteriaStart) {
                int objectEnd = findMatchingBrace(json, objectStart);
                if (objectEnd > objectStart) {
                    String criteriaObject = json.substring(objectStart + 1, objectEnd);
                    
                    // Extract criteria keys (very naive approach)
                    int index = 0;
                    while (index < criteriaObject.length()) {
                        int keyStart = criteriaObject.indexOf("\"", index);
                        if (keyStart < 0) break;
                        
                        int keyEnd = criteriaObject.indexOf("\"", keyStart + 1);
                        if (keyEnd < 0) break;
                        
                        String key = criteriaObject.substring(keyStart + 1, keyEnd);
                        criteria.add(key);
                        
                        // Skip to next criterion
                        index = criteriaObject.indexOf("}", keyEnd);
                        if (index < 0) break;
                        index++;
                    }
                }
            }
        }
        
        return criteria;
    }
    
    /**
     * Find the matching closing brace for an opening brace
     * 
     * @param text The text to search in
     * @param openBraceIndex The index of the opening brace
     * @return The index of the matching closing brace, or -1 if not found
     */
    private int findMatchingBrace(String text, int openBraceIndex) {
        char openBrace = text.charAt(openBraceIndex);
        char closeBrace;
        
        switch (openBrace) {
            case '{': closeBrace = '}'; break;
            case '[': closeBrace = ']'; break;
            case '(': closeBrace = ')'; break;
            default: return -1;
        }
        
        int depth = 1;
        for (int i = openBraceIndex + 1; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == openBrace) {
                depth++;
            } else if (c == closeBrace) {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        
        return -1;
    }
    
    /**
     * Create a new advancement
     * 
     * @param id The advancement ID
     * @param name The display name
     * @return The created advancement
     */
    public AdvancementModel createAdvancement(@NotNull String id, @NotNull String name) {
        AdvancementModel advancement = new AdvancementModel(id, name);
        advancements.put(id, advancement);
        rootAdvancements.add(advancement);
        return advancement;
    }
    
    /**
     * Delete an advancement
     * 
     * @param id The advancement ID
     * @return True if the advancement was deleted, false if it wasn't found
     */
    public boolean deleteAdvancement(@NotNull String id) {
        AdvancementModel advancement = advancements.remove(id);
        if (advancement != null) {
            rootAdvancements.remove(advancement);
            
            // Remove parent-child relationships
            AdvancementModel parent = advancement.getParent();
            if (parent != null) {
                parent.removeChild(advancement);
            }
            
            // Remove as parent from children
            for (AdvancementModel child : advancement.getChildren()) {
                child.setParent(null);
                rootAdvancements.add(child);
            }
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Get all advancements
     * 
     * @return Collection of all advancements
     */
    public Collection<AdvancementModel> getAllAdvancements() {
        return advancements.values();
    }
    
    /**
     * Get root advancements (those without parents)
     * 
     * @return List of root advancements
     */
    public List<AdvancementModel> getRootAdvancements() {
        return new ArrayList<>(rootAdvancements);
    }
    
    /**
     * Get an advancement by ID
     * 
     * @param id The advancement ID
     * @return The advancement, or null if not found
     */
    @Nullable
    public AdvancementModel getAdvancement(@NotNull String id) {
        return advancements.get(id);
    }
}