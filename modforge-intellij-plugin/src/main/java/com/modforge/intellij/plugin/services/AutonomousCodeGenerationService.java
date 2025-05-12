package com.modforge.intellij.plugin.services;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiDocComment;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import com.modforge.intellij.plugin.utils.CompatibilityUtil;
import com.modforge.intellij.plugin.utils.PsiUtils;
import com.modforge.intellij.plugin.utils.ThreadUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for autonomous code generation.
 * This service provides AI-powered code generation capabilities.
 * Compatible with IntelliJ IDEA 2025.1.1.1
 */
@Service
public final class AutonomousCodeGenerationService {
    private static final Logger LOG = Logger.getInstance(AutonomousCodeGenerationService.class);
    
    // Settings
    private final ModForgeSettings settings;
    
    // State tracking
    private final AtomicBoolean isGeneratingCode = new AtomicBoolean(false);
    private final AtomicBoolean isRunningContinuously = new AtomicBoolean(false);
    private final AtomicInteger continuousDevelopmentTaskId = new AtomicInteger(0);
    
    // Performance tracking
    private final ConcurrentHashMap<String, Integer> patternHits = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> patternCache = new ConcurrentHashMap<>();
    
    /**
     * Constructor.
     */
    public AutonomousCodeGenerationService() {
        settings = ModForgeSettings.getInstance();
        LOG.info("AutonomousCodeGenerationService initialized");
    }

    /**
     * Gets the instance of the service.
     *
     * @return The service instance.
     */
    public static AutonomousCodeGenerationService getInstance() {
        return ApplicationManager.getApplication().getService(AutonomousCodeGenerationService.class);
    }

    /**
     * Generates mod code.
     *
     * @param project       The project.
     * @param prompt        The prompt.
     * @param outputPath    The output path.
     * @return A CompletableFuture that completes when the code is generated.
     */
    public CompletableFuture<List<String>> generateModCode(
            @NotNull Project project,
            @NotNull String prompt,
            @NotNull String outputPath) {
        
        if (isGeneratingCode.getAndSet(true)) {
            LOG.warn("Code generation already in progress");
            return CompletableFuture.completedFuture(List.of("Code generation already in progress"));
        }
        
        return ThreadUtils.supplyAsyncVirtual(() -> {
            try {
                LOG.info("Generating mod code with prompt: " + prompt);
                
                // Check pattern cache first
                String cacheKey = prompt + "|" + outputPath;
                if (patternCache.containsKey(cacheKey)) {
                    String cachedResult = patternCache.get(cacheKey);
                    patternHits.compute(cacheKey, (k, v) -> v == null ? 1 : v + 1);
                    LOG.info("Using cached result for prompt (hit count: " + patternHits.get(cacheKey) + ")");
                    return List.of(cachedResult);
                }
                
                // Mock generation for now
                // TODO: Replace with actual API call
                String generatedCode = generateMockCode(prompt);
                
                // Cache the result if pattern learning is enabled
                if (settings.isPatternRecognition()) {
                    patternCache.put(cacheKey, generatedCode);
                    patternHits.put(cacheKey, 1);
                }
                
                // Create the output directory if it doesn't exist
                Path outputDir = Paths.get(outputPath).getParent();
                if (outputDir != null && !Files.exists(outputDir)) {
                    Files.createDirectories(outputDir);
                }
                
                // Write the file
                File outputFile = new File(outputPath);
                FileUtil.writeToFile(outputFile, generatedCode);
                
                // Refresh the file in the IDE
                VirtualFile vFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(outputFile);
                if (vFile != null) {
                    vFile.refresh(false, false);
                    
                    // Open the file in the editor
                    CompatibilityUtil.openFileInEditor(project, vFile, true);
                }
                
                return List.of(generatedCode);
            } catch (Exception e) {
                LOG.error("Error generating mod code", e);
                return List.of("Error: " + e.getMessage());
            } finally {
                isGeneratingCode.set(false);
            }
        });
    }
    
