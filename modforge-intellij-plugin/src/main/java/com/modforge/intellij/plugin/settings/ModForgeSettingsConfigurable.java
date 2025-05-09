package com.modforge.intellij.plugin.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * Configurable for ModForge settings.
 */
public class ModForgeSettingsConfigurable implements Configurable {
    private JPanel mainPanel;
    private JPasswordField apiKeyField;
    private JBCheckBox continuousDevelopmentCheckbox;
    private JBCheckBox patternRecognitionCheckbox;
    private JSpinner updateFrequencySpinner;
    
    @Override
    public @NlsContexts.ConfigurableName String getDisplayName() {
        return "ModForge";
    }
    
    @Override
    public @Nullable JComponent createComponent() {
        // Create UI components
        apiKeyField = new JPasswordField();
        continuousDevelopmentCheckbox = new JBCheckBox("Enable continuous development");
        patternRecognitionCheckbox = new JBCheckBox("Enable pattern recognition to reduce API costs");
        
        // Create spinner with number model
        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(5, 1, 60, 1);
        updateFrequencySpinner = new JSpinner(spinnerModel);
        
        // Create panel
        mainPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("OpenAI API Key:"), apiKeyField, 1, false)
                .addComponent(continuousDevelopmentCheckbox, 1)
                .addComponent(patternRecognitionCheckbox, 1)
                .addLabeledComponent(new JBLabel("Update frequency (minutes):"), updateFrequencySpinner, 1, false)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
        
        mainPanel.setBorder(JBUI.Borders.empty(10));
        
        // Load settings
        loadSettings();
        
        return mainPanel;
    }
    
    @Override
    public boolean isModified() {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        return !settings.getOpenAiApiKey().equals(new String(apiKeyField.getPassword())) ||
               settings.isContinuousDevelopmentEnabled() != continuousDevelopmentCheckbox.isSelected() ||
               settings.isPatternRecognitionEnabled() != patternRecognitionCheckbox.isSelected() ||
               settings.getUpdateFrequencyMinutes() != (int) updateFrequencySpinner.getValue();
    }
    
    @Override
    public void apply() {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        settings.setOpenAiApiKey(new String(apiKeyField.getPassword()));
        settings.setContinuousDevelopmentEnabled(continuousDevelopmentCheckbox.isSelected());
        settings.setPatternRecognitionEnabled(patternRecognitionCheckbox.isSelected());
        settings.setUpdateFrequencyMinutes((int) updateFrequencySpinner.getValue());
    }
    
    @Override
    public void reset() {
        loadSettings();
    }
    
    /**
     * Loads settings into UI components.
     */
    private void loadSettings() {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        apiKeyField.setText(settings.getOpenAiApiKey());
        continuousDevelopmentCheckbox.setSelected(settings.isContinuousDevelopmentEnabled());
        patternRecognitionCheckbox.setSelected(settings.isPatternRecognitionEnabled());
        updateFrequencySpinner.setValue(settings.getUpdateFrequencyMinutes());
    }
}