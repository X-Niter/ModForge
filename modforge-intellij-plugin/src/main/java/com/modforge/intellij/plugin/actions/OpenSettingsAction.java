package com.modforge.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.modforge.intellij.plugin.settings.ModForgeSettingsConfigurable;
import org.jetbrains.annotations.NotNull;

/**
 * Action for opening ModForge settings.
 */
public class OpenSettingsAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(OpenSettingsAction.class);
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // Get project
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        
        LOG.info("Open settings action performed");
        
        // Show settings dialog
        ShowSettingsUtil.getInstance().showSettingsDialog(project, ModForgeSettingsConfigurable.class);
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        // Get project
        Project project = e.getProject();
        
        // Enable only if project is not null
        e.getPresentation().setEnabled(project != null);
    }
}