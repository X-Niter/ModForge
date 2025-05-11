package com.modforge.intellij.plugin.designers.structure;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.modforge.intellij.plugin.designers.structure.models.BlockState;
import com.modforge.intellij.plugin.designers.structure.models.StructureModel;
import com.modforge.intellij.plugin.designers.structure.models.StructurePart;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manager for working with Minecraft structures
 * Handles loading, saving, and editing structures
 */
@Service
public final class StructureManager {
    private static final Logger LOG = Logger.getInstance(StructureManager.class);
    
    private final Project project;
    private final Map<String, StructureModel> structures = new ConcurrentHashMap<>();
    
    public StructureManager(Project project) {
        this.project = project;
    }
    
    /**
     * Load structures from the project
     * 
     * @param basePath The base path to load from
     * @return Number of structures loaded
     */
    public int loadStructures(@NotNull String basePath) {
        structures.clear();
        
        try {
            VirtualFile baseDir = LocalFileSystem.getInstance().findFileByPath(basePath);
            if (baseDir == null || !baseDir.isDirectory()) {
                LOG.warn("Invalid base path for structures: " + basePath);
                return 0;
            }
            
            // Find all structure JSON/NBT files
            List<VirtualFile> structureFiles = findStructureFiles(baseDir);
            LOG.info("Found " + structureFiles.size() + " structure files");
            
            // Load each structure
            for (VirtualFile file : structureFiles) {
                StructureModel structure = loadStructure(file);
                if (structure != null) {
                    structures.put(structure.getId(), structure);
                }
            }
            
            return structures.size();
            
        } catch (Exception e) {
            LOG.error("Error loading structures", e);
            return 0;
        }
    }
    
    /**
     * Find all structure files in a directory
     * 
     * @param dir The directory to search
     * @return List of structure files
     */
    private List<VirtualFile> findStructureFiles(@NotNull VirtualFile dir) {
        List<VirtualFile> results = new ArrayList<>();
        findStructureFilesRecursive(dir, results);
        return results;
    }
    
    /**
     * Recursively find structure files
     * 
     * @param dir The directory to search
     * @param results List to add results to
     */
    private void findStructureFilesRecursive(@NotNull VirtualFile dir, @NotNull List<VirtualFile> results) {
        for (VirtualFile child : dir.getChildren()) {
            if (child.isDirectory()) {
                findStructureFilesRecursive(child, results);
            } else if (child.getName().endsWith(".nbt") || 
                      (child.getName().endsWith(".json") && isStructureFile(child))) {
                results.add(child);
            }
        }
    }
    
