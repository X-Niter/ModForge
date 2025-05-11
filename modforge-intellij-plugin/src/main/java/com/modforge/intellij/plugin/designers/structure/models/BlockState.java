package com.modforge.intellij.plugin.designers.structure.models;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Model class for a Minecraft block state
 * Represents a block with its properties
 */
public class BlockState {
    private String block;
    private Map<String, String> properties;
    
    /**
     * Default constructor for serialization
     */
    public BlockState() {
        this.properties = new HashMap<>();
    }
    
    /**
     * Create a new block state with the given block ID
     * 
     * @param block The block ID (e.g., "minecraft:stone")
     */
    public BlockState(@NotNull String block) {
        this.block = block;
        this.properties = new HashMap<>();
    }
    
    /**
     * Create a new block state with the given block ID and properties
     * 
     * @param block The block ID
     * @param properties The block properties
     */
    public BlockState(@NotNull String block, @NotNull Map<String, String> properties) {
        this.block = block;
        this.properties = new HashMap<>(properties);
    }
    
    /**
     * Set a property value
     * 
     * @param key The property key
     * @param value The property value
     */
    public void setProperty(@NotNull String key, @NotNull String value) {
        properties.put(key, value);
    }
    
    /**
     * Get a property value
     * 
     * @param key The property key
     * @return The property value, or null if not found
     */
    public String getProperty(@NotNull String key) {
        return properties.get(key);
    }
    
    /**
     * Convert this block state to JSON
     * 
     * @return JSON string representation
     */
    public String toJson() {
        StringBuilder json = new StringBuilder();
        json.append("{\"Name\": \"").append(block).append("\"");
        
        if (!properties.isEmpty()) {
            json.append(", \"Properties\": {");
            
            int i = 0;
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                if (i > 0) {
                    json.append(", ");
                }
                
                json.append("\"").append(entry.getKey()).append("\": \"")
                    .append(entry.getValue()).append("\"");
                
                i++;
            }
            
            json.append("}");
        }
        
        json.append("}");
        return json.toString();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BlockState that = (BlockState) o;
        return Objects.equals(block, that.block) && Objects.equals(properties, that.properties);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(block, properties);
    }
    
    @Override
    public String toString() {
        return toJson();
    }
    
    // Getters and Setters
    
    public String getBlock() {
        return block;
    }
    
    public void setBlock(String block) {
        this.block = block;
    }
    
    public Map<String, String> getProperties() {
        return new HashMap<>(properties);
    }
    
    public void setProperties(Map<String, String> properties) {
        this.properties = new HashMap<>(properties);
    }
}