package com.modforge.intellij.plugin.crossloader.generator;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiManager;
import com.modforge.intellij.plugin.crossloader.ArchitecturyService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Generator for Architectury templates.
 * Creates file structures for cross-loader mod development using Architectury.
 */
public class ArchitecturyTemplateGenerator {
    private static final Logger LOG = Logger.getInstance(ArchitecturyTemplateGenerator.class);
    
    private final Project project;
    private final ArchitecturyService architecturyService;
    
    /**
     * Creates a new Architectury template generator.
     * @param project The project
     */
    public ArchitecturyTemplateGenerator(@NotNull Project project) {
        this.project = project;
        this.architecturyService = ArchitecturyService.getInstance(project);
    }
    
    /**
     * Generates Architectury files for a mod.
     * @param modId The mod ID
     * @param modName The mod name
     * @param modDescription The mod description
     * @param packageName The package name
     * @param author The author name
     * @param baseDir The base directory
     * @return Whether generation was successful
     */
    public boolean generateArchitecturyMod(
            @NotNull String modId,
            @NotNull String modName,
            @NotNull String modDescription,
            @NotNull String packageName,
            @NotNull String author,
            @Nullable VirtualFile baseDir
    ) {
        if (baseDir == null) {
            LOG.error("Base directory is null");
            return false;
        }
        
        try {
            // Create module directories
            VirtualFile commonDir = baseDir.createChildDirectory(this, "common");
            VirtualFile forgeDir = baseDir.createChildDirectory(this, "forge");
            VirtualFile fabricDir = baseDir.createChildDirectory(this, "fabric");
            
            // Create source directories
            VirtualFile commonSrcDir = createSourceDirs(commonDir);
            VirtualFile forgeSrcDir = createSourceDirs(forgeDir);
            VirtualFile fabricSrcDir = createSourceDirs(fabricDir);
            
            // Create package directories
            String[] packageParts = packageName.split("\\.");
            
            VirtualFile commonPackageDir = createPackageDirs(commonSrcDir, packageParts);
            VirtualFile forgePackageDir = createPackageDirs(forgeSrcDir, packageParts);
            VirtualFile fabricPackageDir = createPackageDirs(fabricSrcDir, packageParts);
            
            // Create forge package directory (com.example.modid.forge)
            String[] forgePackageParts = (packageName + ".forge").split("\\.");
            VirtualFile forgeSubPackageDir = createPackageDirs(forgeSrcDir, forgePackageParts);
            
            // Create fabric package directory (com.example.modid.fabric)
            String[] fabricPackageParts = (packageName + ".fabric").split("\\.");
            VirtualFile fabricSubPackageDir = createPackageDirs(fabricSrcDir, fabricPackageParts);
            
            // Create common mod files
            createCommonModFiles(commonPackageDir, modId, modName, packageName, author);
            
            // Create forge mod files
            createForgeModFiles(forgeSubPackageDir, modId, modName, packageName, author);
            
            // Create fabric mod files
            createFabricModFiles(fabricSubPackageDir, modId, modName, packageName, author);
            
            // Create resource files
            createResourceFiles(commonDir, forgeDir, fabricDir, modId, modName, modDescription, packageName, author);
            
            // Create build files
            createBuildFiles(baseDir, modId, modName, packageName, author);
            
            return true;
        } catch (IOException e) {
            LOG.error("Error generating Architectury mod", e);
            return false;
        }
    }
    
    /**
     * Creates source directories.
     * @param moduleDir The module directory
     * @return The source directory
     */
    @NotNull
    private VirtualFile createSourceDirs(@NotNull VirtualFile moduleDir) throws IOException {
        VirtualFile srcDir = moduleDir.createChildDirectory(this, "src");
        VirtualFile mainDir = srcDir.createChildDirectory(this, "main");
        VirtualFile javaDir = mainDir.createChildDirectory(this, "java");
        mainDir.createChildDirectory(this, "resources");
        
        return javaDir;
    }
    
    /**
     * Creates package directories.
     * @param sourceDir The source directory
     * @param packageParts The package parts
     * @return The package directory
     */
    @NotNull
    private VirtualFile createPackageDirs(@NotNull VirtualFile sourceDir, @NotNull String[] packageParts) throws IOException {
        VirtualFile currentDir = sourceDir;
        
        for (String part : packageParts) {
            currentDir = currentDir.createChildDirectory(this, part);
        }
        
        return currentDir;
    }
    
