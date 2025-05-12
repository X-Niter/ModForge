package com.modforge.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.modforge.intellij.plugin.utils.CompatibilityUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.modforge.intellij.plugin.services.ModAuthenticationManager;
import com.modforge.intellij.plugin.services.ModForgeNotificationService;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONObject;

/**
 * Action for enhancing selected code with AI assistance.
 */
public class EnhanceCodeAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(EnhanceCodeAction.class);
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        
        if (project == null || editor == null) {
            LOG.warn("Project or editor is null");
            return;
        }
        
        try {
            // Get selection model
            SelectionModel selectionModel = editor.getSelectionModel();
            
            // Get selected text
            String selectedText = selectionModel.getSelectedText();
            
            // If no text is selected, show an error message
            if (selectedText == null || selectedText.isEmpty()) {
                ModForgeNotificationService notificationService = ModForgeNotificationService.getInstance();
                if (notificationService != null) {
                    notificationService.showErrorDialog(
                            project,
                            "No Code Selected",
                            "Please select the code you want to enhance."
                    );
                } else {
                    Messages.showErrorDialog(
                            project,
                            "Please select the code you want to enhance.",
                            "No Code Selected"
                    );
                }
                return;
            }
            
            // Get file info
            VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
            String fileName = file != null ? file.getName() : "unknown";
            
            // Check authentication
            ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
            if (!authManager.isAuthenticated()) {
                ModForgeNotificationService notificationService = ModForgeNotificationService.getInstance();
                if (notificationService != null) {
                    notificationService.showErrorDialog(
                            project,
                            "Authentication Required",
                            "You must be logged in to ModForge to enhance code."
                    );
                } else {
                    Messages.showErrorDialog(
                            project,
                            "You must be logged in to ModForge to enhance code.",
                            "Authentication Required"
                    );
                }
                return;
            }
            
            // Create input data for code enhancement
            JSONObject inputData = new JSONObject();
            inputData.put("code", selectedText);
            inputData.put("fileName", fileName);
            
            // Ask for enhancement type
            String[] enhancementTypes = new String[] {
                    "Optimize Performance",
                    "Improve Readability",
                    "Add Documentation",
                    "Refactor Code",
                    "Fix Potential Bugs"
            };
            
            ModForgeNotificationService notificationService = ModForgeNotificationService.getInstance();
            int enhancementTypeIndex;
            if (notificationService != null) {
                enhancementTypeIndex = notificationService.showChooseDialog(
                        project,
                        "Select Enhancement Type",
                        "What type of enhancement would you like to perform?",
                        enhancementTypes,
                        enhancementTypes[0]
                );
            } else {
                enhancementTypeIndex = Messages.showChooseDialog(
                        project,
                        "What type of enhancement would you like to perform?",
                        "Select Enhancement Type",
                        Messages.getQuestionIcon(),
                        enhancementTypes,
                        enhancementTypes[0]
                );
            }
            
            if (enhancementTypeIndex < 0) {
                // User cancelled
                return;
            }
            
            String enhancementType = enhancementTypes[enhancementTypeIndex];
            inputData.put("enhancementType", enhancementType);
            
            // Run enhancement in background
            ProgressManager.getInstance().run(new Task.Backgroundable(project, "Enhancing Code") {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    try {
                        indicator.setText("Analyzing code...");
                        indicator.setIndeterminate(true);
                        
                        // Get server URL and token
                        ModForgeSettings settings = ModForgeSettings.getInstance();
                        String serverUrl = settings.getServerUrl();
                        String token = settings.getAccessToken();
                        
                        // Check for pattern recognition
                        boolean usePatterns = settings.isPatternRecognition();
                        inputData.put("usePatterns", usePatterns);
                        
                        // Call API to enhance code
                        indicator.setText("Enhancing code with " + enhancementType.toLowerCase() + "...");
                        
                        // Create API client
                        com.modforge.intellij.plugin.api.ApiClient apiClient = new com.modforge.intellij.plugin.api.ApiClient(serverUrl);
                        apiClient.setAuthToken(token);
                        
                        // Send request to enhance code
                        indicator.setText("Sending enhancement request...");
                        String responseJson = apiClient.post("/api/code/enhance", inputData.toJSONString());
                        
                        // Parse response
                        org.json.simple.parser.JSONParser parser = new org.json.simple.parser.JSONParser();
                        JSONObject response = (JSONObject) parser.parse(responseJson);
                        
                        // Extract enhanced code and explanation
                        final String enhancedCode = (String) response.get("code");
                        final String explanation = (String) response.get("explanation");
                        
                        // Apply changes in UI thread
                        com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(() -> {
                            try {
                                // Create dialog to show enhancements
                                com.modforge.intellij.plugin.dialogs.CodeEnhancementDialog dialog = 
                                        new com.modforge.intellij.plugin.dialogs.CodeEnhancementDialog(
                                                project, 
                                                selectedText, 
                                                enhancedCode, 
                                                explanation,
                                                enhancementType);
                                
                                // Show dialog
                                if (dialog.showAndGet()) {
                                    // User accepted changes - apply to editor
                                    com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction(project, () -> {
                                        editor.getDocument().replaceString(
                                                selectionModel.getSelectionStart(),
                                                selectionModel.getSelectionEnd(),
                                                enhancedCode
                                        );
                                    });
                                }
                            } catch (Exception ex) {
                                LOG.error("Error applying code enhancements", ex);
                                ModForgeNotificationService localNotificationService = ModForgeNotificationService.getInstance();
                                if (localNotificationService != null) {
                                    localNotificationService.showErrorDialog(
                                            project,
                                            "Error",
                                            "Error applying code enhancements: " + ex.getMessage()
                                    );
                                } else {
                                    Messages.showErrorDialog(
                                            project,
                                            "Error applying code enhancements: " + ex.getMessage(),
                                            "Error"
                                    );
                                }
                            }
                        });
                    } catch (Exception ex) {
                        LOG.error("Error enhancing code", ex);
                        
                        // Show error in UI thread
                        ModForgeNotificationService localNotificationService = ModForgeNotificationService.getInstance();
                        if (localNotificationService != null) {
                            localNotificationService.showErrorDialog(
                                    project,
                                    "Error Enhancing Code",
                                    "An error occurred while enhancing code: " + ex.getMessage()
                            );
                        } else {
                            Messages.showErrorDialog(
                                    project,
                                    "An error occurred while enhancing code: " + ex.getMessage(),
                                    "Error Enhancing Code"
                            );
                        }
                    }
                }
            });
        } catch (Exception ex) {
            LOG.error("Error in enhance code action", ex);
            
            // Show error
            ModForgeNotificationService notificationService = ModForgeNotificationService.getInstance();
            if (notificationService != null) {
                notificationService.showErrorDialog(
                        project,
                        "Error",
                        "An error occurred: " + ex.getMessage()
                );
            } else {
                Messages.showErrorDialog(
                        project,
                        "An error occurred: " + ex.getMessage(),
                        "Error"
                );
            }
        }
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        // Only enable if authenticated and editor is available with selection
        ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        
        boolean enabled = authManager.isAuthenticated() && 
                          editor != null && 
                          editor.getSelectionModel().hasSelection();
        
        e.getPresentation().setEnabled(enabled);
    }
}