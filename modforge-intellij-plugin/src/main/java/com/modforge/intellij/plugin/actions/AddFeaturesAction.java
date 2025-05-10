package com.modforge.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.modforge.intellij.plugin.auth.ModAuthenticationManager;
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
                Messages.showErrorDialog(
                        project,
                        "You must be logged in to ModForge to add features.",
                        "Authentication Required"
                );
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
                Messages.showErrorDialog(
                        project,
                        "Feature description cannot be empty.",
                        "Empty Description"
                );
                return;
            }
            
            // Show confirmation dialog
            int confirmation = Messages.showYesNoDialog(
                    project,
                    "Add the following feature to your mod?\n\n" + featureDescription,
                    "Confirm Feature Addition",
                    Messages.getQuestionIcon()
            );
            
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
            
            // TODO: Implement feature addition using API
            // This would be replaced with actual API call when implemented
            
            // Show success message
            Messages.showInfoMessage(
                    project,
                    "Feature addition requested. This may take some time.",
                    "Feature Addition"
            );
        } catch (Exception ex) {
            LOG.error("Error in add features action", ex);
            
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