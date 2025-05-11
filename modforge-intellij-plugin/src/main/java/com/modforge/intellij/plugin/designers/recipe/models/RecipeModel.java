package com.modforge.intellij.plugin.designers.recipe.models;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Model class for Minecraft recipes
 * Represents different types of crafting and processing recipes
 */
public class RecipeModel {
    // Recipe identification
    private String id;
    private RecipeType type;
    
    // Common fields
    private String group = "";
    private RecipeItem result;
    private float experience = 0.0f;
    private int cookingTime = 200;
    
    // Shaped crafting fields
    private List<String> pattern = new ArrayList<>();
    private Map<Character, RecipeItem> key = new HashMap<>();
    
    // Shapeless crafting fields
    private List<RecipeItem> ingredients = new ArrayList<>();
    
    /**
     * Default constructor for serialization
     */
    public RecipeModel() {
        // Default constructor required for serialization
    }
    
    /**
     * Create a new recipe with the given ID and type
     * 
     * @param id The recipe ID
     * @param type The recipe type
     */
    public RecipeModel(@NotNull String id, @NotNull RecipeType type) {
        this.id = id;
        this.type = type;
        
        // Initialize with default values based on type
        initializeDefaults();
    }
    
    /**
     * Initialize default values based on recipe type
     */
    private void initializeDefaults() {
        switch (type) {
            case CRAFTING_SHAPED:
                // Default 3x3 empty pattern
                pattern.add("   ");
                pattern.add("   ");
                pattern.add("   ");
                break;
                
            case SMELTING:
            case BLASTING:
            case SMOKING:
            case CAMPFIRE_COOKING:
                // Default cooking times based on type
                if (type == RecipeType.BLASTING) {
                    cookingTime = 100;
                } else if (type == RecipeType.SMOKING) {
                    cookingTime = 100;
                } else if (type == RecipeType.CAMPFIRE_COOKING) {
                    cookingTime = 600;
                }
                break;
        }
        
        // Default result
        result = new RecipeItem("minecraft:air");
    }
    
    /**
     * Set a shaped recipe pattern
     * 
     * @param pattern List of strings representing the pattern
     */
    public void setPattern(@NotNull List<String> pattern) {
        if (type != RecipeType.CRAFTING_SHAPED) {
            throw new IllegalStateException("Pattern is only valid for shaped crafting recipes");
        }
        
        this.pattern.clear();
        this.pattern.addAll(pattern);
    }
    
    /**
     * Add a key mapping for shaped recipes
     * 
     * @param symbol The symbol in the pattern
     * @param item The item for this symbol
     */
    public void addKey(char symbol, @NotNull RecipeItem item) {
        if (type != RecipeType.CRAFTING_SHAPED) {
            throw new IllegalStateException("Keys are only valid for shaped crafting recipes");
        }
        
        key.put(symbol, item);
    }
    
    /**
     * Remove a key mapping for shaped recipes
     * 
     * @param symbol The symbol to remove
     */
    public void removeKey(char symbol) {
        key.remove(symbol);
    }
    
    /**
     * Add an ingredient for shapeless recipes
     * 
     * @param ingredient The ingredient to add
     */
    public void addIngredient(@NotNull RecipeItem ingredient) {
        if (type != RecipeType.CRAFTING_SHAPELESS && 
            type != RecipeType.SMELTING && 
            type != RecipeType.BLASTING && 
            type != RecipeType.SMOKING && 
            type != RecipeType.CAMPFIRE_COOKING) {
            throw new IllegalStateException("Ingredients are only valid for shapeless and cooking recipes");
        }
        
        ingredients.add(ingredient);
    }
    
    /**
     * Remove an ingredient for shapeless recipes
     * 
     * @param ingredient The ingredient to remove
     * @return True if the ingredient was removed, false if it wasn't found
     */
    public boolean removeIngredient(@NotNull RecipeItem ingredient) {
        return ingredients.remove(ingredient);
    }
    
    /**
     * Convert this recipe model to JSON
     * 
     * @return JSON string representation
     */
    public String toJson() {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        
        // Common fields
        json.append("  \"type\": \"").append(getTypeString()).append("\",\n");
        
        if (!group.isEmpty()) {
            json.append("  \"group\": \"").append(group).append("\",\n");
        }
        
        // Type-specific fields
        switch (type) {
            case CRAFTING_SHAPED:
                appendShapedRecipeJson(json);
                break;
                
            case CRAFTING_SHAPELESS:
                appendShapelessRecipeJson(json);
                break;
                
            case SMELTING:
            case BLASTING:
            case SMOKING:
            case CAMPFIRE_COOKING:
                appendCookingRecipeJson(json);
                break;
                
            case STONECUTTING:
                appendStonecuttingRecipeJson(json);
                break;
                
            case SMITHING:
                appendSmithingRecipeJson(json);
                break;
        }
        
        json.append("}");
        return json.toString();
    }
    
