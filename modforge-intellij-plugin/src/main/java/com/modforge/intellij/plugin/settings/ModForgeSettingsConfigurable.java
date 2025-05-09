package com.modforge.intellij.plugin.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * Configurable for ModForge settings in the IDE settings dialog.
 */
public class ModForgeSettingsConfigurable implements Configurable {
    private JBPasswordField apiKeyField;
    private JSpinner maxTokensField;
    private JBCheckBox usePatternLearningField;
    private JBTextField serverUrlField;
    private JBTextField usernameField;
    private JBCheckBox generateJavadocField;
    private JBCheckBox addCopyrightHeaderField;
    private JBTextField copyrightTextField;
    private JBCheckBox showMetricsInStatusBarField;
    private JBCheckBox enableNotificationsField;
    private ComboBox<String> themeSelector;
    
    private final ModForgeSettings settings = ModForgeSettings.getInstance();
    
    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "ModForge";
    }
    
    @Nullable
    @Override
    public JComponent createComponent() {
        // Create tabbed pane for different setting categories
        JTabbedPane tabbedPane = new JTabbedPane();
        
        // Create panels for each tab
        JPanel aiSettingsPanel = createAISettingsPanel();
        JPanel collaborationSettingsPanel = createCollaborationSettingsPanel();
        JPanel codeGenerationSettingsPanel = createCodeGenerationSettingsPanel();
        JPanel uiSettingsPanel = createUISettingsPanel();
        
        // Add panels to tabbed pane
        tabbedPane.addTab("AI Settings", aiSettingsPanel);
        tabbedPane.addTab("Collaboration", collaborationSettingsPanel);
        tabbedPane.addTab("Code Generation", codeGenerationSettingsPanel);
        tabbedPane.addTab("UI", uiSettingsPanel);
        
        // Create main panel
        JPanel mainPanel = new JBPanel<>(new BorderLayout());
        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        mainPanel.setBorder(JBUI.Borders.empty(10));
        
        return mainPanel;
    }
    
    /**
     * Creates the AI settings panel.
     * @return The AI settings panel
     */
    private JPanel createAISettingsPanel() {
        // Create fields
        apiKeyField = new JBPasswordField();
        apiKeyField.setEmptyText("Enter your OpenAI API key");
        
        maxTokensField = new JSpinner(new SpinnerNumberModel(1000, 100, 10000, 100));
        
        usePatternLearningField = new JBCheckBox("Use pattern learning to reduce API costs");
        
        // Create info label
        JBLabel infoLabel = new JBLabel("<html><body>" +
                "ModForge uses OpenAI's API to generate and improve code.<br>" +
                "You will need to provide your own API key to use these features.<br>" +
                "Get an API key at <a href=\"https://platform.openai.com/\">platform.openai.com</a>" +
                "</body></html>");
        infoLabel.setBorder(JBUI.Borders.empty(0, 0, 10, 0));
        
        // Create panel
        JPanel panel = FormBuilder.createFormBuilder()
                .addComponent(infoLabel)
                .addLabeledComponent("OpenAI API Key:", apiKeyField)
                .addLabeledComponent("Max Tokens per Request:", maxTokensField)
                .addComponent(usePatternLearningField)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
        
        panel.setBorder(JBUI.Borders.empty(10));
        
        return panel;
    }
    
    /**
     * Creates the collaboration settings panel.
     * @return The collaboration settings panel
     */
    private JPanel createCollaborationSettingsPanel() {
        // Create fields
        serverUrlField = new JBTextField();
        serverUrlField.setEmptyText("WebSocket server URL for collaboration");
        
        usernameField = new JBTextField();
        usernameField.setEmptyText("Your username for collaboration");
        
        // Create info label
        JBLabel infoLabel = new JBLabel("<html><body>" +
                "Configure collaboration settings for real-time shared editing.<br>" +
                "The default server URL points to the ModForge collaboration service.<br>" +
                "You can also set up your own collaboration server and use it instead." +
                "</body></html>");
        infoLabel.setBorder(JBUI.Borders.empty(0, 0, 10, 0));
        
        // Create panel
        JPanel panel = FormBuilder.createFormBuilder()
                .addComponent(infoLabel)
                .addLabeledComponent("Server URL:", serverUrlField)
                .addLabeledComponent("Username:", usernameField)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
        
        panel.setBorder(JBUI.Borders.empty(10));
        
        return panel;
    }
    
    /**
     * Creates the code generation settings panel.
     * @return The code generation settings panel
     */
    private JPanel createCodeGenerationSettingsPanel() {
        // Create fields
        generateJavadocField = new JBCheckBox("Generate Javadoc comments for generated code");
        
        addCopyrightHeaderField = new JBCheckBox("Add copyright header to generated files");
        
        copyrightTextField = new JBTextField();
        copyrightTextField.setEmptyText("Copyright text (${YEAR} will be replaced with current year)");
        
        // Create info label
        JBLabel infoLabel = new JBLabel("<html><body>" +
                "Configure how ModForge generates code.<br>" +
                "These settings will be applied to all generated files." +
                "</body></html>");
        infoLabel.setBorder(JBUI.Borders.empty(0, 0, 10, 0));
        
        // Create panel
        JPanel panel = FormBuilder.createFormBuilder()
                .addComponent(infoLabel)
                .addComponent(generateJavadocField)
                .addComponent(addCopyrightHeaderField)
                .addLabeledComponent("Copyright Text:", copyrightTextField)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
        
        panel.setBorder(JBUI.Borders.empty(10));
        
        return panel;
    }
    
    /**
     * Creates the UI settings panel.
     * @return The UI settings panel
     */
    private JPanel createUISettingsPanel() {
        // Create fields
        showMetricsInStatusBarField = new JBCheckBox("Show metrics in status bar");
        
        enableNotificationsField = new JBCheckBox("Enable notifications");
        
        // Create theme selector
        themeSelector = new ComboBox<>(new String[] {
                "Use IDE Theme", "Light Theme", "Dark Theme"
        });
        
        // Create info label
        JBLabel infoLabel = new JBLabel("<html><body>" +
                "Configure the ModForge user interface.<br>" +
                "These settings control how ModForge displays information and alerts." +
                "</body></html>");
        infoLabel.setBorder(JBUI.Borders.empty(0, 0, 10, 0));
        
        // Create panel
        JPanel panel = FormBuilder.createFormBuilder()
                .addComponent(infoLabel)
                .addComponent(showMetricsInStatusBarField)
                .addComponent(enableNotificationsField)
                .addLabeledComponent("Theme:", themeSelector)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
        
        panel.setBorder(JBUI.Borders.empty(10));
        
        return panel;
    }
    
    @Override
    public boolean isModified() {
        return !String.valueOf(apiKeyField.getPassword()).equals(settings.getOpenAiApiKey())
                || (Integer) maxTokensField.getValue() != settings.getMaxTokensPerRequest()
                || usePatternLearningField.isSelected() != settings.isUsePatternLearning()
                || !serverUrlField.getText().equals(settings.getCollaborationServerUrl())
                || !usernameField.getText().equals(settings.getUsername())
                || generateJavadocField.isSelected() != settings.isGenerateJavadoc()
                || addCopyrightHeaderField.isSelected() != settings.isAddCopyrightHeader()
                || !copyrightTextField.getText().equals(settings.getCopyrightText())
                || showMetricsInStatusBarField.isSelected() != settings.isShowMetricsInStatusBar()
                || enableNotificationsField.isSelected() != settings.isEnableNotifications();
    }
    
    @Override
    public void apply() throws ConfigurationException {
        // API settings
        settings.setOpenAiApiKey(String.valueOf(apiKeyField.getPassword()));
        settings.setMaxTokensPerRequest((Integer) maxTokensField.getValue());
        settings.setUsePatternLearning(usePatternLearningField.isSelected());
        
        // Collaboration settings
        settings.setCollaborationServerUrl(serverUrlField.getText());
        settings.setUsername(usernameField.getText());
        
        // Code generation settings
        settings.setGenerateJavadoc(generateJavadocField.isSelected());
        settings.setAddCopyrightHeader(addCopyrightHeaderField.isSelected());
        settings.setCopyrightText(copyrightTextField.getText());
        
        // UI settings
        settings.setShowMetricsInStatusBar(showMetricsInStatusBarField.isSelected());
        settings.setEnableNotifications(enableNotificationsField.isSelected());
    }
    
    @Override
    public void reset() {
        // API settings
        apiKeyField.setText(settings.getOpenAiApiKey());
        maxTokensField.setValue(settings.getMaxTokensPerRequest());
        usePatternLearningField.setSelected(settings.isUsePatternLearning());
        
        // Collaboration settings
        serverUrlField.setText(settings.getCollaborationServerUrl());
        usernameField.setText(settings.getUsername());
        
        // Code generation settings
        generateJavadocField.setSelected(settings.isGenerateJavadoc());
        addCopyrightHeaderField.setSelected(settings.isAddCopyrightHeader());
        copyrightTextField.setText(settings.getCopyrightText());
        
        // UI settings
        showMetricsInStatusBarField.setSelected(settings.isShowMetricsInStatusBar());
        enableNotificationsField.setSelected(settings.isEnableNotifications());
    }
    
    @Override
    public void disposeUIResources() {
        // No resources to dispose
    }
}