package com.modforge.intellij.plugin.templates;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.modforge.intellij.plugin.services.ModAuthenticationManager;
import com.modforge.intellij.plugin.utils.ModLoaderContributor;
import com.modforge.intellij.plugin.utils.ModLoaderDetector;
import com.modforge.intellij.plugin.utils.TokenAuthConnectionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Service for managing mod templates.
 */
@Service(Service.Level.PROJECT)
public final class ModTemplateService {
    private static final Logger LOG = Logger.getInstance(ModTemplateService.class);
    private static final Gson GSON = new Gson();
    
    private final Project project;
    private final List<ModTemplate> templates = new CopyOnWriteArrayList<>();
    private final Map<String, ModTemplate> templatesById = new ConcurrentHashMap<>();
    private final AtomicBoolean loaded = new AtomicBoolean(false);
    
    /**
     * Create a new mod template service.
     *
     * @param project The project
     */
    public ModTemplateService(@NotNull Project project) {
        this.project = project;
    }
    
    /**
     * Load templates.
     *
     * @return A CompletableFuture that will be completed when templates are loaded
     */
    @NotNull
    public CompletableFuture<Void> loadTemplates() {
        if (loaded.get()) {
            return CompletableFuture.completedFuture(null);
        }
        
        // Load built-in templates
        CompletableFuture<Void> builtInFuture = loadBuiltInTemplates();
        
        // Load remote templates
        CompletableFuture<Void> remoteFuture = loadRemoteTemplates();
        
        // Combine futures
        return CompletableFuture.allOf(builtInFuture, remoteFuture)
                .thenAccept(v -> loaded.set(true));
    }
    
    /**
     * Get all templates.
     *
     * @return The templates
     */
    @NotNull
    public List<ModTemplate> getTemplates() {
        return Collections.unmodifiableList(templates);
    }
    
    /**
     * Get a template by ID.
     *
     * @param id The template ID
     * @return The template, or null if not found
     */
    @Nullable
    public ModTemplate getTemplate(@NotNull String id) {
        return templatesById.get(id);
    }
    
    /**
     * Get templates by category.
     *
     * @param category The category
     * @return The templates
     */
    @NotNull
    public List<ModTemplate> getTemplatesByCategory(@NotNull String category) {
        return templates.stream()
                .filter(t -> t.getCategory().equals(category))
                .collect(Collectors.toList());
    }
    
    /**
     * Get templates by type.
     *
     * @param type The type
     * @return The templates
     */
    @NotNull
    public List<ModTemplate> getTemplatesByType(@NotNull ModTemplateType type) {
        return templates.stream()
                .filter(t -> t.getType() == type)
                .collect(Collectors.toList());
    }
    
    /**
     * Get unique categories.
     *
     * @return The categories
     */
    @NotNull
    public Set<String> getCategories() {
        return templates.stream()
                .map(ModTemplate::getCategory)
                .collect(Collectors.toSet());
    }
    
    /**
     * Add a template.
     *
     * @param template The template
     */
    public void addTemplate(@NotNull ModTemplate template) {
        templates.add(template);
        templatesById.put(template.getId(), template);
    }
    
    /**
     * Remove a template.
     *
     * @param id The template ID
     * @return True if the template was removed, false otherwise
     */
    public boolean removeTemplate(@NotNull String id) {
        ModTemplate template = templatesById.remove(id);
        if (template != null) {
            templates.remove(template);
            return true;
        }
        
        return false;
    }
    
