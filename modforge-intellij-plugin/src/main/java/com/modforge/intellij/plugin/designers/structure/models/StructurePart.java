package com.modforge.intellij.plugin.designers.structure.models;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Model class for a part of a structure
 * Represents a section of blocks in a structure
 */
public class StructurePart {
    private String id;
    private int x;
    private int y;
    private int z;
    private int width;
    private int height;
    private int depth;
    private BlockState[][][] blocks;
    private Map<String, String> metadata = new HashMap<>();
    
    /**
     * Default constructor for serialization
     */
    public StructurePart() {
        // Default constructor required for serialization
    }
    
    /**
     * Create a new structure part
     * 
     * @param id The part ID
     * @param x The X position
     * @param y The Y position
     * @param z The Z position
     * @param width The width (X dimension)
     * @param height The height (Y dimension)
     * @param depth The depth (Z dimension)
     */
    public StructurePart(@NotNull String id, int x, int y, int z, int width, int height, int depth) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.z = z;
        this.width = width;
        this.height = height;
        this.depth = depth;
        
        // Initialize blocks with air
        this.blocks = new BlockState[height][depth][width];
        
        // Fill with air blocks
        BlockState air = new BlockState("minecraft:air");
        for (int yPos = 0; yPos < height; yPos++) {
            for (int zPos = 0; zPos < depth; zPos++) {
                for (int xPos = 0; xPos < width; xPos++) {
                    this.blocks[yPos][zPos][xPos] = air;
                }
            }
        }
    }
    
    /**
     * Set a block in the structure part
     * 
     * @param x The X position
     * @param y The Y position
     * @param z The Z position
     * @param block The block state
     */
    public void setBlock(int x, int y, int z, @NotNull BlockState block) {
        if (x < 0 || x >= width || y < 0 || y >= height || z < 0 || z >= depth) {
            throw new IllegalArgumentException("Block position out of bounds");
        }
        
        blocks[y][z][x] = block;
    }
    
    /**
     * Get a block from the structure part
     * 
     * @param x The X position
     * @param y The Y position
     * @param z The Z position
     * @return The block state, or null if the position is out of bounds
     */
    @Nullable
    public BlockState getBlock(int x, int y, int z) {
        if (x < 0 || x >= width || y < 0 || y >= height || z < 0 || z >= depth) {
            return null;
        }
        
        return blocks[y][z][x];
    }
    
    /**
     * Fill a region with a block
     * 
     * @param x1 The starting X position
     * @param y1 The starting Y position
     * @param z1 The starting Z position
     * @param x2 The ending X position
     * @param y2 The ending Y position
     * @param z2 The ending Z position
     * @param block The block state
     */
    public void fillRegion(int x1, int y1, int z1, int x2, int y2, int z2, @NotNull BlockState block) {
        // Ensure coordinates are in bounds
        x1 = Math.max(0, Math.min(x1, width - 1));
        y1 = Math.max(0, Math.min(y1, height - 1));
        z1 = Math.max(0, Math.min(z1, depth - 1));
        x2 = Math.max(0, Math.min(x2, width - 1));
        y2 = Math.max(0, Math.min(y2, height - 1));
        z2 = Math.max(0, Math.min(z2, depth - 1));
        
        // Ensure x1 <= x2, y1 <= y2, z1 <= z2
        if (x1 > x2) {
            int temp = x1;
            x1 = x2;
            x2 = temp;
        }
        
        if (y1 > y2) {
            int temp = y1;
            y1 = y2;
            y2 = temp;
        }
        
        if (z1 > z2) {
            int temp = z1;
            z1 = z2;
            z2 = temp;
        }
        
        // Fill the region
        for (int y = y1; y <= y2; y++) {
            for (int z = z1; z <= z2; z++) {
                for (int x = x1; x <= x2; x++) {
                    blocks[y][z][x] = block;
                }
            }
        }
    }
    
    /**
     * Fill the edges of a region with a block (hollow box)
     * 
     * @param x1 The starting X position
     * @param y1 The starting Y position
     * @param z1 The starting Z position
     * @param x2 The ending X position
     * @param y2 The ending Y position
     * @param z2 The ending Z position
     * @param block The block state
     */
    public void fillEdges(int x1, int y1, int z1, int x2, int y2, int z2, @NotNull BlockState block) {
        // Ensure coordinates are in bounds
        x1 = Math.max(0, Math.min(x1, width - 1));
        y1 = Math.max(0, Math.min(y1, height - 1));
        z1 = Math.max(0, Math.min(z1, depth - 1));
        x2 = Math.max(0, Math.min(x2, width - 1));
        y2 = Math.max(0, Math.min(y2, height - 1));
        z2 = Math.max(0, Math.min(z2, depth - 1));
        
        // Ensure x1 <= x2, y1 <= y2, z1 <= z2
        if (x1 > x2) {
            int temp = x1;
            x1 = x2;
            x2 = temp;
        }
        
        if (y1 > y2) {
            int temp = y1;
            y1 = y2;
            y2 = temp;
        }
        
        if (z1 > z2) {
            int temp = z1;
            z1 = z2;
            z2 = temp;
        }
        
        // Fill the edges
        for (int y = y1; y <= y2; y++) {
            for (int z = z1; z <= z2; z++) {
                for (int x = x1; x <= x2; x++) {
                    // Only set blocks that are on the edges of the region
                    if (x == x1 || x == x2 || y == y1 || y == y2 || z == z1 || z == z2) {
                        blocks[y][z][x] = block;
                    }
                }
            }
        }
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
    
    // Getters and Setters
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public int getX() {
        return x;
    }
    
    public void setX(int x) {
        this.x = x;
    }
    
    public int getY() {
        return y;
    }
    
    public void setY(int y) {
        this.y = y;
    }
    
    public int getZ() {
        return z;
    }
    
    public void setZ(int z) {
        this.z = z;
    }
    
    public int getWidth() {
        return width;
    }
    
    public int getHeight() {
        return height;
    }
    
    public int getDepth() {
        return depth;
    }
    
    public Map<String, String> getMetadata() {
        return new HashMap<>(metadata);
    }
    
    public void setMetadata(Map<String, String> metadata) {
        this.metadata = new HashMap<>(metadata);
    }
}