    /**
     * Creates common mod files.
     * @param packageDir The package directory
     * @param modId The mod ID
     * @param modName The mod name
     * @param packageName The package name
     * @param author The author name
     */
    private void createCommonModFiles(
            @NotNull VirtualFile packageDir,
            @NotNull String modId,
            @NotNull String modName,
            @NotNull String packageName,
            @NotNull String author
    ) throws IOException {
        // Create mod platform class
        createFile(packageDir, toClassName(modId) + "Platform.java", generatePlatformClass(
                packageName, modId, modName
        ));
        
        // Create constants class
        createFile(packageDir, "ModConstants.java", generateConstantsClass(
                packageName, modId, modName, author
        ));
        
        // Create common registry
        createFile(packageDir, "ModRegistry.java", generateRegistryClass(
                packageName, modId
        ));
        
        // Create events directory and files
        VirtualFile eventsDir = packageDir.createChildDirectory(this, "events");
        createFile(eventsDir, "ModEvents.java", generateEventsClass(
                packageName, modId
        ));
    }
    
    /**
     * Creates Forge mod files.
     * @param packageDir The package directory
     * @param modId The mod ID
     * @param modName The mod name
     * @param packageName The package name
     * @param author The author name
     */
    private void createForgeModFiles(
            @NotNull VirtualFile packageDir,
            @NotNull String modId,
            @NotNull String modName,
            @NotNull String packageName,
            @NotNull String author
    ) throws IOException {
        // Create Forge mod class
        createFile(packageDir, toClassName(modId) + "Forge.java", generateForgeModClass(
                packageName, modId, modName
        ));
        
        // Create Forge platform implementations
        createFile(packageDir, toClassName(modId) + "PlatformImpl.java", generateForgePlatformImplClass(
                packageName, modId
        ));
    }
    
    /**
     * Creates Fabric mod files.
     * @param packageDir The package directory
     * @param modId The mod ID
     * @param modName The mod name
     * @param packageName The package name
     * @param author The author name
     */
    private void createFabricModFiles(
            @NotNull VirtualFile packageDir,
            @NotNull String modId,
            @NotNull String modName,
            @NotNull String packageName,
            @NotNull String author
    ) throws IOException {
        // Create Fabric mod class
        createFile(packageDir, toClassName(modId) + "Fabric.java", generateFabricModClass(
                packageName, modId, modName
        ));
        
        // Create Fabric platform implementations
        createFile(packageDir, toClassName(modId) + "PlatformImpl.java", generateFabricPlatformImplClass(
                packageName, modId
        ));
    }
    
    /**
     * Creates resource files.
     * @param commonDir The common directory
     * @param forgeDir The forge directory
     * @param fabricDir The fabric directory
     * @param modId The mod ID
     * @param modName The mod name
     * @param modDescription The mod description
     * @param packageName The package name
     * @param author The author name
     */
    private void createResourceFiles(
            @NotNull VirtualFile commonDir,
            @NotNull VirtualFile forgeDir,
            @NotNull VirtualFile fabricDir,
            @NotNull String modId,
            @NotNull String modName,
            @NotNull String modDescription,
            @NotNull String packageName,
            @NotNull String author
    ) throws IOException {
        // Create common resources
        VirtualFile commonResourcesDir = commonDir.findChild("src/main/resources");
        if (commonResourcesDir != null) {
            // Create assets directory
            VirtualFile assetsDir = commonResourcesDir.createChildDirectory(this, "assets");
            VirtualFile modAssetsDir = assetsDir.createChildDirectory(this, modId);
            
            // Create textures directory
            VirtualFile texturesDir = modAssetsDir.createChildDirectory(this, "textures");
            texturesDir.createChildDirectory(this, "item");
            texturesDir.createChildDirectory(this, "block");
            
            // Create lang directory and file
            VirtualFile langDir = modAssetsDir.createChildDirectory(this, "lang");
            createFile(langDir, "en_us.json", generateLangFile(modName));
        }
        
        // Create forge resources
        VirtualFile forgeResourcesDir = forgeDir.findChild("src/main/resources");
        if (forgeResourcesDir != null) {
            // Create META-INF directory
            VirtualFile metaInfDir = forgeResourcesDir.createChildDirectory(this, "META-INF");
            
            // Create mods.toml file
            createFile(metaInfDir, "mods.toml", generateForgeModsToml(
                    modId, modName, modDescription, author
            ));
        }
        
        // Create fabric resources
        VirtualFile fabricResourcesDir = fabricDir.findChild("src/main/resources");
        if (fabricResourcesDir != null) {
            // Create fabric.mod.json file
            createFile(fabricResourcesDir, "fabric.mod.json", generateFabricModJson(
                    modId, modName, modDescription, packageName, author
            ));
        }
    }
    
