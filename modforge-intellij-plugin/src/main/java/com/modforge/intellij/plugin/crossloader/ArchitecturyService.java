package com.modforge.intellij.plugin.crossloader;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service for working with Architectury in cross-loader mod development.
 * Provides utilities for analyzing and generating code that works across
 * multiple mod loaders (Forge, Fabric, Quilt, etc.).
 */
@Service
public final class ArchitecturyService {
    private static final Logger LOG = Logger.getInstance(ArchitecturyService.class);
    
    private final Project project;
    private final Map<String, ModLoaderInfo> detectedLoaders = new ConcurrentHashMap<>();
    private final Map<String, String> loaderEquivalentClasses = new ConcurrentHashMap<>();
    
    // Architectury detection
    private boolean isArchitecturyProject = false;
    private boolean isMultiModuleSetup = false;
    private boolean initialDetectionDone = false;
    
    /**
     * Creates a new Architectury service.
     * @param project The project
     */
    public ArchitecturyService(@NotNull Project project) {
        this.project = project;
        initializeEquivalentClasses();
        
        LOG.info("Initialized ArchitecturyService for project: " + project.getName());
    }
    
    /**
     * Gets the instance of this service.
     * @param project The project
     * @return The service instance
     */
    public static ArchitecturyService getInstance(@NotNull Project project) {
        return project.getService(ArchitecturyService.class);
    }
    
    /**
     * Initializes the map of equivalent classes across mod loaders.
     * These mappings help translate code between different mod loaders.
     */
    private void initializeEquivalentClasses() {
        // Forge -> Fabric mappings
        addEquivalentClass("net.minecraftforge.fml.common.Mod", "net.fabricmc.api.ModInitializer");
        addEquivalentClass("net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent", "net.fabricmc.api.ClientModInitializer");
        addEquivalentClass("net.minecraftforge.registries.ForgeRegistries", "net.minecraft.core.Registry");
        addEquivalentClass("net.minecraftforge.registries.DeferredRegister", "net.fabricmc.fabric.api.registry.Registry");
        addEquivalentClass("net.minecraftforge.registries.RegisterEvent", "net.fabricmc.fabric.api.event.registry.RegistryEntryCallback");
        addEquivalentClass("net.minecraftforge.eventbus.api.SubscribeEvent", "net.fabricmc.fabric.api.event.Event");
        addEquivalentClass("net.minecraftforge.client.event.RegisterColorHandlersEvent", "net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry");
        
        // Quilt mappings (mostly compatible with Fabric, but with some differences)
        addEquivalentClass("net.fabricmc.api.ModInitializer", "org.quiltmc.qsl.base.api.entrypoint.ModInitializer");
        addEquivalentClass("net.fabricmc.api.ClientModInitializer", "org.quiltmc.qsl.base.api.entrypoint.client.ClientModInitializer");
        
        // More mappings would be added in a complete implementation
    }
    
    /**
     * Adds an equivalent class mapping.
     * @param sourceClass The source class
     * @param targetClass The target class
     */
    private void addEquivalentClass(@NotNull String sourceClass, @NotNull String targetClass) {
        loaderEquivalentClasses.put(sourceClass, targetClass);
        loaderEquivalentClasses.put(targetClass, sourceClass); // Add bidirectional mapping
    }
    
    /**
     * Gets the equivalent class for a given class in another mod loader.
     * @param className The class name
     * @param targetLoader The target mod loader
     * @return The equivalent class, or null if none found
     */
    @Nullable
    public String getEquivalentClass(@NotNull String className, @NotNull ModLoader targetLoader) {
        return loaderEquivalentClasses.get(className);
    }
    
    /**
     * Detects mod loaders used in the project.
     * @return The detected mod loaders
     */
    @NotNull
    public List<ModLoaderInfo> detectModLoaders() {
        if (!initialDetectionDone) {
            scanProjectForModLoaders();
            initialDetectionDone = true;
        }
        
        return new ArrayList<>(detectedLoaders.values());
    }
    
    /**
     * Checks if the project is using Architectury.
     * @return Whether the project is using Architectury
     */
    public boolean isArchitecturyProject() {
        if (!initialDetectionDone) {
            scanProjectForModLoaders();
            initialDetectionDone = true;
        }
        
        return isArchitecturyProject;
    }
    
