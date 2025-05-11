package com.modforge.intellij.plugin.ai.generation;

import java.util.Arrays;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.modforge.intellij.plugin.ai.PatternRecognitionService;
import com.modforge.intellij.plugin.util.ModLoaderDetector;
import com.modforge.intellij.plugin.util.ModLoaderDetector.ModLoader;
import com.modforge.intellij.plugin.utils.CompatibilityUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Advanced code generator specifically for Minecraft mods.
 * Supports multiple mod loaders and provides context-aware code generation.
 */
@Service
public final class MinecraftCodeGenerator {
    private static final Logger LOG = Logger.getInstance(MinecraftCodeGenerator.class);
    
    // Timeout for code generation requests
    private static final long CODE_GENERATION_TIMEOUT_SECONDS = 60;
    
    // Patterns for extracting package and class names
    private static final Pattern PACKAGE_PATTERN = Pattern.compile("package\\s+([\\w.]+);");
    private static final Pattern CLASS_PATTERN = Pattern.compile("(public|private|protected)?\\s*class\\s+(\\w+)");
    
    private final Project project;
    private final PatternRecognitionService patternService;
    
    // Template cache for different mod loaders
    private final Map<ModLoader, Map<String, String>> templateCache = new HashMap<>();
    
    /**
     * Create a new Minecraft code generator
     * 
     * @param project The current project
     */
    public MinecraftCodeGenerator(Project project) {
        this.project = project;
        this.patternService = project.getService(PatternRecognitionService.class);
        
        // Initialize template cache
        initializeTemplates();
        
        LOG.info("Minecraft code generator initialized for project: " + project.getName());
    }
    
    /**
     * Initialize the template cache for different mod loaders
     */
    private void initializeTemplates() {
        // Load templates for each mod loader
        for (ModLoader loader : ModLoader.values()) {
            if (loader == ModLoader.UNKNOWN) continue;
            
            Map<String, String> templates = new HashMap<>();
            templateCache.put(loader, templates);
            
            // Load common templates
            loadTemplates(loader, templates);
        }
        
        LOG.info("Initialized template cache with " + templateCache.size() + " mod loaders");
    }
    
    /**
     * Load templates for a specific mod loader
     * 
     * @param loader The mod loader
     * @param templates The template map to populate
     */
    private void loadTemplates(ModLoader loader, Map<String, String> templates) {
        // These would normally be loaded from resource files or a database
        // For now, we'll add a few common templates manually
        
        switch (loader) {
            case FORGE:
                templates.put("block", FORGE_BLOCK_TEMPLATE);
                templates.put("item", FORGE_ITEM_TEMPLATE);
                templates.put("entity", FORGE_ENTITY_TEMPLATE);
                templates.put("tile_entity", FORGE_TILE_ENTITY_TEMPLATE);
                break;
                
            case FABRIC:
                templates.put("block", FABRIC_BLOCK_TEMPLATE);
                templates.put("item", FABRIC_ITEM_TEMPLATE);
                templates.put("entity", FABRIC_ENTITY_TEMPLATE);
                templates.put("block_entity", FABRIC_BLOCK_ENTITY_TEMPLATE);
                break;
                
            case QUILT:
                // Quilt is similar to Fabric
                templates.put("block", FABRIC_BLOCK_TEMPLATE);
                templates.put("item", FABRIC_ITEM_TEMPLATE);
                templates.put("entity", FABRIC_ENTITY_TEMPLATE);
                templates.put("block_entity", FABRIC_BLOCK_ENTITY_TEMPLATE);
                break;
                
            case ARCHITECTURY:
                templates.put("block", ARCHITECTURY_BLOCK_TEMPLATE);
                templates.put("item", ARCHITECTURY_ITEM_TEMPLATE);
                templates.put("entity", ARCHITECTURY_ENTITY_TEMPLATE);
                templates.put("block_entity", ARCHITECTURY_BLOCK_ENTITY_TEMPLATE);
                break;
        }
    }
    