    /**
     * Creates build files.
     * @param baseDir The base directory
     * @param modId The mod ID
     * @param modName The mod name
     * @param packageName The package name
     * @param author The author name
     */
    private void createBuildFiles(
            @NotNull VirtualFile baseDir,
            @NotNull String modId,
            @NotNull String modName,
            @NotNull String packageName,
            @NotNull String author
    ) throws IOException {
        // Create gradle properties file
        createFile(baseDir, "gradle.properties", generateGradleProperties(
                modId, modName
        ));
        
        // Create settings.gradle file
        createFile(baseDir, "settings.gradle", generateSettingsGradle(
                modId
        ));
        
        // Create root build.gradle file
        createFile(baseDir, "build.gradle", generateRootBuildGradle());
        
        // Create common build.gradle file
        VirtualFile commonDir = baseDir.findChild("common");
        if (commonDir != null) {
            createFile(commonDir, "build.gradle", generateCommonBuildGradle());
        }
        
        // Create forge build.gradle file
        VirtualFile forgeDir = baseDir.findChild("forge");
        if (forgeDir != null) {
            createFile(forgeDir, "build.gradle", generateForgeBuildGradle());
        }
        
        // Create fabric build.gradle file
        VirtualFile fabricDir = baseDir.findChild("fabric");
        if (fabricDir != null) {
            createFile(fabricDir, "build.gradle", generateFabricBuildGradle());
        }
    }
    
    /**
     * Creates a file.
     * @param dir The directory
     * @param fileName The file name
     * @param content The file content
     */
    private void createFile(@NotNull VirtualFile dir, @NotNull String fileName, @NotNull String content) throws IOException {
        VirtualFile file = dir.createChildData(this, fileName);
        file.setBinaryContent(content.getBytes());
    }
    
    // Template generation methods
    
    /**
     * Generates a platform class.
     * @param packageName The package name
     * @param modId The mod ID
     * @param modName The mod name
     * @return The platform class
     */
    @NotNull
    private String generatePlatformClass(@NotNull String packageName, @NotNull String modId, @NotNull String modName) {
        String className = toClassName(modId) + "Platform";
        
        return "package " + packageName + ";\n\n" +
               "import dev.architectury.injectables.annotations.ExpectPlatform;\n" +
               "import dev.architectury.platform.Platform;\n\n" +
               "import java.nio.file.Path;\n\n" +
               "/**\n" +
               " * Platform-specific functionality for " + modName + ".\n" +
               " */\n" +
               "public class " + className + " {\n" +
               "    /**\n" +
               "     * Gets whether a mod is loaded.\n" +
               "     * @param modId The mod ID\n" +
               "     * @return Whether the mod is loaded\n" +
               "     */\n" +
               "    @ExpectPlatform\n" +
               "    public static boolean isModLoaded(String modId) {\n" +
               "        // This code is eliminated by the platform-specific implementation\n" +
               "        throw new AssertionError();\n" +
               "    }\n\n" +
               "    /**\n" +
               "     * Gets the configuration directory.\n" +
               "     * @return The configuration directory\n" +
               "     */\n" +
               "    @ExpectPlatform\n" +
               "    public static Path getConfigDir() {\n" +
               "        // This code is eliminated by the platform-specific implementation\n" +
               "        throw new AssertionError();\n" +
               "    }\n\n" +
               "    /**\n" +
               "     * Gets whether we're on Forge.\n" +
               "     * @return Whether we're on Forge\n" +
               "     */\n" +
               "    public static boolean isForge() {\n" +
               "        return Platform.isForge();\n" +
               "    }\n\n" +
               "    /**\n" +
               "     * Gets whether we're on Fabric.\n" +
               "     * @return Whether we're on Fabric\n" +
               "     */\n" +
               "    public static boolean isFabric() {\n" +
               "        return Platform.isFabric();\n" +
               "    }\n\n" +
               "    /**\n" +
               "     * Gets whether we're on Quilt.\n" +
               "     * @return Whether we're on Quilt\n" +
               "     */\n" +
               "    public static boolean isQuilt() {\n" +
               "        return Platform.isQuilt();\n" +
               "    }\n" +
               "}\n";
    }
    
