package com.modforge.intellij.plugin.listeners;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileCopyEvent;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.openapi.vfs.VirtualFileListener;
import com.intellij.openapi.vfs.VirtualFileMoveEvent;
import com.intellij.openapi.vfs.VirtualFilePropertyEvent;
import com.modforge.intellij.plugin.services.ContinuousDevelopmentService;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

/**
 * Listener for file events.
 */
@Service
public final class ModForgeFileListener implements VirtualFileListener {
    private static final Logger LOG = Logger.getInstance(ModForgeFileListener.class);
    
    private final Project project;
    private final Set<String> trackedExtensions = new HashSet<>();
    
    /**
     * Gets the instance of this listener for the specified project.
     * @param project The project
     * @return The listener instance
     */
    public static ModForgeFileListener getInstance(@NotNull Project project) {
        return project.getService(ModForgeFileListener.class);
    }
    
    /**
     * Creates a new instance of this listener.
     * @param project The project
     */
    public ModForgeFileListener(Project project) {
        this.project = project;
        
        // Initialize tracked extensions
        trackedExtensions.add("java");
        trackedExtensions.add("kt");
        trackedExtensions.add("gradle");
        trackedExtensions.add("properties");
        trackedExtensions.add("json");
        trackedExtensions.add("toml");
        
        LOG.info("ModForge file listener initialized");
    }
    
    @Override
    public void contentsChanged(@NotNull VirtualFileEvent event) {
        if (!isTrackedFile(event.getFile())) {
            return;
        }
        
        LOG.info("File contents changed: " + event.getFile().getPath());
        
        // Check if continuous development is enabled
        ModForgeSettings settings = ModForgeSettings.getInstance();
        if (!settings.isContinuousDevelopmentEnabled()) {
            return;
        }
        
        // Get service
        ContinuousDevelopmentService service = ContinuousDevelopmentService.getInstance(project);
        
        if (service == null) {
            LOG.warn("Continuous development service not available");
            return;
        }
        
        // If service is running, this will eventually trigger a check
        if (service.isRunning()) {
            LOG.info("Continuous development is running, changes will be processed");
        }
    }
    
    @Override
    public void fileCreated(@NotNull VirtualFileEvent event) {
        if (!isTrackedFile(event.getFile())) {
            return;
        }
        
        LOG.info("File created: " + event.getFile().getPath());
    }
    
    @Override
    public void fileDeleted(@NotNull VirtualFileEvent event) {
        if (!isTrackedFile(event.getFile())) {
            return;
        }
        
        LOG.info("File deleted: " + event.getFile().getPath());
    }
    
    @Override
    public void fileMoved(@NotNull VirtualFileMoveEvent event) {
        if (!isTrackedFile(event.getFile())) {
            return;
        }
        
        LOG.info("File moved: " + event.getFile().getPath() + " -> " + event.getNewParent().getPath());
    }
    
    @Override
    public void fileCopied(@NotNull VirtualFileCopyEvent event) {
        if (!isTrackedFile(event.getFile())) {
            return;
        }
        
        LOG.info("File copied: " + event.getOriginalFile().getPath() + " -> " + event.getFile().getPath());
    }
    
    @Override
    public void propertyChanged(@NotNull VirtualFilePropertyEvent event) {
        if (!isTrackedFile(event.getFile())) {
            return;
        }
        
        LOG.info("File property changed: " + event.getFile().getPath() + " - " + event.getPropertyName());
    }
    
    /**
     * Checks if a file is tracked by this listener.
     * @param file The file to check
     * @return True if the file is tracked, false otherwise
     */
    private boolean isTrackedFile(VirtualFile file) {
        if (file == null || !file.isValid() || file.isDirectory()) {
            return false;
        }
        
        // Check if file is in project
        String projectBasePath = project.getBasePath();
        if (projectBasePath != null && !file.getPath().startsWith(projectBasePath)) {
            return false;
        }
        
        // Check if file has a tracked extension
        String extension = file.getExtension();
        if (extension == null) {
            return false;
        }
        
        return trackedExtensions.contains(extension.toLowerCase());
    }
}