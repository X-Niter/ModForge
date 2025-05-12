package com.modforge.intellij.plugin.listeners;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.modforge.intellij.plugin.collaboration.CollaborationService;
import com.modforge.intellij.plugin.utils.CompatibilityUtil;
import org.jetbrains.annotations.NotNull;

/**
 * Listener for file editor manager events.
 * This listener tracks when files are opened and closed to provide
 * contextual assistance and collaboration features.
 */
public class ModForgeFileEditorManagerListener implements FileEditorManagerListener {
    private static final Logger LOG = Logger.getInstance(ModForgeFileEditorManagerListener.class);
    
    @Override
    public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        LOG.info("File opened: " + file.getPath());
        
        Project project = source.getProject();
        
        // Check if file is a Java file
        if (isJavaFile(file)) {
            // Get collaboration service
            CollaborationService collaborationService = project.getService(CollaborationService.class);
            
            // Check if file is being collaborated on
            if (collaborationService.isFileCollaborated(file)) {
                LOG.info("File is being collaborated on: " + file.getPath());
                
                // Register editor for collaboration
                collaborationService.registerEditor(file);
            }
            
            // Analyze file for suggestions
            analyzeFileForSuggestions(project, file);
        }
    }
    
    @Override
    public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        LOG.info("File closed: " + file.getPath());
        
        Project project = source.getProject();
        
        // Check if file is a Java file
        if (isJavaFile(file)) {
            // Get collaboration service
            CollaborationService collaborationService = project.getService(CollaborationService.class);
            
            // Check if file is being collaborated on
            if (collaborationService.isFileCollaborated(file)) {
                LOG.info("Unregistering collaboration for file: " + file.getPath());
                
                // Unregister editor for collaboration
                collaborationService.unregisterEditor(file);
            }
        }
    }
    
    @Override
    public void selectionChanged(@NotNull FileEditorManagerEvent event) {
        VirtualFile oldFile = event.getOldFile();
        VirtualFile newFile = event.getNewFile();
        
        if (oldFile != null) {
            LOG.debug("Selection changed from: " + oldFile.getPath());
        }
        
        if (newFile != null) {
            LOG.debug("Selection changed to: " + newFile.getPath());
            
            // Check if file is a Java file
            if (isJavaFile(newFile)) {
                // Update currently active file for collaboration service
                Project project = event.getManager().getProject();
                CollaborationService collaborationService = project.getService(CollaborationService.class);
                collaborationService.setActiveFile(newFile);
            }
        }
    }
    
    /**
     * Checks if a file is a Java file.
     * @param file The file to check
     * @return Whether the file is a Java file
     */
    private boolean isJavaFile(@NotNull VirtualFile file) {
        return !file.isDirectory() && "java".equals(file.getExtension());
    }
    
    /**
     * Analyzes a file for suggestions.
     * @param project The project
     * @param file The file to analyze
     */
    private void analyzeFileForSuggestions(@NotNull Project project, @NotNull VirtualFile file) {
        try {
            // Get PsiFile for the virtual file
            PsiFile psiFile = CompatibilityUtil.getPsiFile(project, file);
            if (psiFile == null) {
                return;
            }
            
            // In a real implementation, this would analyze the file and provide suggestions
            // For now, we'll just log a message
            LOG.info("Analyzing file for suggestions: " + file.getPath());
            
            if (psiFile instanceof PsiJavaFile) {
                PsiJavaFile javaFile = (PsiJavaFile) psiFile;
                
                // Check if the file is a Minecraft mod
                boolean isMinecraftMod = isMinecraftModFile(psiFile);
                if (isMinecraftMod) {
                    LOG.info("File appears to be a Minecraft mod: " + file.getPath());
                    
                    // Register the file for enhanced suggestions
                    // This would be a more elaborate process in a real implementation
                    LOG.info("Registering file for enhanced suggestions: " + file.getPath());
                }
            }
        } catch (Exception e) {
            LOG.error("Error analyzing file for suggestions", e);
        }
    }
    
    /**
     * Checks if a file is a Minecraft mod file.
     * @param psiFile The file to check
     * @return Whether the file is a Minecraft mod file
     */
    private boolean isMinecraftModFile(@NotNull PsiJavaFile psiFile) {
        // Check for imports that suggest this is a Minecraft mod
        String[] modImports = {
                "net.minecraft",
                "net.minecraftforge",
                "net.fabricmc",
                "com.mojang",
                "net.minecraft.client",
                "net.minecraft.server",
                "dev.architectury"
        };
        
        String fileText = psiFile.getText();
        for (String modImport : modImports) {
            if (fileText.contains("import " + modImport)) {
                return true;
            }
        }
        
        // Check for class names that suggest this is a Minecraft mod
        String[] modClassNames = {
                "ModInitializer",
                "ClientModInitializer",
                "DedicatedServerModInitializer",
                "Mod",
                "Plugin",
                "MainClass",
                "ModContainer",
                "AbstractMod",
                "MinecraftMod"
        };
        
        for (String className : modClassNames) {
            if (fileText.contains("class ") && fileText.contains("implements " + className) || 
                    fileText.contains("extends " + className) || 
                    fileText.contains("@" + className)) {
                return true;
            }
        }
        
        return false;
    }
}