    /**
     * Generates a constants class.
     * @param packageName The package name
     * @param modId The mod ID
     * @param modName The mod name
     * @param author The author name
     * @return The constants class
     */
    @NotNull
    private String generateConstantsClass(@NotNull String packageName, @NotNull String modId, @NotNull String modName, @NotNull String author) {
        return "package " + packageName + ";\n\n" +
               "/**\n" +
               " * Constants for " + modName + ".\n" +
               " */\n" +
               "public class ModConstants {\n" +
               "    /**\n" +
               "     * The mod ID.\n" +
               "     */\n" +
               "    public static final String MOD_ID = \"" + modId + "\";\n\n" +
               "    /**\n" +
               "     * The mod name.\n" +
               "     */\n" +
               "    public static final String MOD_NAME = \"" + modName + "\";\n\n" +
               "    /**\n" +
               "     * The mod author.\n" +
               "     */\n" +
               "    public static final String AUTHOR = \"" + author + "\";\n" +
               "}\n";
    }
    
    /**
     * Generates a registry class.
     * @param packageName The package name
     * @param modId The mod ID
     * @return The registry class
     */
    @NotNull
    private String generateRegistryClass(@NotNull String packageName, @NotNull String modId) {
        return "package " + packageName + ";\n\n" +
               "import dev.architectury.registry.registries.DeferredRegister;\n" +
               "import dev.architectury.registry.registries.RegistrySupplier;\n" +
               "import net.minecraft.core.Registry;\n" +
               "import net.minecraft.resources.ResourceLocation;\n" +
               "import net.minecraft.world.item.Item;\n" +
               "import net.minecraft.world.item.CreativeModeTab;\n\n" +
               "/**\n" +
               " * Registry for items, blocks, and other game objects.\n" +
               " */\n" +
               "public class ModRegistry {\n" +
               "    // Item registry\n" +
               "    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ModConstants.MOD_ID, Registry.ITEM_REGISTRY);\n\n" +
               "    // Example item\n" +
               "    public static final RegistrySupplier<Item> EXAMPLE_ITEM = ITEMS.register(\"example_item\",\n" +
               "            () -> new Item(new Item.Properties().tab(CreativeModeTab.TAB_MISC)));\n\n" +
               "    /**\n" +
               "     * Registers all registries.\n" +
               "     */\n" +
               "    public static void register() {\n" +
               "        ITEMS.register();\n" +
               "    }\n" +
               "}\n";
    }
    
    /**
     * Generates an events class.
     * @param packageName The package name
     * @param modId The mod ID
     * @return The events class
     */
    @NotNull
    private String generateEventsClass(@NotNull String packageName, @NotNull String modId) {
        return "package " + packageName + ".events;\n\n" +
               "import dev.architectury.event.events.common.LifecycleEvent;\n" +
               "import dev.architectury.event.events.common.PlayerEvent;\n" +
               "import dev.architectury.event.events.common.TickEvent;\n\n" +
               "/**\n" +
               " * Events for the mod.\n" +
               " */\n" +
               "public class ModEvents {\n" +
               "    /**\n" +
               "     * Registers all event handlers.\n" +
               "     */\n" +
               "    public static void register() {\n" +
               "        // Register server starting event\n" +
               "        LifecycleEvent.SERVER_STARTING.register(server -> {\n" +
               "            System.out.println(\"Server starting!\");\n" +
               "        });\n\n" +
               "        // Register player login event\n" +
               "        PlayerEvent.PLAYER_JOIN.register(player -> {\n" +
               "            System.out.println(\"Player joined: \" + player.getDisplayName().getString());\n" +
               "        });\n\n" +
               "        // Register tick event\n" +
               "        TickEvent.SERVER_POST.register(server -> {\n" +
               "            // Do something every server tick\n" +
               "        });\n" +
               "    }\n" +
               "}\n";
    }
    
