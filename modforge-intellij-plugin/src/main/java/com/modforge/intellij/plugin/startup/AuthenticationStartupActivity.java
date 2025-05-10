package com.modforge.intellij.plugin.startup;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.diagnostic.Logger;
import com.modforge.intellij.plugin.auth.AuthenticationManager;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.jetbrains.annotations.NotNull;

/**
 * Activity that runs when a project is opened to verify authentication.
 */
public class AuthenticationStartupActivity implements StartupActivity.Background {
    private static final Logger LOG = Logger.getInstance(AuthenticationStartupActivity.class);
    
    @Override
    public void runActivity(@NotNull Project project) {
        LOG.info("ModForge authentication verification running on startup");
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        // Only attempt verification if credentials are set
        if (!settings.getUsername().isEmpty() && !settings.getPassword().isEmpty()) {
            if (settings.isAuthenticated()) {
                // Verify existing authentication
                boolean isValid = AuthenticationManager.getInstance().verifyAuthentication();
                
                if (!isValid) {
                    LOG.info("Authentication no longer valid, attempting to re-authenticate");
                    // Try to re-authenticate
                    AuthenticationManager.getInstance().authenticate();
                }
            } else if (!settings.getUsername().isEmpty() && !settings.getPassword().isEmpty()) {
                // Try to authenticate if credentials are present but not authenticated
                LOG.info("Attempting initial authentication with saved credentials");
                AuthenticationManager.getInstance().authenticate();
            }
        }
    }
}