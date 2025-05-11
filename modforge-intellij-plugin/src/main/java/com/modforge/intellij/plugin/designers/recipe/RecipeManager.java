package com.modforge.intellij.plugin.designers.recipe;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.modforge.intellij.plugin.designers.recipe.models.RecipeItem;
import com.modforge.intellij.plugin.designers.recipe.models.RecipeModel;
import com.modforge.intellij.plugin.designers.recipe.models.RecipeModel.RecipeType;
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
 * Manager for working with Minecraft recipes
 * Handles loading, saving, and editing recipes
 */
@Service
public final class RecipeManager {
    private static final Logger LOG = Logger.getInstance(RecipeManager.class);
    
    private final Project project;
    private final Map<String, RecipeModel> recipes = new ConcurrentHashMap<>();
    
    public RecipeManager(Project project) {
        this.project = project;
    }
    
    /**
     * Load recipes from the project
     * 
     * @param basePath The base path to load from
     * @return Number of recipes loaded
     */
    public int loadRecipes(@NotNull String basePath) {
        recipes.clear();
        
        try {
            VirtualFile baseDir = LocalFileSystem.getInstance().findFileByPath(basePath);
            if (baseDir == null || !baseDir.isDirectory()) {
                LOG.warn("Invalid base path for recipes: " + basePath);
                return 0;
            }
            
            // Find all recipe JSON files
            List<VirtualFile> recipeFiles = findRecipeFiles(baseDir);
            LOG.info("Found " + recipeFiles.size() + " recipe files");
            
            // Load each recipe
            for (VirtualFile file : recipeFiles) {
                RecipeModel recipe = loadRecipe(file);
                if (recipe != null) {
                    recipes.put(recipe.getId(), recipe);
                }
            }
            
            return recipes.size();
            
        } catch (Exception e) {
            LOG.error("Error loading recipes", e);
            return 0;
        }
    }
    
    /**
     * Find all recipe JSON files in a directory
     * 
     * @param dir The directory to search
     * @return List of recipe files
     */
    private List<VirtualFile> findRecipeFiles(@NotNull VirtualFile dir) {
        List<VirtualFile> results = new ArrayList<>();
        findRecipeFilesRecursive(dir, results);
        return results;
    }
    
    /**
     * Recursively find recipe JSON files
     * 
     * @param dir The directory to search
     * @param results List to add results to
     */
    private void findRecipeFilesRecursive(@NotNull VirtualFile dir, @NotNull List<VirtualFile> results) {
        for (VirtualFile child : dir.getChildren()) {
            if (child.isDirectory()) {
                findRecipeFilesRecursive(child, results);
            } else if (child.getName().endsWith(".json")) {
                // Check if it's a recipe file
                if (isRecipeFile(child)) {
                    results.add(child);
                }
            }
        }
    }
    