    /**
     * Generates a Forge mod class.
     * @param packageName The package name
     * @param modId The mod ID
     * @param modName The mod name
     * @return The Forge mod class
     */
    @NotNull
    private String generateForgeModClass(@NotNull String packageName, @NotNull String modId, @NotNull String modName) {
        String className = toClassName(modId) + "Forge";
        
        return "package " + packageName + ".forge;\n\n" +
               "import " + packageName + ".ModConstants;\n" +
               "import " + packageName + ".ModRegistry;\n" +
               "import " + packageName + ".events.ModEvents;\n" +
               "import net.minecraftforge.fml.common.Mod;\n" +
               "import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;\n\n" +
               "/**\n" +
               " * Forge implementation of " + modName + ".\n" +
               " */\n" +
               "@Mod(ModConstants.MOD_ID)\n" +
               "public class " + className + " {\n" +
               "    /**\n" +
               "     * Constructs a new Forge mod instance.\n" +
               "     */\n" +
               "    public " + className + "() {\n" +
               "        // Register items, blocks, etc.\n" +
               "        ModRegistry.register();\n\n" +
               "        // Register events\n" +
               "        ModEvents.register();\n\n" +
               "        System.out.println(ModConstants.MOD_NAME + \" initialized on Forge!\");\n" +
               "    }\n" +
               "}\n";
    }
    
    /**
     * Generates a Forge platform implementation class.
     * @param packageName The package name
     * @param modId The mod ID
     * @return The Forge platform implementation class
     */
    @NotNull
    private String generateForgePlatformImplClass(@NotNull String packageName, @NotNull String modId) {
        String className = toClassName(modId) + "PlatformImpl";
        
        return "package " + packageName + ".forge;\n\n" +
               "import net.minecraftforge.fml.loading.FMLLoader;\n" +
               "import net.minecraftforge.fml.loading.FMLPaths;\n\n" +
               "import java.nio.file.Path;\n\n" +
               "/**\n" +
               " * Forge implementation of platform-specific functionality.\n" +
               " */\n" +
               "public class " + className + " {\n" +
               "    /**\n" +
               "     * Gets whether a mod is loaded.\n" +
               "     * @param modId The mod ID\n" +
               "     * @return Whether the mod is loaded\n" +
               "     */\n" +
               "    public static boolean isModLoaded(String modId) {\n" +
               "        return FMLLoader.getLoadingModList().getModFileById(modId) != null;\n" +
               "    }\n\n" +
               "    /**\n" +
               "     * Gets the configuration directory.\n" +
               "     * @return The configuration directory\n" +
               "     */\n" +
               "    public static Path getConfigDir() {\n" +
               "        return FMLPaths.CONFIGDIR.get();\n" +
               "    }\n" +
               "}\n";
    }
    
    /**
     * Generates a Fabric mod class.
     * @param packageName The package name
     * @param modId The mod ID
     * @param modName The mod name
     * @return The Fabric mod class
     */
    @NotNull
    private String generateFabricModClass(@NotNull String packageName, @NotNull String modId, @NotNull String modName) {
        String className = toClassName(modId) + "Fabric";
        
        return "package " + packageName + ".fabric;\n\n" +
               "import " + packageName + ".ModConstants;\n" +
               "import " + packageName + ".ModRegistry;\n" +
               "import " + packageName + ".events.ModEvents;\n" +
               "import net.fabricmc.api.ModInitializer;\n\n" +
               "/**\n" +
               " * Fabric implementation of " + modName + ".\n" +
               " */\n" +
               "public class " + className + " implements ModInitializer {\n" +
               "    @Override\n" +
               "    public void onInitialize() {\n" +
               "        // Register items, blocks, etc.\n" +
               "        ModRegistry.register();\n\n" +
               "        // Register events\n" +
               "        ModEvents.register();\n\n" +
               "        System.out.println(ModConstants.MOD_NAME + \" initialized on Fabric!\");\n" +
               "    }\n" +
               "}\n";
    }
    