    /**
     * Fixes compilation errors.
     *
     * @param project       The project.
     * @param filePath      The file path.
     * @param errors        The compilation errors.
     * @return A CompletableFuture that completes when the errors are fixed.
     */
    public CompletableFuture<Boolean> fixCompilationErrors(
            @NotNull Project project,
            @NotNull String filePath,
            @NotNull List<String> errors) {
        
        if (isGeneratingCode.getAndSet(true)) {
            LOG.warn("Code generation already in progress");
            return CompletableFuture.completedFuture(false);
        }
        
        return ThreadUtils.supplyAsyncVirtual(() -> {
            try {
                LOG.info("Fixing compilation errors in file: " + filePath);
                
                // Get the file
                VirtualFile vFile = CompatibilityUtil.getModFileByRelativePath(project, filePath);
                if (vFile == null) {
                    LOG.error("File not found: " + filePath);
                    return false;
                }
                
                // Read the file content
                String fileContent = new String(vFile.contentsToByteArray());
                
                // Mock fix for now
                // TODO: Replace with actual API call
                String fixedContent = mockFixErrors(fileContent, errors);
                
                // Write the fixed content
                VfsUtil.saveText(vFile, fixedContent);
                
                // Refresh the file
                vFile.refresh(false, false);
                
                return true;
            } catch (Exception e) {
                LOG.error("Error fixing compilation errors", e);
                return false;
            } finally {
                isGeneratingCode.set(false);
            }
        });
    }
    
    /**
     * Adds features to existing code.
     *
     * @param project       The project.
     * @param filePath      The file path.
     * @param featurePrompt The feature prompt.
     * @return A CompletableFuture that completes when the features are added.
     */
    public CompletableFuture<Boolean> addFeatures(
            @NotNull Project project,
            @NotNull String filePath,
            @NotNull String featurePrompt) {
        
        if (isGeneratingCode.getAndSet(true)) {
            LOG.warn("Code generation already in progress");
            return CompletableFuture.completedFuture(false);
        }
        
        return ThreadUtils.supplyAsyncVirtual(() -> {
            try {
                LOG.info("Adding features to file: " + filePath + " with prompt: " + featurePrompt);
                
                // Get the file
                VirtualFile vFile = CompatibilityUtil.getModFileByRelativePath(project, filePath);
                if (vFile == null) {
                    LOG.error("File not found: " + filePath);
                    return false;
                }
                
                // Read the file content
                String fileContent = new String(vFile.contentsToByteArray());
                
                // Mock feature addition for now
                // TODO: Replace with actual API call
                String enhancedContent = mockAddFeature(fileContent, featurePrompt);
                
                // Write the enhanced content
                VfsUtil.saveText(vFile, enhancedContent);
                
                // Refresh the file
                vFile.refresh(false, false);
                
                return true;
            } catch (Exception e) {
                LOG.error("Error adding features", e);
                return false;
            } finally {
                isGeneratingCode.set(false);
            }
        });
    }
    
    /**
     * Starts continuous development mode.
     *
     * @param project The project.
     * @return Whether continuous development was started.
     */
    public boolean startContinuousDevelopment(@NotNull Project project) {
        if (isRunningContinuously.getAndSet(true)) {
            LOG.warn("Continuous development already running");
            return false;
        }
        
        int taskId = continuousDevelopmentTaskId.incrementAndGet();
        LOG.info("Starting continuous development (task ID: " + taskId + ")");
        
        ThreadUtils.runAsyncVirtual(() -> {
            try {
                while (isRunningContinuously.get() && 
                       continuousDevelopmentTaskId.get() == taskId && 
                       !project.isDisposed()) {
                    
                    // TODO: Implement continuous development logic
                    LOG.info("Continuous development tick (task ID: " + taskId + ")");
                    
                    // Sleep for a while
                    Thread.sleep(60000);
                }
            } catch (InterruptedException e) {
                LOG.info("Continuous development interrupted", e);
            } catch (Exception e) {
                LOG.error("Error in continuous development", e);
            } finally {
                isRunningContinuously.set(false);
                LOG.info("Stopping continuous development (task ID: " + taskId + ")");
            }
        });
        
        return true;
    }
    
    /**
     * Stops continuous development mode.
     */
    public void stopContinuousDevelopment() {
        if (isRunningContinuously.getAndSet(false)) {
            LOG.info("Stopping continuous development");
            continuousDevelopmentTaskId.incrementAndGet();
        }
    }
    
    /**
     * Checks if continuous development is enabled.
     *
     * @return Whether continuous development is enabled.
     */
    public boolean isContinuousDevelopmentEnabled() {
        return settings.isEnableContinuousDevelopment();
    }
    
    /**
     * Checks if code is being generated.
     *
     * @return Whether code is being generated.
     */
    public boolean isGeneratingCode() {
        return isGeneratingCode.get();
    }
    
    /**
     * Checks if continuous development is running.
     *
     * @return Whether continuous development is running.
     */
    public boolean isRunningContinuously() {
        return isRunningContinuously.get();
    }
    
