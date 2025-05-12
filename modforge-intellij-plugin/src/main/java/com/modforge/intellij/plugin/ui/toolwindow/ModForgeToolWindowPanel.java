package com.modforge.intellij.plugin.ui.toolwindow;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.util.ui.JBUI;
import com.modforge.intellij.plugin.services.AutonomousCodeGenerationService;
import com.modforge.intellij.plugin.services.ContinuousDevelopmentService;
import com.modforge.intellij.plugin.settings.ModForgeSettings;

import javax.swing.*;
import java.awt.*;

/**
 * The main panel for the ModForge tool window.
 */
public class ModForgeToolWindowPanel extends SimpleToolWindowPanel {
    
    private final Project project;
    private final ToolWindow toolWindow;
    
    public ModForgeToolWindowPanel(Project project, ToolWindow toolWindow) {
        super(true, true);
        this.project = project;
        this.toolWindow = toolWindow;
        
        // Set up the UI
        setupUI();
    }
    
    private void setupUI() {
        // Create toolbar
        ActionManager actionManager = ActionManager.getInstance();
        ActionGroup actionGroup = (ActionGroup) actionManager.getAction("ModForge.ToolbarActions");
        ActionToolbar actionToolbar = actionManager.createActionToolbar("ModForge.Toolbar", actionGroup, true);
        actionToolbar.setTargetComponent(this);
        setToolbar(actionToolbar.getComponent());
        
        // Create tabbed pane for different sections
        JBTabbedPane tabbedPane = new JBTabbedPane();
        
        // Add tabs
        tabbedPane.addTab("Overview", createOverviewPanel());
        tabbedPane.addTab("Generate Code", createGenerateCodePanel());
        tabbedPane.addTab("Fix Errors", createFixErrorsPanel());
        tabbedPane.addTab("Continuous Development", createContinuousDevelopmentPanel());
        tabbedPane.addTab("Settings", createSettingsPanel());
        
        // Set the content
        setContent(tabbedPane);
    }
    
