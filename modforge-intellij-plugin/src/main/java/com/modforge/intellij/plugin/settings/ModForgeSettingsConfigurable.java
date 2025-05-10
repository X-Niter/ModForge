package com.modforge.intellij.plugin.settings;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * Configurable for ModForge settings.
 */
public class ModForgeSettingsConfigurable implements Configurable {
    private static final Logger LOG = Logger.getInstance(ModForgeSettingsConfigurable.class);
    
    private JPanel mainPanel;
    private JBTextField serverUrlField;
    private JBTextField usernameField;
    private JBPasswordField passwordField;
    private JBCheckBox rememberCredentialsCheckBox;
    private JBTextField accessTokenField;
    private JBCheckBox enableContinuousDevelopmentCheckBox;
    private JComboBox<Integer> continuousDevelopmentFrequencyComboBox;
    private JBCheckBox enableAIGenerationCheckBox;
    private JBCheckBox usePatternLearningCheckBox;
    private JBTextField githubUsernameField;
    private JBTextField githubTokenField;
    
    /**
     * Get display name.
     * @return Display name
     */
    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "ModForge";
    }
    
    /**
     * Get help topic.
     * @return Help topic
     */
    @Nullable
    @Override
    public String getHelpTopic() {
        return "ModForge Settings";
    }
    
    /**
     * Create component.
     * @return Component
     */
    @Nullable
    @Override
    public JComponent createComponent() {
        mainPanel = new JBPanel<>(new BorderLayout());
        
        JBTabbedPane tabbedPane = new JBTabbedPane();
        
        // Add tabs
        tabbedPane.addTab("Server", createServerPanel());
        tabbedPane.addTab("Development", createDevelopmentPanel());
        tabbedPane.addTab("GitHub", createGitHubPanel());
        
        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        
        return mainPanel;
    }
    
    /**
     * Create server panel.
     * @return Server panel
     */
    private JPanel createServerPanel() {
        JPanel panel = new JBPanel<>(new GridBagLayout());
        panel.setBorder(JBUI.Borders.empty(10));
        
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = JBUI.insets(5);
        c.weightx = 0.0;
        c.gridx = 0;
        c.gridy = 0;
        
        // Server URL
        panel.add(new JBLabel("Server URL:"), c);
        
        c.gridx = 1;
        c.weightx = 1.0;
        serverUrlField = new JBTextField();
        panel.add(serverUrlField, c);
        
        // Username
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0.0;
        panel.add(new JBLabel("Username:"), c);
        
        c.gridx = 1;
        c.weightx = 1.0;
        usernameField = new JBTextField();
        panel.add(usernameField, c);
        
        // Password
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0.0;
        panel.add(new JBLabel("Password:"), c);
        
        c.gridx = 1;
        c.weightx = 1.0;
        passwordField = new JBPasswordField();
        panel.add(passwordField, c);
        
        // Remember credentials
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0.0;
        panel.add(new JBLabel("Remember Credentials:"), c);
        
        c.gridx = 1;
        c.weightx = 1.0;
        rememberCredentialsCheckBox = new JBCheckBox();
        panel.add(rememberCredentialsCheckBox, c);
        
        // Access token
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0.0;
        panel.add(new JBLabel("Access Token:"), c);
        
        c.gridx = 1;
        c.weightx = 1.0;
        accessTokenField = new JBTextField();
        panel.add(accessTokenField, c);
        
        // Test connection button
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.CENTER;
        JButton testButton = new JButton("Test Connection");
        panel.add(testButton, c);
        
        // Filler
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 2;
        c.weighty = 1.0;
        panel.add(Box.createVerticalGlue(), c);
        
        return panel;
    }
    
    /**
     * Create development panel.
     * @return Development panel
     */
    private JPanel createDevelopmentPanel() {
        JPanel panel = new JBPanel<>(new GridBagLayout());
        panel.setBorder(JBUI.Borders.empty(10));
        
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = JBUI.insets(5);
        c.weightx = 0.0;
        c.gridx = 0;
        c.gridy = 0;
        
        // Enable continuous development
        panel.add(new JBLabel("Enable Continuous Development:"), c);
        
        c.gridx = 1;
        c.weightx = 1.0;
        enableContinuousDevelopmentCheckBox = new JBCheckBox();
        panel.add(enableContinuousDevelopmentCheckBox, c);
        
        // Continuous development frequency
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0.0;
        panel.add(new JBLabel("Continuous Development Frequency (minutes):"), c);
        
        c.gridx = 1;
        c.weightx = 1.0;
        continuousDevelopmentFrequencyComboBox = new ComboBox<>(
                new Integer[] { 1, 2, 5, 10, 15, 30, 60 });
        panel.add(continuousDevelopmentFrequencyComboBox, c);
        
        // Enable AI generation
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0.0;
        panel.add(new JBLabel("Enable AI Generation:"), c);
        
        c.gridx = 1;
        c.weightx = 1.0;
        enableAIGenerationCheckBox = new JBCheckBox();
        panel.add(enableAIGenerationCheckBox, c);
        
        // Use pattern learning
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0.0;
        panel.add(new JBLabel("Use Pattern Learning:"), c);
        
        c.gridx = 1;
        c.weightx = 1.0;
        usePatternLearningCheckBox = new JBCheckBox();
        panel.add(usePatternLearningCheckBox, c);
        
        // Filler
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 2;
        c.weighty = 1.0;
        panel.add(Box.createVerticalGlue(), c);
        
        return panel;
    }
    
    /**
     * Create GitHub panel.
     * @return GitHub panel
     */
    private JPanel createGitHubPanel() {
        JPanel panel = new JBPanel<>(new GridBagLayout());
        panel.setBorder(JBUI.Borders.empty(10));
        
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = JBUI.insets(5);
        c.weightx = 0.0;
        c.gridx = 0;
        c.gridy = 0;
        
        // GitHub username
        panel.add(new JBLabel("GitHub Username:"), c);
        
        c.gridx = 1;
        c.weightx = 1.0;
        githubUsernameField = new JBTextField();
        panel.add(githubUsernameField, c);
        
        // GitHub token
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0.0;
        panel.add(new JBLabel("GitHub Token:"), c);
        
        c.gridx = 1;
        c.weightx = 1.0;
        githubTokenField = new JBTextField();
        panel.add(githubTokenField, c);
        
        // Test GitHub connection button
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.CENTER;
        JButton testGithubButton = new JButton("Test GitHub Connection");
        panel.add(testGithubButton, c);
        
        // Filler
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 2;
        c.weighty = 1.0;
        panel.add(Box.createVerticalGlue(), c);
        
        return panel;
    }
    
    /**
     * Check if settings are modified.
     * @return Whether settings are modified
     */
    @Override
    public boolean isModified() {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        return !serverUrlField.getText().equals(settings.getServerUrl()) ||
               !usernameField.getText().equals(settings.getUsername()) ||
               !String.valueOf(passwordField.getPassword()).equals(settings.getPassword()) ||
               rememberCredentialsCheckBox.isSelected() != settings.isRememberCredentials() ||
               !accessTokenField.getText().equals(settings.getAccessToken()) ||
               enableContinuousDevelopmentCheckBox.isSelected() != settings.isEnableContinuousDevelopment() ||
               !continuousDevelopmentFrequencyComboBox.getSelectedItem().equals(settings.getContinuousDevelopmentFrequency()) ||
               enableAIGenerationCheckBox.isSelected() != settings.isEnableAIGeneration() ||
               usePatternLearningCheckBox.isSelected() != settings.isUsePatternLearning() ||
               !githubUsernameField.getText().equals(settings.getGithubUsername()) ||
               !githubTokenField.getText().equals(settings.getGithubToken());
    }
    
    /**
     * Apply settings.
     * @throws ConfigurationException If configuration is invalid
     */
    @Override
    public void apply() throws ConfigurationException {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        settings.setServerUrl(serverUrlField.getText());
        settings.setUsername(usernameField.getText());
        settings.setPassword(String.valueOf(passwordField.getPassword()));
        settings.setRememberCredentials(rememberCredentialsCheckBox.isSelected());
        settings.setAccessToken(accessTokenField.getText());
        settings.setEnableContinuousDevelopment(enableContinuousDevelopmentCheckBox.isSelected());
        settings.setContinuousDevelopmentFrequency((Integer) continuousDevelopmentFrequencyComboBox.getSelectedItem());
        settings.setEnableAIGeneration(enableAIGenerationCheckBox.isSelected());
        settings.setUsePatternLearning(usePatternLearningCheckBox.isSelected());
        settings.setGithubUsername(githubUsernameField.getText());
        settings.setGithubToken(githubTokenField.getText());
    }
    
    /**
     * Reset settings.
     */
    @Override
    public void reset() {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        serverUrlField.setText(settings.getServerUrl());
        usernameField.setText(settings.getUsername());
        passwordField.setText(settings.getPassword());
        rememberCredentialsCheckBox.setSelected(settings.isRememberCredentials());
        accessTokenField.setText(settings.getAccessToken());
        enableContinuousDevelopmentCheckBox.setSelected(settings.isEnableContinuousDevelopment());
        
        // Find matching frequency or default to first item
        boolean found = false;
        for (int i = 0; i < continuousDevelopmentFrequencyComboBox.getItemCount(); i++) {
            if (continuousDevelopmentFrequencyComboBox.getItemAt(i).equals(settings.getContinuousDevelopmentFrequency())) {
                continuousDevelopmentFrequencyComboBox.setSelectedIndex(i);
                found = true;
                break;
            }
        }
        
        if (!found && continuousDevelopmentFrequencyComboBox.getItemCount() > 0) {
            continuousDevelopmentFrequencyComboBox.setSelectedIndex(0);
        }
        
        enableAIGenerationCheckBox.setSelected(settings.isEnableAIGeneration());
        usePatternLearningCheckBox.setSelected(settings.isUsePatternLearning());
        githubUsernameField.setText(settings.getGithubUsername());
        githubTokenField.setText(settings.getGithubToken());
    }
    
    /**
     * Dispose settings.
     */
    @Override
    public void disposeUIResources() {
        mainPanel = null;
        serverUrlField = null;
        usernameField = null;
        passwordField = null;
        rememberCredentialsCheckBox = null;
        accessTokenField = null;
        enableContinuousDevelopmentCheckBox = null;
        continuousDevelopmentFrequencyComboBox = null;
        enableAIGenerationCheckBox = null;
        usePatternLearningCheckBox = null;
        githubUsernameField = null;
        githubTokenField = null;
    }
}