package com.modforge.intellij.plugin.utils;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import javax.swing.Icon;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for compatibility operations.
 * This class provides various utility methods to ensure compatibility between different versions
 * of Minecraft, mod loaders, and the ModForge plugin.
 */
public final class CompatibilityUtil {
    private static final Logger LOG = Logger.getInstance(CompatibilityUtil.class);
    
    // Dialog result constants
    public static final int DIALOG_YES = 0;
    public static final int DIALOG_NO = 1;
    public static final int DIALOG_CANCEL = 2;
    public static final int DIALOG_OK = 0;
    
    /**
     * Private constructor to prevent instantiation.
     */
    private CompatibilityUtil() {
        // Utility class
    }
    
    /**
     * Shows an error dialog with the given message.
     *
     * @param project The project
     * @param message The message
     * @param title The title
     */
    public static void showErrorDialog(Project project, String message, String title) {
        com.intellij.openapi.ui.Messages.showErrorDialog(project, message, title);
    }
    
    /**
     * Shows a warning dialog with the given message.
     *
     * @param project The project
     * @param message The message
     * @param title The title
     */
    public static void showWarningDialog(Project project, String message, String title) {
        com.intellij.openapi.ui.Messages.showWarningDialog(project, message, title);
    }
    
    /**
     * Shows an information dialog with the given message.
     *
     * @param project The project
     * @param message The message
     * @param title The title
     */
    public static void showInfoDialog(Project project, String message, String title) {
        com.intellij.openapi.ui.Messages.showInfoMessage(project, message, title);
    }
    
    /**
     * Shows a yes/no dialog with the given message.
     *
     * @param project The project
     * @param message The message
     * @param title The title
     * @return The user's choice (DIALOG_YES or DIALOG_NO)
     */
    public static int showYesNoDialog(Project project, String message, String title) {
        int result = com.intellij.openapi.ui.Messages.showYesNoDialog(project, message, title, null);
        return result == com.intellij.openapi.ui.Messages.YES ? DIALOG_YES : DIALOG_NO;
    }
    
    /**
     * Shows a yes/no dialog with the given message and customized button texts.
     *
     * @param project The project
     * @param title The title
     * @param message The message
     * @param yesText The text for the "Yes" button
     * @param noText The text for the "No" button
     * @param icon The icon to display
     * @return The user's choice (DIALOG_YES or DIALOG_NO)
     */
    public static int showYesNoDialog(Project project, String title, String message, String yesText, String noText, Icon icon) {
        int result = com.intellij.openapi.ui.Messages.showYesNoDialog(project, message, title, yesText, noText, icon);
        return result == com.intellij.openapi.ui.Messages.YES ? DIALOG_YES : DIALOG_NO;
    }
    
    /**
     * Shows a dialog with the given options and returns the selected option index.
     *
     * @param project The project
     * @param message The message
     * @param title The title
     * @param options The options
     * @param defaultOption The default option
     * @return The selected option index
     */
    public static int showChooseDialog(Project project, String message, String title, String[] options, String defaultOption) {
        Icon icon = null; // No icon
        return com.intellij.openapi.ui.Messages.showChooseDialog(project, message, title, options, defaultOption, icon);
    }
    
    /**
     * Gets the base directory of the project.
     * Compatible with IntelliJ IDEA 2025.1.1.1.
     *
     * @param project The project
     * @return The base directory, or null if not found
     */
    @Nullable
    public static VirtualFile getProjectBaseDir(@Nullable Project project) {
        if (project == null) {
            return null;
        }
        
        try {
            // First try the new modern API available in IntelliJ IDEA 2020.1+
            String basePath = project.getBasePath();
            if (basePath != null) {
                return com.intellij.openapi.vfs.LocalFileSystem.getInstance().findFileByPath(basePath);
            }
            return null;
        } catch (Exception e) {
            LOG.warn("Error getting project base directory using modern API, falling back to legacy method", e);
            
            // Fall back to legacy method if available
            try {
                // Use reflection to access deprecated method to maintain backward compatibility
                java.lang.reflect.Method getBaseDirMethod = Project.class.getMethod("getBaseDir");
                Object result = getBaseDirMethod.invoke(project);
                if (result instanceof VirtualFile) {
                    return (VirtualFile) result;
                }
            } catch (Exception ex) {
                LOG.warn("Could not get project base directory using any method", ex);
            }
            
            return null;
        }
    }
    
