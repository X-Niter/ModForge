package com.modforge.intellij.plugin.settings;

import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * Component for ModForge settings UI.
 */
public class ModForgeSettingsComponent {
    private final JPanel myMainPanel;
    private final JBTextField serverUrlField = new JBTextField();
    private final JBCheckBox continuousDevelopmentCheckBox = new JBCheckBox("Enable continuous development");
    private final JBCheckBox patternRecognitionCheckBox = new JBCheckBox("Enable pattern recognition");
    
    public ModForgeSettingsComponent() {
        // Set default values
        ModForgeSettings settings = ModForgeSettings.getInstance();
        serverUrlField.setText(settings.getServerUrl());
        continuousDevelopmentCheckBox.setSelected(settings.isContinuousDevelopment());
        patternRecognitionCheckBox.setSelected(settings.isPatternRecognition());
        
        // Create main panel
        myMainPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("Server URL:"), serverUrlField, 1, false)
                .addComponent(new JBLabel("Features:"), 1)
                .addComponent(continuousDevelopmentCheckBox, 1)
                .addComponent(patternRecognitionCheckBox, 1)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
        
        // Add descriptions
        JPanel descriptionPanel = new JPanel(new GridLayout(3, 1));
        
        // Add server URL description
        JLabel serverUrlDescription = new JLabel("The ModForge server URL (default: http://localhost:5000)");
        serverUrlDescription.setFont(serverUrlDescription.getFont().deriveFont(Font.ITALIC));
        serverUrlDescription.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        descriptionPanel.add(serverUrlDescription);
        
        // Add continuous development description
        JLabel continuousDevelopmentDescription = new JLabel("Automatically compile and fix code errors");
        continuousDevelopmentDescription.setFont(continuousDevelopmentDescription.getFont().deriveFont(Font.ITALIC));
        continuousDevelopmentDescription.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        descriptionPanel.add(continuousDevelopmentDescription);
        
        // Add pattern recognition description
        JLabel patternRecognitionDescription = new JLabel("Use pattern recognition to optimize AI requests");
        patternRecognitionDescription.setFont(patternRecognitionDescription.getFont().deriveFont(Font.ITALIC));
        descriptionPanel.add(patternRecognitionDescription);
        
        myMainPanel.add(descriptionPanel, BorderLayout.SOUTH);
    }
    
    public JPanel getPanel() {
        return myMainPanel;
    }
    
    public JComponent getPreferredFocusedComponent() {
        return serverUrlField;
    }
    
    @NotNull
    public String getServerUrl() {
        return serverUrlField.getText().trim();
    }
    
    public boolean getContinuousDevelopment() {
        return continuousDevelopmentCheckBox.isSelected();
    }
    
    public boolean getPatternRecognition() {
        return patternRecognitionCheckBox.isSelected();
    }
    
    public void setServerUrl(@NotNull String serverUrl) {
        serverUrlField.setText(serverUrl);
    }
    
    public void setContinuousDevelopment(boolean selected) {
        continuousDevelopmentCheckBox.setSelected(selected);
    }
    
    public void setPatternRecognition(boolean selected) {
        patternRecognitionCheckBox.setSelected(selected);
    }
}