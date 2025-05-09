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
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.modforge.intellij.plugin.auth.ModAuthenticationManager;
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
                Messages.showErrorDialog(
                        project,
                        "Please select the code you want to enhance.",
                        "No Code Selected"
                );
                return;
            }
            
            // Get file info
            VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
            String fileName = file != null ? file.getName() : "unknown";
            
            // Check authentication
            ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
            if (!authManager.isAuthenticated()) {
                Messages.showErrorDialog(
                        project,
                        "You must be logged in to ModForge to enhance code.",
                        "Authentication Required"
                );
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
            
            int enhancementTypeIndex = Messages.showChooseDialog(
                    project,
                    "What type of enhancement would you like to perform?",
                    "Select Enhancement Type",
                    Messages.getQuestionIcon(),
                    enhancementTypes,
                    enhancementTypes[0]
            );
            
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
                        
                        // TODO: Make actual API call to enhance code
                        // Display finished message in UI thread
                        // This would be replaced with actual API call when implemented
                    } catch (Exception ex) {
                        LOG.error("Error enhancing code", ex);
                        
                        // Show error in UI thread
                        Messages.showErrorDialog(
                                project,
                                "An error occurred while enhancing code: " + ex.getMessage(),
                                "Error Enhancing Code"
                        );
                    }
                }
            });
        } catch (Exception ex) {
            LOG.error("Error in enhance code action", ex);
            
            // Show error
            Messages.showErrorDialog(
                    project,
                    "An error occurred: " + ex.getMessage(),
                    "Error"
            );
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