    /**
     * Checks if the project has any problems.
     *
     * @param project The project
     * @return True if the project has problems, false otherwise
     */
    public static boolean hasProblems(@Nullable Project project) {
        if (project == null) {
            return false;
        }
        
        Collection<VirtualFile> problemFiles = getProblemFiles(project);
        return !problemFiles.isEmpty();
    }
    
    /**
     * Checks if the file has problems.
     *
     * @param project The project
     * @param file The file
     * @return True if the file has problems, false otherwise
     */
    public static boolean hasProblemsIn(@Nullable Project project, @NotNull VirtualFile file) {
        if (project == null) {
            return false;
        }
        
        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        if (psiFile == null) {
            return false;
        }
        
        DaemonCodeAnalyzer analyzer = DaemonCodeAnalyzer.getInstance(project);
        return analyzer.isErrorAnalyzingFinished(psiFile) && 
               analyzer.hasErrors(psiFile);
    }
    
    /**
     * Gets the description of a problem.
     *
     * @param problem The problem
     * @return The description
     */
    public static String getProblemDescription(Object problem) {
        if (problem == null) {
            return "Unknown problem";
        }
        
        try {
            // Different versions of IntelliJ might have different problem classes
            // Try to use reflection to get the description
            Class<?> problemClass = problem.getClass();
            java.lang.reflect.Method getDescriptionMethod = problemClass.getMethod("getDescription");
            
            if (getDescriptionMethod != null) {
                Object result = getDescriptionMethod.invoke(problem);
                if (result instanceof String) {
                    return (String) result;
                }
            }
        } catch (Exception e) {
            LOG.warn("Failed to get problem description", e);
        }
        
        // Fallback to toString if reflection fails
        return problem.toString();
    }
    
