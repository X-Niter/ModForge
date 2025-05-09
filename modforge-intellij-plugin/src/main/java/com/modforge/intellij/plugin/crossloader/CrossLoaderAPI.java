package com.modforge.intellij.plugin.crossloader;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.ThrowableRunnable;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cross-loader API for creating and managing mods that work across multiple mod loaders.
 * Provides templates, converters, and utilities for cross-loader development.
 */
public class CrossLoaderAPI {
    private static final Logger LOG = Logger.getInstance(CrossLoaderAPI.class);
    
    private final Project project;
    private final ArchitecturyService architecturyService;
    private final Map<String, TemplateProvider> templateProviders = new ConcurrentHashMap<>();
    
    /**
     * Creates a new cross-loader API.
     * @param project The project
     */
    public CrossLoaderAPI(@NotNull Project project) {
        this.project = project;
        this.architecturyService = ArchitecturyService.getInstance(project);
        registerTemplateProviders();
    }
    
    /**
     * Gets the instance of the cross-loader API.
     * @param project The project
     * @return The cross-loader API
     */
    public static CrossLoaderAPI getInstance(@NotNull Project project) {
        return new CrossLoaderAPI(project);
    }
    
    /**
     * Registers template providers for different features.
     */
    private void registerTemplateProviders() {
        // Register Architectury template providers
        registerArchitecturyTemplateProviders();
        
        // Register other template providers (like direct Forge/Fabric converters)
        registerDirectConverterTemplateProviders();
    }
    
    /**
     * Registers Architectury template providers.
     */
    private void registerArchitecturyTemplateProviders() {
        // Register templates for each feature type
        for (ArchitecturyService.ArchitecturyFeatureType featureType : ArchitecturyService.ArchitecturyFeatureType.values()) {
            String templateKey = "architectury." + featureType.name().toLowerCase();
            templateProviders.put(templateKey, new ArchitecturyTemplateProvider(featureType));
        }
    }
    
    /**
     * Registers direct converter template providers (without Architectury).
     */
    private void registerDirectConverterTemplateProviders() {
        // Register direct converters for common mod features
        templateProviders.put("direct.forge_to_fabric", new DirectConverterTemplateProvider(
                ArchitecturyService.ModLoader.FORGE, ArchitecturyService.ModLoader.FABRIC));
        
        templateProviders.put("direct.fabric_to_forge", new DirectConverterTemplateProvider(
                ArchitecturyService.ModLoader.FABRIC, ArchitecturyService.ModLoader.FORGE));
    }
    
    /**
     * Gets a template for a specific feature.
     * @param templateKey The template key
     * @param templateParams The template parameters
     * @return The template, or null if no template provider exists
     */
    @Nullable
    public String getTemplate(@NotNull String templateKey, @NotNull Map<String, String> templateParams) {
        TemplateProvider provider = templateProviders.get(templateKey);
        if (provider == null) {
            LOG.warn("No template provider found for key: " + templateKey);
            return null;
        }
        
        return provider.getTemplate(templateParams);
    }
    
    /**
     * Gets a list of available template keys.
     * @return The available template keys
     */
    @NotNull
    public List<String> getAvailableTemplateKeys() {
        return List.copyOf(templateProviders.keySet());
    }
    
    /**
     * Converts a file from one mod loader to another.
     * @param sourceFile The source file
     * @param sourceLoader The source mod loader
     * @param targetLoader The target mod loader
     * @return A future with the converted file content, or null if conversion is not possible
     */
    @NotNull
    public CompletableFuture<String> convertFile(@NotNull PsiFile sourceFile, 
                                                @NotNull ArchitecturyService.ModLoader sourceLoader, 
                                                @NotNull ArchitecturyService.ModLoader targetLoader) {
        CompletableFuture<String> future = new CompletableFuture<>();
        
        // Run in background
        AppExecutorUtil.getAppExecutorService().submit(() -> {
            try {
                if (sourceFile instanceof PsiJavaFile) {
                    PsiJavaFile javaFile = (PsiJavaFile) sourceFile;
                    if (javaFile.getClasses().length > 0) {
                        String converted = architecturyService.convertClass(
                                javaFile.getClasses()[0], sourceLoader, targetLoader);
                        future.complete(converted);
                    } else {
                        future.complete(null);
                    }
                } else {
                    // For non-Java files, we need a different approach
                    future.complete(convertNonJavaFile(sourceFile, sourceLoader, targetLoader));
                }
            } catch (Exception e) {
                LOG.error("Error converting file", e);
                future.completeExceptionally(e);
            }
        });
        
        return future;
    }
    
