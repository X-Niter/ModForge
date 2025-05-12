package com.modforge.intellij.plugin.ui;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.components.panels.VerticalLayout;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UI;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.Objects;

/**
 * Configuration UI for ModForge settings.
 * Compatible with IntelliJ IDEA 2025.1.1.1
 */
public class ModForgeConfigurable implements Configurable {
    private final Project project;
    private final ModForgeSettings settings;
    
    private JPanel mainPanel;
    private JBTextField serverUrlField;
    private JBTextField githubUsernameField;
    private JBPasswordField accessTokenField;
    private JBCheckBox useGitHubAuthenticationCheckBox;
    private JBCheckBox patternRecognitionCheckBox;
    private JBCheckBox continuousDevelopmentCheckBox;
    private JBCheckBox analyticsEnabledCheckBox;
    private JBCheckBox autoCommitCheckBox;
    private JBCheckBox autoSaveCheckBox;
    private JBCheckBox autoSyncCheckBox;
    private ComboBox<String> defaultLanguageComboBox;
    private ComboBox<String> defaultModLoaderComboBox;
    private ComboBox<Integer> minecraftVersionComboBox;
    private JSpinner maxThreadsSpinner;
    private JSpinner requestTimeoutSpinner;
    private TextFieldWithBrowseButton gitExecutablePathField;

    /**
     * Creates a new instance of the ModForge configurable.
     *
     * @param project The project.
     */
    public ModForgeConfigurable(Project project) {
        this.project = project;
        this.settings = ModForgeSettings.getInstance();
    }

