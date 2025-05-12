package com.modforge.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.modforge.intellij.plugin.utils.CompatibilityUtil;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.modforge.intellij.plugin.services.ModAuthenticationManager;
import com.modforge.intellij.plugin.services.ModForgeNotificationService;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONObject;

import javax.swing.*;
import java.awt.*;

/**
 * Action for adding features to a mod.
 */
public class AddFeaturesAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(AddFeaturesAction.class);
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        
        if (project == null) {
            LOG.warn("Project is null");
            return;
        }
        
        try {
            // Check authentication
            ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
            if (!authManager.isAuthenticated()) {
                ModForgeNotificationService notificationService = ModForgeNotificationService.getInstance();
                if (notificationService != null) {
                    notificationService.showErrorDialog(
                            project,
                            "Authentication Required",
                            "You must be logged in to ModForge to add features."
                    );
                } else {
                    CompatibilityUtil.showErrorDialog(
                            project,
                            "You must be logged in to ModForge to add features.",
                            "Authentication Required"
                    );
                }
                return;
            }
            
            // Show feature description dialog
            FeatureDescriptionDialog dialog = new FeatureDescriptionDialog(project);
            boolean proceed = dialog.showAndGet();
            
            if (!proceed) {
                // User cancelled
                return;
            }
            
            // Get feature description
            String featureDescription = dialog.getFeatureDescription();
            
            // Check if description is empty
            if (featureDescription == null || featureDescription.trim().isEmpty()) {
                ModForgeNotificationService notificationService = ModForgeNotificationService.getInstance();
                if (notificationService != null) {
                    notificationService.showErrorDialog(
                            project,
                            "Empty Description",
                            "Feature description cannot be empty."
                    );
                } else {
                    CompatibilityUtil.showErrorDialog(
                            project,
                            "Feature description cannot be empty.",
                            "Empty Description"
                    );
                }
                return;
            }
            
            // Show confirmation dialog
            int confirmation;
            ModForgeNotificationService notificationService = ModForgeNotificationService.getInstance();
            if (notificationService != null) {
                confirmation = notificationService.showYesNoDialog(
                        project,
                        "Confirm Feature Addition",
                        "Add the following feature to your mod?\n\n" + featureDescription
                );
            } else {
                confirmation = CompatibilityUtil.showYesNoDialog(
                        project,
                        "Add the following feature to your mod?\n\n" + featureDescription,
                        "Confirm Feature Addition"
                );
            }
            
            if (confirmation != Messages.YES) {
                // User cancelled
                return;
            }
            
            // Create input data for feature addition
            JSONObject inputData = new JSONObject();
            inputData.put("featureDescription", featureDescription);
            
            // Get settings
            ModForgeSettings settings = ModForgeSettings.getInstance();
            boolean usePatterns = settings.isPatternRecognition();
            inputData.put("usePatterns", usePatterns);
            
            // Run feature addition in background
            com.intellij.openapi.progress.ProgressManager.getInstance().run(
                    new com.intellij.openapi.progress.Task.Backgroundable(project, "Adding Feature") {
                        @Override
                        public void run(@NotNull com.intellij.openapi.progress.ProgressIndicator indicator) {
                            try {
                                // Update progress indicator
                                indicator.setText("Analyzing feature request...");
                                indicator.setIndeterminate(true);
                                
                                // Get server URL and token
                                String serverUrl = settings.getServerUrl();
                                String token = settings.getAccessToken();
                                
                                // Create API client
                                com.modforge.intellij.plugin.api.ApiClient apiClient = 
                                        new com.modforge.intellij.plugin.api.ApiClient(serverUrl);
                                apiClient.setAuthToken(token);
                                
                                // Send feature addition request
                                indicator.setText("Sending feature request to API...");
                                String responseJson = apiClient.post("/api/mod/add-feature", inputData.toJSONString());
                                
                                // Parse response
                                org.json.simple.parser.JSONParser parser = new org.json.simple.parser.JSONParser();
                                org.json.simple.JSONObject response = (org.json.simple.JSONObject) parser.parse(responseJson);
                                
                                // Get request ID for tracking
                                final String requestId = (String) response.get("requestId");
                                final boolean success = (Boolean) response.getOrDefault("success", false);
                                final String message = (String) response.getOrDefault("message", "Feature addition initiated");
                                
                                // Show result in UI thread
                                com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(() -> {
                                    if (success) {
                                        // Show success message with request ID for tracking
                                        ModForgeNotificationService notificationService = ModForgeNotificationService.getInstance();
                                        if (notificationService != null) {
                                            notificationService.showInfoMessage(
                                                    project,
                                                    "Feature Addition Requested",
                                                    message + "\n\nRequest ID: " + requestId + 
                                                    "\n\nYou can track the progress in the ModForge panel."
                                            );
                                        } else {
                                            com.modforge.intellij.plugin.utils.CompatibilityUtil.showInfoDialog(
                                                    project,
                                                    message + "\n\nRequest ID: " + requestId + 
                                                    "\n\nYou can track the progress in the ModForge panel.",
                                                    "Feature Addition Requested"
                                            );
                                        }
                                        
                                        // Open ModForge tool window to show progress
                                        com.intellij.openapi.wm.ToolWindowManager.getInstance(project)
                                                .getToolWindow("ModForge")
                                                .show(null);
                                    } else {
                                        // Show error message
                                        ModForgeNotificationService notificationService = ModForgeNotificationService.getInstance();
                                        if (notificationService != null) {
                                            notificationService.showErrorDialog(
                                                    project,
                                                    "Feature Addition Failed",
                                                    "Failed to add feature: " + message
                                            );
                                        } else {
                                            CompatibilityUtil.showErrorDialog(
                                                    project,
                                                    "Failed to add feature: " + message,
                                                    "Feature Addition Failed"
                                            );
                                        }
                                    }
                                });
                            } catch (Exception ex) {
                                LOG.error("Error adding feature", ex);
                                
                                // Show error in UI thread
                                com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(() -> {
                                    ModForgeNotificationService errNotificationService = ModForgeNotificationService.getInstance();
                                    if (errNotificationService != null) {
                                        errNotificationService.showErrorDialog(
                                                project,
                                                "Error Adding Feature",
                                                "An error occurred while adding the feature: " + ex.getMessage()
                                        );
                                    } else {
                                        CompatibilityUtil.showErrorDialog(
                                                project,
                                                "An error occurred while adding the feature: " + ex.getMessage(),
                                                "Error Adding Feature"
                                        );
                                    }
                                });
                            }
                        }
                    }
            );
        } catch (Exception ex) {
            LOG.error("Error in add features action", ex);
            
            // Show error
            ModForgeNotificationService notificationService = ModForgeNotificationService.getInstance();
            if (notificationService != null) {
                notificationService.showErrorDialog(
                        project,
                        "Error",
                        "An error occurred: " + ex.getMessage()
                );
            } else {
                CompatibilityUtil.showErrorDialog(
                        project,
                        "An error occurred: " + ex.getMessage(),
                        "Error"
                );
            }
        }
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        // Only enable if authenticated and project is available
        ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
        
        // Enable if authenticated and project is available
        e.getPresentation().setEnabled(authManager.isAuthenticated() && e.getProject() != null);
    }
    
    /**
     * Dialog for entering feature description.
     */
    private static class FeatureDescriptionDialog extends DialogWrapper {
        private final JBTextArea descriptionField;
        
        public FeatureDescriptionDialog(Project project) {
            super(project, true);
            
            descriptionField = new JBTextArea(10, 50);
            descriptionField.setLineWrap(true);
            descriptionField.setWrapStyleWord(true);
            
            setTitle("Add Feature");
            init();
        }
        
        @Override
        protected @Nullable JComponent createCenterPanel() {
            // Create scroll pane for description
            JBScrollPane scrollPane = new JBScrollPane(descriptionField);
            scrollPane.setPreferredSize(new Dimension(400, 200));
            
            // Build form
            FormBuilder formBuilder = FormBuilder.createFormBuilder()
                    .addComponent(new JBLabel("Describe the feature you want to add:"))
                    .addComponent(scrollPane)
                    .addComponentFillVertically(new JPanel(), 0);
            
            return JBUI.Panels.simplePanel().addToCenter(formBuilder.getPanel());
        }
        
        public String getFeatureDescription() {
            return descriptionField.getText();
        }
    }
}