    /**
     * Converts a non-Java file from one mod loader to another.
     * @param sourceFile The source file
     * @param sourceLoader The source mod loader
     * @param targetLoader The target mod loader
     * @return The converted file content, or null if conversion is not possible
     */
    @Nullable
    private String convertNonJavaFile(@NotNull PsiFile sourceFile, 
                                    @NotNull ArchitecturyService.ModLoader sourceLoader, 
                                    @NotNull ArchitecturyService.ModLoader targetLoader) {
        String fileName = sourceFile.getName();
        String fileContent = sourceFile.getText();
        
        // Handle different config/metadata files
        if (sourceLoader == ArchitecturyService.ModLoader.FORGE && targetLoader == ArchitecturyService.ModLoader.FABRIC) {
            if (fileName.equals("mods.toml")) {
                return convertForgeTomlToFabricJson(fileContent);
            }
        } else if (sourceLoader == ArchitecturyService.ModLoader.FABRIC && targetLoader == ArchitecturyService.ModLoader.FORGE) {
            if (fileName.equals("fabric.mod.json")) {
                return convertFabricJsonToForgeToml(fileContent);
            }
        }
        
        // For other files, no conversion
        return null;
    }
    
    /**
     * Converts a Forge mods.toml file to a Fabric fabric.mod.json file.
     * @param forgeToml The Forge mods.toml content
     * @return The Fabric fabric.mod.json content
     */
    @NotNull
    private String convertForgeTomlToFabricJson(@NotNull String forgeToml) {
        // This is a simplified implementation
        // In a real implementation, we would parse the TOML and convert it to JSON
        
        // Extract information from the TOML
        String modId = extractValue(forgeToml, "modId");
        String modName = extractValue(forgeToml, "displayName");
        String modDesc = extractValue(forgeToml, "description");
        String version = extractValue(forgeToml, "version");
        String authors = extractValue(forgeToml, "authors");
        
        return "{\n" +
               "  \"schemaVersion\": 1,\n" +
               "  \"id\": \"" + (modId != null ? modId : "mod_id") + "\",\n" +
               "  \"version\": \"" + (version != null ? version : "1.0.0") + "\",\n" +
               "  \"name\": \"" + (modName != null ? modName : "Mod Name") + "\",\n" +
               "  \"description\": \"" + (modDesc != null ? modDesc : "Description") + "\",\n" +
               "  \"authors\": [" + formatAuthors(authors) + "],\n" +
               "  \"environment\": \"*\",\n" +
               "  \"entrypoints\": {\n" +
               "    \"main\": [\n" +
               "      \"com.example." + (modId != null ? modId : "mod_id") + "." + toClassName(modId != null ? modId : "mod_id") + "\"\n" +
               "    ],\n" +
               "    \"client\": [\n" +
               "      \"com.example." + (modId != null ? modId : "mod_id") + ".client." + toClassName(modId != null ? modId : "mod_id") + "Client\"\n" +
               "    ]\n" +
               "  },\n" +
               "  \"depends\": {\n" +
               "    \"fabricloader\": \">=0.14.0\",\n" +
               "    \"fabric\": \"*\",\n" +
               "    \"minecraft\": \">=1.18.2\"\n" +
               "  }\n" +
               "}";
    }
    
