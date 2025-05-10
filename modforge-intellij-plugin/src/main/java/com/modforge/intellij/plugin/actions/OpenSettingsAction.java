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
        Project project = e.getProject();
        
        try {
            LOG.info("Opening ModForge settings");
            
            // Open settings dialog
            ShowSettingsUtil.getInstance().showSettingsDialog(
                    project,
                    ModForgeSettingsConfigurable.class
            );
        } catch (Exception ex) {
            LOG.error("Error opening settings", ex);
        }
    }
}