    /**
     * Gets pattern learning statistics.
     *
     * @return Statistics about pattern learning.
     */
    public String getPatternLearningStats() {
        int totalPatterns = patternCache.size();
        int totalHits = patternHits.values().stream().mapToInt(Integer::intValue).sum();
        
        return "Pattern learning stats:\n" +
               "- Total patterns: " + totalPatterns + "\n" +
               "- Total cache hits: " + totalHits;
    }
    
    /**
     * Clears the pattern cache.
     */
    public void clearPatternCache() {
        LOG.info("Clearing pattern cache");
        patternCache.clear();
        patternHits.clear();
    }
    
    /**
     * Fixes code with errors.
     *
     * @param code         The code with errors.
     * @param errorMessage The error message.
     * @param language     The programming language.
     * @return A CompletableFuture that completes with the fixed code.
     */
    public CompletableFuture<String> fixCode(
            @NotNull String code,
            @NotNull String errorMessage,
            @NotNull String language) {
        
        if (isGeneratingCode.getAndSet(true)) {
            LOG.warn("Code generation already in progress");
            return CompletableFuture.completedFuture(null);
        }
        
        return ThreadUtils.supplyAsyncVirtual(() -> {
            try {
                LOG.info("Fixing code in language: " + language);
                
                // Check pattern cache first
                String cacheKey = code + "|" + errorMessage + "|" + language;
                if (patternCache.containsKey(cacheKey)) {
                    String cachedResult = patternCache.get(cacheKey);
                    patternHits.compute(cacheKey, (k, v) -> v == null ? 1 : v + 1);
                    LOG.info("Using cached result for fixing (hit count: " + patternHits.get(cacheKey) + ")");
                    return cachedResult;
                }
                
                // Mock fix for now
                // TODO: Replace with actual API call
                String fixedCode = mockFixErrors(code, List.of(errorMessage.split("\n")));
                
                // Cache the result if pattern learning is enabled
                if (settings.isPatternRecognition()) {
                    patternCache.put(cacheKey, fixedCode);
                    patternHits.put(cacheKey, 1);
                }
                
                return fixedCode;
            } catch (Exception e) {
                LOG.error("Error fixing code", e);
                return null;
            } finally {
                isGeneratingCode.set(false);
            }
        });
    }
    
    // Mock implementations for testing - to be replaced with API calls
    
    /**
     * Generates mock code for testing.
     *
     * @param prompt The prompt.
     * @return Generated code.
     */
    private String generateMockCode(String prompt) {
        // This is a temporary implementation
        if (prompt.toLowerCase().contains("block")) {
            return "package com.example.mod;\n\n" +
                   "import net.minecraft.world.level.block.Block;\n" +
                   "import net.minecraft.world.level.block.state.BlockState;\n" +
                   "import net.minecraft.world.level.material.Material;\n\n" +
                   "/**\n" +
                   " * Custom block implementation.\n" +
                   " */\n" +
                   "public class CustomBlock extends Block {\n" +
                   "    /**\n" +
                   "     * Constructor.\n" +
                   "     */\n" +
                   "    public CustomBlock() {\n" +
                   "        super(Properties.of(Material.STONE).strength(3.0f, 3.0f));\n" +
                   "    }\n" +
                   "}\n";
        } else if (prompt.toLowerCase().contains("item")) {
            return "package com.example.mod;\n\n" +
                   "import net.minecraft.world.item.Item;\n\n" +
                   "/**\n" +
                   " * Custom item implementation.\n" +
                   " */\n" +
                   "public class CustomItem extends Item {\n" +
                   "    /**\n" +
                   "     * Constructor.\n" +
                   "     */\n" +
                   "    public CustomItem() {\n" +
                   "        super(new Properties().tab(ModCreativeTab.INSTANCE));\n" +
                   "    }\n" +
                   "}\n";
        } else {
            return "package com.example.mod;\n\n" +
                   "import net.minecraft.world.level.Level;\n" +
                   "import net.minecraft.world.entity.player.Player;\n\n" +
                   "/**\n" +
                   " * Utility class.\n" +
                   " */\n" +
                   "public class ModUtils {\n" +
                   "    /**\n" +
                   "     * Private constructor.\n" +
                   "     */\n" +
                   "    private ModUtils() {\n" +
                   "        // Utility class\n" +
                   "    }\n\n" +
                   "    /**\n" +
                   "     * Example method.\n" +
                   "     *\n" +
                   "     * @param player The player.\n" +
                   "     * @param level  The level.\n" +
                   "     * @return Whether the player is in the overworld.\n" +
                   "     */\n" +
                   "    public static boolean isPlayerInOverworld(Player player, Level level) {\n" +
                   "        return level.dimension() == Level.OVERWORLD;\n" +
                   "    }\n" +
                   "}\n";
        }
    }
    