    /**
     * Converts a Fabric fabric.mod.json file to a Forge mods.toml file.
     * @param fabricJson The Fabric fabric.mod.json content
     * @return The Forge mods.toml content
     */
    @NotNull
    private String convertFabricJsonToForgeToml(@NotNull String fabricJson) {
        // This is a simplified implementation
        // In a real implementation, we would parse the JSON and convert it to TOML
        
        // Extract information from the JSON
        String modId = extractJsonValue(fabricJson, "id");
        String modName = extractJsonValue(fabricJson, "name");
        String modDesc = extractJsonValue(fabricJson, "description");
        String version = extractJsonValue(fabricJson, "version");
        String authors = extractJsonArray(fabricJson, "authors");
        
        return "modLoader=\"javafml\"\n" +
               "loaderVersion=\"[40,)\"\n" +
               "license=\"All Rights Reserved\"\n" +
               "[[mods]]\n" +
               "modId=\"" + (modId != null ? modId : "mod_id") + "\"\n" +
               "version=\"" + (version != null ? version : "1.0.0") + "\"\n" +
               "displayName=\"" + (modName != null ? modName : "Mod Name") + "\"\n" +
               "description='''\n" + 
               (modDesc != null ? modDesc : "Description") + "\n" +
               "'''\n" +
               "authors=\"" + (authors != null ? authors : "Author") + "\"\n" +
               "[[dependencies." + (modId != null ? modId : "mod_id") + "]]\n" +
               "    modId=\"forge\"\n" +
               "    mandatory=true\n" +
               "    versionRange=\"[40,)\"\n" +
               "    ordering=\"NONE\"\n" +
               "    side=\"BOTH\"\n" +
               "[[dependencies." + (modId != null ? modId : "mod_id") + "]]\n" +
               "    modId=\"minecraft\"\n" +
               "    mandatory=true\n" +
               "    versionRange=\"[1.18.2,1.19)\"\n" +
               "    ordering=\"NONE\"\n" +
               "    side=\"BOTH\"\n";
    }
    