    /**
     * Gets the display name of the configurable.
     *
     * @return The display name.
     */
    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "ModForge";
    }

    /**
     * Gets the help topic.
     *
     * @return The help topic.
     */
    @Nullable
    @Override
    public String getHelpTopic() {
        return "ModForge.Settings";
    }

    /**
     * Creates the UI component.
     *
     * @return The UI component.
     */
    @Nullable
    @Override
    public JComponent createComponent() {
        mainPanel = new JPanel(new VerticalLayout(JBUI.scale(10)));
        
        // Server settings panel
        JPanel serverPanel = createServerPanel();
        
        // Authentication settings panel
        JPanel authPanel = createAuthPanel();
        
        // Features settings panel
        JPanel featuresPanel = createFeaturesPanel();
        
        // Minecraft settings panel
        JPanel minecraftPanel = createMinecraftPanel();
        
        // Performance settings panel
        JPanel performancePanel = createPerformancePanel();
        
        // Add panels to main panel
        mainPanel.add(serverPanel);
        mainPanel.add(authPanel);
        mainPanel.add(featuresPanel);
        mainPanel.add(minecraftPanel);
        mainPanel.add(performancePanel);
        
        // Load settings
        loadSettings();
        
        return mainPanel;
    }

    /**
     * Creates the server settings panel.
     *
     * @return The server settings panel.
     */
    private JPanel createServerPanel() {
        serverUrlField = new JBTextField();
        
        return UI.PanelFactory.panel(new JPanel(new BorderLayout()))
                .withLabel("Server Settings")
                .withComment("Configuration for the ModForge server")
                .withBorder(JBUI.Borders.empty(5))
                .andJBUI()
                .createPanel();
    }

    /**
     * Creates the authentication settings panel.
     *
     * @return The authentication settings panel.
     */
    private JPanel createAuthPanel() {
        githubUsernameField = new JBTextField();
        accessTokenField = new JBPasswordField();
        useGitHubAuthenticationCheckBox = new JBCheckBox("Use GitHub Authentication");
        
        FormBuilder builder = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("GitHub Username:"), githubUsernameField, 1, false)
                .addLabeledComponent(new JBLabel("Access Token:"), accessTokenField, 1, false)
                .addComponentToRightColumn(useGitHubAuthenticationCheckBox, 1);
        
        JPanel panel = builder.getPanel();
        panel.setBorder(JBUI.Borders.empty(5));
        
        JPanel authPanel = UI.PanelFactory.panel(panel)
                .withLabel("Authentication")
                .withComment("GitHub authentication settings")
                .andJBUI()
                .createPanel();
        
        return authPanel;
    }

    /**
     * Creates the features settings panel.
     *
     * @return The features settings panel.
     */
    private JPanel createFeaturesPanel() {
        patternRecognitionCheckBox = new JBCheckBox("Enable Pattern Recognition");
        continuousDevelopmentCheckBox = new JBCheckBox("Enable Continuous Development");
        analyticsEnabledCheckBox = new JBCheckBox("Enable Analytics");
        autoCommitCheckBox = new JBCheckBox("Auto Commit");
        autoSaveCheckBox = new JBCheckBox("Auto Save");
        autoSyncCheckBox = new JBCheckBox("Auto Sync");
        
        JPanel checkboxPanel = new JPanel(new GridLayout(0, 2));
        checkboxPanel.add(patternRecognitionCheckBox);
        checkboxPanel.add(continuousDevelopmentCheckBox);
        checkboxPanel.add(analyticsEnabledCheckBox);
        checkboxPanel.add(autoCommitCheckBox);
        checkboxPanel.add(autoSaveCheckBox);
        checkboxPanel.add(autoSyncCheckBox);
        
        JPanel featuresPanel = UI.PanelFactory.panel(checkboxPanel)
                .withLabel("Features")
                .withComment("ModForge feature settings")
                .andJBUI()
                .createPanel();
        
        return featuresPanel;
    }

    /**
     * Creates the Minecraft settings panel.
     *
     * @return The Minecraft settings panel.
     */
    private JPanel createMinecraftPanel() {
        defaultLanguageComboBox = new ComboBox<>(new String[] { "java", "kotlin", "python", "json" });
        defaultModLoaderComboBox = new ComboBox<>(new String[] { "forge", "fabric", "quilt", "architectury" });
        minecraftVersionComboBox = new ComboBox<>(new Integer[] { 118, 119, 120, 121 });
        
        FormBuilder builder = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("Default Language:"), defaultLanguageComboBox, 1, false)
                .addLabeledComponent(new JBLabel("Default Mod Loader:"), defaultModLoaderComboBox, 1, false)
                .addLabeledComponent(new JBLabel("Minecraft Version:"), minecraftVersionComboBox, 1, false);
        
        JPanel panel = builder.getPanel();
        panel.setBorder(JBUI.Borders.empty(5));
        
        JPanel minecraftPanel = UI.PanelFactory.panel(panel)
                .withLabel("Minecraft")
                .withComment("Minecraft development settings")
                .andJBUI()
                .createPanel();
        
        return minecraftPanel;
    }

    /**
     * Creates the performance settings panel.
     *
     * @return The performance settings panel.
     */
    private JPanel createPerformancePanel() {
        maxThreadsSpinner = new JSpinner(new SpinnerNumberModel(4, 1, 32, 1));
        requestTimeoutSpinner = new JSpinner(new SpinnerNumberModel(30, 1, 300, 1));
        gitExecutablePathField = new TextFieldWithBrowseButton();
        
        FormBuilder builder = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("Max Threads:"), maxThreadsSpinner, 1, false)
                .addLabeledComponent(new JBLabel("Request Timeout (seconds):"), requestTimeoutSpinner, 1, false)
                .addLabeledComponent(new JBLabel("Git Executable Path:"), gitExecutablePathField, 1, false);
        
        JPanel panel = builder.getPanel();
        panel.setBorder(JBUI.Borders.empty(5));
        
        JPanel performancePanel = UI.PanelFactory.panel(panel)
                .withLabel("Performance")
                .withComment("Performance settings")
                .andJBUI()
                .createPanel();
        
        return performancePanel;
    }

    /**
     * Loads the settings into the UI.
     */
    private void loadSettings() {
        serverUrlField.setText(settings.getServerUrl());
        githubUsernameField.setText(settings.getGitHubUsername());
        
        String accessToken = settings.getAccessToken();
        if (accessToken != null) {
            accessTokenField.setText(accessToken);
        }
        
        useGitHubAuthenticationCheckBox.setSelected(settings.isUseGitHubAuthentication());
        patternRecognitionCheckBox.setSelected(settings.isPatternRecognition());
        continuousDevelopmentCheckBox.setSelected(settings.isContinuousDevelopment());
        analyticsEnabledCheckBox.setSelected(settings.isAnalyticsEnabled());
        autoCommitCheckBox.setSelected(settings.isAutoCommit());
        autoSaveCheckBox.setSelected(settings.isAutoSave());
        autoSyncCheckBox.setSelected(settings.isAutoSync());
        
        defaultLanguageComboBox.setSelectedItem(settings.getDefaultLanguage());
        defaultModLoaderComboBox.setSelectedItem(settings.getDefaultModLoader());
        minecraftVersionComboBox.setSelectedItem(settings.getMinecraftVersion());
        
        maxThreadsSpinner.setValue(settings.getMaxThreads());
        requestTimeoutSpinner.setValue(settings.getRequestTimeout());
        gitExecutablePathField.setText(settings.getGitExecutablePath());
    }

    /**
     * Checks if the settings have been modified.
     *
     * @return Whether the settings have been modified.
     */
    @Override
    public boolean isModified() {
        boolean modified = false;
        
        modified |= !Objects.equals(serverUrlField.getText(), settings.getServerUrl());
        modified |= !Objects.equals(githubUsernameField.getText(), settings.getGitHubUsername());
        
        // Compare password field with access token
        char[] passwordChars = accessTokenField.getPassword();
        String accessToken = settings.getAccessToken();
        modified |= accessToken == null && passwordChars.length > 0;
        modified |= accessToken != null && !Arrays.equals(passwordChars, accessToken.toCharArray());
        
        modified |= useGitHubAuthenticationCheckBox.isSelected() != settings.isUseGitHubAuthentication();
        modified |= patternRecognitionCheckBox.isSelected() != settings.isPatternRecognition();
        modified |= continuousDevelopmentCheckBox.isSelected() != settings.isContinuousDevelopment();
        modified |= analyticsEnabledCheckBox.isSelected() != settings.isAnalyticsEnabled();
        modified |= autoCommitCheckBox.isSelected() != settings.isAutoCommit();
        modified |= autoSaveCheckBox.isSelected() != settings.isAutoSave();
        modified |= autoSyncCheckBox.isSelected() != settings.isAutoSync();
        
        modified |= !Objects.equals(defaultLanguageComboBox.getSelectedItem(), settings.getDefaultLanguage());
        modified |= !Objects.equals(defaultModLoaderComboBox.getSelectedItem(), settings.getDefaultModLoader());
        modified |= !Objects.equals(minecraftVersionComboBox.getSelectedItem(), settings.getMinecraftVersion());
        
        modified |= (Integer) maxThreadsSpinner.getValue() != settings.getMaxThreads();
        modified |= (Integer) requestTimeoutSpinner.getValue() != settings.getRequestTimeout();
        modified |= !Objects.equals(gitExecutablePathField.getText(), settings.getGitExecutablePath());
        
        return modified;
    }

    /**
     * Applies the settings from the UI.
     *
     * @throws ConfigurationException If the settings are invalid.
     */
    @Override
    public void apply() throws ConfigurationException {
        settings.setServerUrl(serverUrlField.getText());
        settings.setGitHubUsername(githubUsernameField.getText());
        
        char[] passwordChars = accessTokenField.getPassword();
        if (passwordChars.length > 0) {
            settings.setAccessToken(new String(passwordChars));
        }
        
        settings.setUseGitHubAuthentication(useGitHubAuthenticationCheckBox.isSelected());
        settings.setPatternRecognition(patternRecognitionCheckBox.isSelected());
        settings.setContinuousDevelopment(continuousDevelopmentCheckBox.isSelected());
        settings.setAnalyticsEnabled(analyticsEnabledCheckBox.isSelected());
        settings.setAutoCommit(autoCommitCheckBox.isSelected());
        settings.setAutoSave(autoSaveCheckBox.isSelected());
        settings.setAutoSync(autoSyncCheckBox.isSelected());
        
        settings.setDefaultLanguage((String) defaultLanguageComboBox.getSelectedItem());
        settings.setDefaultModLoader((String) defaultModLoaderComboBox.getSelectedItem());
        settings.setMinecraftVersion((Integer) minecraftVersionComboBox.getSelectedItem());
        
        settings.setMaxThreads((Integer) maxThreadsSpinner.getValue());
        settings.setRequestTimeout((Integer) requestTimeoutSpinner.getValue());
        settings.setGitExecutablePath(gitExecutablePathField.getText());
    }

    /**
     * Resets the settings to their default values.
     */
    @Override
    public void reset() {
        loadSettings();
    }

    /**
     * Disposes the UI resources.
     */
    @Override
    public void disposeUIResources() {
        mainPanel = null;
        serverUrlField = null;
        githubUsernameField = null;
        accessTokenField = null;
        useGitHubAuthenticationCheckBox = null;
        patternRecognitionCheckBox = null;
        continuousDevelopmentCheckBox = null;
        analyticsEnabledCheckBox = null;
        autoCommitCheckBox = null;
        autoSaveCheckBox = null;
        autoSyncCheckBox = null;
        defaultLanguageComboBox = null;
        defaultModLoaderComboBox = null;
        minecraftVersionComboBox = null;
        maxThreadsSpinner = null;
        requestTimeoutSpinner = null;
        gitExecutablePathField = null;
    }
}