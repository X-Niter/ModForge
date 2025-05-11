package com.modforge.intellij.plugin.designers.recipe.models;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Model class for recipe items
 * Represents an item in a recipe with optional NBT data and count
 */
public class RecipeItem {
    private String item;
    private int count = 1;
    private Map<String, String> nbt = new HashMap<>();
    private boolean useTag = false;
    
    /**
     * Default constructor for serialization
     */
    public RecipeItem() {
        // Default constructor required for serialization
    }
    
    /**
     * Create a new recipe item with the given item ID
     * 
     * @param item The item ID (e.g., "minecraft:stone")
     */
    public RecipeItem(@NotNull String item) {
        this.item = item;
    }
    
    /**
     * Create a new recipe item with the given item ID and count
     * 
     * @param item The item ID (e.g., "minecraft:stone")
     * @param count The item count
     */
    public RecipeItem(@NotNull String item, int count) {
        this.item = item;
        this.count = Math.max(1, count);
    }
    
    /**
     * Add an NBT tag to this item
     * 
     * @param key The NBT key
     * @param value The NBT value
     */
    public void addNbt(@NotNull String key, @NotNull String value) {
        nbt.put(key, value);
    }
    
    /**
     * Remove an NBT tag from this item
     * 
     * @param key The NBT key to remove
     * @return The previous value, or null if the key wasn't present
     */
    @Nullable
    public String removeNbt(@NotNull String key) {
        return nbt.remove(key);
    }
    
    /**
     * Convert this item to JSON
     * 
     * @return JSON string representation
     */
    public String toJson() {
        StringBuilder json = new StringBuilder();
        
        // Start object
        json.append("{");
        
        // Item or tag
        String itemType = useTag ? "tag" : "item";
        json.append("\"").append(itemType).append("\": \"").append(item).append("\"");
        
        // Count (if more than 1)
        if (count > 1) {
            json.append(", \"count\": ").append(count);
        }
        
        // NBT (if present)
        if (!nbt.isEmpty()) {
            json.append(", \"nbt\": {");
            int i = 0;
            for (Map.Entry<String, String> entry : nbt.entrySet()) {
                json.append("\"").append(entry.getKey()).append("\": ");
                
                // Try to determine if the value is a string or a number/boolean
                String value = entry.getValue();
                if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false") ||
                    value.matches("^-?\\d+(\\.\\d+)?$")) {
                    // Numeric or boolean value
                    json.append(value);
                } else {
                    // String value
                    json.append("\"").append(value).append("\"");
                }
                
                if (i < nbt.size() - 1) {
                    json.append(", ");
                }
                i++;
            }
            json.append("}");
        }
        
        // End object
        json.append("}");
        
        return json.toString();
    }
    
    /**
     * Check if this recipe item is valid
     * 
     * @return True if the item is valid
     */
    public boolean isValid() {
        return item != null && !item.isEmpty() && !item.equals("minecraft:air");
    }
    
    // Getters and Setters
    
    public String getItem() {
        return item;
    }
    
    public void setItem(String item) {
        this.item = item;
    }
    
    public int getCount() {
        return count;
    }
    
    public void setCount(int count) {
        this.count = Math.max(1, count);
    }
    
    public Map<String, String> getNbt() {
        return new HashMap<>(nbt);
    }
    
    public void setNbt(Map<String, String> nbt) {
        this.nbt.clear();
        if (nbt != null) {
            this.nbt.putAll(nbt);
        }
    }
    
    public boolean isUseTag() {
        return useTag;
    }
    
    public void setUseTag(boolean useTag) {
        this.useTag = useTag;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RecipeItem that = (RecipeItem) o;
        return count == that.count && useTag == that.useTag && Objects.equals(item, that.item) && Objects.equals(nbt, that.nbt);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(item, count, nbt, useTag);
    }
    
    @Override
    public String toString() {
        return useTag ? "#" + item + " x" + count : item + " x" + count;
    }
}