    /**
     * Append shaped recipe JSON
     * 
     * @param json The JSON builder to append to
     */
    private void appendShapedRecipeJson(StringBuilder json) {
        // Pattern
        json.append("  \"pattern\": [\n");
        for (int i = 0; i < pattern.size(); i++) {
            json.append("    \"").append(pattern.get(i)).append("\"");
            if (i < pattern.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }
        json.append("  ],\n");
        
        // Key
        json.append("  \"key\": {\n");
        int keyCount = 0;
        for (Map.Entry<Character, RecipeItem> entry : key.entrySet()) {
            json.append("    \"").append(entry.getKey()).append("\": ");
            json.append(entry.getValue().toJson());
            if (keyCount < key.size() - 1) {
                json.append(",");
            }
            json.append("\n");
            keyCount++;
        }
        json.append("  },\n");
        
        // Result
        json.append("  \"result\": ");
        json.append(result.toJson());
        json.append("\n");
    }
    
    /**
     * Append shapeless recipe JSON
     * 
     * @param json The JSON builder to append to
     */
    private void appendShapelessRecipeJson(StringBuilder json) {
        // Ingredients
        json.append("  \"ingredients\": [\n");
        for (int i = 0; i < ingredients.size(); i++) {
            json.append("    ");
            json.append(ingredients.get(i).toJson());
            if (i < ingredients.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }
        json.append("  ],\n");
        
        // Result
        json.append("  \"result\": ");
        json.append(result.toJson());
        json.append("\n");
    }
    
    /**
     * Append cooking recipe JSON
     * 
     * @param json The JSON builder to append to
     */
    private void appendCookingRecipeJson(StringBuilder json) {
        // Ingredient (cooking recipes have only one ingredient)
        json.append("  \"ingredient\": ");
        if (!ingredients.isEmpty()) {
            json.append(ingredients.get(0).toJson());
        } else {
            json.append("{\"item\": \"minecraft:air\"}");
        }
        json.append(",\n");
        
        // Result
        json.append("  \"result\": \"").append(result.getItem()).append("\",\n");
        
        // Experience
        json.append("  \"experience\": ").append(experience).append(",\n");
        
        // Cooking time
        String timeField = type == RecipeType.SMELTING ? "cookingtime" : "cookingTime";
        json.append("  \"").append(timeField).append("\": ").append(cookingTime).append("\n");
    }
    
    /**
     * Append stonecutting recipe JSON
     * 
     * @param json The JSON builder to append to
     */
    private void appendStonecuttingRecipeJson(StringBuilder json) {
        // Ingredient
        json.append("  \"ingredient\": ");
        if (!ingredients.isEmpty()) {
            json.append(ingredients.get(0).toJson());
        } else {
            json.append("{\"item\": \"minecraft:air\"}");
        }
        json.append(",\n");
        
        // Result
        json.append("  \"result\": \"").append(result.getItem()).append("\",\n");
        
        // Count
        json.append("  \"count\": ").append(result.getCount()).append("\n");
    }
    
    /**
     * Append smithing recipe JSON
     * 
     * @param json The JSON builder to append to
     */
    private void appendSmithingRecipeJson(StringBuilder json) {
        // Base
        json.append("  \"base\": ");
        if (ingredients.size() > 0) {
            json.append(ingredients.get(0).toJson());
        } else {
            json.append("{\"item\": \"minecraft:air\"}");
        }
        json.append(",\n");
        
        // Addition
        json.append("  \"addition\": ");
        if (ingredients.size() > 1) {
            json.append(ingredients.get(1).toJson());
        } else {
            json.append("{\"item\": \"minecraft:air\"}");
        }
        json.append(",\n");
        
        // Result
        json.append("  \"result\": ");
        json.append(result.toJson());
        json.append("\n");
    }
    
    /**
     * Get the type string for JSON serialization
     * 
     * @return The type string
     */
    private String getTypeString() {
        switch (type) {
            case CRAFTING_SHAPED:
                return "minecraft:crafting_shaped";
            case CRAFTING_SHAPELESS:
                return "minecraft:crafting_shapeless";
            case SMELTING:
                return "minecraft:smelting";
            case BLASTING:
                return "minecraft:blasting";
            case SMOKING:
                return "minecraft:smoking";
            case CAMPFIRE_COOKING:
                return "minecraft:campfire_cooking";
            case STONECUTTING:
                return "minecraft:stonecutting";
            case SMITHING:
                return "minecraft:smithing";
            default:
                return "minecraft:crafting_shaped";
        }
    }
    
    // Getters and Setters
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public RecipeType getType() {
        return type;
    }
    
    public void setType(RecipeType type) {
        this.type = type;
        initializeDefaults();
    }
    
    public String getGroup() {
        return group;
    }
    
    public void setGroup(String group) {
        this.group = group;
    }
    
    public RecipeItem getResult() {
        return result;
    }
    
    public void setResult(RecipeItem result) {
        this.result = result;
    }
    
    public float getExperience() {
        return experience;
    }
    
    public void setExperience(float experience) {
        this.experience = experience;
    }
    
    public int getCookingTime() {
        return cookingTime;
    }
    
    public void setCookingTime(int cookingTime) {
        this.cookingTime = cookingTime;
    }
    
    public List<String> getPattern() {
        return new ArrayList<>(pattern);
    }
    
    public Map<Character, RecipeItem> getKey() {
        return new HashMap<>(key);
    }
    
    public List<RecipeItem> getIngredients() {
        return new ArrayList<>(ingredients);
    }
    
    /**
     * Enum for different recipe types
     */
    public enum RecipeType {
        CRAFTING_SHAPED,
        CRAFTING_SHAPELESS,
        SMELTING,
        BLASTING,
        SMOKING,
        CAMPFIRE_COOKING,
        STONECUTTING,
        SMITHING
    }
}