    /**
     * Gets the virtual file from a problem.
     *
     * @param problem The problem
     * @return The virtual file, or null if not found
     */
    @Nullable
    public static VirtualFile getProblemVirtualFile(Object problem) {
        if (problem == null) {
            return null;
        }
        
        try {
            // Different versions of IntelliJ might have different problem classes
            // Try to use reflection to get the file
            Class<?> problemClass = problem.getClass();
            
            // Try to get the file directly
            try {
                java.lang.reflect.Method getFileMethod = problemClass.getMethod("getFile");
                if (getFileMethod != null) {
                    Object result = getFileMethod.invoke(problem);
                    if (result instanceof VirtualFile) {
                        return (VirtualFile) result;
                    }
                }
            } catch (NoSuchMethodException e) {
                // Ignore, try other methods
            }
            
            // Try to get the PSI element and then the file
            try {
                java.lang.reflect.Method getPsiElementMethod = problemClass.getMethod("getPsiElement");
                if (getPsiElementMethod != null) {
                    Object psiElement = getPsiElementMethod.invoke(problem);
                    if (psiElement != null) {
                        Class<?> psiElementClass = psiElement.getClass();
                        java.lang.reflect.Method getContainingFileMethod = psiElementClass.getMethod("getContainingFile");
                        if (getContainingFileMethod != null) {
                            Object psiFile = getContainingFileMethod.invoke(psiElement);
                            if (psiFile != null) {
                                Class<?> psiFileClass = psiFile.getClass();
                                java.lang.reflect.Method getVirtualFileMethod = psiFileClass.getMethod("getVirtualFile");
                                if (getVirtualFileMethod != null) {
                                    Object vFile = getVirtualFileMethod.invoke(psiFile);
                                    if (vFile instanceof VirtualFile) {
                                        return (VirtualFile) vFile;
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // Ignore, fall through to return null
            }
        } catch (Exception e) {
            LOG.warn("Failed to get problem virtual file", e);
        }
        
        return null;
    }
    
    /**
     * A radio button component compatible with JetBrains UI components.
     * This is a wrapper around JRadioButton to ensure compatibility with different IntelliJ versions.
     */
    public static class JBRadioButton extends javax.swing.JRadioButton {
        /**
         * Creates a new radio button with no text.
         */
        public JBRadioButton() {
            super();
            setup();
        }
        
        /**
         * Creates a new radio button with the specified text.
         *
         * @param text The text
         */
        public JBRadioButton(String text) {
            super(text);
            setup();
        }
        
        /**
         * Creates a new radio button with the specified text and selection state.
         *
         * @param text The text
         * @param selected Whether the radio button is selected
         */
        public JBRadioButton(String text, boolean selected) {
            super(text, selected);
            setup();
        }
        
        /**
         * Sets up the radio button with JetBrains UI components look and feel.
         */
        private void setup() {
            setOpaque(false);
            setFocusable(true);
            putClientProperty("JButton.backgroundColor", null);
            putClientProperty("JButton.borderColor", null);
        }
    }
    
    /**
     * Opens a file in the editor.
     *
     * @param project The project
     * @param file The file to open
     * @param requestFocus Whether to request focus
     */
    public static void openFileInEditor(@NotNull Project project, @NotNull VirtualFile file, boolean requestFocus) {
        com.intellij.openapi.fileEditor.OpenFileDescriptor descriptor = new com.intellij.openapi.fileEditor.OpenFileDescriptor(project, file);
        com.intellij.openapi.fileEditor.FileEditorManager.getInstance(project).openEditor(descriptor, requestFocus);
    }
    
    /**
     * Gets a mod file by its relative path.
     *
     * @param project The project
     * @param relativePath The relative path from the project base directory
     * @return The virtual file, or null if not found
     */
    @Nullable
    public static VirtualFile getModFileByRelativePath(@NotNull Project project, @NotNull String relativePath) {
        VirtualFile baseDir = getProjectBaseDir(project);
        if (baseDir == null) {
            return null;
        }
        
        return baseDir.findFileByRelativePath(relativePath);
    }
    
    /**
     * Runs a task on the UI thread.
     *
     * @param runnable The runnable to run
     */
    public static void runOnUiThread(@NotNull Runnable runnable) {
        if (ApplicationManager.getApplication().isDispatchThread()) {
            runnable.run();
        } else {
            ApplicationManager.getApplication().invokeLater(runnable);
        }
    }
    
    /**
     * Returns a PSI file from a virtual file.
     *
     * @param project The project
     * @param file The virtual file
     * @return The PSI file, or null if not found
     */
    @Nullable
    public static PsiFile getPsiFile(@NotNull Project project, @NotNull VirtualFile file) {
        return PsiManager.getInstance(project).findFile(file);
    }
    
    /**
     * Gets the project base path as a string.
     *
     * @param project The project
     * @return The project base path, or null if not found
     */
    @Nullable
    public static String getProjectBasePath(@NotNull Project project) {
        VirtualFile baseDir = getProjectBaseDir(project);
        return baseDir != null ? baseDir.getPath() : null;
    }
    
    /**
     * Gets a compatible application executor service.
     *
     * @return The executor service
     */
    @NotNull
    public static ExecutorService getCompatibleAppExecutorService() {
        return Executors.newCachedThreadPool();
    }
    
    /**
     * Gets a compatible application scheduled executor service.
     *
     * @return The scheduled executor service
     */
    @NotNull
    public static ScheduledExecutorService getCompatibleAppScheduledExecutorService() {
        return Executors.newScheduledThreadPool(1);
    }
    
    /**
     * Handles project opening events.
     *
     * @param project The project
     * @param callback The callback to run when the project is opened
     */
    public static void handleProjectOpened(@NotNull Project project, @NotNull Consumer<Project> callback) {
        callback.accept(project);
    }
    
    /**
     * Runs an action in read mode.
     *
     * @param supplier The supplier
     * @param <T> The result type
     * @return The result
     */
    public static <T> T runReadAction(@NotNull Supplier<T> supplier) {
        return ReadAction.compute(supplier::get);
    }
    
    /**
     * Runs an action in write mode.
     *
     * @param runnable The runnable
     */
    public static void runWriteAction(@NotNull Runnable runnable) {
        WriteAction.run(runnable::run);
    }
    
    /**
     * Computes a value in write action.
     *
     * @param supplier The supplier
     * @param <T> The result type
     * @return The result
     */
    public static <T> T computeInWriteAction(@NotNull Supplier<T> supplier) {
        return WriteAction.compute(supplier::get);
    }
    
    /**
     * Shows an input dialog with a project.
     *
     * @param project The project
     * @param message The message
     * @param title The title
     * @param initialValue The initial value
     * @return The user's input, or null if canceled
     */
    @Nullable
    public static String showInputDialogWithProject(
            @Nullable Project project,
            @NotNull String message,
            @NotNull String title,
            @Nullable String initialValue) {
        return com.intellij.openapi.ui.Messages.showInputDialog(project, message, title, null, initialValue, null);
    }
    
    /**
     * Shows an input dialog.
     *
     * @param project The project
     * @param message The message
     * @param title The title
     * @param initialValue The initial value
     * @return The user's input, or null if canceled
     */
    @Nullable
    public static String showInputDialog(
            @Nullable Project project,
            @NotNull String message,
            @NotNull String title,
            @Nullable String initialValue) {
        return com.intellij.openapi.ui.Messages.showInputDialog(project, message, title, null, initialValue, null);
    }
    
    /**
     * Gets all problem files.
     *
     * @param project The project
     * @return The problem files
     */
    @NotNull
    public static Collection<VirtualFile> getProblemFiles(@Nullable Project project) {
        Collection<VirtualFile> result = new ArrayList<>();
        if (project == null) {
            return result;
        }
        
        DaemonCodeAnalyzer analyzer = DaemonCodeAnalyzer.getInstance(project);
        
        // Get all files in the project
        VirtualFile baseDir = getProjectBaseDir(project);
        if (baseDir != null) {
            collectFilesWithProblems(project, baseDir, analyzer, result);
        }
        
        return result;
    }
    
    /**
     * Recursively collects files with problems.
     *
     * @param project The project
     * @param directory The directory to search
     * @param analyzer The analyzer
     * @param result The collection to add results to
     */
    private static void collectFilesWithProblems(
            @NotNull Project project, 
            @NotNull VirtualFile directory, 
            @NotNull DaemonCodeAnalyzer analyzer,
            @NotNull Collection<VirtualFile> result) {
        
        for (VirtualFile child : directory.getChildren()) {
            if (child.isDirectory()) {
                collectFilesWithProblems(project, child, analyzer, result);
            } else {
                PsiFile psiFile = PsiManager.getInstance(project).findFile(child);
                if (psiFile != null && analyzer.isErrorAnalyzingFinished(psiFile) && analyzer.hasErrors(psiFile)) {
                    result.add(child);
                }
            }
        }
    }
    
    /**
     * Gets problems for a file.
     *
     * @param project The project
     * @param file The file
     * @return The problems
     */
    @NotNull
    public static Collection<Object> getProblemsForFile(@Nullable Project project, @Nullable VirtualFile file) {
        Collection<Object> result = new ArrayList<>();
        if (project == null || file == null) {
            return result;
        }
        
        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        if (psiFile == null) {
            return result;
        }
        
        DaemonCodeAnalyzer analyzer = DaemonCodeAnalyzer.getInstance(project);
        if (analyzer.isErrorAnalyzingFinished(psiFile) && analyzer.hasErrors(psiFile)) {
            // Since we can't get the actual problems from DaemonCodeAnalyzer directly,
            // we'll create a generic problem description
            result.add(new Problem(file, "Compilation error detected"));
        }
        
        return result;
    }
    
    /**
     * Simple problem class to represent code issues.
     */
    public static class Problem {
        private final VirtualFile file;
        private final String description;
        
        /**
         * Creates a new problem.
         *
         * @param file The file with the problem
         * @param description The problem description
         */
        public Problem(VirtualFile file, String description) {
            this.file = file;
            this.description = description;
        }
        
        /**
         * Gets the file with the problem.
         *
         * @return The file
         */
        public VirtualFile getFile() {
            return file;
        }
        
        /**
         * Gets the problem description.
         *
         * @return The description
         */
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * Extracts a version from a file content.
     *
     * @param content The file content
     * @param versionKey The version key to look for
     * @return The extracted version, or null if not found
     */
    @Nullable
    public static String extractVersionFromFile(@Nullable String content, @Nullable String versionKey) {
        if (content == null || versionKey == null) {
            return null;
        }
        
        try {
            // Look for patterns like: minecraft_version = "1.16.5" or "minecraft_version": "1.16.5"
            Pattern pattern = Pattern.compile(
                    versionKey + "\\s*[=:]\\s*[\"']([^\"']+)[\"']", 
                    Pattern.MULTILINE);
            Matcher matcher = pattern.matcher(content);
            if (matcher.find()) {
                return matcher.group(1);
            }
            
            // Try to match the key with the standard properties format key=value (no quotes)
            pattern = Pattern.compile(versionKey + "\\s*=\\s*([^\\s]+)");
            matcher = pattern.matcher(content);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception e) {
            LOG.warn("Failed to extract version from file content for key: " + versionKey, e);
        }
        
        return null;
    }
    
    /**
     * Checks if a file exists at the given path.
     *
     * @param pathStr The path to check
     * @return True if the file exists, false otherwise
     */
    public static boolean fileExists(@NotNull String pathStr) {
        try {
            Path path = Paths.get(pathStr);
            return Files.exists(path) && Files.isRegularFile(path);
        } catch (Exception e) {
            LOG.warn("Failed to check if file exists: " + pathStr, e);
            return false;
        }
    }
    
    /**
     * Checks if a file contains the given text.
     *
     * @param pathStr The path of the file
     * @param text The text to check for
     * @return True if the file contains the text, false otherwise
     */
    public static boolean fileContainsText(@NotNull String pathStr, @NotNull String text) {
        try {
            Path path = Paths.get(pathStr);
            if (!Files.exists(path) || !Files.isRegularFile(path)) {
                return false;
            }
            
            String content = Files.readString(path, StandardCharsets.UTF_8);
            return content.contains(text);
        } catch (IOException e) {
            LOG.warn("Failed to check if file contains text: " + pathStr, e);
            return false;
        } catch (Exception e) {
            LOG.warn("Unexpected error checking if file contains text: " + pathStr, e);
            return false;
        }
    }
    
    // Second implementation of extractVersionFromFile was removed to fix duplication
    
    /**
     * Checks if a mod loader version is compatible with a given Minecraft version.
     *
     * @param modLoaderVersion The mod loader version
     * @param minecraftVersion The Minecraft version
     * @param modLoaderType The mod loader type
     * @return True if the mod loader version is compatible, false otherwise
     */
    public static boolean isModLoaderVersionCompatible(@NotNull String modLoaderVersion, 
                                                      @NotNull String minecraftVersion,
                                                      @NotNull String modLoaderType) {
        // This would normally use some compatibility logic to check if the mod loader version
        // is compatible with the given Minecraft version. For simplicity, we'll just assume
        // they are compatible.
        return true;
    }
    
    /**
     * Gets a compatible version of a mod loader for a given Minecraft version.
     *
     * @param minecraftVersion The Minecraft version
     * @param modLoaderType The mod loader type
     * @return A compatible mod loader version, or null if not found
     */
    @Nullable
    public static String getCompatibleModLoaderVersion(@NotNull String minecraftVersion, 
                                                      @NotNull String modLoaderType) {
        // This would normally use some lookup table or API to get a compatible mod loader version
        // for the given Minecraft version. For simplicity, we'll just return a placeholder version.
        
        // These are just placeholder versions, not actual compatible versions.
        // In a real implementation, this would use a more sophisticated mapping.
        switch (modLoaderType.toLowerCase()) {
            case "forge":
                if (minecraftVersion.startsWith("1.16")) {
                    return "36.2.0";
                } else if (minecraftVersion.startsWith("1.18")) {
                    return "40.1.0";
                } else if (minecraftVersion.startsWith("1.19")) {
                    return "43.1.1";
                } else if (minecraftVersion.startsWith("1.20")) {
                    return "46.0.1";
                }
                break;
            case "fabric":
                if (minecraftVersion.startsWith("1.16")) {
                    return "0.14.9";
                } else if (minecraftVersion.startsWith("1.18")) {
                    return "0.14.9";
                } else if (minecraftVersion.startsWith("1.19")) {
                    return "0.14.9";
                } else if (minecraftVersion.startsWith("1.20")) {
                    return "0.14.21";
                }
                break;
            case "quilt":
                if (minecraftVersion.startsWith("1.18")) {
                    return "0.17.1-beta.3";
                } else if (minecraftVersion.startsWith("1.19")) {
                    return "0.17.1";
                } else if (minecraftVersion.startsWith("1.20")) {
                    return "0.19.0-beta.18";
                }
                break;
        }
        
        return null;
    }
}