    /**
     * Generate a project from a template.
     *
     * @param template   The template
     * @param outputDir  The output directory
     * @param variables  The variables to use
     * @return A CompletableFuture that will be completed when the project is generated
     */
    @NotNull
    public CompletableFuture<Void> generateProject(
            @NotNull ModTemplate template,
            @NotNull File outputDir,
            @NotNull Map<String, String> variables
    ) {
        return CompletableFuture.runAsync(() -> {
            try {
                // Create output directory if it doesn't exist
                if (!outputDir.exists() && !outputDir.mkdirs()) {
                    throw new IllegalStateException("Failed to create output directory: " + outputDir);
                }
                
                // Process all files
                for (Map.Entry<String, String> entry : template.getFiles().entrySet()) {
                    String path = entry.getKey();
                    String content = entry.getValue();
                    
                    // Process path and content with variables
                    path = processTemplate(path, variables);
                    content = processTemplate(content, variables);
                    
                    // Write file
                    File outputFile = new File(outputDir, path);
                    
                    // Create parent directories if they don't exist
                    File parentDir = outputFile.getParentFile();
                    if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
                        throw new IllegalStateException("Failed to create directory: " + parentDir);
                    }
                    
                    // Write file
                    Files.write(outputFile.toPath(), content.getBytes());
                }
            } catch (Exception e) {
                LOG.error("Error generating project", e);
                throw new IllegalStateException("Error generating project: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * Process a template string by replacing variables.
     *
     * @param template  The template string
     * @param variables The variables
     * @return The processed string
     */
    @NotNull
    private String processTemplate(@NotNull String template, @NotNull Map<String, String> variables) {
        String result = template;
        
        // Replace all variables
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            
            result = result.replace("${" + key + "}", value);
        }
        
        return result;
    }
    
    /**
     * Load built-in templates.
     *
     * @return A CompletableFuture that will be completed when templates are loaded
     */
    @NotNull
    private CompletableFuture<Void> loadBuiltInTemplates() {
        return CompletableFuture.runAsync(() -> {
            try {
                // Let's first add templates from loader contributors
                List<ModLoaderContributor.ModLoaderTemplate> contributorTemplates = 
                        ModLoaderDetector.getAvailableTemplates();
                
                for (ModLoaderContributor.ModLoaderTemplate contribTemplate : contributorTemplates) {
                    ModLoaderContributor contributor = 
                            ModLoaderDetector.getContributorById(getLoaderIdFromTemplateId(contribTemplate.getId()));
                    
                    if (contributor == null) {
                        LOG.warn("Could not find contributor for template: " + contribTemplate.getId());
                        continue;
                    }
                    
                    // Create template files based on contributor type
                    Map<String, String> files = new HashMap<>();
                    Map<String, String> variables = new HashMap<>();
                    
                    setupTemplateForLoader(contributor.getModLoaderId(), files, variables);
                    
                    ModTemplateType type = ModTemplateType.fromId(contributor.getModLoaderId());
                    
                    ModTemplate template = new ModTemplate.Builder()
                            .id(contribTemplate.getId())
                            .name(contribTemplate.getName())
                            .description(contribTemplate.getDescription())
                            .category("ModForge Templates")
                            .type(type)
                            .files(files)
                            .variables(variables)
                            .build();
                    
                    addTemplate(template);
                    LOG.info("Added template from contributor: " + contribTemplate.getId());
                }
                
                // Still keep the built-in example templates, separate from the loader templates
                
                // Forge Example Mod
                Map<String, String> forgeFiles = new HashMap<>();
                forgeFiles.put("build.gradle", getForgeGradleTemplate());
                forgeFiles.put("src/main/java/${package}/ExampleMod.java", getForgeMainClassTemplate());
                forgeFiles.put("src/main/resources/META-INF/mods.toml", getForgeModsTomlTemplate());
                
                Map<String, String> forgeVariables = new HashMap<>();
                forgeVariables.put("name", "Example Mod");
                forgeVariables.put("modid", "examplemod");
                forgeVariables.put("version", "1.0.0");
                forgeVariables.put("group", "com.example");
                forgeVariables.put("package", "com/example/examplemod");
                forgeVariables.put("description", "An example mod");
                forgeVariables.put("author", "ExampleAuthor");
                forgeVariables.put("minecraft_version", "1.19.2");
                forgeVariables.put("forge_version", "43.1.7");
                
                ModTemplate forgeTemplate = new ModTemplate.Builder()
                        .id("forge-example")
                        .name("Forge Example Mod")
                        .description("A simple example mod for Minecraft Forge")
                        .category("Examples")
                        .type(ModTemplateType.FORGE)
                        .files(forgeFiles)
                        .variables(forgeVariables)
                        .build();
                
                // Fabric Example Mod
                Map<String, String> fabricFiles = new HashMap<>();
                fabricFiles.put("build.gradle", getFabricGradleTemplate());
                fabricFiles.put("src/main/java/${package}/ExampleMod.java", getFabricMainClassTemplate());
                fabricFiles.put("src/main/resources/fabric.mod.json", getFabricModJsonTemplate());
                
                Map<String, String> fabricVariables = new HashMap<>();
                fabricVariables.put("name", "Example Mod");
                fabricVariables.put("modid", "examplemod");
                fabricVariables.put("version", "1.0.0");
                fabricVariables.put("group", "com.example");
                fabricVariables.put("package", "com/example/examplemod");
                fabricVariables.put("description", "An example mod");
                fabricVariables.put("author", "ExampleAuthor");
                fabricVariables.put("minecraft_version", "1.19.2");
                fabricVariables.put("fabric_version", "0.76.0+1.19.2");
                
                ModTemplate fabricTemplate = new ModTemplate.Builder()
                        .id("fabric-example")
                        .name("Fabric Example Mod")
                        .description("A simple example mod for Fabric")
                        .category("Examples")
                        .type(ModTemplateType.FABRIC)
                        .files(fabricFiles)
                        .variables(fabricVariables)
                        .build();
                
                // Add the example templates
                addTemplate(forgeTemplate);
                addTemplate(fabricTemplate);
                
                LOG.info("Loaded built-in templates");
            } catch (Exception e) {
                LOG.error("Error loading built-in templates", e);
            }
        });
    }
    
    /**
     * Load remote templates.
     *
     * @return A CompletableFuture that will be completed when templates are loaded
     */
    @NotNull
    private CompletableFuture<Void> loadRemoteTemplates() {
        ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
        if (!authManager.isAuthenticated()) {
            LOG.info("Not authenticated, skipping remote templates");
            return CompletableFuture.completedFuture(null);
        }
        
        String serverUrl = authManager.getServerUrl();
        String token = authManager.getToken();
        
        String templatesUrl = serverUrl.endsWith("/") ? serverUrl + "templates" : serverUrl + "/templates";
        
        return TokenAuthConnectionUtil.executeGet(templatesUrl, token)
                .thenApply(response -> {
                    try {
                        JsonArray templatesArray = GSON.fromJson(response, JsonArray.class);
                        for (JsonElement element : templatesArray) {
                            JsonObject templateObj = element.getAsJsonObject();
                            
                            // Parse template data
                            String id = templateObj.get("id").getAsString();
                            String name = templateObj.get("name").getAsString();
                            String description = templateObj.get("description").getAsString();
                            String category = templateObj.get("category").getAsString();
                            String typeStr = templateObj.get("type").getAsString();
                            
                            ModTemplateType type = ModTemplateType.fromId(typeStr);
                            
                            // Parse files
                            Map<String, String> files = new HashMap<>();
                            JsonObject filesObj = templateObj.getAsJsonObject("files");
                            for (Map.Entry<String, JsonElement> fileEntry : filesObj.entrySet()) {
                                files.put(fileEntry.getKey(), fileEntry.getValue().getAsString());
                            }
                            
                            // Parse variables
                            Map<String, String> variables = new HashMap<>();
                            JsonObject varsObj = templateObj.getAsJsonObject("variables");
                            for (Map.Entry<String, JsonElement> varEntry : varsObj.entrySet()) {
                                variables.put(varEntry.getKey(), varEntry.getValue().getAsString());
                            }
                            
                            // Create template
                            ModTemplate template = new ModTemplate.Builder()
                                    .id(id)
                                    .name(name)
                                    .description(description)
                                    .category(category)
                                    .type(type)
                                    .files(files)
                                    .variables(variables)
                                    .build();
                            
                            // Add template
                            addTemplate(template);
                        }
                        
                        LOG.info("Loaded " + templatesArray.size() + " remote templates");
                    } catch (Exception e) {
                        LOG.error("Error parsing remote templates", e);
                    }
                    
                    return null;
                })
                .exceptionally(e -> {
                    LOG.error("Error loading remote templates", e);
                    return null;
                });
    }
    
    /**
     * Get the Forge Gradle template.
     *
     * @return The template
     */
    @NotNull
    private String getForgeGradleTemplate() {
        return "buildscript {\n" +
                "    repositories {\n" +
                "        maven { url = 'https://maven.minecraftforge.net' }\n" +
                "        mavenCentral()\n" +
                "    }\n" +
                "    dependencies {\n" +
                "        classpath group: 'net.minecraftforge.gradle', name: 'ForgeGradle', version: '5.1.+', changing: true\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "apply plugin: 'net.minecraftforge.gradle'\n" +
                "apply plugin: 'eclipse'\n" +
                "apply plugin: 'maven-publish'\n" +
                "\n" +
                "version = '${version}'\n" +
                "group = '${group}'\n" +
                "archivesBaseName = '${modid}'\n" +
                "\n" +
                "java.toolchain.languageVersion = JavaLanguageVersion.of(17)\n" +
                "\n" +
                "minecraft {\n" +
                "    mappings channel: 'official', version: '${minecraft_version}'\n" +
                "\n" +
                "    runs {\n" +
                "        client {\n" +
                "            workingDirectory project.file('run')\n" +
                "            property 'forge.logging.markers', 'REGISTRIES'\n" +
                "            property 'forge.logging.console.level', 'debug'\n" +
                "        }\n" +
                "\n" +
                "        server {\n" +
                "            workingDirectory project.file('run')\n" +
                "            property 'forge.logging.markers', 'REGISTRIES'\n" +
                "            property 'forge.logging.console.level', 'debug'\n" +
                "        }\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "dependencies {\n" +
                "    minecraft 'net.minecraftforge:forge:${minecraft_version}-${forge_version}'\n" +
                "}\n" +
                "\n" +
                "jar {\n" +
                "    manifest {\n" +
                "        attributes([\n" +
                "            \"Specification-Title\": \"${name}\",\n" +
                "            \"Specification-Vendor\": \"${author}\",\n" +
                "            \"Specification-Version\": \"1\",\n" +
                "            \"Implementation-Title\": \"${name}\",\n" +
                "            \"Implementation-Version\": \"${version}\",\n" +
                "            \"Implementation-Vendor\" :\"${author}\",\n" +
                "            \"Implementation-Timestamp\": new Date().format(\"yyyy-MM-dd'T'HH:mm:ssZ\")\n" +
                "        ])\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "publishing {\n" +
                "    publications {\n" +
                "        mavenJava(MavenPublication) {\n" +
                "            artifact jar\n" +
                "        }\n" +
                "    }\n" +
                "    repositories {\n" +
                "        maven {\n" +
                "            url \"file:///${project.projectDir}/mcmodsrepo\"\n" +
                "        }\n" +
                "    }\n" +
                "}\n";
    }
    
    /**
     * Get the Forge main class template.
     *
     * @return The template
     */
    @NotNull
    private String getForgeMainClassTemplate() {
        return "package ${group}.${modid};\n" +
                "\n" +
                "import net.minecraft.world.item.Item;\n" +
                "import net.minecraft.world.level.block.Block;\n" +
                "import net.minecraftforge.common.MinecraftForge;\n" +
                "import net.minecraftforge.event.server.ServerStartingEvent;\n" +
                "import net.minecraftforge.eventbus.api.IEventBus;\n" +
                "import net.minecraftforge.eventbus.api.SubscribeEvent;\n" +
                "import net.minecraftforge.fml.common.Mod;\n" +
                "import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;\n" +
                "import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;\n" +
                "import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;\n" +
                "import net.minecraftforge.registries.DeferredRegister;\n" +
                "import net.minecraftforge.registries.ForgeRegistries;\n" +
                "import org.apache.logging.log4j.LogManager;\n" +
                "import org.apache.logging.log4j.Logger;\n" +
                "\n" +
                "/**\n" +
                " * Main class for ${name}.\n" +
                " * This is an example mod created by ModForge AI.\n" +
                " */\n" +
                "@Mod(\"${modid}\")\n" +
                "public class ExampleMod {\n" +
                "    // Logger for this mod\n" +
                "    private static final Logger LOGGER = LogManager.getLogger();\n" +
                "    \n" +
                "    // Item registry\n" +
                "    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, \"${modid}\");\n" +
                "    \n" +
                "    // Block registry\n" +
                "    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, \"${modid}\");\n" +
                "    \n" +
                "    public ExampleMod() {\n" +
                "        // Register mod event listeners\n" +
                "        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();\n" +
                "        modEventBus.addListener(this::setup);\n" +
                "        modEventBus.addListener(this::clientSetup);\n" +
                "        \n" +
                "        // Register deferred registers\n" +
                "        ITEMS.register(modEventBus);\n" +
                "        BLOCKS.register(modEventBus);\n" +
                "        \n" +
                "        // Register ourselves for server and other game events\n" +
                "        MinecraftForge.EVENT_BUS.register(this);\n" +
                "    }\n" +
                "    \n" +
                "    private void setup(final FMLCommonSetupEvent event) {\n" +
                "        LOGGER.info(\"Common setup for ${name}\");\n" +
                "    }\n" +
                "    \n" +
                "    private void clientSetup(final FMLClientSetupEvent event) {\n" +
                "        LOGGER.info(\"Client setup for ${name}\");\n" +
                "    }\n" +
                "    \n" +
                "    @SubscribeEvent\n" +
                "    public void onServerStarting(ServerStartingEvent event) {\n" +
                "        LOGGER.info(\"Server starting for ${name}\");\n" +
                "    }\n" +
                "}\n";
    }
    
    /**
     * Get the Forge mods.toml template.
     *
     * @return The template
     */
    @NotNull
    private String getForgeModsTomlTemplate() {
        return "modLoader=\"javafml\"\n" +
                "loaderVersion=\"[41,)\"\n" +
                "license=\"All Rights Reserved\"\n" +
                "\n" +
                "[[mods]]\n" +
                "modId=\"${modid}\"\n" +
                "version=\"${version}\"\n" +
                "displayName=\"${name}\"\n" +
                "authors=\"${author}\"\n" +
                "description='''${description}'''\n" +
                "\n" +
                "[[dependencies.${modid}]]\n" +
                "    modId=\"forge\"\n" +
                "    mandatory=true\n" +
                "    versionRange=\"[41,)\"\n" +
                "    ordering=\"NONE\"\n" +
                "    side=\"BOTH\"\n" +
                "\n" +
                "[[dependencies.${modid}]]\n" +
                "    modId=\"minecraft\"\n" +
                "    mandatory=true\n" +
                "    versionRange=\"[1.19,1.20)\"\n" +
                "    ordering=\"NONE\"\n" +
                "    side=\"BOTH\"\n";
    }
    
    /**
     * Get the Fabric Gradle template.
     *
     * @return The template
     */
    @NotNull
    private String getFabricGradleTemplate() {
        return "plugins {\n" +
                "    id 'fabric-loom' version '0.12-SNAPSHOT'\n" +
                "    id 'maven-publish'\n" +
                "}\n" +
                "\n" +
                "version = '${version}'\n" +
                "group = '${group}'\n" +
                "\n" +
                "base {\n" +
                "    archivesName = '${modid}'\n" +
                "}\n" +
                "\n" +
                "repositories {\n" +
                "    // Add repositories for dependencies here\n" +
                "    // Fabric: https://fabricmc.net/develop/\n" +
                "    maven { url \"https://maven.fabricmc.net/\" }\n" +
                "}\n" +
                "\n" +
                "dependencies {\n" +
                "    // Minecraft\n" +
                "    minecraft \"com.mojang:minecraft:${minecraft_version}\"\n" +
                "    \n" +
                "    // Yarn mappings\n" +
                "    mappings \"net.fabricmc:yarn:${minecraft_version}+build.1:v2\"\n" +
                "    \n" +
                "    // Fabric loader\n" +
                "    modImplementation \"net.fabricmc:fabric-loader:0.14.9\"\n" +
                "    \n" +
                "    // Fabric API\n" +
                "    modImplementation \"net.fabricmc.fabric-api:fabric-api:${fabric_version}\"\n" +
                "}\n" +
                "\n" +
                "processResources {\n" +
                "    inputs.property \"version\", project.version\n" +
                "    filteringCharset \"UTF-8\"\n" +
                "    \n" +
                "    filesMatching(\"fabric.mod.json\") {\n" +
                "        expand \"version\": project.version\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "tasks.withType(JavaCompile).configureEach {\n" +
                "    it.options.encoding = \"UTF-8\"\n" +
                "    it.options.release = 17\n" +
                "}\n" +
                "\n" +
                "java {\n" +
                "    // Loom adds the necessary sourcesJar and javadocJar tasks\n" +
                "    withSourcesJar()\n" +
                "}\n" +
                "\n" +
                "jar {\n" +
                "    from(\"LICENSE\") {\n" +
                "        rename { \"${it}_${project.archivesBaseName}\" }\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "publishing {\n" +
                "    publications {\n" +
                "        mavenJava(MavenPublication) {\n" +
                "            from components.java\n" +
                "        }\n" +
                "    }\n" +
                "}\n";
    }
    
    /**
     * Get the Fabric main class template.
     *
     * @return The template
     */
    @NotNull
    private String getFabricMainClassTemplate() {
        return "package ${group}.${modid};\n" +
                "\n" +
                "import net.fabricmc.api.ModInitializer;\n" +
                "import net.fabricmc.fabric.api.item.v1.FabricItemSettings;\n" +
                "import net.minecraft.item.Item;\n" +
                "import net.minecraft.item.ItemGroup;\n" +
                "import net.minecraft.util.Identifier;\n" +
                "import net.minecraft.util.registry.Registry;\n" +
                "import org.slf4j.Logger;\n" +
                "import org.slf4j.LoggerFactory;\n" +
                "\n" +
                "/**\n" +
                " * Main class for ${name}.\n" +
                " * This is an example mod created by ModForge AI.\n" +
                " */\n" +
                "public class ExampleMod implements ModInitializer {\n" +
                "    // Mod ID\n" +
                "    public static final String MOD_ID = \"${modid}\";\n" +
                "    \n" +
                "    // Logger for this mod\n" +
                "    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);\n" +
                "    \n" +
                "    // Example item\n" +
                "    public static final Item EXAMPLE_ITEM = new Item(new FabricItemSettings().group(ItemGroup.MISC));\n" +
                "    \n" +
                "    @Override\n" +
                "    public void onInitialize() {\n" +
                "        LOGGER.info(\"Initializing ${name}\");\n" +
                "        \n" +
                "        // Register items\n" +
                "        Registry.register(Registry.ITEM, new Identifier(MOD_ID, \"example_item\"), EXAMPLE_ITEM);\n" +
                "    }\n" +
                "}\n";
    }
    
    /**
     * Get the Fabric mod.json template.
     *
     * @return The template
     */
    @NotNull
    private String getFabricModJsonTemplate() {
        return "{\n" +
                "  \"schemaVersion\": 1,\n" +
                "  \"id\": \"${modid}\",\n" +
                "  \"version\": \"${version}\",\n" +
                "  \"name\": \"${name}\",\n" +
                "  \"description\": \"${description}\",\n" +
                "  \"authors\": [\n" +
                "    \"${author}\"\n" +
                "  ],\n" +
                "  \"contact\": {},\n" +
                "  \"license\": \"All Rights Reserved\",\n" +
                "  \"environment\": \"*\",\n" +
                "  \"entrypoints\": {\n" +
                "    \"main\": [\n" +
                "      \"${group}.${modid}.ExampleMod\"\n" +
                "    ]\n" +
                "  },\n" +
                "  \"depends\": {\n" +
                "    \"fabricloader\": \">=0.14.9\",\n" +
                "    \"fabric\": \">=0.58.0\",\n" +
                "    \"minecraft\": \"1.19.x\",\n" +
                "    \"java\": \">=17\"\n" +
                "  }\n" +
                "}\n";
    }
    
    /**
     * Get the Quilt Gradle template.
     *
     * @return The template
     */
    @NotNull
    private String getQuiltGradleTemplate() {
        return "plugins {\n" +
                "    id 'org.quiltmc.loom' version '1.0.+'\n" +
                "}\n" +
                "\n" +
                "group = '${group}'\n" +
                "version = '${version}'\n" +
                "archivesBaseName = '${modid}'\n" +
                "\n" +
                "repositories {\n" +
                "    mavenCentral()\n" +
                "    maven { url = 'https://maven.quiltmc.org/repository/release' }\n" +
                "}\n" +
                "\n" +
                "dependencies {\n" +
                "    minecraft 'com.mojang:minecraft:${minecraft_version}'\n" +
                "    mappings 'org.quiltmc:quilt-mappings:${minecraft_version}+build.1:v2'\n" +
                "    modImplementation 'org.quiltmc:quilt-loader:${quilt_version}'\n" +
                "    modImplementation 'org.quiltmc.quilted-fabric-api:quilted-fabric-api:${quilt_version}'\n" +
                "}\n" +
                "\n" +
                "java {\n" +
                "    sourceCompatibility = JavaVersion.VERSION_17\n" +
                "    targetCompatibility = JavaVersion.VERSION_17\n" +
                "}\n" +
                "\n" +
                "tasks.withType(JavaCompile).configureEach {\n" +
                "    options.encoding = 'UTF-8'\n" +
                "    options.release = 17\n" +
                "}\n";
    }
    
    /**
     * Get the Quilt main class template.
     *
     * @return The template
     */
    @NotNull
    private String getQuiltMainClassTemplate() {
        return "package ${group}.${modid};\n" +
                "\n" +
                "import org.slf4j.Logger;\n" +
                "import org.slf4j.LoggerFactory;\n" +
                "\n" +
                "public class ExampleMod {\n" +
                "    public static final String MOD_ID = \"${modid}\";\n" +
                "    public static final Logger LOGGER = LoggerFactory.getLogger(\"${modid}\");\n" +
                "\n" +
                "    public static void init() {\n" +
                "        LOGGER.info(\"Initializing ${name} mod\");\n" +
                "    }\n" +
                "}\n";
    }
    
    /**
     * Get the Quilt mod.json template.
     *
     * @return The template
     */
    @NotNull
    private String getQuiltModJsonTemplate() {
        return "{\n" +
                "  \"schema_version\": 1,\n" +
                "  \"quilt_loader\": {\n" +
                "    \"group\": \"${group}\",\n" +
                "    \"id\": \"${modid}\",\n" +
                "    \"version\": \"${version}\",\n" +
                "    \"name\": \"${name}\",\n" +
                "    \"description\": \"${description}\",\n" +
                "    \"authors\": [\n" +
                "      \"${author}\"\n" +
                "    ],\n" +
                "    \"contact\": {},\n" +
                "    \"license\": \"All Rights Reserved\",\n" +
                "    \"environment\": \"*\",\n" +
                "    \"entrypoints\": {\n" +
                "      \"init\": [\n" +
                "        \"${group}.${modid}.ExampleMod::init\"\n" +
                "      ]\n" +
                "    },\n" +
                "    \"depends\": [\n" +
                "      {\n" +
                "        \"id\": \"quilt_loader\",\n" +
                "        \"versions\": \">=0.17.0-\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": \"quilted_fabric_api\",\n" +
                "        \"versions\": \">=5.0.0-\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": \"minecraft\",\n" +
                "        \"versions\": \"~1.19\"\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}\n";
    }
    
    /**
     * Get the Architectury settings.gradle template.
     *
     * @return The template
     */
    @NotNull
    private String getArchitecturySettingsGradleTemplate() {
        return "pluginManagement {\n" +
                "    repositories {\n" +
                "        gradlePluginPortal()\n" +
                "        maven { url = 'https://maven.minecraftforge.net/' }\n" +
                "        maven { url = 'https://maven.fabricmc.net/' }\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "rootProject.name = '${modid}'\n" +
                "include 'common', 'fabric', 'forge'\n";
    }
    
    /**
     * Get the Architectury root build.gradle template.
     *
     * @return The template
     */
    @NotNull
    private String getArchitecturyRootGradleTemplate() {
        return "plugins {\n" +
                "    id 'architectury-plugin' version '3.4-SNAPSHOT'\n" +
                "    id 'dev.architectury.loom' version '1.0-SNAPSHOT' apply false\n" +
                "}\n" +
                "\n" +
                "architectury {\n" +
                "    minecraft = '${minecraft_version}'\n" +
                "}\n" +
                "\n" +
                "subprojects {\n" +
                "    apply plugin: 'java'\n" +
                "    apply plugin: 'architectury-plugin'\n" +
                "    apply plugin: 'maven-publish'\n" +
                "\n" +
                "    group = '${group}'\n" +
                "    version = '${version}'\n" +
                "    \n" +
                "    repositories {\n" +
                "        mavenCentral()\n" +
                "        maven { url = 'https://maven.architectury.dev/' }\n" +
                "        maven { url = 'https://maven.minecraftforge.net/' }\n" +
                "        maven { url = 'https://maven.fabricmc.net/' }\n" +
                "    }\n" +
                "    \n" +
                "    java {\n" +
                "        sourceCompatibility = JavaVersion.VERSION_17\n" +
                "        targetCompatibility = JavaVersion.VERSION_17\n" +
                "    }\n" +
                "    \n" +
                "    tasks.withType(JavaCompile) {\n" +
                "        options.encoding = 'UTF-8'\n" +
                "        options.release = 17\n" +
                "    }\n" +
                "}\n";
    }
    
    /**
     * Get the Architectury common build.gradle template.
     *
     * @return The template
     */
    @NotNull
    private String getArchitecturyCommonGradleTemplate() {
        return "plugins {\n" +
                "    id 'dev.architectury.loom'\n" +
                "}\n" +
                "\n" +
                "architectury {\n" +
                "    common('forge', 'fabric')\n" +
                "}\n" +
                "\n" +
                "dependencies {\n" +
                "    minecraft \"com.mojang:minecraft:${minecraft_version}\"\n" +
                "    // Common implementation\n" +
                "    modImplementation \"dev.architectury:architectury:${architectury_version}\"\n" +
                "}\n";
    }
    
    /**
     * Get the Architectury Fabric build.gradle template.
     *
     * @return The template
     */
    @NotNull
    private String getArchitecturyFabricGradleTemplate() {
        return "plugins {\n" +
                "    id 'dev.architectury.loom'\n" +
                "}\n" +
                "\n" +
                "architectury {\n" +
                "    platformSetupLoomIde()\n" +
                "}\n" +
                "\n" +
                "dependencies {\n" +
                "    minecraft \"com.mojang:minecraft:${minecraft_version}\"\n" +
                "    mappings \"net.fabricmc:yarn:${minecraft_version}+build.1:v2\"\n" +
                "    modImplementation \"net.fabricmc:fabric-loader:0.14.9\"\n" +
                "    modImplementation \"net.fabricmc.fabric-api:fabric-api:${fabric_version}\"\n" +
                "    modImplementation \"dev.architectury:architectury-fabric:${architectury_version}\"\n" +
                "    \n" +
                "    implementation project(\":common\")\n" +
                "}\n";
    }
    
    /**
     * Get the Architectury Forge build.gradle template.
     *
     * @return The template
     */
    @NotNull
    private String getArchitecturyForgeGradleTemplate() {
        return "plugins {\n" +
                "    id 'dev.architectury.loom'\n" +
                "    id 'net.minecraftforge.gradle'\n" +
                "}\n" +
                "\n" +
                "architectury {\n" +
                "    platformSetupLoomIde()\n" +
                "}\n" +
                "\n" +
                "dependencies {\n" +
                "    minecraft \"com.mojang:minecraft:${minecraft_version}\"\n" +
                "    forge \"net.minecraftforge:forge:${minecraft_version}-${forge_version}\"\n" +
                "    modImplementation \"dev.architectury:architectury-forge:${architectury_version}\"\n" +
                "    \n" +
                "    implementation project(\":common\")\n" +
                "}\n";
    }
    
    /**
     * Get the Architectury common main class template.
     *
     * @return The template
     */
    @NotNull
    private String getArchitecturyCommonMainClassTemplate() {
        return "package ${group}.${modid};\n" +
                "\n" +
                "import dev.architectury.platform.Platform;\n" +
                "\n" +
                "public class ExampleMod {\n" +
                "    public static final String MOD_ID = \"${modid}\";\n" +
                "    \n" +
                "    public static void init() {\n" +
                "        System.out.println(\"Hello from common platform: \" + Platform.getEnv());\n" +
                "    }\n" +
                "}\n";
    }
    
    /**
     * Get the Architectury Fabric main class template.
     *
     * @return The template
     */
    @NotNull
    private String getArchitecturyFabricMainClassTemplate() {
        return "package ${group}.${modid}.fabric;\n" +
                "\n" +
                "import ${group}.${modid}.ExampleMod;\n" +
                "import net.fabricmc.api.ModInitializer;\n" +
                "\n" +
                "public class ExampleModFabric implements ModInitializer {\n" +
                "    @Override\n" +
                "    public void onInitialize() {\n" +
                "        ExampleMod.init();\n" +
                "    }\n" +
                "}\n";
    }
    
    /**
     * Get the Architectury Forge main class template.
     *
     * @return The template
     */
    @NotNull
    private String getArchitecturyForgeMainClassTemplate() {
        return "package ${group}.${modid}.forge;\n" +
                "\n" +
                "import ${group}.${modid}.ExampleMod;\n" +
                "import net.minecraftforge.fml.common.Mod;\n" +
                "import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;\n" +
                "\n" +
                "@Mod(ExampleMod.MOD_ID)\n" +
                "public class ExampleModForge {\n" +
                "    public ExampleModForge() {\n" +
                "        ExampleMod.init();\n" +
                "    }\n" +
                "}\n";
    }
    
    /**
     * Get the Architectury Fabric mod.json template.
     *
     * @return The template
     */
    @NotNull
    private String getArchitecturyFabricModJsonTemplate() {
        return "{\n" +
                "  \"schemaVersion\": 1,\n" +
                "  \"id\": \"${modid}\",\n" +
                "  \"version\": \"${version}\",\n" +
                "  \"name\": \"${name}\",\n" +
                "  \"description\": \"${description}\",\n" +
                "  \"authors\": [\n" +
                "    \"${author}\"\n" +
                "  ],\n" +
                "  \"contact\": {},\n" +
                "  \"license\": \"All Rights Reserved\",\n" +
                "  \"environment\": \"*\",\n" +
                "  \"entrypoints\": {\n" +
                "    \"main\": [\n" +
                "      \"${group}.${modid}.fabric.ExampleModFabric\"\n" +
                "    ]\n" +
                "  },\n" +
                "  \"depends\": {\n" +
                "    \"fabricloader\": \">=0.14.9\",\n" +
                "    \"fabric\": \"*\",\n" +
                "    \"minecraft\": \"~1.19\",\n" +
                "    \"java\": \">=17\",\n" +
                "    \"architectury\": \">=5.0.0\"\n" +
                "  }\n" +
                "}\n";
    }
    
    /**
     * Get the Architectury Forge mods.toml template.
     *
     * @return The template
     */
    @NotNull
    private String getArchitecturyForgeModsTomlTemplate() {
        return "modLoader=\"javafml\"\n" +
                "loaderVersion=\"[41,)\"\n" +
                "license=\"All Rights Reserved\"\n" +
                "\n" +
                "[[mods]]\n" +
                "modId=\"${modid}\"\n" +
                "version=\"${version}\"\n" +
                "displayName=\"${name}\"\n" +
                "authors=\"${author}\"\n" +
                "description='''${description}'''\n" +
                "\n" +
                "[[dependencies.${modid}]]\n" +
                "    modId=\"forge\"\n" +
                "    mandatory=true\n" +
                "    versionRange=\"[41,)\"\n" +
                "    ordering=\"NONE\"\n" +
                "    side=\"BOTH\"\n" +
                "\n" +
                "[[dependencies.${modid}]]\n" +
                "    modId=\"minecraft\"\n" +
                "    mandatory=true\n" +
                "    versionRange=\"[1.19,1.20)\"\n" +
                "    ordering=\"NONE\"\n" +
                "    side=\"BOTH\"\n" +
                "    \n" +
                "[[dependencies.${modid}]]\n" +
                "    modId=\"architectury\"\n" +
                "    mandatory=true\n" +
                "    versionRange=\"[5.0.0,)\"\n" +
                "    ordering=\"AFTER\"\n" +
                "    side=\"BOTH\"\n";
    }
    
    /**
     * Extract the loader ID from a template ID.
     *
     * @param templateId The template ID
     * @return The loader ID, or the original template ID if no delimiter is found
     */
    @NotNull
    private String getLoaderIdFromTemplateId(@NotNull String templateId) {
        int dashIndex = templateId.indexOf('-');
        if (dashIndex > 0) {
            return templateId.substring(0, dashIndex);
        }
        return templateId;
    }
    
    /**
     * Set up template files and variables based on the mod loader.
     *
     * @param loaderId  The mod loader ID
     * @param files     The files map to populate
     * @param variables The variables map to populate
     */
    private void setupTemplateForLoader(
            @NotNull String loaderId,
            @NotNull Map<String, String> files,
            @NotNull Map<String, String> variables
    ) {
        // Set common variables first
        variables.put("name", "Example Mod");
        variables.put("modid", "examplemod");
        variables.put("version", "1.0.0");
        variables.put("group", "com.example");
        variables.put("package", "com/example/examplemod");
        variables.put("description", "An example mod");
        variables.put("author", "ExampleAuthor");
        variables.put("minecraft_version", "1.19.2");
        
        // Then set up loader-specific files and variables
        switch (loaderId) {
            case "forge":
                files.put("build.gradle", getForgeGradleTemplate());
                files.put("src/main/java/${package}/ExampleMod.java", getForgeMainClassTemplate());
                files.put("src/main/resources/META-INF/mods.toml", getForgeModsTomlTemplate());
                variables.put("forge_version", "43.1.7");
                break;
                
            case "fabric":
                files.put("build.gradle", getFabricGradleTemplate());
                files.put("src/main/java/${package}/ExampleMod.java", getFabricMainClassTemplate());
                files.put("src/main/resources/fabric.mod.json", getFabricModJsonTemplate());
                variables.put("fabric_version", "0.76.0+1.19.2");
                break;
                
            case "quilt":
                // For Quilt, we'll use a modified version of the Fabric templates
                files.put("build.gradle", getQuiltGradleTemplate());
                files.put("src/main/java/${package}/ExampleMod.java", getQuiltMainClassTemplate());
                files.put("src/main/resources/quilt.mod.json", getQuiltModJsonTemplate());
                variables.put("quilt_version", "0.19.0-beta.18+1.19.2");
                break;
                
            case "architectury":
                // For Architectury, we need a multi-project structure
                files.put("settings.gradle", getArchitecturySettingsGradleTemplate());
                files.put("build.gradle", getArchitecturyRootGradleTemplate());
                files.put("common/build.gradle", getArchitecturyCommonGradleTemplate());
                files.put("fabric/build.gradle", getArchitecturyFabricGradleTemplate());
                files.put("forge/build.gradle", getArchitecturyForgeGradleTemplate());
                files.put("common/src/main/java/${package}/ExampleMod.java", getArchitecturyCommonMainClassTemplate());
                files.put("fabric/src/main/java/${package}/fabric/ExampleModFabric.java", getArchitecturyFabricMainClassTemplate());
                files.put("forge/src/main/java/${package}/forge/ExampleModForge.java", getArchitecturyForgeMainClassTemplate());
                files.put("fabric/src/main/resources/fabric.mod.json", getArchitecturyFabricModJsonTemplate());
                files.put("forge/src/main/resources/META-INF/mods.toml", getArchitecturyForgeModsTomlTemplate());
                variables.put("architectury_version", "6.5.69");
                variables.put("fabric_version", "0.76.0+1.19.2");
                variables.put("forge_version", "43.1.7");
                break;
                
            default:
                LOG.warn("Unknown loader ID: " + loaderId);
                break;
        }
    }
}