    /**
     * Checks if the project is using a multi-module setup.
     * @return Whether the project is using a multi-module setup
     */
    public boolean isMultiModuleSetup() {
        if (!initialDetectionDone) {
            scanProjectForModLoaders();
            initialDetectionDone = true;
        }
        
        return isMultiModuleSetup;
    }
    
    /**
     * Scans the project for mod loaders and Architectury.
     */
    private void scanProjectForModLoaders() {
        detectedLoaders.clear();
        
        Module[] modules = ModuleManager.getInstance(project).getModules();
        isMultiModuleSetup = modules.length > 1;
        
        // Check each module
        for (Module module : modules) {
            String moduleName = module.getName();
            ModuleRootManager rootManager = ModuleRootManager.getInstance(module);
            
            // Check for common module names that indicate specific mod loaders
            if (moduleName.contains("forge") || moduleName.endsWith("-forge")) {
                detectedLoaders.put("forge", new ModLoaderInfo(ModLoader.FORGE, module, "net.minecraftforge"));
            } else if (moduleName.contains("fabric") || moduleName.endsWith("-fabric")) {
                detectedLoaders.put("fabric", new ModLoaderInfo(ModLoader.FABRIC, module, "net.fabricmc"));
            } else if (moduleName.contains("quilt") || moduleName.endsWith("-quilt")) {
                detectedLoaders.put("quilt", new ModLoaderInfo(ModLoader.QUILT, module, "org.quiltmc"));
            } else if (moduleName.contains("common") || moduleName.endsWith("-common")) {
                // This might be an Architectury common module
                checkForArchitecturyInModule(module);
            }
            
            // Check library classes in the module
            for (VirtualFile lib : rootManager.getFiles(OrderRootType.CLASSES)) {
                String libPath = lib.getPath();
                if (libPath.contains("architectury")) {
                    isArchitecturyProject = true;
                }
                
                // Detect mod loaders by their libraries
                if (libPath.contains("forge") || libPath.contains("minecraftforge")) {
                    detectedLoaders.putIfAbsent("forge", new ModLoaderInfo(ModLoader.FORGE, module, "net.minecraftforge"));
                } else if (libPath.contains("fabric") || libPath.contains("fabricmc")) {
                    detectedLoaders.putIfAbsent("fabric", new ModLoaderInfo(ModLoader.FABRIC, module, "net.fabricmc"));
                } else if (libPath.contains("quilt") || libPath.contains("quiltmc")) {
                    detectedLoaders.putIfAbsent("quilt", new ModLoaderInfo(ModLoader.QUILT, module, "org.quiltmc"));
                }
            }
        }
        
        LOG.info("Detected mod loaders: " + detectedLoaders.keySet());
        LOG.info("Is Architectury project: " + isArchitecturyProject);
        LOG.info("Is multi-module setup: " + isMultiModuleSetup);
    }
    
    /**
     * Checks if a module is using Architectury.
     * @param module The module
     */
    private void checkForArchitecturyInModule(@NotNull Module module) {
        ModuleRootManager rootManager = ModuleRootManager.getInstance(module);
        for (VirtualFile lib : rootManager.getFiles(OrderRootType.CLASSES)) {
            if (lib.getPath().contains("architectury")) {
                isArchitecturyProject = true;
                break;
            }
        }
        
        // More detailed check would analyze imports and gradle files
    }
    
    /**
     * Gets an Architectury code snippet to help with cross-loader development.
     * @param featureType The feature type
     * @return The code snippet
     */
    @NotNull
    public String getArchitecturySnippet(@NotNull ArchitecturyFeatureType featureType) {
        switch (featureType) {
            case MOD_INITIALIZATION:
                return getModInitializationSnippet();
            case REGISTRY:
                return getRegistrySnippet();
            case NETWORK:
                return getNetworkSnippet();
            case EVENT_HANDLING:
                return getEventHandlingSnippet();
            case PLATFORM_CONDITION:
                return getPlatformConditionSnippet();
            default:
                return "// No snippet available for this feature type";
        }
    }
    
