package com.modforge.intellij.plugin.designers.structure.models;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Model class for Minecraft structures
 * Represents a structure template that can be placed in the world
 */
public class StructureModel {
    private String id;
    private String name;
    private StructureType type;
    private final List<StructurePart> parts = new ArrayList<>();
    private Map<String, String> metadata = new HashMap<>();
    
    /**
     * Default constructor for serialization
     */
    public StructureModel() {
        // Default constructor required for serialization
    }
    
    /**
     * Create a new structure model with the given ID and type
     * 
     * @param id The structure ID
     * @param type The structure type
     */
    public StructureModel(@NotNull String id, @NotNull StructureType type) {
        this.id = id;
        this.type = type;
        this.name = id;
    }
    
    /**
     * Create a new structure model with the given ID, name, and type
     * 
     * @param id The structure ID
     * @param name The structure name
     * @param type The structure type
     */
    public StructureModel(@NotNull String id, @NotNull String name, @NotNull StructureType type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }
    
    /**
     * Add a part to this structure
     * 
     * @param part The part to add
     */
    public void addPart(@NotNull StructurePart part) {
        parts.add(part);
    }
    
    /**
     * Remove a part from this structure
     * 
     * @param part The part to remove
     * @return True if the part was removed, false if it wasn't present
     */
    public boolean removePart(@NotNull StructurePart part) {
        return parts.remove(part);
    }
    
    /**
     * Set a metadata value
     * 
     * @param key The metadata key
     * @param value The metadata value
     */
    public void setMetadata(@NotNull String key, @NotNull String value) {
        metadata.put(key, value);
    }
    
    /**
     * Get a metadata value
     * 
     * @param key The metadata key
     * @return The metadata value, or null if not found
     */
    @Nullable
    public String getMetadata(@NotNull String key) {
        return metadata.get(key);
    }
    
    /**
     * Convert this structure model to NBT JSON format
     * 
     * @return JSON string representation
     */
    public String toJson() {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        
        // Basic properties
        json.append("  \"size\": ").append(getSize()).append(",\n");
        json.append("  \"entities\": [],\n");
        
        // Palette
        json.append("  \"palette\": [\n");
        List<BlockState> palette = generatePalette();
        for (int i = 0; i < palette.size(); i++) {
            json.append("    ").append(palette.get(i).toJson());
            if (i < palette.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }
        json.append("  ],\n");
        
        // Blocks
        json.append("  \"blocks\": [\n");
        List<BlockEntry> blocks = generateBlocks(palette);
        for (int i = 0; i < blocks.size(); i++) {
            json.append("    ").append(blocks.get(i).toJson());
            if (i < blocks.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }
        json.append("  ]\n");
        
        json.append("}");
        return json.toString();
    }
    
    /**
     * Generate a palette from the structure parts
     * 
     * @return List of unique block states
     */
    private List<BlockState> generatePalette() {
        List<BlockState> palette = new ArrayList<>();
        
        // Add air as the first block state
        palette.add(new BlockState("minecraft:air"));
        
        // Add all unique block states from parts
        for (StructurePart part : parts) {
            for (int y = 0; y < part.getHeight(); y++) {
                for (int z = 0; z < part.getDepth(); z++) {
                    for (int x = 0; x < part.getWidth(); x++) {
                        BlockState block = part.getBlock(x, y, z);
                        if (block != null && !block.getBlock().equals("minecraft:air") && !palette.contains(block)) {
                            palette.add(block);
                        }
                    }
                }
            }
        }
        
        return palette;
    }
    
    /**
     * Generate blocks entries from the structure parts
     * 
     * @param palette The block state palette
     * @return List of block entries
     */
    private List<BlockEntry> generateBlocks(List<BlockState> palette) {
        List<BlockEntry> blocks = new ArrayList<>();
        
        // Add all blocks from parts
        for (StructurePart part : parts) {
            for (int y = 0; y < part.getHeight(); y++) {
                for (int z = 0; z < part.getDepth(); z++) {
                    for (int x = 0; x < part.getWidth(); x++) {
                        BlockState block = part.getBlock(x, y, z);
                        if (block != null && !block.getBlock().equals("minecraft:air")) {
                            int paletteIndex = palette.indexOf(block);
                            if (paletteIndex >= 0) {
                                blocks.add(new BlockEntry(
                                    part.getX() + x,
                                    part.getY() + y,
                                    part.getZ() + z,
                                    paletteIndex
                                ));
                            }
                        }
                    }
                }
            }
        }
        
        return blocks;
    }
    
    /**
     * Get the size of the structure
     * 
     * @return Array with [x, y, z] dimensions
     */
    private int[] getSize() {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        
        for (StructurePart part : parts) {
            minX = Math.min(minX, part.getX());
            minY = Math.min(minY, part.getY());
            minZ = Math.min(minZ, part.getZ());
            
            maxX = Math.max(maxX, part.getX() + part.getWidth() - 1);
            maxY = Math.max(maxY, part.getY() + part.getHeight() - 1);
            maxZ = Math.max(maxZ, part.getZ() + part.getDepth() - 1);
        }
        
        // If there are no parts, return a default size
        if (parts.isEmpty()) {
            return new int[] { 1, 1, 1 };
        }
        
        return new int[] {
            maxX - minX + 1,
            maxY - minY + 1,
            maxZ - minZ + 1
        };
    }
    
    /**
     * Block entry for the structure JSON
     */
    private static class BlockEntry {
        private final int x;
        private final int y;
        private final int z;
        private final int state;
        
        public BlockEntry(int x, int y, int z, int state) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.state = state;
        }
        
        public String toJson() {
            return "{\"pos\": [" + x + ", " + y + ", " + z + "], \"state\": " + state + "}";
        }
    }
    
    // Getters and Setters
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public StructureType getType() {
        return type;
    }
    
    public void setType(StructureType type) {
        this.type = type;
    }
    
    public List<StructurePart> getParts() {
        return new ArrayList<>(parts);
    }
    
    public Map<String, String> getMetadata() {
        return new HashMap<>(metadata);
    }
    
    public void setMetadata(Map<String, String> metadata) {
        this.metadata = new HashMap<>(metadata);
    }
    
    /**
     * Enum for different structure types
     */
    public enum StructureType {
        BUILDING,
        RUIN,
        DUNGEON,
        VILLAGE,
        TEMPLE,
        MONUMENT,
        CUSTOM
    }
}