    /**
     * Extracts a value from a TOML file.
     * @param toml The TOML content
     * @param key The key
     * @return The value, or null if not found
     */
    @Nullable
    private String extractValue(@NotNull String toml, @NotNull String key) {
        String[] lines = toml.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith(key + "=")) {
                String value = line.substring(key.length() + 1).trim();
                
                // Remove quotes if present
                if (value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }
                
                return value;
            }
        }
        
        return null;
    }
    
    /**
     * Extracts a value from a JSON file.
     * @param json The JSON content
     * @param key The key
     * @return The value, or null if not found
     */
    @Nullable
    private String extractJsonValue(@NotNull String json, @NotNull String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]+)\"";
        java.util.regex.Pattern r = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = r.matcher(json);
        
        if (m.find()) {
            return m.group(1);
        }
        
        return null;
    }
    
    /**
     * Extracts an array from a JSON file.
     * @param json The JSON content
     * @param key The key
     * @return The array values as a comma-separated string, or null if not found
     */
    @Nullable
    private String extractJsonArray(@NotNull String json, @NotNull String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*\\[([^\\]]+)\\]";
        java.util.regex.Pattern r = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = r.matcher(json);
        
        if (m.find()) {
            String values = m.group(1);
            values = values.replaceAll("\"", "");
            values = values.replaceAll("\\s+", "");
            return values;
        }
        
        return null;
    }
    
    /**
     * Formats authors for JSON.
     * @param authors The authors as a comma-separated string
     * @return The formatted authors
     */
    @NotNull
    private String formatAuthors(@Nullable String authors) {
        if (authors == null || authors.isEmpty()) {
            return "\"Author\"";
        }
        
        String[] authorArray = authors.split(",");
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < authorArray.length; i++) {
            if (i > 0) {
                result.append(", ");
            }
            result.append("\"").append(authorArray[i].trim()).append("\"");
        }
        
        return result.toString();
    }
    
    /**
     * Converts a string to a class name (PascalCase).
     * @param str The string
     * @return The class name
     */
    @NotNull
    private String toClassName(@Nullable String str) {
        if (str == null || str.isEmpty()) {
            return "Mod";
        }
        
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        
        for (char c : str.toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                if (capitalizeNext) {
                    result.append(Character.toUpperCase(c));
                    capitalizeNext = false;
                } else {
                    result.append(c);
                }
            } else {
                capitalizeNext = true;
            }
        }
        
        return result.toString();
    }
    
    /**
     * Interface for template providers.
     */
    public interface TemplateProvider {
        /**
         * Gets a template with the specified parameters.
         * @param params The template parameters
         * @return The template
         */
        @NotNull
        String getTemplate(@NotNull Map<String, String> params);
        
        /**
         * Gets metadata about the template.
         * @return The template metadata
         */
        @NotNull
        TemplateMetadata getMetadata();
    }
    
    /**
     * Class for template metadata.
     */
    public static class TemplateMetadata {
        private final String name;
        private final String description;
        private final List<String> requiredParams;
        private final List<String> optionalParams;
        
        /**
         * Creates new template metadata.
         * @param name The template name
         * @param description The template description
         * @param requiredParams The required parameters
         * @param optionalParams The optional parameters
         */
        public TemplateMetadata(@NotNull String name, @NotNull String description, 
                               @NotNull List<String> requiredParams, @NotNull List<String> optionalParams) {
            this.name = name;
            this.description = description;
            this.requiredParams = requiredParams;
            this.optionalParams = optionalParams;
        }
        
        /**
         * Gets the template name.
         * @return The template name
         */
        @NotNull
        public String getName() {
            return name;
        }
        
        /**
         * Gets the template description.
         * @return The template description
         */
        @NotNull
        public String getDescription() {
            return description;
        }
        
        /**
         * Gets the required parameters.
         * @return The required parameters
         */
        @NotNull
        public List<String> getRequiredParams() {
            return requiredParams;
        }
        
        /**
         * Gets the optional parameters.
         * @return The optional parameters
         */
        @NotNull
        public List<String> getOptionalParams() {
            return optionalParams;
        }
    }
    
    /**
     * Architectury template provider.
     */
    private class ArchitecturyTemplateProvider implements TemplateProvider {
        private final ArchitecturyService.ArchitecturyFeatureType featureType;
        private final TemplateMetadata metadata;
        
        /**
         * Creates a new Architectury template provider.
         * @param featureType The feature type
         */
        public ArchitecturyTemplateProvider(@NotNull ArchitecturyService.ArchitecturyFeatureType featureType) {
            this.featureType = featureType;
            this.metadata = createMetadata();
        }
        
        /**
         * Creates metadata for this template provider.
         * @return The metadata
         */
        @NotNull
        private TemplateMetadata createMetadata() {
            String name = "Architectury " + featureType.name();
            String description = "Template for " + featureType.name().toLowerCase().replace('_', ' ') + 
                                " using Architectury";
            List<String> requiredParams = List.of("MOD_ID");
            List<String> optionalParams = List.of("PACKAGE_NAME", "AUTHOR");
            
            return new TemplateMetadata(name, description, requiredParams, optionalParams);
        }
        
        @Override
        @NotNull
        public String getTemplate(@NotNull Map<String, String> params) {
            String template = architecturyService.getArchitecturySnippet(featureType);
            
            // Replace template parameters
            for (Map.Entry<String, String> entry : params.entrySet()) {
                template = template.replace("${" + entry.getKey() + "}", entry.getValue());
            }
            
            return template;
        }
        
        @Override
        @NotNull
        public TemplateMetadata getMetadata() {
            return metadata;
        }
    }
    
    /**
     * Direct converter template provider.
     */
    private class DirectConverterTemplateProvider implements TemplateProvider {
        private final ArchitecturyService.ModLoader sourceLoader;
        private final ArchitecturyService.ModLoader targetLoader;
        private final TemplateMetadata metadata;
        
        /**
         * Creates a new direct converter template provider.
         * @param sourceLoader The source mod loader
         * @param targetLoader The target mod loader
         */
        public DirectConverterTemplateProvider(@NotNull ArchitecturyService.ModLoader sourceLoader, 
                                              @NotNull ArchitecturyService.ModLoader targetLoader) {
            this.sourceLoader = sourceLoader;
            this.targetLoader = targetLoader;
            this.metadata = createMetadata();
        }
        
        /**
         * Creates metadata for this template provider.
         * @return The metadata
         */
        @NotNull
        private TemplateMetadata createMetadata() {
            String name = sourceLoader.getDisplayName() + " to " + targetLoader.getDisplayName() + " Converter";
            String description = "Direct converter from " + sourceLoader.getDisplayName() + 
                                " to " + targetLoader.getDisplayName() + " (without Architectury)";
            List<String> requiredParams = List.of("MOD_ID", "CLASS_NAME");
            List<String> optionalParams = List.of("PACKAGE_NAME", "AUTHOR");
            
            return new TemplateMetadata(name, description, requiredParams, optionalParams);
        }
        
        @Override
        @NotNull
        public String getTemplate(@NotNull Map<String, String> params) {
            String modId = params.getOrDefault("MOD_ID", "example_mod");
            String className = params.getOrDefault("CLASS_NAME", "ExampleMod");
            String packageName = params.getOrDefault("PACKAGE_NAME", "com.example." + modId);
            
            if (sourceLoader == ArchitecturyService.ModLoader.FORGE && targetLoader == ArchitecturyService.ModLoader.FABRIC) {
                return getForgeToFabricTemplate(modId, className, packageName);
            } else if (sourceLoader == ArchitecturyService.ModLoader.FABRIC && targetLoader == ArchitecturyService.ModLoader.FORGE) {
                return getFabricToForgeTemplate(modId, className, packageName);
            }
            
            return "// No template available for conversion from " + sourceLoader + " to " + targetLoader;
        }
        
        /**
         * Gets a template for converting from Forge to Fabric.
         * @param modId The mod ID
         * @param className The class name
         * @param packageName The package name
         * @return The template
         */
        @NotNull
        private String getForgeToFabricTemplate(@NotNull String modId, @NotNull String className, @NotNull String packageName) {
            return "package " + packageName + ";\n\n" +
                   "import net.fabricmc.api.ModInitializer;\n" +
                   "import net.fabricmc.fabric.api.item.v1.FabricItemSettings;\n" +
                   "import net.minecraft.core.Registry;\n" +
                   "import net.minecraft.resources.ResourceLocation;\n" +
                   "import net.minecraft.world.item.Item;\n" +
                   "import net.minecraft.world.item.CreativeModeTab;\n\n" +
                   "public class " + className + " implements ModInitializer {\n" +
                   "    public static final String MOD_ID = \"" + modId + "\";\n\n" +
                   "    // Example item\n" +
                   "    public static final Item EXAMPLE_ITEM = new Item(new FabricItemSettings().group(CreativeModeTab.TAB_MISC));\n\n" +
                   "    @Override\n" +
                   "    public void onInitialize() {\n" +
                   "        // Register items\n" +
                   "        Registry.register(Registry.ITEM, new ResourceLocation(MOD_ID, \"example_item\"), EXAMPLE_ITEM);\n" +
                   "        \n" +
                   "        System.out.println(\"" + className + " initialized!\");\n" +
                   "    }\n" +
                   "}";
        }
        
        /**
         * Gets a template for converting from Fabric to Forge.
         * @param modId The mod ID
         * @param className The class name
         * @param packageName The package name
         * @return The template
         */
        @NotNull
        private String getFabricToForgeTemplate(@NotNull String modId, @NotNull String className, @NotNull String packageName) {
            return "package " + packageName + ";\n\n" +
                   "import net.minecraft.world.item.Item;\n" +
                   "import net.minecraft.world.item.CreativeModeTab;\n" +
                   "import net.minecraftforge.fml.common.Mod;\n" +
                   "import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;\n" +
                   "import net.minecraftforge.registries.DeferredRegister;\n" +
                   "import net.minecraftforge.registries.ForgeRegistries;\n" +
                   "import net.minecraftforge.registries.RegistryObject;\n\n" +
                   "@Mod(\"" + modId + "\")\n" +
                   "public class " + className + " {\n" +
                   "    public static final String MOD_ID = \"" + modId + "\";\n\n" +
                   "    // Create a Deferred Register for items\n" +
                   "    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);\n\n" +
                   "    // Example item\n" +
                   "    public static final RegistryObject<Item> EXAMPLE_ITEM = ITEMS.register(\"example_item\",\n" +
                   "            () -> new Item(new Item.Properties().tab(CreativeModeTab.TAB_MISC)));\n\n" +
                   "    public " + className + "() {\n" +
                   "        // Register the deferred registers to the appropriate event buses\n" +
                   "        ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());\n" +
                   "        \n" +
                   "        System.out.println(\"" + className + " initialized!\");\n" +
                   "    }\n" +
                   "}";
        }
        
        @Override
        @NotNull
        public TemplateMetadata getMetadata() {
            return metadata;
        }
    }
}