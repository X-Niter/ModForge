package com.modforge.intellij.plugin.ui.toolwindow.panels;

import com.intellij.icons.AllIcons;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import com.modforge.intellij.plugin.settings.ModForgeSettingsConfigurable;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.net.URI;

/**
 * Panel for configuring ModForge settings.
 */
public class SettingsPanel {
    private static final Logger LOG = Logger.getInstance(SettingsPanel.class);
    
    private final Project project;
    private final ModForgeSettings settings;
    
    private JPanel mainPanel;
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
    private JButton saveButton;
    private JButton resetButton;
    
    /**
     * Creates a new SettingsPanel.
     * @param project The project
     */
    public SettingsPanel(@NotNull Project project) {
        this.project = project;
        this.settings = ModForgeSettings.getInstance();
        createUI();
        loadSettings();
    }
    
    /**
     * Creates the UI.
     */
    private void createUI() {
        // Create tabbed pane for different setting categories
        JTabbedPane tabbedPane = new JTabbedPane();
        
        // Create panels for each tab
        JPanel aiSettingsPanel = createAISettingsPanel();
        JPanel collaborationSettingsPanel = createCollaborationSettingsPanel();
        JPanel codeGenerationSettingsPanel = createCodeGenerationSettingsPanel();
        JPanel uiSettingsPanel = createUISettingsPanel();
        JPanel aboutPanel = createAboutPanel();
        
        // Add panels to tabbed pane
        tabbedPane.addTab("AI Settings", AllIcons.General.Settings, aiSettingsPanel);
        tabbedPane.addTab("Collaboration", AllIcons.Nodes.Plugin, collaborationSettingsPanel);
        tabbedPane.addTab("Code Generation", AllIcons.Actions.Edit, codeGenerationSettingsPanel);
        tabbedPane.addTab("UI", AllIcons.General.ProjectStructure, uiSettingsPanel);
        tabbedPane.addTab("About", AllIcons.General.Information, aboutPanel);
        
        // Create action panel
        JPanel actionPanel = new JBPanel<>(new FlowLayout(FlowLayout.RIGHT));
        actionPanel.setBorder(JBUI.Borders.empty(10, 0, 0, 0));
        
        resetButton = new JButton("Reset to Defaults");
        resetButton.addActionListener(this::resetSettings);
        
        saveButton = new JButton("Apply Settings");
        saveButton.addActionListener(this::saveSettings);
        
        actionPanel.add(resetButton);
        actionPanel.add(saveButton);
        
        // Create main panel
        mainPanel = new JBPanel<>(new BorderLayout());
        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        mainPanel.add(actionPanel, BorderLayout.SOUTH);
        mainPanel.setBorder(JBUI.Borders.empty(10));
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
        
        // Create panel
        JPanel panel = FormBuilder.createFormBuilder()
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
        
        // Create panel
        JPanel panel = FormBuilder.createFormBuilder()
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
        
        // Create panel
        JPanel panel = FormBuilder.createFormBuilder()
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
        ComboBox<String> themeSelector = new ComboBox<>(new String[] {
                "Use IDE Theme", "Light Theme", "Dark Theme"
        });
        
        // Create panel
        JPanel panel = FormBuilder.createFormBuilder()
                .addComponent(showMetricsInStatusBarField)
                .addComponent(enableNotificationsField)
                .addLabeledComponent("Theme:", themeSelector)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
        
        panel.setBorder(JBUI.Borders.empty(10));
        
        return panel;
    }
    
    /**
     * Creates the about panel.
     * @return The about panel
     */
    private JPanel createAboutPanel() {
        JPanel panel = new JBPanel<>(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(10));
        
        // Create HTML content
        JEditorPane editorPane = new JEditorPane();
        editorPane.setEditable(false);
        editorPane.setContentType("text/html");
        editorPane.setBackground(UIUtil.getPanelBackground());
        
        // Set up styles
        HTMLEditorKit kit = new HTMLEditorKit();
        editorPane.setEditorKit(kit);
        StyleSheet styleSheet = kit.getStyleSheet();
        styleSheet.addRule("body { font-family: Arial, sans-serif; font-size: 12pt; margin: 10px; }");
        styleSheet.addRule("h1 { font-size: 16pt; color: #4A86E8; }");
        styleSheet.addRule("h2 { font-size: 14pt; color: #6AA84F; }");
        styleSheet.addRule("a { color: #4A86E8; }");
        
        // Set content
        editorPane.setText(
                "<html><body>" +
                "<h1>ModForge</h1>" +
                "<p>An advanced AI-powered Minecraft mod development platform.</p>" +
                "<p>Version: 1.0.0</p>" +
                "<h2>Features</h2>" +
                "<ul>" +
                "<li>Comprehensive cross-loader mod creation support</li>" +
                "<li>Real-time collaboration with WebSocket support</li>" +
                "<li>Dynamic library analysis and intelligent code generation</li>" +
                "<li>Advanced project setup and cross-platform mod development</li>" +
                "<li>Integrated AI-enhanced development workflow</li>" +
                "</ul>" +
                "<p><a href=\"https://modforge.io/docs\">Documentation</a> | " +
                "<a href=\"https://modforge.io/support\">Support</a></p>" +
                "</body></html>"
        );
        
        // Add hyperlink listener
        editorPane.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                try {
                    BrowserUtil.browse(new URI(e.getURL().toString()));
                } catch (Exception ex) {
                    LOG.error("Error opening URL", ex);
                }
            }
        });
        
        // Add to panel
        panel.add(new JScrollPane(editorPane), BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Loads settings into the UI.
     */
    private void loadSettings() {
        // AI settings
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
    
    /**
     * Saves settings from the UI.
     * @param e The action event
     */
    private void saveSettings(ActionEvent e) {
        // AI settings
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
        
        // Show confirmation
        JOptionPane.showMessageDialog(
                mainPanel,
                "Settings saved successfully.",
                "Settings Saved",
                JOptionPane.INFORMATION_MESSAGE
        );
    }
    
    /**
     * Resets settings to defaults.
     * @param e The action event
     */
    private void resetSettings(ActionEvent e) {
        int result = JOptionPane.showConfirmDialog(
                mainPanel,
                "Are you sure you want to reset all settings to defaults?",
                "Reset Settings",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        
        if (result == JOptionPane.YES_OPTION) {
            // Reset to defaults
            settings.setOpenAiApiKey("");
            settings.setMaxTokensPerRequest(1000);
            settings.setUsePatternLearning(true);
            settings.setCollaborationServerUrl("wss://modforge.io/ws/collaboration");
            settings.setUsername("");
            settings.setGenerateJavadoc(true);
            settings.setAddCopyrightHeader(true);
            settings.setCopyrightText("Copyright (c) ${YEAR} ModForge Team");
            settings.setShowMetricsInStatusBar(true);
            settings.setEnableNotifications(true);
            
            // Reload UI
            loadSettings();
            
            // Show confirmation
            JOptionPane.showMessageDialog(
                    mainPanel,
                    "Settings have been reset to defaults.",
                    "Settings Reset",
                    JOptionPane.INFORMATION_MESSAGE
            );
        }
    }
    
    /**
     * Gets the content component.
     * @return The content component
     */
    public JComponent getContent() {
        return mainPanel;
    }
}