    /**
     * Creates the overview panel.
     */
    private JComponent createOverviewPanel() {
        JBPanel<JBPanel<?>> panel = new JBPanel<>(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(10));
        
        // Create header
        JPanel headerPanel = new JPanel(new BorderLayout());
        JBLabel titleLabel = new JBLabel("ModForge AI Assistant");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 18));
        headerPanel.add(titleLabel, BorderLayout.NORTH);
        
        JBLabel subtitleLabel = new JBLabel("Autonomous Minecraft Mod Development");
        subtitleLabel.setFont(subtitleLabel.getFont().deriveFont(Font.PLAIN, 14));
        headerPanel.add(subtitleLabel, BorderLayout.SOUTH);
        headerPanel.setBorder(JBUI.Borders.empty(0, 0, 15, 0));
        
        panel.add(headerPanel, BorderLayout.NORTH);
        
        // Create content
        JPanel contentPanel = new JPanel(new BorderLayout());
        JBLabel infoLabel = new JBLabel("<html><body style='width: 300px'>" +
                "ModForge provides AI-powered tools to help you develop Minecraft mods more efficiently. " +
                "The plugin offers the following features:" +
                "<ul>" +
                "<li><b>Code Generation:</b> Generate mod code from natural language descriptions</li>" +
                "<li><b>Error Fixing:</b> Automatically fix compilation errors</li>" +
                "<li><b>Documentation:</b> Generate documentation for your code</li>" +
                "<li><b>Continuous Development:</b> Let the AI continuously improve your mod</li>" +
                "</ul>" +
                "Use the tabs above to access different features of the plugin." +
                "</body></html>");
        infoLabel.setBorder(JBUI.Borders.empty(10));
        
        contentPanel.add(infoLabel, BorderLayout.NORTH);
        
        // Add version info
        JBLabel versionLabel = new JBLabel("Version: 1.0.0");
        versionLabel.setBorder(JBUI.Borders.empty(10, 10, 5, 10));
        contentPanel.add(versionLabel, BorderLayout.SOUTH);
        
        panel.add(contentPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Creates the generate code panel.
     */
    private JComponent createGenerateCodePanel() {
        JBPanel<JBPanel<?>> panel = new JBPanel<>(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(10));
        
        // Title
        JBLabel titleLabel = new JBLabel("Generate Code");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16));
        titleLabel.setBorder(JBUI.Borders.empty(0, 0, 10, 0));
        panel.add(titleLabel, BorderLayout.NORTH);
        
        // Content Panel
        JPanel contentPanel = new JPanel(new BorderLayout(0, 10));
        contentPanel.setBorder(JBUI.Borders.empty(10));
        
        // Description
        JBLabel descriptionLabel = new JBLabel("<html><body style='width: 300px'>" +
                "Describe what you want to generate in natural language. " +
                "The AI will generate code based on your description." +
                "</body></html>");
        contentPanel.add(descriptionLabel, BorderLayout.NORTH);
        
        // Prompt field
        JPanel promptPanel = new JPanel(new BorderLayout());
        promptPanel.setBorder(JBUI.Borders.empty(10, 0));
        
        JBLabel promptLabel = new JBLabel("Prompt:");
        promptPanel.add(promptLabel, BorderLayout.NORTH);
        
        JTextArea promptArea = new JTextArea();
        promptArea.setLineWrap(true);
        promptArea.setWrapStyleWord(true);
        promptArea.setRows(5);
        JBScrollPane promptScrollPane = new JBScrollPane(promptArea);
        promptScrollPane.setBorder(JBUI.Borders.empty(5, 0));
        promptPanel.add(promptScrollPane, BorderLayout.CENTER);
        
        contentPanel.add(promptPanel, BorderLayout.CENTER);
        
        // Button Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        
        // Generate Button
        JButton generateButton = new JButton("Generate Code");
        generateButton.addActionListener(e -> generateCode(promptArea.getText()));
        buttonPanel.add(generateButton);
        
        // Clear Button
        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> promptArea.setText(""));
        buttonPanel.add(clearButton);
        
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        panel.add(contentPanel, BorderLayout.CENTER);
        
        // Example Panel
        JPanel examplePanel = new JPanel(new BorderLayout());
        examplePanel.setBorder(JBUI.Borders.empty(10, 10, 0, 10));
        
        JBLabel exampleLabel = new JBLabel("<html><body style='width: 300px'>" +
                "<b>Example prompts:</b>" +
                "<ul>" +
                "<li>Create a simple mod that adds a new sword item</li>" +
                "<li>Generate a block with custom texture that emits light</li>" +
                "<li>Write a mob entity class with custom AI for a friendly companion</li>" +
                "</ul>" +
                "</body></html>");
        examplePanel.add(exampleLabel, BorderLayout.CENTER);
        
        panel.add(examplePanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    /**
     * Creates the fix errors panel.
     */
    private JComponent createFixErrorsPanel() {
        JBPanel<JBPanel<?>> panel = new JBPanel<>(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(10));
        
        // Title
        JBLabel titleLabel = new JBLabel("Fix Errors");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16));
        titleLabel.setBorder(JBUI.Borders.empty(0, 0, 10, 0));
        panel.add(titleLabel, BorderLayout.NORTH);
        
        // Content
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBorder(JBUI.Borders.empty(10));
        
        JBLabel infoLabel = new JBLabel("<html><body style='width: 300px'>" +
                "This tool automatically fixes compilation errors in your code. " +
                "Select a file with errors in the editor, then click 'Fix Errors' to fix them." +
                "</body></html>");
        contentPanel.add(infoLabel, BorderLayout.NORTH);
        
        // Error list
        JBLabel errorsLabel = new JBLabel("Current errors:");
        errorsLabel.setBorder(JBUI.Borders.empty(10, 0, 5, 0));
        contentPanel.add(errorsLabel, BorderLayout.CENTER);
        
        JTextArea errorList = new JTextArea();
        errorList.setEditable(false);
        errorList.setBackground(new JBColor(new Color(245, 245, 245), new Color(60, 60, 60)));
        JBScrollPane errorScrollPane = new JBScrollPane(errorList);
        errorScrollPane.setPreferredSize(new Dimension(300, 200));
        contentPanel.add(errorScrollPane, BorderLayout.SOUTH);
        
        panel.add(contentPanel, BorderLayout.CENTER);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        buttonPanel.setBorder(JBUI.Borders.empty(10));
        
        JButton refreshButton = new JButton("Refresh Errors");
        refreshButton.addActionListener(e -> refreshErrors(errorList));
        buttonPanel.add(refreshButton);
        
        JButton fixButton = new JButton("Fix All Errors");
        fixButton.addActionListener(e -> fixAllErrors());
        buttonPanel.add(fixButton);
        
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        // Initialize error list
        refreshErrors(errorList);
        
        return panel;
    }
    
    /**
     * Creates the continuous development panel.
     */
    private JComponent createContinuousDevelopmentPanel() {
        JBPanel<JBPanel<?>> panel = new JBPanel<>(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(10));
        
        // Title
        JBLabel titleLabel = new JBLabel("Continuous Development");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16));
        titleLabel.setBorder(JBUI.Borders.empty(0, 0, 10, 0));
        panel.add(titleLabel, BorderLayout.NORTH);
        
        // Content
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBorder(JBUI.Borders.empty(10));
        
        JBLabel infoLabel = new JBLabel("<html><body style='width: 300px'>" +
                "Continuous development mode enables the AI to automatically monitor your project, " +
                "fix errors, and make improvements without manual intervention. " +
                "Toggle the switch below to enable or disable this feature." +
                "</body></html>");
        contentPanel.add(infoLabel, BorderLayout.NORTH);
        
        // Status Panel
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(JBUI.Borders.empty(10, 0));
        
        JBLabel statusLabel = new JBLabel("Current Status:");
        statusPanel.add(statusLabel, BorderLayout.NORTH);
        
        // Get current status
        ContinuousDevelopmentService service = ContinuousDevelopmentService.getInstance(project);
        boolean isRunning = service != null && service.isRunning();
        
        JBLabel statusValueLabel = new JBLabel(isRunning ? 
                "<html><font color='green'>Running</font></html>" : 
                "<html><font color='red'>Stopped</font></html>");
        statusValueLabel.setFont(statusValueLabel.getFont().deriveFont(Font.BOLD));
        statusValueLabel.setBorder(JBUI.Borders.empty(5, 20));
        statusPanel.add(statusValueLabel, BorderLayout.CENTER);
        
        contentPanel.add(statusPanel, BorderLayout.CENTER);
        
        panel.add(contentPanel, BorderLayout.CENTER);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        buttonPanel.setBorder(JBUI.Borders.empty(10));
        
        JToggleButton toggleButton = new JToggleButton(isRunning ? "Disable" : "Enable", isRunning);
        toggleButton.addActionListener(e -> {
            boolean newState = toggleButton.isSelected();
            // Update settings
            ModForgeSettings settings = ModForgeSettings.getInstance();
            settings.setContinuousDevelopmentEnabled(newState);
            
            // Start or stop service
            if (newState) {
                service.start();
                statusValueLabel.setText("<html><font color='green'>Running</font></html>");
                toggleButton.setText("Disable");
            } else {
                service.stop();
                statusValueLabel.setText("<html><font color='red'>Stopped</font></html>");
                toggleButton.setText("Enable");
            }
        });
        buttonPanel.add(toggleButton);
        
        JButton viewLogsButton = new JButton("View Logs");
        viewLogsButton.addActionListener(e -> viewContinuousDevelopmentLogs());
        buttonPanel.add(viewLogsButton);
        
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    /**
     * Creates the settings panel.
     */
    private JComponent createSettingsPanel() {
        JBPanel<JBPanel<?>> panel = new JBPanel<>(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(10));
        
        // Title
        JBLabel titleLabel = new JBLabel("Settings");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16));
        titleLabel.setBorder(JBUI.Borders.empty(0, 0, 10, 0));
        panel.add(titleLabel, BorderLayout.NORTH);
        
        // Content
        JPanel contentPanel = new JPanel(new GridLayout(0, 1, 0, 10));
        contentPanel.setBorder(JBUI.Borders.empty(10));
        
        // API Key Settings
        JPanel apiKeyPanel = new JPanel(new BorderLayout());
        JBLabel apiKeyLabel = new JBLabel("OpenAI API Key:");
        apiKeyPanel.add(apiKeyLabel, BorderLayout.WEST);
        
        // Get current API key
        ModForgeSettings settings = ModForgeSettings.getInstance();
        String apiKey = settings.getOpenAiApiKey();
        
        // Password field to hide API key
        JPasswordField apiKeyField = new JPasswordField(apiKey);
        apiKeyField.setEchoChar('•');
        apiKeyPanel.add(apiKeyField, BorderLayout.CENTER);
        
        JButton saveApiKeyButton = new JButton("Save");
        saveApiKeyButton.addActionListener(e -> {
            String newApiKey = new String(apiKeyField.getPassword());
            settings.setOpenAiApiKey(newApiKey);
            JOptionPane.showMessageDialog(
                    panel,
                    "API key saved successfully!",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE
            );
        });
        apiKeyPanel.add(saveApiKeyButton, BorderLayout.EAST);
        
        contentPanel.add(apiKeyPanel);
        
        // Continuous Development Settings
        JPanel continuousPanel = new JPanel(new BorderLayout());
        JBLabel continuousLabel = new JBLabel("Enable Continuous Development:");
        continuousPanel.add(continuousLabel, BorderLayout.WEST);
        
        JCheckBox continuousCheckbox = new JCheckBox();
        continuousCheckbox.setSelected(settings.isContinuousDevelopmentEnabled());
        continuousCheckbox.addActionListener(e -> 
                settings.setContinuousDevelopmentEnabled(continuousCheckbox.isSelected()));
        continuousPanel.add(continuousCheckbox, BorderLayout.CENTER);
        
        contentPanel.add(continuousPanel);
        
        // Pattern Recognition Settings
        JPanel patternPanel = new JPanel(new BorderLayout());
        JBLabel patternLabel = new JBLabel("Enable Pattern Recognition:");
        patternPanel.add(patternLabel, BorderLayout.WEST);
        
        JCheckBox patternCheckbox = new JCheckBox();
        patternCheckbox.setSelected(settings.isPatternRecognitionEnabled());
        patternCheckbox.addActionListener(e -> 
                settings.setPatternRecognitionEnabled(patternCheckbox.isSelected()));
        patternPanel.add(patternCheckbox, BorderLayout.CENTER);
        
        contentPanel.add(patternPanel);
        
        // Update frequency settings
        JPanel frequencyPanel = new JPanel(new BorderLayout());
        JBLabel frequencyLabel = new JBLabel("Update Frequency (minutes):");
        frequencyPanel.add(frequencyLabel, BorderLayout.WEST);
        
        JTextField frequencyField = new JTextField(String.valueOf(settings.getUpdateFrequencyMinutes()));
        frequencyField.addActionListener(e -> {
            try {
                int frequency = Integer.parseInt(frequencyField.getText());
                if (frequency > 0) {
                    settings.setUpdateFrequencyMinutes(frequency);
                }
            } catch (NumberFormatException ex) {
                // Ignore invalid input
                frequencyField.setText(String.valueOf(settings.getUpdateFrequencyMinutes()));
            }
        });
        frequencyPanel.add(frequencyField, BorderLayout.CENTER);
        
        contentPanel.add(frequencyPanel);
        
        panel.add(contentPanel, BorderLayout.CENTER);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        buttonPanel.setBorder(JBUI.Borders.empty(10, 0, 0, 0));
        
        JButton resetButton = new JButton("Reset to Defaults");
        resetButton.addActionListener(e -> {
            settings.resetToDefaults();
            apiKeyField.setText(settings.getOpenAiApiKey());
            continuousCheckbox.setSelected(settings.isContinuousDevelopmentEnabled());
            patternCheckbox.setSelected(settings.isPatternRecognitionEnabled());
            frequencyField.setText(String.valueOf(settings.getUpdateFrequencyMinutes()));
        });
        buttonPanel.add(resetButton);
        
        JButton saveAllButton = new JButton("Save All");
        saveAllButton.addActionListener(e -> {
            // Save API key
            settings.setOpenAiApiKey(new String(apiKeyField.getPassword()));
            
            // Save other settings
            settings.setContinuousDevelopmentEnabled(continuousCheckbox.isSelected());
            settings.setPatternRecognitionEnabled(patternCheckbox.isSelected());
            
            // Save frequency
            try {
                int frequency = Integer.parseInt(frequencyField.getText());
                if (frequency > 0) {
                    settings.setUpdateFrequencyMinutes(frequency);
                }
            } catch (NumberFormatException ex) {
                frequencyField.setText(String.valueOf(settings.getUpdateFrequencyMinutes()));
            }
            
            JOptionPane.showMessageDialog(
                    panel,
                    "Settings saved successfully!",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE
            );
        });
        buttonPanel.add(saveAllButton);
        
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    /**
     * Generates code from a prompt.
     * @param prompt The prompt to generate code from
     */
    private void generateCode(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please enter a prompt.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }
        
        // Get service
        AutonomousCodeGenerationService service = AutonomousCodeGenerationService.getInstance(project);
        
        if (service == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "Code generation service not available.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }
        
        // Show progress dialog
        JDialog progressDialog = new JDialog();
        progressDialog.setTitle("Generating Code");
        progressDialog.setSize(300, 100);
        progressDialog.setLocationRelativeTo(this);
        progressDialog.setModal(true);
        
        JPanel progressPanel = new JPanel(new BorderLayout());
        progressPanel.setBorder(JBUI.Borders.empty(10));
        
        JBLabel progressLabel = new JBLabel("Generating code, please wait...");
        progressPanel.add(progressLabel, BorderLayout.NORTH);
        
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressPanel.add(progressBar, BorderLayout.CENTER);
        
        progressDialog.add(progressPanel);
        
        // Generate code in background thread
        SwingUtilities.invokeLater(() -> {
            try {
                // Show progress dialog
                progressDialog.setVisible(true);
                
                // Generate code
                service.generateCode(prompt, null, null)
                        .thenAccept(code -> {
                            // Close progress dialog
                            progressDialog.dispose();
                            
                            // Show result
                            if (code == null || code.isEmpty()) {
                                JOptionPane.showMessageDialog(
                                        this,
                                        "Failed to generate code.",
                                        "Error",
                                        JOptionPane.ERROR_MESSAGE
                                );
                                return;
                            }
                            
                            // Show code in dialog
                            JDialog resultDialog = new JDialog();
                            resultDialog.setTitle("Generated Code");
                            resultDialog.setSize(600, 400);
                            resultDialog.setLocationRelativeTo(this);
                            resultDialog.setModal(true);
                            
                            JPanel resultPanel = new JPanel(new BorderLayout());
                            resultPanel.setBorder(JBUI.Borders.empty(10));
                            
                            JTextArea codeArea = new JTextArea(code);
                            codeArea.setEditable(false);
                            JBScrollPane scrollPane = new JBScrollPane(codeArea);
                            resultPanel.add(scrollPane, BorderLayout.CENTER);
                            
                            // Buttons
                            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING));
                            
                            JButton closeButton = new JButton("Close");
                            closeButton.addActionListener(e -> resultDialog.dispose());
                            buttonPanel.add(closeButton);
                            
                            JButton copyButton = new JButton("Copy to Clipboard");
                            copyButton.addActionListener(e -> {
                                codeArea.selectAll();
                                codeArea.copy();
                                codeArea.select(0, 0);
                            });
                            buttonPanel.add(copyButton);
                            
                            resultPanel.add(buttonPanel, BorderLayout.SOUTH);
                            
                            resultDialog.add(resultPanel);
                            resultDialog.setVisible(true);
                        })
                        .exceptionally(ex -> {
                            // Close progress dialog
                            progressDialog.dispose();
                            
                            // Show error
                            JOptionPane.showMessageDialog(
                                    this,
                                    "Error generating code: " + ex.getMessage(),
                                    "Error",
                                    JOptionPane.ERROR_MESSAGE
                            );
                            return null;
                        });
            } catch (Exception ex) {
                // Close progress dialog
                progressDialog.dispose();
                
                // Show error
                JOptionPane.showMessageDialog(
                        this,
                        "Error generating code: " + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        });
    }
    
    /**
     * Refreshes the list of errors.
     * @param errorTextArea The text area to update
     */
    private void refreshErrors(JTextArea errorTextArea) {
        // This would fetch the current errors from the compilation listener
        errorTextArea.setText("No errors found.");
    }
    
    /**
     * Fixes all errors in the project.
     */
    private void fixAllErrors() {
        // This would trigger the fix all errors action
        JOptionPane.showMessageDialog(
                this,
                "This feature is not yet implemented.",
                "Not Implemented",
                JOptionPane.INFORMATION_MESSAGE
        );
    }
    
    /**
     * Views the continuous development logs.
     */
    private void viewContinuousDevelopmentLogs() {
        // This would show the continuous development logs
        JOptionPane.showMessageDialog(
                this,
                "This feature is not yet implemented.",
                "Not Implemented",
                JOptionPane.INFORMATION_MESSAGE
        );
    }
}