    /**
     * Mock implementation of fixing errors.
     *
     * @param fileContent The file content.
     * @param errors      The errors.
     * @return Fixed file content.
     */
    private String mockFixErrors(String fileContent, List<String> errors) {
        if (errors.stream().anyMatch(error -> error.contains("cannot find symbol"))) {
            // Add an import statement if the error is "cannot find symbol"
            if (!fileContent.contains("import java.util.List;") && 
                errors.stream().anyMatch(error -> error.contains("List"))) {
                fileContent = fileContent.replaceFirst("package ([^;]+);", 
                                                       "package $1;\n\nimport java.util.List;");
            }
        }
        
        if (errors.stream().anyMatch(error -> error.contains("unclosed string literal"))) {
            // Fix unclosed string literals
            Pattern pattern = Pattern.compile("String [^=]+=\\s*\"([^\"]*)(?:\\n|$)");
            Matcher matcher = pattern.matcher(fileContent);
            if (matcher.find()) {
                fileContent = fileContent.replaceFirst("String [^=]+=\\s*\"([^\"]*)(?:\\n|$)", 
                                                        "String $1 = \"$2\";");
            }
        }
        
        return fileContent;
    }
    
    /**
     * Generates implementation for a class or interface.
     *
     * @param project      The project.
     * @param psiClass     The PSI class.
     * @param outputPath   The output path.
     * @param prompt       The prompt.
     * @return A CompletableFuture that completes when the implementation is generated.
     */
    public CompletableFuture<Boolean> generateImplementation(
            @NotNull Project project,
            @NotNull PsiClass psiClass,
            @NotNull String outputPath,
            @NotNull String prompt) {
        
        if (isGeneratingCode.getAndSet(true)) {
            LOG.warn("Code generation already in progress");
            return CompletableFuture.completedFuture(false);
        }
        
        return ThreadUtils.supplyAsyncVirtual(() -> {
            try {
                LOG.info("Generating implementation for " + psiClass.getQualifiedName() + " with prompt: " + prompt);
                
                // Build a detailed prompt
                StringBuilder detailedPrompt = new StringBuilder();
                detailedPrompt.append("Generate a complete implementation for the ");
                detailedPrompt.append(psiClass.isInterface() ? "interface" : "abstract class");
                detailedPrompt.append(" ");
                detailedPrompt.append(psiClass.getQualifiedName());
                detailedPrompt.append(".\n\n");
                
                // Add class details
                detailedPrompt.append("Class details:\n");
                
                if (psiClass.isInterface()) {
                    detailedPrompt.append("- Type: Interface\n");
                } else {
                    detailedPrompt.append("- Type: Abstract class\n");
                }
                
                // Add methods to implement
                PsiMethod[] methods = psiClass.getMethods();
                if (methods.length > 0) {
                    detailedPrompt.append("- Methods to implement:\n");
                    for (PsiMethod method : methods) {
                        if (psiClass.isInterface() || method.hasModifierProperty(PsiModifier.ABSTRACT)) {
                            detailedPrompt.append("  - ")
                                    .append(PsiUtils.getMethodSignature(method))
                                    .append("\n");
                            
                            // Add JavaDoc if available
                            PsiDocComment docComment = method.getDocComment();
                            if (docComment != null) {
                                detailedPrompt.append("    Documentation: ")
                                        .append(docComment.getText().replaceAll("\\s+", " ").trim())
                                        .append("\n");
                            }
                        }
                    }
                }
                
                // Add original prompt
                detailedPrompt.append("\n").append(prompt);
                
                // Generate the implementation
                String generatedCode = generateMockImplementation(psiClass, detailedPrompt.toString());
                
                // Create the output file
                File outputFile = new File(outputPath);
                if (!outputFile.getParentFile().exists()) {
                    if (!outputFile.getParentFile().mkdirs()) {
                        LOG.warn("Failed to create directories for output file: " + outputPath);
                        return false;
                    }
                }
                
                // Write the file
                FileUtil.writeToFile(outputFile, generatedCode);
                
                // Refresh the file
                VirtualFile vFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(outputFile);
                if (vFile != null) {
                    vFile.refresh(false, false);
                    
                    // Open the file in the editor
                    CompatibilityUtil.openFileInEditor(project, vFile, true);
                }
                
                return true;
            } catch (Exception e) {
                LOG.error("Error generating implementation", e);
                return false;
            } finally {
                isGeneratingCode.set(false);
            }
        });
    }
    