    /**
     * Check if a file is a recipe file
     * 
     * @param file The file to check
     * @return True if it's a recipe file
     */
    private boolean isRecipeFile(@NotNull VirtualFile file) {
        try {
            String content = new String(file.contentsToByteArray());
            // Check for recipe-specific JSON keys
            return content.contains("\"type\"") && 
                   (content.contains("\"result\"") || 
                    content.contains("\"pattern\"") || 
                    content.contains("\"ingredients\"") || 
                    content.contains("\"ingredient\""));
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * Load a recipe from a file
     * 
     * @param file The file to load from
     * @return The loaded recipe model, or null if loading failed
     */
    @Nullable
    private RecipeModel loadRecipe(@NotNull VirtualFile file) {
        try {
            String content = new String(file.contentsToByteArray());
            
            // Extract recipe ID from filename
            String id = file.getNameWithoutExtension().replace('_', ':');
            if (!id.contains(":")) {
                id = "minecraft:" + id;
            }
            
            // Extract recipe type from JSON
            RecipeType type = extractRecipeType(content);
            if (type == null) {
                LOG.warn("Could not determine recipe type for " + file.getPath());
                return null;
            }
            
            RecipeModel recipe = new RecipeModel(id, type);
            
            // Extract fields based on recipe type
            switch (type) {
                case CRAFTING_SHAPED:
                    extractShapedRecipeFields(content, recipe);
                    break;
                    
                case CRAFTING_SHAPELESS:
                    extractShapelessRecipeFields(content, recipe);
                    break;
                    
                case SMELTING:
                case BLASTING:
                case SMOKING:
                case CAMPFIRE_COOKING:
                    extractCookingRecipeFields(content, recipe);
                    break;
                    
                case STONECUTTING:
                    extractStonecuttingRecipeFields(content, recipe);
                    break;
                    
                case SMITHING:
                    extractSmithingRecipeFields(content, recipe);
                    break;
            }
            
            return recipe;
            
        } catch (Exception e) {
            LOG.error("Error loading recipe from " + file.getPath(), e);
            return null;
        }
    }
    
    /**
     * Extract the recipe type from JSON
     * 
     * @param json The JSON content
     * @return The recipe type, or null if not found
     */
    @Nullable
    private RecipeType extractRecipeType(@NotNull String json) {
        // Extract the type string
        Pattern typePattern = Pattern.compile("\"type\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = typePattern.matcher(json);
        
        if (matcher.find()) {
            String typeString = matcher.group(1);
            
            // Map the type string to a RecipeType
            if (typeString.endsWith("crafting_shaped")) {
                return RecipeType.CRAFTING_SHAPED;
            } else if (typeString.endsWith("crafting_shapeless")) {
                return RecipeType.CRAFTING_SHAPELESS;
            } else if (typeString.endsWith("smelting")) {
                return RecipeType.SMELTING;
            } else if (typeString.endsWith("blasting")) {
                return RecipeType.BLASTING;
            } else if (typeString.endsWith("smoking")) {
                return RecipeType.SMOKING;
            } else if (typeString.endsWith("campfire_cooking")) {
                return RecipeType.CAMPFIRE_COOKING;
            } else if (typeString.endsWith("stonecutting")) {
                return RecipeType.STONECUTTING;
            } else if (typeString.endsWith("smithing")) {
                return RecipeType.SMITHING;
            }
        }
        
        return null;
    }
    
    /**
     * Extract fields for a shaped crafting recipe
     * 
     * @param json The JSON content
     * @param recipe The recipe model to populate
     */
    private void extractShapedRecipeFields(@NotNull String json, @NotNull RecipeModel recipe) {
        // Extract pattern
        List<String> pattern = extractStringArray(json, "pattern");
        if (!pattern.isEmpty()) {
            recipe.setPattern(pattern);
        }
        
        // Extract result
        RecipeItem result = extractRecipeItem(json, "result");
        if (result != null) {
            recipe.setResult(result);
        }
        
        // Extract key mappings
        String keySection = extractSection(json, "key");
        if (keySection != null) {
            Map<Character, RecipeItem> keyMap = extractKeyMap(keySection);
            for (Map.Entry<Character, RecipeItem> entry : keyMap.entrySet()) {
                recipe.addKey(entry.getKey(), entry.getValue());
            }
        }
        
        // Extract group
        String group = extractString(json, "group");
        if (group != null) {
            recipe.setGroup(group);
        }
    }
    
    /**
     * Extract fields for a shapeless crafting recipe
     * 
     * @param json The JSON content
     * @param recipe The recipe model to populate
     */
    private void extractShapelessRecipeFields(@NotNull String json, @NotNull RecipeModel recipe) {
        // Extract ingredients
        String ingredientsSection = extractSection(json, "ingredients");
        if (ingredientsSection != null) {
            List<RecipeItem> ingredients = extractIngredientsList(ingredientsSection);
            for (RecipeItem ingredient : ingredients) {
                recipe.addIngredient(ingredient);
            }
        }
        
        // Extract result
        RecipeItem result = extractRecipeItem(json, "result");
        if (result != null) {
            recipe.setResult(result);
        }
        
        // Extract group
        String group = extractString(json, "group");
        if (group != null) {
            recipe.setGroup(group);
        }
    }
    
    /**
     * Extract fields for a cooking recipe
     * 
     * @param json The JSON content
     * @param recipe The recipe model to populate
     */
    private void extractCookingRecipeFields(@NotNull String json, @NotNull RecipeModel recipe) {
        // Extract ingredient
        RecipeItem ingredient = extractRecipeItem(json, "ingredient");
        if (ingredient != null) {
            recipe.addIngredient(ingredient);
        }
        
        // Extract result (which is just a string for cooking recipes)
        String resultItem = extractString(json, "result");
        if (resultItem != null) {
            recipe.setResult(new RecipeItem(resultItem));
        }
        
        // Extract experience
        Float experience = extractFloat(json, "experience");
        if (experience != null) {
            recipe.setExperience(experience);
        }
        
        // Extract cooking time
        Integer cookingTime = extractInteger(json, "cookingtime");
        if (cookingTime == null) {
            cookingTime = extractInteger(json, "cookingTime");
        }
        if (cookingTime != null) {
            recipe.setCookingTime(cookingTime);
        }
        
        // Extract group
        String group = extractString(json, "group");
        if (group != null) {
            recipe.setGroup(group);
        }
    }
    
    /**
     * Extract fields for a stonecutting recipe
     * 
     * @param json The JSON content
     * @param recipe The recipe model to populate
     */
    private void extractStonecuttingRecipeFields(@NotNull String json, @NotNull RecipeModel recipe) {
        // Extract ingredient
        RecipeItem ingredient = extractRecipeItem(json, "ingredient");
        if (ingredient != null) {
            recipe.addIngredient(ingredient);
        }
        
        // Extract result (which is just a string for stonecutting recipes)
        String resultItem = extractString(json, "result");
        if (resultItem != null) {
            recipe.setResult(new RecipeItem(resultItem));
        }
        
        // Extract count
        Integer count = extractInteger(json, "count");
        if (count != null && count > 0) {
            recipe.getResult().setCount(count);
        }
        
        // Extract group
        String group = extractString(json, "group");
        if (group != null) {
            recipe.setGroup(group);
        }
    }
    
    /**
     * Extract fields for a smithing recipe
     * 
     * @param json The JSON content
     * @param recipe The recipe model to populate
     */
    private void extractSmithingRecipeFields(@NotNull String json, @NotNull RecipeModel recipe) {
        // Extract base
        RecipeItem base = extractRecipeItem(json, "base");
        if (base != null) {
            recipe.addIngredient(base);
        }
        
        // Extract addition
        RecipeItem addition = extractRecipeItem(json, "addition");
        if (addition != null) {
            recipe.addIngredient(addition);
        }
        
        // Extract result
        RecipeItem result = extractRecipeItem(json, "result");
        if (result != null) {
            recipe.setResult(result);
        }
    }
    
    /**
     * Extract a string array from JSON
     * 
     * @param json The JSON content
     * @param key The key to extract
     * @return The extracted string array
     */
    private List<String> extractStringArray(@NotNull String json, @NotNull String key) {
        List<String> result = new ArrayList<>();
        
        // Find the array
        Pattern arrayPattern = Pattern.compile("\"" + key + "\"\\s*:\\s*\\[(.*?)\\]", Pattern.DOTALL);
        Matcher matcher = arrayPattern.matcher(json);
        
        if (matcher.find()) {
            String arrayContent = matcher.group(1);
            
            // Extract each string in the array
            Pattern stringPattern = Pattern.compile("\"([^\"]+)\"");
            Matcher stringMatcher = stringPattern.matcher(arrayContent);
            
            while (stringMatcher.find()) {
                result.add(stringMatcher.group(1));
            }
        }
        
        return result;
    }
    
    /**
     * Extract a recipe item from JSON
     * 
     * @param json The JSON content
     * @param key The key to extract
     * @return The extracted recipe item, or null if not found
     */
    @Nullable
    private RecipeItem extractRecipeItem(@NotNull String json, @NotNull String key) {
        // Find the item object
        Pattern itemPattern = Pattern.compile("\"" + key + "\"\\s*:\\s*(\\{.*?\\}|\"[^\"]+\")", Pattern.DOTALL);
        Matcher matcher = itemPattern.matcher(json);
        
        if (matcher.find()) {
            String itemValue = matcher.group(1);
            
            if (itemValue.startsWith("\"")) {
                // Simple string value (just the item ID)
                String itemId = itemValue.substring(1, itemValue.length() - 1);
                return new RecipeItem(itemId);
            } else {
                // Object value
                RecipeItem item = new RecipeItem("minecraft:air");
                
                // Extract item or tag
                String itemId = extractString(itemValue, "item");
                if (itemId != null) {
                    item.setItem(itemId);
                } else {
                    String tagId = extractString(itemValue, "tag");
                    if (tagId != null) {
                        item.setItem(tagId);
                        item.setUseTag(true);
                    }
                }
                
                // Extract count
                Integer count = extractInteger(itemValue, "count");
                if (count != null && count > 0) {
                    item.setCount(count);
                }
                
                // TODO: Extract NBT data
                
                return item;
            }
        }
        
        return null;
    }
    
    /**
     * Extract a section from JSON
     * 
     * @param json The JSON content
     * @param key The key to extract
     * @return The extracted section, or null if not found
     */
    @Nullable
    private String extractSection(@NotNull String json, @NotNull String key) {
        // Find the section
        Pattern sectionPattern = Pattern.compile("\"" + key + "\"\\s*:\\s*(\\{.*?\\}|\\[.*?\\])", Pattern.DOTALL);
        Matcher matcher = sectionPattern.matcher(json);
        
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return null;
    }
    
    /**
     * Extract a key map from a key section
     * 
     * @param keySection The key section
     * @return The extracted key map
     */
    private Map<Character, RecipeItem> extractKeyMap(@NotNull String keySection) {
        Map<Character, RecipeItem> keyMap = new HashMap<>();
        
        // Extract each key-value pair
        Pattern keyPattern = Pattern.compile("\"(.)\"\\s*:\\s*(\\{.*?\\})", Pattern.DOTALL);
        Matcher matcher = keyPattern.matcher(keySection);
        
        while (matcher.find()) {
            char key = matcher.group(1).charAt(0);
            String itemJson = matcher.group(2);
            
            // Parse the item
            RecipeItem item = new RecipeItem("minecraft:air");
            
            // Extract item or tag
            String itemId = extractString(itemJson, "item");
            if (itemId != null) {
                item.setItem(itemId);
            } else {
                String tagId = extractString(itemJson, "tag");
                if (tagId != null) {
                    item.setItem(tagId);
                    item.setUseTag(true);
                }
            }
            
            keyMap.put(key, item);
        }
        
        return keyMap;
    }
    
    /**
     * Extract a list of ingredients from an ingredients section
     * 
     * @param ingredientsSection The ingredients section
     * @return The extracted ingredients
     */
    private List<RecipeItem> extractIngredientsList(@NotNull String ingredientsSection) {
        List<RecipeItem> ingredients = new ArrayList<>();
        
        // Extract each ingredient object
        Pattern ingredientPattern = Pattern.compile("\\{(.*?)\\}", Pattern.DOTALL);
        Matcher matcher = ingredientPattern.matcher(ingredientsSection);
        
        while (matcher.find()) {
            String itemJson = "{" + matcher.group(1) + "}";
            
            // Parse the item
            RecipeItem item = new RecipeItem("minecraft:air");
            
            // Extract item or tag
            String itemId = extractString(itemJson, "item");
            if (itemId != null) {
                item.setItem(itemId);
            } else {
                String tagId = extractString(itemJson, "tag");
                if (tagId != null) {
                    item.setItem(tagId);
                    item.setUseTag(true);
                }
            }
            
            ingredients.add(item);
        }
        
        return ingredients;
    }
    
    /**
     * Extract a string value from JSON
     * 
     * @param json The JSON content
     * @param key The key to extract
     * @return The extracted string, or null if not found
     */
    @Nullable
    private String extractString(@NotNull String json, @NotNull String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(json);
        
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return null;
    }
    
    /**
     * Extract an integer value from JSON
     * 
     * @param json The JSON content
     * @param key The key to extract
     * @return The extracted integer, or null if not found
     */
    @Nullable
    private Integer extractInteger(@NotNull String json, @NotNull String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*(\\d+)");
        Matcher matcher = pattern.matcher(json);
        
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        
        return null;
    }
    
    /**
     * Extract a float value from JSON
     * 
     * @param json The JSON content
     * @param key The key to extract
     * @return The extracted float, or null if not found
     */
    @Nullable
    private Float extractFloat(@NotNull String json, @NotNull String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*([\\d.]+)");
        Matcher matcher = pattern.matcher(json);
        
        if (matcher.find()) {
            try {
                return Float.parseFloat(matcher.group(1));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        
        return null;
    }
    
    /**
     * Save recipes to the project
     * 
     * @param outputPath The output path to save to
     * @return Number of recipes saved
     */
    public int saveRecipes(@NotNull String outputPath) {
        int savedCount = 0;
        
        try {
            // Create output directory if it doesn't exist
            Path dirPath = Paths.get(outputPath);
            Files.createDirectories(dirPath);
            
            // Save each recipe
            for (RecipeModel recipe : recipes.values()) {
                String filename = recipe.getId().replace(':', '_') + ".json";
                Path filePath = dirPath.resolve(filename);
                
                try {
                    String json = recipe.toJson();
                    Files.writeString(filePath, json);
                    savedCount++;
                } catch (IOException e) {
                    LOG.error("Error saving recipe " + recipe.getId(), e);
                }
            }
            
            // Refresh the virtual file system
            LocalFileSystem.getInstance().refreshAndFindFileByPath(outputPath);
            
            return savedCount;
            
        } catch (Exception e) {
            LOG.error("Error saving recipes", e);
            return savedCount;
        }
    }
    
    /**
     * Create a new recipe
     * 
     * @param id The recipe ID
     * @param type The recipe type
     * @return The created recipe
     */
    public RecipeModel createRecipe(@NotNull String id, @NotNull RecipeType type) {
        RecipeModel recipe = new RecipeModel(id, type);
        recipes.put(id, recipe);
        return recipe;
    }
    
    /**
     * Delete a recipe
     * 
     * @param id The recipe ID
     * @return True if the recipe was deleted, false if it wasn't found
     */
    public boolean deleteRecipe(@NotNull String id) {
        return recipes.remove(id) != null;
    }
    
    /**
     * Get all recipes
     * 
     * @return Collection of all recipes
     */
    public Collection<RecipeModel> getAllRecipes() {
        return recipes.values();
    }
    
    /**
     * Get recipes of a specific type
     * 
     * @param type The recipe type
     * @return List of recipes of the specified type
     */
    public List<RecipeModel> getRecipesByType(@NotNull RecipeType type) {
        List<RecipeModel> result = new ArrayList<>();
        
        for (RecipeModel recipe : recipes.values()) {
            if (recipe.getType() == type) {
                result.add(recipe);
            }
        }
        
        return result;
    }
    
    /**
     * Get a recipe by ID
     * 
     * @param id The recipe ID
     * @return The recipe, or null if not found
     */
    @Nullable
    public RecipeModel getRecipe(@NotNull String id) {
        return recipes.get(id);
    }
}