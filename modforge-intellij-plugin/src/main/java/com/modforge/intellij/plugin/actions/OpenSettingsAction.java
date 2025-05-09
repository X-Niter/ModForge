package com.modforge.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.modforge.intellij.plugin.settings.ModForgeSettingsConfigurable;
import org.jetbrains.annotations.NotNull;

/**
 * Action to open the ModForge settings dialog.
 */
public class OpenSettingsAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        
        if (project == null) {
            return;
        }
        
        ShowSettingsUtil.getInstance().showSettingsDialog(project, ModForgeSettingsConfigurable.class);
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(e.getProject() != null);
    }
}