    /**
     * Generates a Fabric platform implementation class.
     * @param packageName The package name
     * @param modId The mod ID
     * @return The Fabric platform implementation class
     */
    @NotNull
    private String generateFabricPlatformImplClass(@NotNull String packageName, @NotNull String modId) {
        String className = toClassName(modId) + "PlatformImpl";
        
        return "package " + packageName + ".fabric;\n\n" +
               "import net.fabricmc.loader.api.FabricLoader;\n\n" +
               "import java.nio.file.Path;\n\n" +
               "/**\n" +
               " * Fabric implementation of platform-specific functionality.\n" +
               " */\n" +
               "public class " + className + " {\n" +
               "    /**\n" +
               "     * Gets whether a mod is loaded.\n" +
               "     * @param modId The mod ID\n" +
               "     * @return Whether the mod is loaded\n" +
               "     */\n" +
               "    public static boolean isModLoaded(String modId) {\n" +
               "        return FabricLoader.getInstance().isModLoaded(modId);\n" +
               "    }\n\n" +
               "    /**\n" +
               "     * Gets the configuration directory.\n" +
               "     * @return The configuration directory\n" +
               "     */\n" +
               "    public static Path getConfigDir() {\n" +
               "        return FabricLoader.getInstance().getConfigDir();\n" +
               "    }\n" +
               "}\n";
    }
    
    /**
     * Generates a language file.
     * @param modName The mod name
     * @return The language file
     */
    @NotNull
    private String generateLangFile(@NotNull String modName) {
        return "{\n" +
               "  \"item.MOD_ID.example_item\": \"Example Item\",\n" +
               "  \"itemGroup.MOD_ID.tab\": \"" + modName + "\"\n" +
               "}\n";
    }
    
    /**
     * Generates a Forge mods.toml file.
     * @param modId The mod ID
     * @param modName The mod name
     * @param modDescription The mod description
     * @param author The author name
     * @return The Forge mods.toml file
     */
    @NotNull
    private String generateForgeModsToml(@NotNull String modId, @NotNull String modName, @NotNull String modDescription, @NotNull String author) {
        return "modLoader=\"javafml\"\n" +
               "loaderVersion=\"[40,)\"\n" +
               "license=\"All Rights Reserved\"\n\n" +
               "[[mods]]\n" +
               "modId=\"" + modId + "\"\n" +
               "version=\"${version}\"\n" +
               "displayName=\"" + modName + "\"\n" +
               "authors=\"" + author + "\"\n" +
               "description='''\n" + modDescription + "\n'''\n\n" +
               "[[dependencies." + modId + "]]\n" +
               "    modId=\"forge\"\n" +
               "    mandatory=true\n" +
               "    versionRange=\"[40,)\"\n" +
               "    ordering=\"NONE\"\n" +
               "    side=\"BOTH\"\n" +
               "[[dependencies." + modId + "]]\n" +
               "    modId=\"minecraft\"\n" +
               "    mandatory=true\n" +
               "    versionRange=\"[1.18.2,1.19)\"\n" +
               "    ordering=\"NONE\"\n" +
               "    side=\"BOTH\"\n";
    }
    
    /**
     * Generates a Fabric mod.json file.
     * @param modId The mod ID
     * @param modName The mod name
     * @param modDescription The mod description
     * @param packageName The package name
     * @param author The author name
     * @return The Fabric mod.json file
     */
    @NotNull
    private String generateFabricModJson(@NotNull String modId, @NotNull String modName, @NotNull String modDescription, @NotNull String packageName, @NotNull String author) {
        return "{\n" +
               "  \"schemaVersion\": 1,\n" +
               "  \"id\": \"" + modId + "\",\n" +
               "  \"version\": \"${version}\",\n" +
               "  \"name\": \"" + modName + "\",\n" +
               "  \"description\": \"" + modDescription + "\",\n" +
               "  \"authors\": [\"" + author + "\"],\n" +
               "  \"contact\": {\n" +
               "    \"homepage\": \"https://example.com/\",\n" +
               "    \"sources\": \"https://github.com/example/" + modId + "\"\n" +
               "  },\n" +
               "  \"license\": \"All Rights Reserved\",\n" +
               "  \"icon\": \"assets/" + modId + "/icon.png\",\n" +
               "  \"environment\": \"*\",\n" +
               "  \"entrypoints\": {\n" +
               "    \"main\": [\"" + packageName + ".fabric." + toClassName(modId) + "Fabric\"]\n" +
               "  },\n" +
               "  \"depends\": {\n" +
               "    \"fabricloader\": \">=0.14.9\",\n" +
               "    \"fabric\": \"*\",\n" +
               "    \"minecraft\": \"1.18.x\",\n" +
               "    \"java\": \">=17\",\n" +
               "    \"architectury\": \">=4.9.83\"\n" +
               "  }\n" +
               "}\n";
    }
    