    /**
     * Gets a code snippet for mod initialization.
     * @return The code snippet
     */
    @NotNull
    private String getModInitializationSnippet() {
        return "// Architectury Mod Initialization\n" +
               "import dev.architectury.platform.Platform;\n\n" +
               "public class ${MOD_ID}Platform {\n" +
               "    public static final String MOD_ID = \"${MOD_ID}\";\n\n" +
               "    public static void initialize() {\n" +
               "        // Common initialization code\n" +
               "        if (Platform.isForgeLike()) {\n" +
               "            // Forge-specific initialization\n" +
               "        } else if (Platform.isFabric()) {\n" +
               "            // Fabric-specific initialization\n" +
               "        } else if (Platform.isQuilt()) {\n" +
               "            // Quilt-specific initialization\n" +
               "        }\n" +
               "    }\n" +
               "}";
    }
    
    /**
     * Gets a code snippet for registry management.
     * @return The code snippet
     */
    @NotNull
    private String getRegistrySnippet() {
        return "// Architectury Registry Management\n" +
               "import dev.architectury.registry.registries.DeferredRegister;\n" +
               "import dev.architectury.registry.registries.RegistrySupplier;\n" +
               "import net.minecraft.core.Registry;\n" +
               "import net.minecraft.resources.ResourceLocation;\n" +
               "import net.minecraft.world.item.Item;\n\n" +
               "public class ${MOD_ID}Registry {\n" +
               "    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(\"${MOD_ID}\", Registry.ITEM_REGISTRY);\n\n" +
               "    // Register an item\n" +
               "    public static final RegistrySupplier<Item> EXAMPLE_ITEM = ITEMS.register(\"example_item\", \n" +
               "        () -> new Item(new Item.Properties().tab(CreativeModeTab.TAB_MISC)));\n\n" +
               "    public static void register() {\n" +
               "        ITEMS.register();\n" +
               "    }\n" +
               "}";
    }
    
    /**
     * Gets a code snippet for network handling.
     * @return The code snippet
     */
    @NotNull
    private String getNetworkSnippet() {
        return "// Architectury Network Handling\n" +
               "import dev.architectury.networking.NetworkManager;\n" +
               "import dev.architectury.networking.NetworkChannel;\n" +
               "import net.minecraft.network.FriendlyByteBuf;\n" +
               "import net.minecraft.resources.ResourceLocation;\n\n" +
               "public class ${MOD_ID}Network {\n" +
               "    private static final NetworkChannel CHANNEL = NetworkChannel.create(\n" +
               "        new ResourceLocation(\"${MOD_ID}\", \"main\"));\n\n" +
               "    public static void register() {\n" +
               "        CHANNEL.register(ExamplePacket.class, ExamplePacket::encode, ExamplePacket::new, \n" +
               "            (packet, context) -> {\n" +
               "                // Handle packet\n" +
               "                context.queue(() -> {\n" +
               "                    // Run on main thread\n" +
               "                    packet.handle(context);\n" +
               "                });\n" +
               "            });\n" +
               "    }\n\n" +
               "    public static class ExamplePacket {\n" +
               "        private final String message;\n\n" +
               "        public ExamplePacket(FriendlyByteBuf buf) {\n" +
               "            message = buf.readUtf();\n" +
               "        }\n\n" +
               "        public ExamplePacket(String message) {\n" +
               "            this.message = message;\n" +
               "        }\n\n" +
               "        public void encode(FriendlyByteBuf buf) {\n" +
               "            buf.writeUtf(message);\n" +
               "        }\n\n" +
               "        public void handle(NetworkManager.PacketContext context) {\n" +
               "            // Handle packet\n" +
               "        }\n" +
               "    }\n" +
               "}";
    }
    
    /**
     * Gets a code snippet for event handling.
     * @return The code snippet
     */
    @NotNull
    private String getEventHandlingSnippet() {
        return "// Architectury Event Handling\n" +
               "import dev.architectury.event.EventResult;\n" +
               "import dev.architectury.event.events.common.EntityEvent;\n" +
               "import dev.architectury.event.events.common.LifecycleEvent;\n" +
               "import dev.architectury.event.events.client.ClientLifecycleEvent;\n\n" +
               "public class ${MOD_ID}Events {\n" +
               "    public static void register() {\n" +
               "        // Server/Common lifecycle events\n" +
               "        LifecycleEvent.SERVER_STARTING.register(server -> {\n" +
               "            // Server starting\n" +
               "        });\n\n" +
               "        // Client-specific events\n" +
               "        ClientLifecycleEvent.CLIENT_STARTED.register(client -> {\n" +
               "            // Client started\n" +
               "        });\n\n" +
               "        // Entity events\n" +
               "        EntityEvent.LIVING_DEATH.register((entity, source) -> {\n" +
               "            // Entity died\n" +
               "            return EventResult.pass();\n" +
               "        });\n" +
               "    }\n" +
               "}";
    }
    
