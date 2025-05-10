package com.modforge.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import com.modforge.intellij.plugin.ui.dialog.LoginDialog;
import org.jetbrains.annotations.NotNull;

/**
 * Action to login to ModForge server.
 */
public class LoginAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(LoginAction.class);
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        
        LOG.info("Opening login dialog");
        
        // Show login dialog
        LoginDialog dialog = new LoginDialog(project);
        if (dialog.showAndGet()) {
            // Login successful, update settings
            ModForgeSettings settings = ModForgeSettings.getInstance();
            settings.setAuthenticated(true);
            
            LOG.info("Login successful");
        } else {
            LOG.info("Login cancelled");
        }
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        // Only enable if we have a project and are not authenticated
        e.getPresentation().setEnabledAndVisible(project != null && !settings.isAuthenticated());
    }
}