    /**
     * Generate code based on a description
     * 
     * @param description The description of the code to generate
     * @param targetDirectory The directory where the code should be created
     * @param loaderHint Optional hint about the mod loader to use
     * @return CompletableFuture that resolves to the generated file
     */
    public CompletableFuture<GeneratedCode> generateCode(
            String description, 
            String targetDirectory,
            @Nullable String loaderHint) {
        
        // Create a progress indicator task
        AtomicReference<CompletableFuture<GeneratedCode>> futureRef = new AtomicReference<>();
        
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Generating Minecraft Code") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                indicator.setText("Analyzing project structure...");
                
                // Detect mod loader if not specified
                ModLoader loader = determineModLoader(loaderHint);
                
                indicator.setText("Preparing code generation...");
                
                // Try to determine what type of code to generate
                CodeType codeType = determineCodeType(description);
                
                // Prepare template and context
                String template = getTemplate(loader, codeType);
                Map<String, String> context = prepareContext(description, loader, codeType);
                
                indicator.setText("Generating code...");
                
                // Generate the code
                CompletableFuture<GeneratedCode> future = callGenerateCodeApi(description, template, context)
                    .thenCompose(code -> {
                        indicator.setText("Finalizing and saving code...");
                        return finalizeAndSaveCode(code, targetDirectory, loader, codeType);
                    });
                
                futureRef.set(future);
                
                // Wait for completion with timeout
                try {
                    future.get(CODE_GENERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    indicator.setText("Code generation complete");
                } catch (Exception e) {
                    LOG.error("Error generating code", e);
                    futureRef.set(CompletableFuture.failedFuture(
                        new RuntimeException("Code generation failed: " + e.getMessage(), e)));
                }
            }
        });
        
        // Return the future from the background task
        return CompletableFuture.supplyAsync(() -> {
            try {
                while (futureRef.get() == null) {
                    Thread.sleep(100); // Wait for task to start
                }
                return futureRef.get().get(CODE_GENERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (Exception e) {
                throw new RuntimeException("Code generation failed", e);
            }
        }, AppExecutorUtil.getAppExecutorService());
    }
    
    /**
     * Determine the mod loader for the project
     * 
     * @param loaderHint Optional hint about which loader to use
     * @return The detected mod loader
     */
    private ModLoader determineModLoader(@Nullable String loaderHint) {
        // Use hint if provided
        if (loaderHint != null && !loaderHint.isEmpty()) {
            try {
                return ModLoader.valueOf(loaderHint.toUpperCase());
            } catch (IllegalArgumentException e) {
                LOG.warn("Invalid loader hint: " + loaderHint);
            }
        }
        
        // Detect from project
        ModLoader detected = ModLoaderDetector.detectModLoader(project);
        
        // If unable to detect, default to Forge
        if (detected == ModLoader.UNKNOWN) {
            LOG.info("Unable to detect mod loader, defaulting to Forge");
            return ModLoader.FORGE;
        }
        
        return detected;
    }
    
    /**
     * Determine the type of code to generate based on the description
     * 
     * @param description The code description
     * @return The detected code type
     */
    private CodeType determineCodeType(String description) {
        String lowerDesc = description.toLowerCase();
        
        // Simple keyword-based detection
        if (lowerDesc.contains("block") && !lowerDesc.contains("entity")) {
            return CodeType.BLOCK;
        } else if (lowerDesc.contains("item")) {
            return CodeType.ITEM;
        } else if (lowerDesc.contains("entity") && !lowerDesc.contains("block")) {
            return CodeType.ENTITY;
        } else if ((lowerDesc.contains("tile") && lowerDesc.contains("entity")) || 
                  (lowerDesc.contains("block") && lowerDesc.contains("entity"))) {
            return CodeType.BLOCK_ENTITY;
        }
        
        // Default to generic if we can't determine
        return CodeType.GENERIC;
    }
    
    /**
     * Get the template for a specific mod loader and code type
     * 
     * @param loader The mod loader
     * @param codeType The code type
     * @return The template string
     */
    private String getTemplate(ModLoader loader, CodeType codeType) {
        Map<String, String> templates = templateCache.get(loader);
        if (templates == null) {
            return "";
        }
        
        String templateKey = codeType.getTemplateKey();
        String template = templates.get(templateKey);
        
        return template != null ? template : "";
    }
    
    /**
     * Prepare the context for code generation
     * 
     * @param description The code description
     * @param loader The mod loader
     * @param codeType The code type
     * @return The context map
     */
    private Map<String, String> prepareContext(String description, ModLoader loader, CodeType codeType) {
        Map<String, String> context = new HashMap<>();
        
        // Add basic context
        context.put("description", description);
        context.put("modLoader", loader.name());
        context.put("codeType", codeType.name());
        
        // Try to extract a name for the class from the description
        String className = extractClassName(description, codeType);
        context.put("className", className);
        
        // Try to determine package name from project structure
        String packageName = determinePackageName(loader, codeType);
        context.put("packageName", packageName);
        
        // Add mod loader-specific context
        switch (loader) {
            case FORGE:
                context.put("registryName", className.toLowerCase());
                break;
            case FABRIC:
            case QUILT:
                context.put("identifier", className.toLowerCase());
                break;
            case ARCHITECTURY:
                context.put("identifier", className.toLowerCase());
                context.put("platform", "common"); // Default to common platform
                break;
        }
        
        return context;
    }
    
    /**
     * Extract a class name from the description
     * 
     * @param description The code description
     * @param codeType The code type
     * @return The extracted class name
     */
    private String extractClassName(String description, CodeType codeType) {
        // Try to extract a name from the description
        String name = "";
        
        // Look for phrases like "Create a Diamond Sword item"
        Pattern pattern = Pattern.compile("\\b([A-Z][a-z]*(?:\\s+[A-Z][a-z]*)*)\\s+" + codeType.name().toLowerCase(), 
                                         Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(description);
        
        if (matcher.find()) {
            name = matcher.group(1);
        } else {
            // Just use a default name based on type
            name = "Custom" + codeType.name().substring(0, 1).toUpperCase() + 
                   codeType.name().substring(1).toLowerCase();
        }
        
        // Convert to proper class name (remove spaces, capitalize words)
        name = Arrays.stream(name.split("\\s+"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                .reduce("", String::concat);
        
        return name;
    }
    
    /**
     * Determine the package name for the generated code
     * 
     * @param loader The mod loader
     * @param codeType The code type
     * @return The package name
     */
    private String determinePackageName(ModLoader loader, CodeType codeType) {
        // Try to find an existing package for the code type
        String typeFolder = codeType.name().toLowerCase();
        
        // Default to a reasonable package name
        return "com.example.mod." + typeFolder + "s";
    }
    
    /**
     * Call the code generation API to generate the code
     * 
     * @param description The code description
     * @param template The template to use
     * @param context The context for generation
     * @return CompletableFuture that resolves to the generated code
     */
    private CompletableFuture<String> callGenerateCodeApi(
            String description, 
            String template, 
            Map<String, String> context) {
        
        // Build the prompt for code generation
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate Minecraft mod code based on this description: ").append(description).append("\n\n");
        
        if (!template.isEmpty()) {
            prompt.append("Use this template as a guide:\n").append(template).append("\n\n");
        }
        
        prompt.append("Context information:\n");
        for (Map.Entry<String, String> entry : context.entrySet()) {
            prompt.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        
        prompt.append("\nPlease generate complete, well-documented code that follows best practices for the specified mod loader.");
        
        // Use pattern recognition service to optimize API calls
        return CompletableFuture.supplyAsync(() -> {
            // Check if we have a pattern match first
            String patternMatch = patternService.tryMatchPattern(prompt.toString(), null);
            if (patternMatch != null) {
                return patternMatch;
            }
            
            // If no pattern match, make the API call
            // This is a placeholder for the actual API call
            // In a real implementation, this would call an API like OpenAI
            String generatedCode = "// Generated code for " + context.get("className") + "\n\n" +
                                  "// This is a placeholder for actual generated code";
            
            // Record the successful call for pattern learning
            patternService.recordSuccessfulCall(prompt.toString(), generatedCode, null);
            
            return generatedCode;
        }, AppExecutorUtil.getAppExecutorService());
    }
    
    /**
     * Finalize and save the generated code to a file
     * 
     * @param code The generated code
     * @param targetDirectory The directory to save to
     * @param loader The mod loader
     * @param codeType The code type
     * @return CompletableFuture that resolves to the generated file info
     */
    private CompletableFuture<GeneratedCode> finalizeAndSaveCode(
            String code, 
            String targetDirectory, 
            ModLoader loader, 
            CodeType codeType) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Extract the package and class name from the generated code
                String packageName = extractPackageName(code);
                String className = extractClassName(code);
                
                if (className.isEmpty()) {
                    // Fallback to the code type if we couldn't extract a class name
                    className = "Custom" + codeType.name().substring(0, 1).toUpperCase() + 
                               codeType.name().substring(1).toLowerCase();
                }
                
                // Create the directory structure for the package
                String packagePath = packageName.replace('.', '/');
                String fullPath = targetDirectory + "/" + packagePath;
                
                File directory = new File(fullPath);
                if (!directory.exists()) {
                    directory.mkdirs();
                }
                
                // Save the code to a file
                String fileName = className + ".java";
                String filePath = fullPath + "/" + fileName;
                
                Files.write(Paths.get(filePath), code.getBytes());
                
                // Refresh the virtual file system
                VirtualFile virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(filePath);
                
                if (virtualFile != null) {
                    // Open the file in the editor
                    CompatibilityUtil.runOnUIThread(() -> {
                        CompatibilityUtil.openFileInEditor(project, virtualFile, true);
                    });
                }
                
                // Return the information about the generated code
                return new GeneratedCode(className, packageName, filePath, code, loader, codeType);
                
            } catch (IOException e) {
                throw new RuntimeException("Failed to save generated code", e);
            }
        }, AppExecutorUtil.getAppExecutorService());
    }
    
    /**
     * Extract the package name from generated code
     * 
     * @param code The generated code
     * @return The extracted package name
     */
    private String extractPackageName(String code) {
        Matcher matcher = PACKAGE_PATTERN.matcher(code);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "com.example.mod";
    }
    
    /**
     * Extract the class name from generated code
     * 
     * @param code The generated code
     * @return The extracted class name
     */
    private String extractClassName(String code) {
        Matcher matcher = CLASS_PATTERN.matcher(code);
        if (matcher.find()) {
            return matcher.group(2);
        }
        return "";
    }
    
    /**
     * Enum for different types of Minecraft code
     */
    public enum CodeType {
        BLOCK("block"),
        ITEM("item"),
        ENTITY("entity"),
        BLOCK_ENTITY("block_entity", "tile_entity"),
        WORLD_GEN("world_gen"),
        BIOME("biome"),
        RECIPE("recipe"),
        CRAFTING("crafting"),
        ENCHANTMENT("enchantment"),
        GENERIC("generic");
        
        private final String templateKey;
        private final String[] alternateKeys;
        
        CodeType(String templateKey, String... alternateKeys) {
            this.templateKey = templateKey;
            this.alternateKeys = alternateKeys;
        }
        
        public String getTemplateKey() {
            return templateKey;
        }
        
        public String[] getAlternateKeys() {
            return alternateKeys;
        }
    }
    
    /**
     * Data class representing generated code
     */
    public static class GeneratedCode {
        private final String className;
        private final String packageName;
        private final String filePath;
        private final String code;
        private final ModLoader modLoader;
        private final CodeType codeType;
        
        public GeneratedCode(String className, String packageName, String filePath, String code, 
                           ModLoader modLoader, CodeType codeType) {
            this.className = className;
            this.packageName = packageName;
            this.filePath = filePath;
            this.code = code;
            this.modLoader = modLoader;
            this.codeType = codeType;
        }
        
        public String getClassName() {
            return className;
        }
        
        public String getPackageName() {
            return packageName;
        }
        
        public String getFilePath() {
            return filePath;
        }
        
        public String getCode() {
            return code;
        }
        
        public ModLoader getModLoader() {
            return modLoader;
        }
        
        public CodeType getCodeType() {
            return codeType;
        }
    }
    
    // Templates for different mod loaders
    
    private static final String FORGE_BLOCK_TEMPLATE = 
        "package ${packageName};\n\n" +
        "import net.minecraft.world.level.block.Block;\n" +
        "import net.minecraft.world.level.block.state.BlockState;\n" +
        "import net.minecraft.world.level.material.Material;\n" +
        "import net.minecraft.world.level.block.SoundType;\n" +
        "import net.minecraft.world.item.BlockItem;\n" +
        "import net.minecraft.world.item.Item;\n" +
        "import net.minecraft.world.item.CreativeModeTab;\n" +
        "import net.minecraftforge.registries.DeferredRegister;\n" +
        "import net.minecraftforge.registries.ForgeRegistries;\n" +
        "import net.minecraftforge.registries.RegistryObject;\n\n" +
        "public class ${className} extends Block {\n" +
        "    public ${className}() {\n" +
        "        super(Properties.of(Material.STONE)\n" +
        "            .strength(1.5f, 6.0f)\n" +
        "            .sound(SoundType.STONE));\n" +
        "    }\n" +
        "}\n";
    
    private static final String FORGE_ITEM_TEMPLATE = 
        "package ${packageName};\n\n" +
        "import net.minecraft.world.item.Item;\n" +
        "import net.minecraft.world.item.CreativeModeTab;\n" +
        "import net.minecraftforge.registries.DeferredRegister;\n" +
        "import net.minecraftforge.registries.ForgeRegistries;\n" +
        "import net.minecraftforge.registries.RegistryObject;\n\n" +
        "public class ${className} extends Item {\n" +
        "    public ${className}() {\n" +
        "        super(new Item.Properties().tab(CreativeModeTab.TAB_MISC));\n" +
        "    }\n" +
        "}\n";
    
    private static final String FORGE_ENTITY_TEMPLATE = 
        "package ${packageName};\n\n" +
        "import net.minecraft.world.entity.EntityType;\n" +
        "import net.minecraft.world.entity.MobCategory;\n" +
        "import net.minecraft.world.entity.Mob;\n" +
        "import net.minecraft.world.level.Level;\n" +
        "import net.minecraftforge.registries.DeferredRegister;\n" +
        "import net.minecraftforge.registries.ForgeRegistries;\n" +
        "import net.minecraftforge.registries.RegistryObject;\n\n" +
        "public class ${className} extends Mob {\n" +
        "    public ${className}(EntityType<? extends Mob> entityType, Level level) {\n" +
        "        super(entityType, level);\n" +
        "    }\n" +
        "}\n";
    
    private static final String FORGE_TILE_ENTITY_TEMPLATE = 
        "package ${packageName};\n\n" +
        "import net.minecraft.core.BlockPos;\n" +
        "import net.minecraft.world.level.block.entity.BlockEntity;\n" +
        "import net.minecraft.world.level.block.entity.BlockEntityType;\n" +
        "import net.minecraft.world.level.block.state.BlockState;\n" +
        "import net.minecraftforge.registries.DeferredRegister;\n" +
        "import net.minecraftforge.registries.ForgeRegistries;\n" +
        "import net.minecraftforge.registries.RegistryObject;\n\n" +
        "public class ${className} extends BlockEntity {\n" +
        "    public ${className}(BlockPos pos, BlockState state) {\n" +
        "        super(null, pos, state); // Replace null with your BlockEntityType\n" +
        "    }\n" +
        "}\n";
    
    private static final String FABRIC_BLOCK_TEMPLATE = 
        "package ${packageName};\n\n" +
        "import net.minecraft.block.Block;\n" +
        "import net.minecraft.block.Material;\n" +
        "import net.minecraft.sound.BlockSoundGroup;\n" +
        "import net.minecraft.util.Identifier;\n" +
        "import net.minecraft.registry.Registry;\n\n" +
        "public class ${className} extends Block {\n" +
        "    public ${className}() {\n" +
        "        super(Settings.of(Material.STONE)\n" +
        "            .strength(1.5f, 6.0f)\n" +
        "            .sounds(BlockSoundGroup.STONE));\n" +
        "    }\n" +
        "}\n";
    
    private static final String FABRIC_ITEM_TEMPLATE = 
        "package ${packageName};\n\n" +
        "import net.minecraft.item.Item;\n" +
        "import net.minecraft.item.ItemGroup;\n" +
        "import net.minecraft.util.Identifier;\n" +
        "import net.minecraft.registry.Registry;\n\n" +
        "public class ${className} extends Item {\n" +
        "    public ${className}() {\n" +
        "        super(new Item.Settings().group(ItemGroup.MISC));\n" +
        "    }\n" +
        "}\n";
    
    private static final String FABRIC_ENTITY_TEMPLATE = 
        "package ${packageName};\n\n" +
        "import net.minecraft.entity.EntityType;\n" +
        "import net.minecraft.entity.SpawnGroup;\n" +
        "import net.minecraft.entity.mob.MobEntity;\n" +
        "import net.minecraft.world.World;\n" +
        "import net.minecraft.util.Identifier;\n" +
        "import net.minecraft.registry.Registry;\n\n" +
        "public class ${className} extends MobEntity {\n" +
        "    public ${className}(EntityType<? extends MobEntity> entityType, World world) {\n" +
        "        super(entityType, world);\n" +
        "    }\n" +
        "}\n";
    
    private static final String FABRIC_BLOCK_ENTITY_TEMPLATE = 
        "package ${packageName};\n\n" +
        "import net.minecraft.block.BlockState;\n" +
        "import net.minecraft.block.entity.BlockEntity;\n" +
        "import net.minecraft.block.entity.BlockEntityType;\n" +
        "import net.minecraft.util.math.BlockPos;\n" +
        "import net.minecraft.util.Identifier;\n" +
        "import net.minecraft.registry.Registry;\n\n" +
        "public class ${className} extends BlockEntity {\n" +
        "    public ${className}(BlockPos pos, BlockState state) {\n" +
        "        super(null, pos, state); // Replace null with your BlockEntityType\n" +
        "    }\n" +
        "}\n";
    
    private static final String ARCHITECTURY_BLOCK_TEMPLATE = 
        "package ${packageName};\n\n" +
        "import dev.architectury.registry.registries.DeferredRegister;\n" +
        "import dev.architectury.registry.registries.RegistrySupplier;\n" +
        "import net.minecraft.block.Block;\n" +
        "import net.minecraft.block.Material;\n" +
        "import net.minecraft.sound.BlockSoundGroup;\n" +
        "import net.minecraft.util.Identifier;\n" +
        "import net.minecraft.registry.Registry;\n\n" +
        "public class ${className} extends Block {\n" +
        "    public ${className}() {\n" +
        "        super(Settings.of(Material.STONE)\n" +
        "            .strength(1.5f, 6.0f)\n" +
        "            .sounds(BlockSoundGroup.STONE));\n" +
        "    }\n" +
        "}\n";
    
    private static final String ARCHITECTURY_ITEM_TEMPLATE = 
        "package ${packageName};\n\n" +
        "import dev.architectury.registry.registries.DeferredRegister;\n" +
        "import dev.architectury.registry.registries.RegistrySupplier;\n" +
        "import net.minecraft.item.Item;\n" +
        "import net.minecraft.item.ItemGroup;\n" +
        "import net.minecraft.util.Identifier;\n" +
        "import net.minecraft.registry.Registry;\n\n" +
        "public class ${className} extends Item {\n" +
        "    public ${className}() {\n" +
        "        super(new Item.Settings().group(ItemGroup.MISC));\n" +
        "    }\n" +
        "}\n";
    
    private static final String ARCHITECTURY_ENTITY_TEMPLATE = 
        "package ${packageName};\n\n" +
        "import dev.architectury.registry.registries.DeferredRegister;\n" +
        "import dev.architectury.registry.registries.RegistrySupplier;\n" +
        "import net.minecraft.entity.EntityType;\n" +
        "import net.minecraft.entity.SpawnGroup;\n" +
        "import net.minecraft.entity.mob.MobEntity;\n" +
        "import net.minecraft.world.World;\n" +
        "import net.minecraft.util.Identifier;\n" +
        "import net.minecraft.registry.Registry;\n\n" +
        "public class ${className} extends MobEntity {\n" +
        "    public ${className}(EntityType<? extends MobEntity> entityType, World world) {\n" +
        "        super(entityType, world);\n" +
        "    }\n" +
        "}\n";
    
    private static final String ARCHITECTURY_BLOCK_ENTITY_TEMPLATE = 
        "package ${packageName};\n\n" +
        "import dev.architectury.registry.registries.DeferredRegister;\n" +
        "import dev.architectury.registry.registries.RegistrySupplier;\n" +
        "import net.minecraft.block.BlockState;\n" +
        "import net.minecraft.block.entity.BlockEntity;\n" +
        "import net.minecraft.block.entity.BlockEntityType;\n" +
        "import net.minecraft.util.math.BlockPos;\n" +
        "import net.minecraft.util.Identifier;\n" +
        "import net.minecraft.registry.Registry;\n\n" +
        "public class ${className} extends BlockEntity {\n" +
        "    public ${className}(BlockPos pos, BlockState state) {\n" +
        "        super(null, pos, state); // Replace null with your BlockEntityType\n" +
        "    }\n" +
        "}\n";
}