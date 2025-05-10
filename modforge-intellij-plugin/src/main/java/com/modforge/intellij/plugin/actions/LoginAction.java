package com.modforge.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.modforge.intellij.plugin.auth.ModAuthenticationManager;
import com.modforge.intellij.plugin.ui.login.LoginDialog;
import org.jetbrains.annotations.NotNull;

/**
 * Action for logging in to ModForge.
 */
public class LoginAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(LoginAction.class);
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        
        try {
            // Create a new login dialog
            LoginDialog dialog = new LoginDialog(project);
            
            // Show dialog
            boolean loggedIn = dialog.showAndGet();
            
            if (loggedIn) {
                LOG.info("User logged in successfully");
            }
        } catch (Exception ex) {
            LOG.error("Error during login action", ex);
        }
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        // Disable the action if already authenticated
        ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
        
        // Only enable if not authenticated
        e.getPresentation().setEnabled(!authManager.isAuthenticated());
    }
}