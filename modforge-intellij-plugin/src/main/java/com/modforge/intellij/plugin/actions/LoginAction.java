package com.modforge.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.modforge.intellij.plugin.auth.AuthenticationManager;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import com.modforge.intellij.plugin.ui.dialog.LoginDialog;
import org.jetbrains.annotations.NotNull;

/**
 * Action for logging in to the ModForge server.
 */
public class LoginAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        
        if (project == null) {
            return;
        }
        
        // Check if already authenticated
        if (ModForgeSettings.getInstance().isAuthenticated()) {
            // Already authenticated, show dialog asking if user wants to re-authenticate
            if (!confirmReauthentication(project)) {
                return;
            }
        }
        
        // Show the login dialog
        LoginDialog dialog = new LoginDialog(project);
        if (dialog.showAndGet()) {
            // Login successful
            // No need to handle this here as the dialog does it itself
        }
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        // Enable action only when the project is available
        e.getPresentation().setEnabledAndVisible(e.getProject() != null);
        
        // Update the text based on authentication status
        if (ModForgeSettings.getInstance().isAuthenticated()) {
            e.getPresentation().setText("Re-authenticate with ModForge");
        } else {
            e.getPresentation().setText("Login to ModForge");
        }
    }
    
    /**
     * Confirms if the user wants to re-authenticate.
     * @param project The project
     * @return True if the user wants to re-authenticate, false otherwise
     */
    private boolean confirmReauthentication(Project project) {
        return true; // Always allow re-authentication for now
    }
}