    /**
     * Generates a mock implementation for a class or interface.
     *
     * @param psiClass     The PSI class.
     * @param prompt       The prompt.
     * @return The generated code.
     */
    private String generateMockImplementation(PsiClass psiClass, String prompt) {
        // This is a temporary implementation
        String className = psiClass.getName();
        String implName = className + "Impl";
        String packageName = ((PsiJavaFile) psiClass.getContainingFile()).getPackageName();
        
        StringBuilder sb = new StringBuilder();
        
        // Add package declaration
        sb.append("package ").append(packageName).append(";\n\n");
        
        // Add imports (simplified)
        sb.append("import ").append(psiClass.getQualifiedName()).append(";\n\n");
        
        // Add class JavaDoc
        sb.append("/**\n");
        sb.append(" * Implementation of ").append(className).append(".\n");
        sb.append(" * Generated by ModForge on ").append(new java.util.Date()).append(".\n");
        sb.append(" */\n");
        
        // Add class declaration
        sb.append("public class ").append(implName);
        if (psiClass.isInterface()) {
            sb.append(" implements ");
        } else {
            sb.append(" extends ");
        }
        sb.append(className).append(" {\n\n");
        
        // Add methods
        PsiMethod[] methods = psiClass.getMethods();
        for (PsiMethod method : methods) {
            if (psiClass.isInterface() || method.hasModifierProperty(PsiModifier.ABSTRACT)) {
                // Add method JavaDoc if available
                PsiDocComment docComment = method.getDocComment();
                if (docComment != null) {
                    sb.append(docComment.getText()).append("\n");
                } else {
                    sb.append("    /**\n");
                    sb.append("     * {@inheritDoc}\n");
                    sb.append("     */\n");
                }
                
                // Add method signature
                sb.append("    @Override\n");
                sb.append("    public ");
                
                // Add return type
                PsiType returnType = method.getReturnType();
                if (returnType != null) {
                    sb.append(returnType.getPresentableText()).append(" ");
                }
                
                // Add method name and parameters
                sb.append(method.getName()).append("(");
                PsiParameter[] parameters = method.getParameterList().getParameters();
                for (int i = 0; i < parameters.length; i++) {
                    PsiParameter parameter = parameters[i];
                    sb.append(parameter.getType().getPresentableText())
                            .append(" ")
                            .append(parameter.getName());
                    
                    if (i < parameters.length - 1) {
                        sb.append(", ");
                    }
                }
                sb.append(") {\n");
                
                // Add method body (stub implementation)
                if (!returnType.equals(PsiType.VOID)) {
                    if (returnType.equals(PsiType.BOOLEAN)) {
                        sb.append("        return false;\n");
                    } else if (returnType instanceof PsiPrimitiveType) {
                        if (returnType.equals(PsiType.INT) || 
                            returnType.equals(PsiType.BYTE) || 
                            returnType.equals(PsiType.SHORT) || 
                            returnType.equals(PsiType.LONG)) {
                            sb.append("        return 0;\n");
                        } else if (returnType.equals(PsiType.FLOAT) || 
                                  returnType.equals(PsiType.DOUBLE)) {
                            sb.append("        return 0.0;\n");
                        } else if (returnType.equals(PsiType.CHAR)) {
                            sb.append("        return '\\0';\n");
                        }
                    } else {
                        sb.append("        return null;\n");
                    }
                } else {
                    sb.append("        // TODO: Implement this method\n");
                }
                
                sb.append("    }\n\n");
            }
        }
        
        // Close class
        sb.append("}\n");
        
        return sb.toString();
    }
    
    /**
     * Mock implementation of adding features.
     *
     * @param fileContent   The file content.
     * @param featurePrompt The feature prompt.
     * @return Enhanced file content.
     */
    private String mockAddFeature(String fileContent, String featurePrompt) {
        if (featurePrompt.toLowerCase().contains("logging")) {
            // Add logging
            if (!fileContent.contains("import java.util.logging.Logger;")) {
                fileContent = fileContent.replaceFirst("package ([^;]+);", 
                                                       "package $1;\n\nimport java.util.logging.Logger;");
            }
            
            // Add a logger field
            if (!fileContent.contains("private static final Logger LOGGER")) {
                fileContent = fileContent.replaceFirst("public class ([^\\s{]+)\\s*\\{", 
                                                       "public class $1 {\n    " + 
                                                       "private static final Logger LOGGER = " + 
                                                       "Logger.getLogger($1.class.getName());");
            }
        }
        
        return fileContent;
    }
}