    /**
     * Generates a gradle.properties file.
     * @param modId The mod ID
     * @param modName The mod name
     * @return The gradle.properties file
     */
    @NotNull
    private String generateGradleProperties(@NotNull String modId, @NotNull String modName) {
        return "# Project metadata\n" +
               "archives_base_name=" + modId + "\n" +
               "maven_group=com.example\n" +
               "group=com.example\n" +
               "mod_version=1.0.0\n\n" +
               "# Architectury properties\n" +
               "minecraft_version=1.18.2\n" +
               "architectury_version=4.9.83\n\n" +
               "# Forge properties\n" +
               "forge_version=40.1.84\n\n" +
               "# Fabric properties\n" +
               "fabric_loader_version=0.14.9\n" +
               "fabric_api_version=0.58.0+1.18.2\n\n" +
               "# Gradle settings\n" +
               "org.gradle.jvmargs=-Xmx3G\n" +
               "org.gradle.daemon=false\n";
    }
    
    /**
     * Generates a settings.gradle file.
     * @param modId The mod ID
     * @return The settings.gradle file
     */
    @NotNull
    private String generateSettingsGradle(@NotNull String modId) {
        return "pluginManagement {\n" +
               "    repositories {\n" +
               "        maven { url \"https://maven.fabricmc.net/\" }\n" +
               "        maven { url \"https://maven.architectury.dev/\" }\n" +
               "        maven { url \"https://maven.minecraftforge.net/\" }\n" +
               "        gradlePluginPortal()\n" +
               "    }\n" +
               "}\n\n" +
               "rootProject.name = \"" + modId + "\"\n" +
               "include 'common'\n" +
               "include 'fabric'\n" +
               "include 'forge'\n";
    }
    
    /**
     * Generates a root build.gradle file.
     * @return The root build.gradle file
     */
    @NotNull
    private String generateRootBuildGradle() {
        return "plugins {\n" +
               "    id 'architectury-plugin' version '3.4-SNAPSHOT'\n" +
               "    id 'dev.architectury.loom' version '0.12.0-SNAPSHOT' apply false\n" +
               "}\n\n" +
               "architectury {\n" +
               "    minecraft = rootProject.minecraft_version\n" +
               "}\n\n" +
               "subprojects {\n" +
               "    apply plugin: 'dev.architectury.loom'\n\n" +
               "    loom {\n" +
               "        silentMojangMappingsLicense()\n" +
               "    }\n\n" +
               "    dependencies {\n" +
               "        minecraft \"com.mojang:minecraft:${rootProject.minecraft_version}\"\n" +
               "        // Add mappings\n" +
               "        // example: mappings \"net.fabricmc:yarn:1.18.2+build.3:v2\"\n" +
               "    }\n" +
               "}\n\n" +
               "allprojects {\n" +
               "    apply plugin: 'java'\n" +
               "    apply plugin: 'architectury-plugin'\n" +
               "    apply plugin: 'maven-publish'\n\n" +
               "    archivesBaseName = rootProject.archives_base_name\n" +
               "    version = rootProject.mod_version\n" +
               "    group = rootProject.maven_group\n\n" +
               "    repositories {\n" +
               "        // Add repositories\n" +
               "        maven { url \"https://maven.architectury.dev/\" }\n" +
               "        maven { url \"https://maven.fabricmc.net/\" }\n" +
               "        maven { url \"https://maven.minecraftforge.net/\" }\n" +
               "    }\n\n" +
               "    tasks.withType(JavaCompile) {\n" +
               "        options.encoding = 'UTF-8'\n" +
               "        options.release = 17\n" +
               "    }\n\n" +
               "    java {\n" +
               "        withSourcesJar()\n" +
               "    }\n" +
               "}\n";
    }
    
