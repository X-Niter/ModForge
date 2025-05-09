package com.modforge.intellij.plugin.crossloader.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.NlsActions;
import com.intellij.util.PlatformIcons;
import com.modforge.intellij.plugin.crossloader.ui.CrossLoaderProjectSetupDialog;
import com.modforge.intellij.plugin.services.ModForgeProjectService;
import org.jetbrains.annotations.NotNull;

/**
 * Action for creating a new cross-loader mod project.
 * This action opens the cross-loader project setup dialog.
 */
public class CreateCrossLoaderModAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(CreateCrossLoaderModAction.class);
    
    /**
     * Creates a new create cross-loader mod action.
     */
    public CreateCrossLoaderModAction() {
        super("Create Cross-Loader Mod", "Create a new Minecraft mod that works across multiple mod loaders", PlatformIcons.ADD_ICON);
    }
    
    /**
     * Creates a new create cross-loader mod action with custom text.
     * @param text The action text
     * @param description The action description
     */
    public CreateCrossLoaderModAction(@NotNull @NlsActions.ActionText String text, @NotNull @NlsActions.ActionDescription String description) {
        super(text, description, PlatformIcons.ADD_ICON);
    }
    
    /**
     * Performs the action.
     * @param e The action event
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        
        // Open the dialog
        CrossLoaderProjectSetupDialog dialog = new CrossLoaderProjectSetupDialog(project);
        if (dialog.showAndGet()) {
            // Dialog was confirmed
            CrossLoaderProjectSetupDialog.CrossLoaderProjectConfig config = dialog.getConfiguration();
            
            // Create the project
            createCrossLoaderProject(project, config);
        }
    }
    
    /**
     * Creates a cross-loader project.
     * @param project The project
     * @param config The project configuration
     */
    private void createCrossLoaderProject(@NotNull Project project, @NotNull CrossLoaderProjectSetupDialog.CrossLoaderProjectConfig config) {
        try {
            // Get the ModForge project service
            ModForgeProjectService projectService = ModForgeProjectService.getInstance(project);
            
            // TODO: Implement project creation
            // In a real implementation, this would create the project files and structure
            
            LOG.info("Creating cross-loader project: " + config.getModId());
            LOG.info("Mod name: " + config.getModName());
            LOG.info("Using Architectury: " + config.isUseArchitectury());
            LOG.info("Selected loaders: " + config.getSelectedLoaders());
            
            // Show a success message for now
            Messages.showInfoMessage(
                    project,
                    "Created cross-loader project '" + config.getModName() + "' successfully!",
                    "Project Created"
            );
        } catch (Exception ex) {
            LOG.error("Error creating cross-loader project", ex);
            
            // Show an error message
            Messages.showErrorDialog(
                    project,
                    "Failed to create cross-loader project: " + ex.getMessage(),
                    "Error Creating Project"
            );
        }
    }
    
    /**
     * Updates the action presentation.
     * @param e The action event
     */
    @Override
    public void update(@NotNull AnActionEvent e) {
        // Enable the action if we have a project
        Project project = e.getData(CommonDataKeys.PROJECT);
        e.getPresentation().setEnabled(project != null);
    }
}