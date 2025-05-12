package com.modforge.intellij.plugin.ui;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UI;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * Configurable for ModForge plugin settings.
 * Compatible with IntelliJ IDEA 2025.1.1.1
 */
public class ModForgeConfigurable implements Configurable {
    private JPanel mainPanel;
    private JBTextField usernameField;
    private JBTextField serverUrlField;
    private JBTextField githubUsernameField;
    private JBTextField accessTokenField;
    private JBCheckBox enableContinuousDevelopmentCheckBox;
    private JBCheckBox usePatternLearningCheckBox;
    private JBCheckBox preserveExistingCodeCheckBox;
    private JSlider memoryThresholdSlider;
    
    private final ModForgeSettings settings;
    
    public ModForgeConfigurable() {
        this.settings = ModForgeSettings.getInstance();
    }

    @Override
    public @NlsContexts.ConfigurableName String getDisplayName() {
        return "ModForge AI";
    }

    @Override
    public @Nullable JComponent createComponent() {
        // Initialize UI components
        usernameField = new JBTextField();
        serverUrlField = new JBTextField();
        githubUsernameField = new JBTextField();
        accessTokenField = new JBTextField();
        enableContinuousDevelopmentCheckBox = new JBCheckBox("Enable continuous development");
        usePatternLearningCheckBox = new JBCheckBox("Use pattern learning to reduce API costs");
        preserveExistingCodeCheckBox = new JBCheckBox("Preserve existing code where possible");
        
        memoryThresholdSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 75);
        memoryThresholdSlider.setMajorTickSpacing(25);
        memoryThresholdSlider.setMinorTickSpacing(5);
        memoryThresholdSlider.setPaintTicks(true);
        memoryThresholdSlider.setPaintLabels(true);
        
        // Create section panels
        JPanel userSection = createUserSection();
        JPanel aiSection = createAISection();
        JPanel githubSection = createGitHubSection();
        JPanel advancedSection = createAdvancedSection();
        
        // Create main panel
        mainPanel = FormBuilder.createFormBuilder()
                .addComponent(userSection, 1)
                .addSeparator(10)
                .addComponent(aiSection, 1)
                .addSeparator(10)
                .addComponent(githubSection, 1)
                .addSeparator(10)
                .addComponent(advancedSection, 1)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
        
        mainPanel.setBorder(JBUI.Borders.empty(10));
        
        // Load settings
        loadSettings();
        
        return mainPanel;
    }

    @NotNull
    private JPanel createUserSection() {
        return UI.PanelFactory.panel(new JPanel(new GridLayout(2, 2, 10, 10)))
                .withLabel("User Settings")
                .withComment("Configure your ModForge account details.")
                .addLabeled("Username:", usernameField)
                .addLabeled("Server URL:", serverUrlField)
                .createPanel();
    }

    @NotNull
    private JPanel createAISection() {
        JPanel panel = new JPanel(new GridLayout(3, 1, 10, 10));
        panel.add(usePatternLearningCheckBox);
        panel.add(preserveExistingCodeCheckBox);
        panel.add(enableContinuousDevelopmentCheckBox);
        
        return UI.PanelFactory.panel(panel)
                .withLabel("AI Settings")
                .withComment("Configure AI behavior and optimization settings.")
                .createPanel();
    }

    @NotNull
    private JPanel createGitHubSection() {
        return UI.PanelFactory.panel(new JPanel(new GridLayout(2, 2, 10, 10)))
                .withLabel("GitHub Integration")
                .withComment("Configure GitHub integration for version control and collaboration.")
                .addLabeled("GitHub Username:", githubUsernameField)
                .addLabeled("Access Token:", accessTokenField)
                .createPanel();
    }

    @NotNull
    private JPanel createAdvancedSection() {
        JPanel sliderPanel = new JPanel(new BorderLayout());
        sliderPanel.add(new JBLabel("Memory Threshold:"), BorderLayout.WEST);
        sliderPanel.add(memoryThresholdSlider, BorderLayout.CENTER);
        sliderPanel.add(new JBLabel("%"), BorderLayout.EAST);
        
        return UI.PanelFactory.panel(sliderPanel)
                .withLabel("Advanced Settings")
                .withComment("Configure advanced settings for ModForge.")
                .createPanel();
    }

    @Override
    public boolean isModified() {
        return !usernameField.getText().equals(settings.getUsername()) ||
               !serverUrlField.getText().equals(settings.getServerUrl()) ||
               !githubUsernameField.getText().equals(settings.getGitHubUsername()) ||
               !accessTokenField.getText().equals(settings.getAccessToken()) ||
               enableContinuousDevelopmentCheckBox.isSelected() != settings.isEnableContinuousDevelopment() ||
               usePatternLearningCheckBox.isSelected() != settings.isUsePatternLearning() ||
               preserveExistingCodeCheckBox.isSelected() != settings.isPreserveExistingCode() ||
               memoryThresholdSlider.getValue() != settings.getMemoryThreshold();
    }

    @Override
    public void apply() throws ConfigurationException {
        settings.setUsername(usernameField.getText());
        settings.setServerUrl(serverUrlField.getText());
        settings.setGitHubUsername(githubUsernameField.getText());
        settings.setAccessToken(accessTokenField.getText());
        settings.setEnableContinuousDevelopment(enableContinuousDevelopmentCheckBox.isSelected());
        settings.setUsePatternLearning(usePatternLearningCheckBox.isSelected());
        settings.setPreserveExistingCode(preserveExistingCodeCheckBox.isSelected());
        settings.setMemoryThreshold(memoryThresholdSlider.getValue());
    }

    @Override
    public void reset() {
        loadSettings();
    }

    private void loadSettings() {
        usernameField.setText(settings.getUsername());
        serverUrlField.setText(settings.getServerUrl());
        githubUsernameField.setText(settings.getGitHubUsername());
        accessTokenField.setText(settings.getAccessToken());
        enableContinuousDevelopmentCheckBox.setSelected(settings.isEnableContinuousDevelopment());
        usePatternLearningCheckBox.setSelected(settings.isUsePatternLearning());
        preserveExistingCodeCheckBox.setSelected(settings.isPreserveExistingCode());
        memoryThresholdSlider.setValue(settings.getMemoryThreshold());
    }
}