    /**
     * Gets a code snippet for platform conditions.
     * @return The code snippet
     */
    @NotNull
    private String getPlatformConditionSnippet() {
        return "// Architectury Platform Conditions\n" +
               "import dev.architectury.injectables.annotations.ExpectPlatform;\n" +
               "import dev.architectury.platform.Platform;\n\n" +
               "public class ${MOD_ID}Platform {\n" +
               "    // Check which platform (mod loader) we're on\n" +
               "    public static boolean isForge() {\n" +
               "        return Platform.isForge();\n" +
               "    }\n\n" +
               "    public static boolean isFabric() {\n" +
               "        return Platform.isFabric();\n" +
               "    }\n\n" +
               "    // Platform-specific methods (implemented in platform subprojects)\n" +
               "    @ExpectPlatform\n" +
               "    public static boolean isModLoaded(String modId) {\n" +
               "        // This code is eliminated by the compiler, and replaced by platform-specific code\n" +
               "        throw new AssertionError();\n" +
               "    }\n\n" +
               "    // Usage of platform-specific code\n" +
               "    public static void example() {\n" +
               "        if (Platform.isForge()) {\n" +
               "            // Forge-specific code\n" +
               "        } else if (Platform.isFabric()) {\n" +
               "            // Fabric-specific code\n" +
               "        } else if (Platform.isQuilt()) {\n" +
               "            // Quilt-specific code\n" +
               "        }\n" +
               "    }\n" +
               "}";
    }
    
    /**
     * Converts a class from one mod loader to another.
     * @param sourceClass The source class
     * @param sourceLoader The source mod loader
     * @param targetLoader The target mod loader
     * @return The converted class, or null if conversion is not possible
     */
    @Nullable
    public String convertClass(@NotNull PsiClass sourceClass, @NotNull ModLoader sourceLoader, @NotNull ModLoader targetLoader) {
        // In a real implementation, this would analyze the class structure and convert it
        // For demonstration, we just return a basic converted class
        String className = sourceClass.getName();
        if (className == null) {
            return null;
        }
        
        // Get the file text and convert it
        PsiFile file = sourceClass.getContainingFile();
        String fileText = file.getText();
        
        // Convert imports
        List<String> imports = extractImports(fileText);
        List<String> convertedImports = imports.stream()
                .map(importStr -> convertImport(importStr, sourceLoader, targetLoader))
                .collect(Collectors.toList());
        
        // Convert annotations
        String modifiedText = fileText;
        if (sourceLoader == ModLoader.FORGE && targetLoader == ModLoader.FABRIC) {
            modifiedText = modifiedText.replace("@Mod(", "@ModInitializer // Converted from @Mod(");
            modifiedText = modifiedText.replace("@SubscribeEvent", "@Override // Converted from @SubscribeEvent");
        } else if (sourceLoader == ModLoader.FABRIC && targetLoader == ModLoader.FORGE) {
            modifiedText = modifiedText.replace("@ModInitializer", "@Mod(${MOD_ID}) // Converted from @ModInitializer");
            modifiedText = modifiedText.replace("@Override", "@SubscribeEvent // Converted from @Override");
        }
        
        return modifiedText;
    }
    
