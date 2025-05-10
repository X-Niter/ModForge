package com.modforge.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.modforge.intellij.plugin.settings.ModForgeSettingsConfigurable;
import org.jetbrains.annotations.NotNull;

/**
 * Action to open ModForge settings.
 */
public class OpenSettingsAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(OpenSettingsAction.class);
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        
        if (project == null) {
            return;
        }
        
        LOG.info("Opening ModForge settings");
        
        // Show settings dialog
        ShowSettingsUtil.getInstance().showSettingsDialog(project, ModForgeSettingsConfigurable.class);
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        // Only enable if we have a project
        Project project = e.getProject();
        e.getPresentation().setEnabledAndVisible(project != null);
    }
}