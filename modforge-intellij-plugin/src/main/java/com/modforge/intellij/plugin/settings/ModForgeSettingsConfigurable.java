package com.modforge.intellij.plugin.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * Configurable for ModForge settings.
 */
public class ModForgeSettingsConfigurable implements Configurable {
    private JBTextField serverUrlField;
    private JBCheckBox rememberCredentialsCheckBox;
    private JBCheckBox enableContinuousDevelopmentCheckBox;
    private JSpinner continuousDevelopmentFrequencySpinner;
    private JBCheckBox enableAIGenerationCheckBox;
    private JBCheckBox usePatternLearningCheckBox;
    private JBTextField githubTokenField;
    private JBTextField githubUsernameField;
    
    @Override
    public @NlsContexts.ConfigurableName String getDisplayName() {
        return "ModForge";
    }

    @Override
    public @Nullable JComponent createComponent() {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        // Create UI components
        serverUrlField = new JBTextField(settings.getServerUrl());
        rememberCredentialsCheckBox = new JBCheckBox("Remember credentials", settings.isRememberCredentials());
        enableContinuousDevelopmentCheckBox = new JBCheckBox("Enable continuous development", settings.isEnableContinuousDevelopment());
        
        // Frequency spinner
        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(
                settings.getContinuousDevelopmentFrequency(),
                1, 60, 1);
        continuousDevelopmentFrequencySpinner = new JSpinner(spinnerModel);
        
        // AI generation settings
        enableAIGenerationCheckBox = new JBCheckBox("Enable AI code generation", settings.isEnableAIGeneration());
        usePatternLearningCheckBox = new JBCheckBox("Use pattern learning to reduce API costs", settings.isUsePatternLearning());
        
        // GitHub integration
        githubTokenField = new JBTextField(settings.getGithubToken());
        githubUsernameField = new JBTextField(settings.getGithubUsername());
        
        // Create UI layout
        JPanel frequencyPanel = new JPanel(new BorderLayout());
        frequencyPanel.add(new JBLabel("Check frequency (minutes): "), BorderLayout.WEST);
        frequencyPanel.add(continuousDevelopmentFrequencySpinner, BorderLayout.CENTER);
        
        // Build form
        return FormBuilder.createFormBuilder()
                .addLabeledComponent("Server URL:", serverUrlField)
                .addComponent(rememberCredentialsCheckBox)
                .addSeparator("Continuous Development")
                .addComponent(enableContinuousDevelopmentCheckBox)
                .addComponent(frequencyPanel)
                .addSeparator("AI Generation")
                .addComponent(enableAIGenerationCheckBox)
                .addComponent(usePatternLearningCheckBox)
                .addSeparator("GitHub Integration")
                .addLabeledComponent("GitHub Token:", githubTokenField)
                .addLabeledComponent("GitHub Username:", githubUsernameField)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
    }

    @Override
    public boolean isModified() {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        return !serverUrlField.getText().equals(settings.getServerUrl()) ||
                rememberCredentialsCheckBox.isSelected() != settings.isRememberCredentials() ||
                enableContinuousDevelopmentCheckBox.isSelected() != settings.isEnableContinuousDevelopment() ||
                (Integer) continuousDevelopmentFrequencySpinner.getValue() != settings.getContinuousDevelopmentFrequency() ||
                enableAIGenerationCheckBox.isSelected() != settings.isEnableAIGeneration() ||
                usePatternLearningCheckBox.isSelected() != settings.isUsePatternLearning() ||
                !githubTokenField.getText().equals(settings.getGithubToken()) ||
                !githubUsernameField.getText().equals(settings.getGithubUsername());
    }

    @Override
    public void apply() {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        settings.setServerUrl(serverUrlField.getText().trim());
        settings.setRememberCredentials(rememberCredentialsCheckBox.isSelected());
        settings.setEnableContinuousDevelopment(enableContinuousDevelopmentCheckBox.isSelected());
        settings.setContinuousDevelopmentFrequency((Integer) continuousDevelopmentFrequencySpinner.getValue());
        settings.setEnableAIGeneration(enableAIGenerationCheckBox.isSelected());
        settings.setUsePatternLearning(usePatternLearningCheckBox.isSelected());
        settings.setGithubToken(githubTokenField.getText().trim());
        settings.setGithubUsername(githubUsernameField.getText().trim());
    }

    @Override
    public void reset() {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        serverUrlField.setText(settings.getServerUrl());
        rememberCredentialsCheckBox.setSelected(settings.isRememberCredentials());
        enableContinuousDevelopmentCheckBox.setSelected(settings.isEnableContinuousDevelopment());
        continuousDevelopmentFrequencySpinner.setValue(settings.getContinuousDevelopmentFrequency());
        enableAIGenerationCheckBox.setSelected(settings.isEnableAIGeneration());
        usePatternLearningCheckBox.setSelected(settings.isUsePatternLearning());
        githubTokenField.setText(settings.getGithubToken());
        githubUsernameField.setText(settings.getGithubUsername());
    }
}