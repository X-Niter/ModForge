package com.modforge.intellij.plugin.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.UI;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * Settings panel for the ModForge plugin.
 */
public final class ModForgeSettingsConfigurable implements Configurable {
    // UI components
    private JPanel mainPanel;
    private JBPasswordField apiKeyField;
    private ComboBox<String> modelComboBox;
    private JSpinner maxTokensSpinner;
    private JSpinner temperatureSpinner;
    private JBCheckBox continuousDevelopmentCheckbox;
    private JBCheckBox patternRecognitionCheckbox;
    private JSpinner checkIntervalSpinner;
    private JBCheckBox autoCompileCheckbox;
    private JBCheckBox autoFixCheckbox;
    private JBCheckBox autoDocumentCheckbox;
    private JBCheckBox forgeSupportCheckbox;
    private JBCheckBox fabricSupportCheckbox;
    private JBCheckBox quiltSupportCheckbox;
    
    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "ModForge";
    }
    
    @Override
    public @Nullable JComponent createComponent() {
        // Initialize UI components
        createUIComponents();
        
        // Create form
        FormBuilder formBuilder = FormBuilder.createFormBuilder();
        
        // OpenAI settings section
        formBuilder.addComponent(new JBLabel("OpenAI Settings"));
        formBuilder.addLabeledComponent("API Key:", apiKeyField);
        formBuilder.addLabeledComponent("Model:", modelComboBox);
        formBuilder.addLabeledComponent("Max Tokens:", maxTokensSpinner);
        formBuilder.addLabeledComponent("Temperature:", temperatureSpinner);
        formBuilder.addSeparator();
        
        // Feature toggles section
        formBuilder.addComponent(new JBLabel("Feature Toggles"));
        formBuilder.addComponent(continuousDevelopmentCheckbox);
        formBuilder.addComponent(patternRecognitionCheckbox);
        formBuilder.addSeparator();
        
        // Development settings section
        formBuilder.addComponent(new JBLabel("Development Settings"));
        formBuilder.addLabeledComponent("Check Interval (ms):", checkIntervalSpinner);
        formBuilder.addComponent(autoCompileCheckbox);
        formBuilder.addComponent(autoFixCheckbox);
        formBuilder.addComponent(autoDocumentCheckbox);
        formBuilder.addSeparator();
        
        // Mod loader settings section
        formBuilder.addComponent(new JBLabel("Mod Loader Support"));
        formBuilder.addComponent(forgeSupportCheckbox);
        formBuilder.addComponent(fabricSupportCheckbox);
        formBuilder.addComponent(quiltSupportCheckbox);
        
        // Add some spacing
        mainPanel = formBuilder.addComponentFillVertically(new JPanel(), 0).getPanel();
        
        return mainPanel;
    }
    
    /**
     * Creates the UI components.
     */
    private void createUIComponents() {
        // OpenAI settings
        apiKeyField = new JBPasswordField();
        
        modelComboBox = new ComboBox<>(new String[] {
                "gpt-4",
                "gpt-4-turbo",
                "gpt-3.5-turbo",
                "gpt-3.5-turbo-16k"
        });
        
        maxTokensSpinner = new JSpinner(new SpinnerNumberModel(2048, 16, 32768, 16));
        
        temperatureSpinner = new JSpinner(new SpinnerNumberModel(0.7, 0.0, 2.0, 0.1));
        
        // Feature toggles
        continuousDevelopmentCheckbox = new JBCheckBox("Enable Continuous Development", false);
        patternRecognitionCheckbox = new JBCheckBox("Enable Pattern Recognition", true);
        
        // Development settings
        checkIntervalSpinner = new JSpinner(new SpinnerNumberModel(60000, 1000, 3600000, 1000));
        
        autoCompileCheckbox = new JBCheckBox("Auto-Compile", true);
        autoFixCheckbox = new JBCheckBox("Auto-Fix Errors", true);
        autoDocumentCheckbox = new JBCheckBox("Auto-Document Code", false);
        
        // Mod loader settings
        forgeSupportCheckbox = new JBCheckBox("Forge", true);
        fabricSupportCheckbox = new JBCheckBox("Fabric", true);
        quiltSupportCheckbox = new JBCheckBox("Quilt", true);
    }
    
    @Override
    public boolean isModified() {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        // OpenAI settings
        if (!settings.getOpenAiApiKey().equals(new String(apiKeyField.getPassword())) ||
                !settings.getOpenAiModel().equals(modelComboBox.getItem()) ||
                settings.getMaxTokens() != (int) maxTokensSpinner.getValue() ||
                Math.abs(settings.getTemperature() - (double) temperatureSpinner.getValue()) > 0.001) {
            return true;
        }
        
        // Feature toggles
        if (settings.isContinuousDevelopmentEnabled() != continuousDevelopmentCheckbox.isSelected() ||
                settings.isPatternRecognitionEnabled() != patternRecognitionCheckbox.isSelected()) {
            return true;
        }
        
        // Development settings
        if (settings.getCheckIntervalMs() != (long) (int) checkIntervalSpinner.getValue() ||
                settings.isAutoCompileEnabled() != autoCompileCheckbox.isSelected() ||
                settings.isAutoFixEnabled() != autoFixCheckbox.isSelected() ||
                settings.isAutoDocumentEnabled() != autoDocumentCheckbox.isSelected()) {
            return true;
        }
        
        // Mod loader settings
        if (settings.isForgeSupported() != forgeSupportCheckbox.isSelected() ||
                settings.isFabricSupported() != fabricSupportCheckbox.isSelected() ||
                settings.isQuiltSupported() != quiltSupportCheckbox.isSelected()) {
            return true;
        }
        
        return false;
    }
    
    @Override
    public void apply() throws ConfigurationException {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        // OpenAI settings
        settings.setOpenAiApiKey(new String(apiKeyField.getPassword()));
        settings.setOpenAiModel(modelComboBox.getItem());
        settings.setMaxTokens((int) maxTokensSpinner.getValue());
        settings.setTemperature((double) temperatureSpinner.getValue());
        
        // Feature toggles
        settings.setContinuousDevelopmentEnabled(continuousDevelopmentCheckbox.isSelected());
        settings.setPatternRecognitionEnabled(patternRecognitionCheckbox.isSelected());
        
        // Development settings
        settings.setCheckIntervalMs((long) (int) checkIntervalSpinner.getValue());
        settings.setAutoCompileEnabled(autoCompileCheckbox.isSelected());
        settings.setAutoFixEnabled(autoFixCheckbox.isSelected());
        settings.setAutoDocumentEnabled(autoDocumentCheckbox.isSelected());
        
        // Mod loader settings
        settings.setForgeSupported(forgeSupportCheckbox.isSelected());
        settings.setFabricSupported(fabricSupportCheckbox.isSelected());
        settings.setQuiltSupported(quiltSupportCheckbox.isSelected());
    }
    
    @Override
    public void reset() {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        // OpenAI settings
        apiKeyField.setText(settings.getOpenAiApiKey());
        modelComboBox.setItem(settings.getOpenAiModel());
        maxTokensSpinner.setValue(settings.getMaxTokens());
        temperatureSpinner.setValue(settings.getTemperature());
        
        // Feature toggles
        continuousDevelopmentCheckbox.setSelected(settings.isContinuousDevelopmentEnabled());
        patternRecognitionCheckbox.setSelected(settings.isPatternRecognitionEnabled());
        
        // Development settings
        checkIntervalSpinner.setValue((int) settings.getCheckIntervalMs());
        autoCompileCheckbox.setSelected(settings.isAutoCompileEnabled());
        autoFixCheckbox.setSelected(settings.isAutoFixEnabled());
        autoDocumentCheckbox.setSelected(settings.isAutoDocumentEnabled());
        
        // Mod loader settings
        forgeSupportCheckbox.setSelected(settings.isForgeSupported());
        fabricSupportCheckbox.setSelected(settings.isFabricSupported());
        quiltSupportCheckbox.setSelected(settings.isQuiltSupported());
    }
    
    @Override
    public void disposeUIResources() {
        mainPanel = null;
        apiKeyField = null;
        modelComboBox = null;
        maxTokensSpinner = null;
        temperatureSpinner = null;
        continuousDevelopmentCheckbox = null;
        patternRecognitionCheckbox = null;
        checkIntervalSpinner = null;
        autoCompileCheckbox = null;
        autoFixCheckbox = null;
        autoDocumentCheckbox = null;
        forgeSupportCheckbox = null;
        fabricSupportCheckbox = null;
        quiltSupportCheckbox = null;
    }
}