    /**
     * Generates a common build.gradle file.
     * @return The common build.gradle file
     */
    @NotNull
    private String generateCommonBuildGradle() {
        return "architectury {\n" +
               "    common(rootProject.enabled_platforms.split(\",\"))\n" +
               "}\n\n" +
               "dependencies {\n" +
               "    // Add dependencies\n" +
               "    modImplementation \"dev.architectury:architectury:${rootProject.architectury_version}\"\n" +
               "}\n\n" +
               "publishing {\n" +
               "    publications {\n" +
               "        mavenCommon(MavenPublication) {\n" +
               "            artifactId = rootProject.archives_base_name\n" +
               "            from components.java\n" +
               "        }\n" +
               "    }\n\n" +
               "    repositories {\n" +
               "        // Add repositories\n" +
               "    }\n" +
               "}\n";
    }
    
    /**
     * Generates a Forge build.gradle file.
     * @return The Forge build.gradle file
     */
    @NotNull
    private String generateForgeBuildGradle() {
        return "architectury {\n" +
               "    platformSetupLoomIde()\n" +
               "}\n\n" +
               "loom {\n" +
               "    forge {\n" +
               "        mixinConfig \"mixins.${rootProject.archives_base_name}.json\"\n" +
               "    }\n" +
               "}\n\n" +
               "dependencies {\n" +
               "    forge \"net.minecraftforge:forge:${rootProject.minecraft_version}-${rootProject.forge_version}\"\n" +
               "    // Add Forge-specific dependencies\n" +
               "    modImplementation \"dev.architectury:architectury-forge:${rootProject.architectury_version}\"\n\n" +
               "    implementation project(\":common\")\n" +
               "}\n\n" +
               "processResources {\n" +
               "    inputs.property \"version\", project.version\n\n" +
               "    filesMatching(\"META-INF/mods.toml\") {\n" +
               "        expand \"version\": project.version\n" +
               "    }\n" +
               "}\n\n" +
               "publishing {\n" +
               "    publications {\n" +
               "        mavenForge(MavenPublication) {\n" +
               "            artifactId = rootProject.archives_base_name + \"-forge\"\n" +
               "            from components.java\n" +
               "        }\n" +
               "    }\n\n" +
               "    repositories {\n" +
               "        // Add repositories\n" +
               "    }\n" +
               "}\n";
    }
    
    /**
     * Generates a Fabric build.gradle file.
     * @return The Fabric build.gradle file
     */
    @NotNull
    private String generateFabricBuildGradle() {
        return "architectury {\n" +
               "    platformSetupLoomIde()\n" +
               "}\n\n" +
               "loom {\n" +
               "    accessWidenerPath = project(\":common\").file(\"src/main/resources/example.accesswidener\")\n" +
               "}\n\n" +
               "dependencies {\n" +
               "    modImplementation \"net.fabricmc:fabric-loader:${rootProject.fabric_loader_version}\"\n" +
               "    modImplementation \"net.fabricmc.fabric-api:fabric-api:${rootProject.fabric_api_version}\"\n" +
               "    // Add Fabric-specific dependencies\n" +
               "    modImplementation \"dev.architectury:architectury-fabric:${rootProject.architectury_version}\"\n\n" +
               "    implementation project(\":common\")\n" +
               "}\n\n" +
               "processResources {\n" +
               "    inputs.property \"version\", project.version\n\n" +
               "    filesMatching(\"fabric.mod.json\") {\n" +
               "        expand \"version\": project.version\n" +
               "    }\n" +
               "}\n\n" +
               "publishing {\n" +
               "    publications {\n" +
               "        mavenFabric(MavenPublication) {\n" +
               "            artifactId = rootProject.archives_base_name + \"-fabric\"\n" +
               "            from components.java\n" +
               "        }\n" +
               "    }\n\n" +
               "    repositories {\n" +
               "        // Add repositories\n" +
               "    }\n" +
               "}\n";
    }
    
    /**
     * Converts a string to a class name (PascalCase).
     * @param str The string
     * @return The class name
     */
    @NotNull
    private String toClassName(@NotNull String str) {
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
}