    /**
     * Extracts imports from a file.
     * @param fileText The file text
     * @return The imports
     */
    @NotNull
    private List<String> extractImports(@NotNull String fileText) {
        List<String> imports = new ArrayList<>();
        String[] lines = fileText.split("\n");
        
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("import ") && line.endsWith(";")) {
                imports.add(line);
            }
        }
        
        return imports;
    }
    
    /**
     * Converts an import from one mod loader to another.
     * @param importStr The import string
     * @param sourceLoader The source mod loader
     * @param targetLoader The target mod loader
     * @return The converted import
     */
    @NotNull
    private String convertImport(@NotNull String importStr, @NotNull ModLoader sourceLoader, @NotNull ModLoader targetLoader) {
        // Extract class name from import
        String className = importStr.substring(7, importStr.length() - 1); // Remove "import " and ";"
        
        // Check if this class has an equivalent in the target loader
        String targetClass = getEquivalentClass(className, targetLoader);
        if (targetClass != null) {
            return "import " + targetClass + ";";
        }
        
        return importStr;
    }
    
    /**
     * Gets the project structure recommendation for Architectury.
     * @return The project structure recommendation
     */
    @NotNull
    public String getProjectStructureRecommendation() {
        return "# Recommended Architectury Project Structure\n\n" +
               "```\n" +
               "my-mod/\n" +
               "├── gradle/\n" +
               "├── common/\n" +
               "│   ├── src/main/java/\n" +
               "│   │   └── com/example/mymod/\n" +
               "│   │       ├── MyMod.java            # Common mod code\n" +
               "│   │       ├── registry/             # Common registry code\n" +
               "│   │       └── events/               # Common event handling\n" +
               "│   └── src/main/resources/\n" +
               "│       └── assets/mymod/\n" +
               "├── fabric/\n" +
               "│   ├── src/main/java/\n" +
               "│   │   └── com/example/mymod/fabric/\n" +
               "│   │       └── MyModFabric.java      # Fabric entry point\n" +
               "│   └── src/main/resources/\n" +
               "│       ├── fabric.mod.json           # Fabric mod metadata\n" +
               "│       └── assets/mymod/\n" +
               "├── forge/\n" +
               "│   ├── src/main/java/\n" +
               "│   │   └── com/example/mymod/forge/\n" +
               "│   │       └── MyModForge.java       # Forge entry point\n" +
               "│   └── src/main/resources/\n" +
               "│       ├── META-INF/mods.toml        # Forge mod metadata\n" +
               "│       └── assets/mymod/\n" +
               "├── quilt/                            # Optional Quilt support\n" +
               "│   ├── src/main/java/\n" +
               "│   │   └── com/example/mymod/quilt/\n" +
               "│   │       └── MyModQuilt.java       # Quilt entry point\n" +
               "│   └── src/main/resources/\n" +
               "│       ├── quilt.mod.json            # Quilt mod metadata\n" +
               "│       └── assets/mymod/\n" +
               "├── build.gradle\n" +
               "├── common.gradle\n" +
               "├── fabric.gradle\n" +
               "├── forge.gradle\n" +
               "├── quilt.gradle\n" +
               "└── settings.gradle\n" +
               "```\n\n" +
               "## Key Files for Cross-Loader Development\n\n" +
               "### Common Module (`common/src/main/java/com/example/mymod/MyMod.java`)\n" +
               "Contains shared code that works across all platforms.\n\n" +
               "### Platform Modules\n" +
               "- **Forge**: `forge/src/main/java/com/example/mymod/forge/MyModForge.java`\n" +
               "- **Fabric**: `fabric/src/main/java/com/example/mymod/fabric/MyModFabric.java`\n" +
               "- **Quilt**: `quilt/src/main/java/com/example/mymod/quilt/MyModQuilt.java`\n\n" +
               "Each platform module contains platform-specific code and metadata.";
    }
    
    /**
     * Gets the recommended build configuration for Architectury.
     * @return The build configuration recommendation
     */
    @NotNull
    public String getBuildConfigurationRecommendation() {
        return "# Recommended Architectury Build Configuration\n\n" +
               "## Root `build.gradle`\n\n" +
               "```groovy\n" +
               "plugins {\n" +
               "    id \"architectury-plugin\" version \"3.4-SNAPSHOT\"\n" +
               "    id \"dev.architectury.loom\" version \"1.0-SNAPSHOT\" apply false\n" +
               "}\n\n" +
               "architectury {\n" +
               "    minecraft = rootProject.minecraft_version\n" +
               "}\n\n" +
               "subprojects {\n" +
               "    apply plugin: \"dev.architectury.loom\"\n\n" +
               "    loom {\n" +
               "        silentMojangMappingsLicense()\n" +
               "    }\n\n" +
               "    dependencies {\n" +
               "        minecraft \"com.mojang:minecraft:${rootProject.minecraft_version}\"\n" +
               "        mappings \"net.fabricmc:yarn:${rootProject.yarn_mappings}:v2\"\n" +
               "    }\n" +
               "}\n" +
               "```\n\n" +
               "## Common Module `common/build.gradle`\n\n" +
               "```groovy\n" +
               "architectury {\n" +
               "    common(rootProject.enabled_platforms.split(\",\"))\n" +
               "}\n\n" +
               "dependencies {\n" +
               "    // Common dependencies\n" +
               "    modImplementation \"dev.architectury:architectury:${rootProject.architectury_version}\"\n" +
               "}\n" +
               "```\n\n" +
               "## Fabric Module `fabric/build.gradle`\n\n" +
               "```groovy\n" +
               "architectury {\n" +
               "    platformSetupLoomIde()\n" +
               "}\n\n" +
               "loom {\n" +
               "    accessWidenerPath = project(\":common\").file(\"src/main/resources/mymod.accesswidener\")\n" +
               "}\n\n" +
               "dependencies {\n" +
               "    modImplementation \"net.fabricmc:fabric-loader:${rootProject.fabric_loader_version}\"\n" +
               "    modImplementation \"net.fabricmc.fabric-api:fabric-api:${rootProject.fabric_api_version}\"\n" +
               "    modImplementation \"dev.architectury:architectury-fabric:${rootProject.architectury_version}\"\n\n" +
               "    implementation project(\":common\")\n" +
               "}\n" +
               "```\n\n" +
               "## Forge Module `forge/build.gradle`\n\n" +
               "```groovy\n" +
               "architectury {\n" +
               "    platformSetupLoomIde()\n" +
               "}\n\n" +
               "loom {\n" +
               "    forge {\n" +
               "        convertAccessWideners = true\n" +
               "        extraAccessWideners.add loom.accessWidenerPath.get().toString()\n" +
               "    }\n" +
               "}\n\n" +
               "dependencies {\n" +
               "    forge \"net.minecraftforge:forge:${rootProject.minecraft_version}-${rootProject.forge_version}\"\n" +
               "    modImplementation \"dev.architectury:architectury-forge:${rootProject.architectury_version}\"\n\n" +
               "    implementation project(\":common\")\n" +
               "}\n" +
               "```\n\n" +
               "## gradle.properties\n\n" +
               "```properties\n" +
               "org.gradle.jvmargs=-Xmx2G\n\n" +
               "minecraft_version=1.19.2\n" +
               "enabled_platforms=fabric,forge,quilt\n\n" +
               "architectury_version=6.5.85\n\n" +
               "fabric_loader_version=0.14.21\n" +
               "fabric_api_version=0.76.0+1.19.2\n\n" +
               "forge_version=43.2.8\n\n" +
               "quilt_loader_version=0.19.1\n" +
               "quilt_fabric_api_version=4.0.0-beta.30+0.76.0-1.19.2\n" +
               "```";
    }
    
    /**
     * Feature types supported by Architectury.
     */
    public enum ArchitecturyFeatureType {
        MOD_INITIALIZATION,
        REGISTRY,
        NETWORK,
        EVENT_HANDLING,
        PLATFORM_CONDITION
    }
    
    /**
     * Mod loaders supported by Architectury.
     */
    public enum ModLoader {
        FORGE("Forge"),
        FABRIC("Fabric"),
        QUILT("Quilt");
        
        private final String displayName;
        
        ModLoader(String displayName) {
            this.displayName = displayName;
        }
        
        @NotNull
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * Information about a mod loader detected in the project.
     */
    public static class ModLoaderInfo {
        private final ModLoader loader;
        private final Module module;
        private final String basePackage;
        
        public ModLoaderInfo(@NotNull ModLoader loader, @NotNull Module module, @NotNull String basePackage) {
            this.loader = loader;
            this.module = module;
            this.basePackage = basePackage;
        }
        
        @NotNull
        public ModLoader getLoader() {
            return loader;
        }
        
        @NotNull
        public Module getModule() {
            return module;
        }
        
        @NotNull
        public String getBasePackage() {
            return basePackage;
        }
        
        @Override
        public String toString() {
            return loader.getDisplayName() + " (" + module.getName() + ")";
        }
    }
}