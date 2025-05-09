package com.modforge.intellij.plugin.listeners;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.openapi.vfs.VirtualFileListener;
import com.intellij.openapi.vfs.VirtualFileManagerListener;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.jetbrains.annotations.NotNull;

/**
 * Service that listens for file changes.
 * This service is used to detect changes to files and trigger appropriate actions.
 */
@Service(Service.Level.PROJECT)
public final class ModForgeFileListener implements VirtualFileListener {
    private static final Logger LOG = Logger.getInstance(ModForgeFileListener.class);
    
    private final Project project;
    
    /**
     * Creates a new ModForgeFileListener.
     * @param project The project
     */
    public ModForgeFileListener(@NotNull Project project) {
        this.project = project;
        
        // Register listener
        project.getBaseDir().getFileSystem().addVirtualFileListener(this);
        
        LOG.info("File listener created for project: " + project.getName());
    }
    
    /**
     * Gets the ModForgeFileListener for a project.
     * @param project The project
     * @return The ModForgeFileListener
     */
    public static ModForgeFileListener getInstance(@NotNull Project project) {
        return project.getService(ModForgeFileListener.class);
    }
    
    @Override
    public void fileCreated(@NotNull VirtualFileEvent event) {
        VirtualFile file = event.getFile();
        
        if (!shouldProcessFile(file)) {
            return;
        }
        
        LOG.info("File created: " + file.getPath());
    }
    
    @Override
    public void fileDeleted(@NotNull VirtualFileEvent event) {
        VirtualFile file = event.getFile();
        
        if (!shouldProcessFile(file)) {
            return;
        }
        
        LOG.info("File deleted: " + file.getPath());
    }
    
    @Override
    public void contentsChanged(@NotNull VirtualFileEvent event) {
        VirtualFile file = event.getFile();
        
        if (!shouldProcessFile(file)) {
            return;
        }
        
        LOG.info("File changed: " + file.getPath());
        
        // Get PSI file
        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        
        if (psiFile == null) {
            return;
        }
        
        // Process file
        processFileChange(psiFile);
    }
    
    /**
     * Processes a file change.
     * @param psiFile The PSI file
     */
    private void processFileChange(@NotNull PsiFile psiFile) {
        // Check if the file is a Java file
        if (psiFile instanceof PsiJavaFile) {
            LOG.info("Processing Java file change: " + psiFile.getName());
            processJavaFileChange((PsiJavaFile) psiFile);
        }
    }
    
    /**
     * Processes a Java file change.
     * @param javaFile The Java file
     */
    private void processJavaFileChange(@NotNull PsiJavaFile javaFile) {
        // Extract package name
        String packageName = javaFile.getPackageName();
        LOG.info("Java file package: " + packageName);
        
        // Extract class names
        String[] classNames = javaFile.getClasses().stream()
                .map(psiClass -> psiClass.getName())
                .toArray(String[]::new);
        
        if (classNames.length > 0) {
            LOG.info("Java file classes: " + String.join(", ", classNames));
        }
    }
    
    /**
     * Checks if a file should be processed.
     * @param file The file
     * @return True if the file should be processed, false otherwise
     */
    private boolean shouldProcessFile(@NotNull VirtualFile file) {
        // Check if file exists
        if (!file.exists()) {
            return false;
        }
        
        // Check if file is in project
        if (!file.getPath().startsWith(project.getBasePath())) {
            return false;
        }
        
        // Check if file is a source file
        String extension = file.getExtension();
        return extension != null && (
                extension.equals("java") ||
                extension.equals("kt") ||
                extension.equals("gradle") ||
                extension.equals("gradle.kts") ||
                extension.equals("xml")
        );
    }
    
    /**
     * Disposes the listener.
     */
    public void dispose() {
        LOG.info("Disposing file listener for project: " + project.getName());
        
        // Unregister listener
        project.getBaseDir().getFileSystem().removeVirtualFileListener(this);
    }
}