    /**
     * Check if a file is a structure file
     * 
     * @param file The file to check
     * @return True if it's a structure file
     */
    private boolean isStructureFile(@NotNull VirtualFile file) {
        try {
            String content = new String(file.contentsToByteArray());
            // Simple check for structure-specific JSON keys
            return content.contains("\"size\"") && 
                   content.contains("\"palette\"") && 
                   content.contains("\"blocks\"");
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * Load a structure from a file
     * 
     * @param file The file to load from
     * @return The loaded structure model, or null if loading failed
     */
    @Nullable
    private StructureModel loadStructure(@NotNull VirtualFile file) {
        try {
            String content = new String(file.contentsToByteArray());
            
            // Extract structure ID from filename
            String id = file.getNameWithoutExtension().replace('_', ':');
            if (!id.contains(":")) {
                id = "minecraft:" + id;
            }
            
            // Create a new structure
            StructureModel structure = new StructureModel(id, StructureModel.StructureType.CUSTOM);
            
            // Parse the JSON/NBT data
            // This is a very simplified parser - in reality, use a proper NBT/JSON parser
            
            // Extract size
            int[] size = extractSize(content);
            
            // Extract palette
            List<BlockState> palette = extractPalette(content);
            
            // Extract blocks
            List<Map<String, Object>> blocks = extractBlocks(content);
            
            // Create a single structure part for now
            StructurePart part = new StructurePart(id + "_main", 0, 0, 0, size[0], size[1], size[2]);
            
            // Add blocks to the part
            for (Map<String, Object> block : blocks) {
                int[] pos = (int[]) block.get("pos");
                int state = (int) block.get("state");
                
                if (state >= 0 && state < palette.size()) {
                    part.setBlock(pos[0], pos[1], pos[2], palette.get(state));
                }
            }
            
            // Add the part to the structure
            structure.addPart(part);
            
            return structure;
            
        } catch (Exception e) {
            LOG.error("Error loading structure from " + file.getPath(), e);
            return null;
        }
    }
    
    /**
     * Extract the size from structure JSON
     * 
     * @param json The JSON content
     * @return Array with [x, y, z] dimensions
     */
    private int[] extractSize(String json) {
        int[] size = new int[] { 1, 1, 1 }; // Default size
        
        // Extract size array
        Pattern sizePattern = Pattern.compile("\"size\"\\s*:\\s*\\[(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\]");
        Matcher matcher = sizePattern.matcher(json);
        
        if (matcher.find()) {
            try {
                size[0] = Integer.parseInt(matcher.group(1));
                size[1] = Integer.parseInt(matcher.group(2));
                size[2] = Integer.parseInt(matcher.group(3));
            } catch (NumberFormatException e) {
                LOG.warn("Error parsing structure size", e);
            }
        }
        
        return size;
    }
    
    /**
     * Extract the palette from structure JSON
     * 
     * @param json The JSON content
     * @return List of block states
     */
    private List<BlockState> extractPalette(String json) {
        List<BlockState> palette = new ArrayList<>();
        
        // Simplified pattern for extracting palette entries
        Pattern palettePattern = Pattern.compile("\"palette\"\\s*:\\s*\\[(.*?)\\]", Pattern.DOTALL);
        Matcher paletteMatcher = palettePattern.matcher(json);
        
        if (paletteMatcher.find()) {
            String paletteContent = paletteMatcher.group(1);
            
            // Extract each palette entry
            Pattern entryPattern = Pattern.compile("\\{(.*?)\\}", Pattern.DOTALL);
            Matcher entryMatcher = entryPattern.matcher(paletteContent);
            
            while (entryMatcher.find()) {
                String entry = entryMatcher.group(1);
                
                // Extract block name
                Pattern namePattern = Pattern.compile("\"Name\"\\s*:\\s*\"([^\"]+)\"");
                Matcher nameMatcher = namePattern.matcher(entry);
                
                if (nameMatcher.find()) {
                    String blockName = nameMatcher.group(1);
                    BlockState blockState = new BlockState(blockName);
                    
                    // Extract properties
                    Pattern propsPattern = Pattern.compile("\"Properties\"\\s*:\\s*\\{(.*?)\\}", Pattern.DOTALL);
                    Matcher propsMatcher = propsPattern.matcher(entry);
                    
                    if (propsMatcher.find()) {
                        String propsContent = propsMatcher.group(1);
                        
                        // Extract each property
                        Pattern propPattern = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"([^\"]+)\"");
                        Matcher propMatcher = propPattern.matcher(propsContent);
                        
                        while (propMatcher.find()) {
                            String key = propMatcher.group(1);
                            String value = propMatcher.group(2);
                            blockState.setProperty(key, value);
                        }
                    }
                    
                    palette.add(blockState);
                }
            }
        }
        
        // Ensure palette has at least one entry (air)
        if (palette.isEmpty()) {
            palette.add(new BlockState("minecraft:air"));
        }
        
        return palette;
    }
    
    /**
     * Extract blocks from structure JSON
     * 
     * @param json The JSON content
     * @return List of block entries
     */
    private List<Map<String, Object>> extractBlocks(String json) {
        List<Map<String, Object>> blocks = new ArrayList<>();
        
        // Simplified pattern for extracting blocks
        Pattern blocksPattern = Pattern.compile("\"blocks\"\\s*:\\s*\\[(.*?)\\]", Pattern.DOTALL);
        Matcher blocksMatcher = blocksPattern.matcher(json);
        
        if (blocksMatcher.find()) {
            String blocksContent = blocksMatcher.group(1);
            
            // Extract each block entry
            Pattern entryPattern = Pattern.compile("\\{(.*?)\\}", Pattern.DOTALL);
            Matcher entryMatcher = entryPattern.matcher(blocksContent);
            
            while (entryMatcher.find()) {
                String entry = entryMatcher.group(1);
                Map<String, Object> block = new HashMap<>();
                
                // Extract position
                Pattern posPattern = Pattern.compile("\"pos\"\\s*:\\s*\\[(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\]");
                Matcher posMatcher = posPattern.matcher(entry);
                
                if (posMatcher.find()) {
                    try {
                        int x = Integer.parseInt(posMatcher.group(1));
                        int y = Integer.parseInt(posMatcher.group(2));
                        int z = Integer.parseInt(posMatcher.group(3));
                        block.put("pos", new int[] { x, y, z });
                    } catch (NumberFormatException e) {
                        LOG.warn("Error parsing block position", e);
                        continue;
                    }
                } else {
                    continue; // Skip blocks without position
                }
                
                // Extract state
                Pattern statePattern = Pattern.compile("\"state\"\\s*:\\s*(\\d+)");
                Matcher stateMatcher = statePattern.matcher(entry);
                
                if (stateMatcher.find()) {
                    try {
                        int state = Integer.parseInt(stateMatcher.group(1));
                        block.put("state", state);
                    } catch (NumberFormatException e) {
                        LOG.warn("Error parsing block state", e);
                        continue;
                    }
                } else {
                    continue; // Skip blocks without state
                }
                
                blocks.add(block);
            }
        }
        
        return blocks;
    }
    
    /**
     * Save structures to the project
     * 
     * @param outputPath The output path to save to
     * @return Number of structures saved
     */
    public int saveStructures(@NotNull String outputPath) {
        int savedCount = 0;
        
        try {
            // Create output directory if it doesn't exist
            Path dirPath = Paths.get(outputPath);
            Files.createDirectories(dirPath);
            
            // Save each structure
            for (StructureModel structure : structures.values()) {
                String filename = structure.getId().replace(':', '_') + ".json";
                Path filePath = dirPath.resolve(filename);
                
                try {
                    String json = structure.toJson();
                    Files.writeString(filePath, json);
                    savedCount++;
                } catch (IOException e) {
                    LOG.error("Error saving structure " + structure.getId(), e);
                }
            }
            
            // Refresh the virtual file system
            LocalFileSystem.getInstance().refreshAndFindFileByPath(outputPath);
            
            return savedCount;
            
        } catch (Exception e) {
            LOG.error("Error saving structures", e);
            return savedCount;
        }
    }
    
    /**
     * Create a new structure
     * 
     * @param id The structure ID
     * @param type The structure type
     * @return The created structure
     */
    public StructureModel createStructure(@NotNull String id, @NotNull StructureModel.StructureType type) {
        StructureModel structure = new StructureModel(id, type);
        structures.put(id, structure);
        return structure;
    }
    
    /**
     * Delete a structure
     * 
     * @param id The structure ID
     * @return True if the structure was deleted, false if it wasn't found
     */
    public boolean deleteStructure(@NotNull String id) {
        return structures.remove(id) != null;
    }
    
    /**
     * Get all structures
     * 
     * @return Collection of all structures
     */
    public Collection<StructureModel> getAllStructures() {
        return structures.values();
    }
    
    /**
     * Get structures of a specific type
     * 
     * @param type The structure type
     * @return List of structures of the specified type
     */
    public List<StructureModel> getStructuresByType(@NotNull StructureModel.StructureType type) {
        List<StructureModel> result = new ArrayList<>();
        
        for (StructureModel structure : structures.values()) {
            if (structure.getType() == type) {
                result.add(structure);
            }
        }
        
        return result;
    }
    
    /**
     * Get a structure by ID
     * 
     * @param id The structure ID
     * @return The structure, or null if not found
     */
    @Nullable
    public StructureModel getStructure(@NotNull String id) {
        return structures.get(id);
    }
}