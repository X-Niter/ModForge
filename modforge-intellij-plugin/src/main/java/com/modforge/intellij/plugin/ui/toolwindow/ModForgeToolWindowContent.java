package com.modforge.intellij.plugin.ui.toolwindow;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import com.modforge.intellij.plugin.ai.PatternRecognitionService;
import com.modforge.intellij.plugin.services.AutonomousCodeGenerationService;
import com.modforge.intellij.plugin.services.ContinuousDevelopmentService;
import com.modforge.intellij.plugin.settings.ModForgeSettings;

import javax.swing.*;
import java.awt.*;

/**
 * Content for ModForge tool window.
 */
public class ModForgeToolWindowContent {
    private static final Logger LOG = Logger.getInstance(ModForgeToolWindowContent.class);
    
    private final Project project;
    private final ToolWindow toolWindow;
    private JPanel mainPanel;
    
    /**
     * Constructor.
     * @param project The project
     * @param toolWindow The tool window
     */
    public ModForgeToolWindowContent(Project project, ToolWindow toolWindow) {
        this.project = project;
        this.toolWindow = toolWindow;
        
        LOG.info("Creating ModForge tool window content for project: " + project.getName());
        
        // Create content
        createContent();
    }
    
    /**
     * Create the content.
     */
    private void createContent() {
        mainPanel = new JBPanel<>(new BorderLayout());
        mainPanel.setBorder(JBUI.Borders.empty(10));
        
        JBTabbedPane tabbedPane = new JBTabbedPane();
        
        // Add tabs
        tabbedPane.addTab("Dashboard", createDashboardPanel());
        tabbedPane.addTab("Continuous Development", createContinuousDevelopmentPanel());
        tabbedPane.addTab("Code Generation", createCodeGenerationPanel());
        tabbedPane.addTab("Pattern Recognition", createPatternRecognitionPanel());
        tabbedPane.addTab("GitHub Integration", createGitHubIntegrationPanel());
        
        mainPanel.add(tabbedPane, BorderLayout.CENTER);
    }
    
    /**
     * Create dashboard panel.
     * @return The dashboard panel
     */
    private JPanel createDashboardPanel() {
        JPanel panel = new JBPanel<>(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(10));
        
        // Add title
        JBLabel titleLabel = new JBLabel("ModForge Dashboard");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16));
        panel.add(titleLabel, BorderLayout.NORTH);
        
        // Add content
        JPanel contentPanel = new JBPanel<>(new GridLayout(0, 1, 0, 10));
        contentPanel.setBorder(JBUI.Borders.empty(10, 0, 0, 0));
        
        // Status
        addStatusSection(contentPanel);
        
        // Quick actions
        addQuickActionsSection(contentPanel);
        
        // Metrics
        addMetricsSection(contentPanel);
        
        // Wrap in scroll pane
        JBScrollPane scrollPane = new JBScrollPane(contentPanel);
        scrollPane.setBorder(JBUI.Borders.empty());
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Add status section to panel.
     * @param panel The panel to add to
     */
    private void addStatusSection(JPanel panel) {
        JPanel statusPanel = new JBPanel<>(new GridLayout(0, 2, 10, 5));
        statusPanel.setBorder(JBUI.Borders.emptyTop(10));
        
        // Authentication status
        statusPanel.add(new JBLabel("Authentication:"));
        ModForgeSettings settings = ModForgeSettings.getInstance();
        String authStatus = settings.isAuthenticated() ? "Authenticated" : "Not authenticated";
        statusPanel.add(new JBLabel(authStatus));
        
        // Continuous development status
        statusPanel.add(new JBLabel("Continuous Development:"));
        ContinuousDevelopmentService continuousDevelopmentService = 
                project.getService(ContinuousDevelopmentService.class);
        String cdStatus = "Disabled";
        if (continuousDevelopmentService != null) {
            cdStatus = continuousDevelopmentService.isRunning() ? "Running" : "Stopped";
        }
        statusPanel.add(new JBLabel(cdStatus));
        
        // Pattern recognition status
        statusPanel.add(new JBLabel("Pattern Recognition:"));
        PatternRecognitionService patternRecognitionService = 
                PatternRecognitionService.getInstance();
        String prStatus = "Disabled";
        if (patternRecognitionService != null) {
            prStatus = patternRecognitionService.isEnabled() ? "Enabled" : "Disabled";
        }
        statusPanel.add(new JBLabel(prStatus));
        
        // Add title
        JPanel titlePanel = new JBPanel<>(new BorderLayout());
        JBLabel title = new JBLabel("Status");
        title.setFont(title.getFont().deriveFont(Font.BOLD));
        titlePanel.add(title, BorderLayout.NORTH);
        titlePanel.add(statusPanel, BorderLayout.CENTER);
        
        panel.add(titlePanel);
    }
    
    /**
     * Add quick actions section to panel.
     * @param panel The panel to add to
     */
    private void addQuickActionsSection(JPanel panel) {
        JPanel actionsPanel = new JBPanel<>(new GridLayout(0, 1, 0, 5));
        actionsPanel.setBorder(JBUI.Borders.emptyTop(10));
        
        // Toggle continuous development
        JButton toggleCdButton = new JButton("Toggle Continuous Development");
        toggleCdButton.addActionListener(e -> {
            ContinuousDevelopmentService continuousDevelopmentService = 
                    project.getService(ContinuousDevelopmentService.class);
            if (continuousDevelopmentService != null) {
                boolean isRunning = continuousDevelopmentService.toggle();
                toggleCdButton.setText((isRunning ? "Stop" : "Start") + " Continuous Development");
            }
        });
        actionsPanel.add(toggleCdButton);
        
        // Toggle pattern recognition
        JButton togglePrButton = new JButton("Toggle Pattern Recognition");
        togglePrButton.addActionListener(e -> {
            PatternRecognitionService patternRecognitionService = 
                    PatternRecognitionService.getInstance();
            if (patternRecognitionService != null) {
                boolean isEnabled = patternRecognitionService.toggle();
                togglePrButton.setText((isEnabled ? "Disable" : "Enable") + " Pattern Recognition");
            }
        });
        actionsPanel.add(togglePrButton);
        
        // Open settings
        JButton settingsButton = new JButton("Open Settings");
        settingsButton.addActionListener(e -> {
            com.intellij.openapi.options.ShowSettingsUtil.getInstance().showSettingsDialog(
                    project, "ModForge");
        });
        actionsPanel.add(settingsButton);
        
        // Add title
        JPanel titlePanel = new JBPanel<>(new BorderLayout());
        JBLabel title = new JBLabel("Quick Actions");
        title.setFont(title.getFont().deriveFont(Font.BOLD));
        titlePanel.add(title, BorderLayout.NORTH);
        titlePanel.add(actionsPanel, BorderLayout.CENTER);
        
        panel.add(titlePanel);
    }
    
    /**
     * Add metrics section to panel.
     * @param panel The panel to add to
     */
    private void addMetricsSection(JPanel panel) {
        JPanel metricsPanel = new JBPanel<>(new GridLayout(0, 2, 10, 5));
        metricsPanel.setBorder(JBUI.Borders.emptyTop(10));
        
        // Total API requests
        metricsPanel.add(new JBLabel("Total API Requests:"));
        metricsPanel.add(new JBLabel("0"));
        
        // Pattern matches
        metricsPanel.add(new JBLabel("Pattern Matches:"));
        metricsPanel.add(new JBLabel("0"));
        
        // API calls
        metricsPanel.add(new JBLabel("API Calls:"));
        metricsPanel.add(new JBLabel("0"));
        
        // Estimated tokens saved
        metricsPanel.add(new JBLabel("Estimated Tokens Saved:"));
        metricsPanel.add(new JBLabel("0"));
        
        // Estimated cost saved
        metricsPanel.add(new JBLabel("Estimated Cost Saved:"));
        metricsPanel.add(new JBLabel("$0.00"));
        
        // Add title
        JPanel titlePanel = new JBPanel<>(new BorderLayout());
        JBLabel title = new JBLabel("Metrics");
        title.setFont(title.getFont().deriveFont(Font.BOLD));
        titlePanel.add(title, BorderLayout.NORTH);
        titlePanel.add(metricsPanel, BorderLayout.CENTER);
        
        panel.add(titlePanel);
    }
    
    /**
     * Create continuous development panel.
     * @return The continuous development panel
     */
    private JPanel createContinuousDevelopmentPanel() {
        JPanel panel = new JBPanel<>(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(10));
        
        // Add title
        JBLabel titleLabel = new JBLabel("Continuous Development");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16));
        panel.add(titleLabel, BorderLayout.NORTH);
        
        // Add content
        JPanel contentPanel = new JBPanel<>(new GridLayout(0, 1, 0, 10));
        contentPanel.setBorder(JBUI.Borders.empty(10, 0, 0, 0));
        
        // Settings
        addContinuousDevelopmentSettingsSection(contentPanel);
        
        // Status
        addContinuousDevelopmentStatusSection(contentPanel);
        
        // Wrap in scroll pane
        JBScrollPane scrollPane = new JBScrollPane(contentPanel);
        scrollPane.setBorder(JBUI.Borders.empty());
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Add continuous development settings section to panel.
     * @param panel The panel to add to
     */
    private void addContinuousDevelopmentSettingsSection(JPanel panel) {
        JPanel settingsPanel = new JBPanel<>(new GridLayout(0, 2, 10, 5));
        settingsPanel.setBorder(JBUI.Borders.emptyTop(10));
        
        // Enable continuous development
        settingsPanel.add(new JBLabel("Enable Continuous Development:"));
        JCheckBox enableCdCheckBox = new JCheckBox();
        ModForgeSettings settings = ModForgeSettings.getInstance();
        enableCdCheckBox.setSelected(settings.isEnableContinuousDevelopment());
        enableCdCheckBox.addActionListener(e -> {
            settings.setEnableContinuousDevelopment(enableCdCheckBox.isSelected());
        });
        settingsPanel.add(enableCdCheckBox);
        
        // Frequency
        settingsPanel.add(new JBLabel("Frequency (minutes):"));
        JSpinner frequencySpinner = new JSpinner(new SpinnerNumberModel(
                settings.getContinuousDevelopmentFrequency(), 1, 60, 1));
        frequencySpinner.addChangeListener(e -> {
            settings.setContinuousDevelopmentFrequency((Integer) frequencySpinner.getValue());
        });
        settingsPanel.add(frequencySpinner);
        
        // Add title
        JPanel titlePanel = new JBPanel<>(new BorderLayout());
        JBLabel title = new JBLabel("Settings");
        title.setFont(title.getFont().deriveFont(Font.BOLD));
        titlePanel.add(title, BorderLayout.NORTH);
        titlePanel.add(settingsPanel, BorderLayout.CENTER);
        
        panel.add(titlePanel);
    }
    
    /**
     * Add continuous development status section to panel.
     * @param panel The panel to add to
     */
    private void addContinuousDevelopmentStatusSection(JPanel panel) {
        JPanel statusPanel = new JBPanel<>(new GridLayout(0, 2, 10, 5));
        statusPanel.setBorder(JBUI.Borders.emptyTop(10));
        
        // Status
        statusPanel.add(new JBLabel("Status:"));
        ContinuousDevelopmentService continuousDevelopmentService = 
                project.getService(ContinuousDevelopmentService.class);
        String status = "Disabled";
        if (continuousDevelopmentService != null) {
            status = continuousDevelopmentService.isRunning() ? "Running" : "Stopped";
        }
        statusPanel.add(new JBLabel(status));
        
        // Last run
        statusPanel.add(new JBLabel("Last Run:"));
        statusPanel.add(new JBLabel("Never"));
        
        // Next run
        statusPanel.add(new JBLabel("Next Run:"));
        statusPanel.add(new JBLabel("N/A"));
        
        // Add title
        JPanel titlePanel = new JBPanel<>(new BorderLayout());
        JBLabel title = new JBLabel("Status");
        title.setFont(title.getFont().deriveFont(Font.BOLD));
        titlePanel.add(title, BorderLayout.NORTH);
        titlePanel.add(statusPanel, BorderLayout.CENTER);
        
        panel.add(titlePanel);
    }
    
    /**
     * Create code generation panel.
     * @return The code generation panel
     */
    private JPanel createCodeGenerationPanel() {
        JPanel panel = new JBPanel<>(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(10));
        
        // Add title
        JBLabel titleLabel = new JBLabel("AI Code Generation");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16));
        panel.add(titleLabel, BorderLayout.NORTH);
        
        // Add content
        JPanel contentPanel = new JBPanel<>(new GridLayout(0, 1, 0, 10));
        contentPanel.setBorder(JBUI.Borders.empty(10, 0, 0, 0));
        
        // Settings
        addCodeGenerationSettingsSection(contentPanel);
        
        // Generate code form
        addGenerateCodeSection(contentPanel);
        
        // Fix code form
        addFixCodeSection(contentPanel);
        
        // Wrap in scroll pane
        JBScrollPane scrollPane = new JBScrollPane(contentPanel);
        scrollPane.setBorder(JBUI.Borders.empty());
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Add code generation settings section to panel.
     * @param panel The panel to add to
     */
    private void addCodeGenerationSettingsSection(JPanel panel) {
        JPanel settingsPanel = new JBPanel<>(new GridLayout(0, 2, 10, 5));
        settingsPanel.setBorder(JBUI.Borders.emptyTop(10));
        
        // Enable AI generation
        settingsPanel.add(new JBLabel("Enable AI Generation:"));
        JCheckBox enableAiCheckBox = new JCheckBox();
        ModForgeSettings settings = ModForgeSettings.getInstance();
        enableAiCheckBox.setSelected(settings.isEnableAIGeneration());
        enableAiCheckBox.addActionListener(e -> {
            settings.setEnableAIGeneration(enableAiCheckBox.isSelected());
        });
        settingsPanel.add(enableAiCheckBox);
        
        // Add title
        JPanel titlePanel = new JBPanel<>(new BorderLayout());
        JBLabel title = new JBLabel("Settings");
        title.setFont(title.getFont().deriveFont(Font.BOLD));
        titlePanel.add(title, BorderLayout.NORTH);
        titlePanel.add(settingsPanel, BorderLayout.CENTER);
        
        panel.add(titlePanel);
    }
    
    /**
     * Add generate code section to panel.
     * @param panel The panel to add to
     */
    private void addGenerateCodeSection(JPanel panel) {
        JPanel generatePanel = new JBPanel<>(new BorderLayout());
        generatePanel.setBorder(JBUI.Borders.emptyTop(10));
        
        // Form
        JPanel formPanel = new JBPanel<>(new GridLayout(0, 1, 0, 5));
        
        // Prompt
        formPanel.add(new JBLabel("Prompt:"));
        JTextArea promptTextArea = new JTextArea(5, 0);
        promptTextArea.setLineWrap(true);
        promptTextArea.setWrapStyleWord(true);
        JBScrollPane promptScrollPane = new JBScrollPane(promptTextArea);
        formPanel.add(promptScrollPane);
        
        // Generate button
        JButton generateButton = new JButton("Generate Code");
        generateButton.addActionListener(e -> {
            String prompt = promptTextArea.getText();
            if (prompt.isEmpty()) {
                return;
            }
            
            AutonomousCodeGenerationService codeGenerationService = 
                    project.getService(AutonomousCodeGenerationService.class);
            if (codeGenerationService != null) {
                String code = codeGenerationService.generateCode(prompt);
                if (code != null) {
                    // TODO: Show generated code
                }
            }
        });
        formPanel.add(generateButton);
        
        // Add title
        JBLabel title = new JBLabel("Generate Code");
        title.setFont(title.getFont().deriveFont(Font.BOLD));
        generatePanel.add(title, BorderLayout.NORTH);
        generatePanel.add(formPanel, BorderLayout.CENTER);
        
        panel.add(generatePanel);
    }
    
    /**
     * Add fix code section to panel.
     * @param panel The panel to add to
     */
    private void addFixCodeSection(JPanel panel) {
        JPanel fixPanel = new JBPanel<>(new BorderLayout());
        fixPanel.setBorder(JBUI.Borders.emptyTop(10));
        
        // Form
        JPanel formPanel = new JBPanel<>(new GridLayout(0, 1, 0, 5));
        
        // Code
        formPanel.add(new JBLabel("Code:"));
        JTextArea codeTextArea = new JTextArea(5, 0);
        codeTextArea.setLineWrap(true);
        codeTextArea.setWrapStyleWord(true);
        JBScrollPane codeScrollPane = new JBScrollPane(codeTextArea);
        formPanel.add(codeScrollPane);
        
        // Error message
        formPanel.add(new JBLabel("Error Message:"));
        JTextArea errorTextArea = new JTextArea(3, 0);
        errorTextArea.setLineWrap(true);
        errorTextArea.setWrapStyleWord(true);
        JBScrollPane errorScrollPane = new JBScrollPane(errorTextArea);
        formPanel.add(errorScrollPane);
        
        // Fix button
        JButton fixButton = new JButton("Fix Code");
        fixButton.addActionListener(e -> {
            String code = codeTextArea.getText();
            String errorMessage = errorTextArea.getText();
            if (code.isEmpty()) {
                return;
            }
            
            AutonomousCodeGenerationService codeGenerationService = 
                    project.getService(AutonomousCodeGenerationService.class);
            if (codeGenerationService != null) {
                String fixedCode = codeGenerationService.fixCode(code, errorMessage);
                if (fixedCode != null) {
                    // TODO: Show fixed code
                }
            }
        });
        formPanel.add(fixButton);
        
        // Add title
        JBLabel title = new JBLabel("Fix Code");
        title.setFont(title.getFont().deriveFont(Font.BOLD));
        fixPanel.add(title, BorderLayout.NORTH);
        fixPanel.add(formPanel, BorderLayout.CENTER);
        
        panel.add(fixPanel);
    }
    
    /**
     * Create pattern recognition panel.
     * @return The pattern recognition panel
     */
    private JPanel createPatternRecognitionPanel() {
        JPanel panel = new JBPanel<>(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(10));
        
        // Add title
        JBLabel titleLabel = new JBLabel("Pattern Recognition");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16));
        panel.add(titleLabel, BorderLayout.NORTH);
        
        // Add content
        JPanel contentPanel = new JBPanel<>(new GridLayout(0, 1, 0, 10));
        contentPanel.setBorder(JBUI.Borders.empty(10, 0, 0, 0));
        
        // Settings
        addPatternRecognitionSettingsSection(contentPanel);
        
        // Status
        addPatternRecognitionStatusSection(contentPanel);
        
        // Wrap in scroll pane
        JBScrollPane scrollPane = new JBScrollPane(contentPanel);
        scrollPane.setBorder(JBUI.Borders.empty());
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Add pattern recognition settings section to panel.
     * @param panel The panel to add to
     */
    private void addPatternRecognitionSettingsSection(JPanel panel) {
        JPanel settingsPanel = new JBPanel<>(new GridLayout(0, 2, 10, 5));
        settingsPanel.setBorder(JBUI.Borders.emptyTop(10));
        
        // Enable pattern recognition
        settingsPanel.add(new JBLabel("Use Pattern Learning:"));
        JCheckBox enablePrCheckBox = new JCheckBox();
        ModForgeSettings settings = ModForgeSettings.getInstance();
        enablePrCheckBox.setSelected(settings.isUsePatternLearning());
        enablePrCheckBox.addActionListener(e -> {
            settings.setUsePatternLearning(enablePrCheckBox.isSelected());
            
            // Update service
            PatternRecognitionService patternRecognitionService = 
                    PatternRecognitionService.getInstance();
            if (patternRecognitionService != null) {
                patternRecognitionService.setEnabled(enablePrCheckBox.isSelected());
            }
        });
        settingsPanel.add(enablePrCheckBox);
        
        // Add title
        JPanel titlePanel = new JBPanel<>(new BorderLayout());
        JBLabel title = new JBLabel("Settings");
        title.setFont(title.getFont().deriveFont(Font.BOLD));
        titlePanel.add(title, BorderLayout.NORTH);
        titlePanel.add(settingsPanel, BorderLayout.CENTER);
        
        panel.add(titlePanel);
    }
    
    /**
     * Add pattern recognition status section to panel.
     * @param panel The panel to add to
     */
    private void addPatternRecognitionStatusSection(JPanel panel) {
        JPanel statusPanel = new JBPanel<>(new GridLayout(0, 2, 10, 5));
        statusPanel.setBorder(JBUI.Borders.emptyTop(10));
        
        // Status
        statusPanel.add(new JBLabel("Status:"));
        PatternRecognitionService patternRecognitionService = 
                PatternRecognitionService.getInstance();
        String status = "Disabled";
        if (patternRecognitionService != null) {
            status = patternRecognitionService.isEnabled() ? "Enabled" : "Disabled";
        }
        statusPanel.add(new JBLabel(status));
        
        // Total patterns
        statusPanel.add(new JBLabel("Total Patterns:"));
        statusPanel.add(new JBLabel("0"));
        
        // Pattern matches
        statusPanel.add(new JBLabel("Pattern Matches:"));
        statusPanel.add(new JBLabel("0"));
        
        // Add title
        JPanel titlePanel = new JBPanel<>(new BorderLayout());
        JBLabel title = new JBLabel("Status");
        title.setFont(title.getFont().deriveFont(Font.BOLD));
        titlePanel.add(title, BorderLayout.NORTH);
        titlePanel.add(statusPanel, BorderLayout.CENTER);
        
        panel.add(titlePanel);
    }
    
    /**
     * Create GitHub integration panel.
     * @return The GitHub integration panel
     */
    private JPanel createGitHubIntegrationPanel() {
        JPanel panel = new JBPanel<>(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(10));
        
        // Add title
        JBLabel titleLabel = new JBLabel("GitHub Integration");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16));
        panel.add(titleLabel, BorderLayout.NORTH);
        
        // Add content
        JPanel contentPanel = new JBPanel<>(new GridLayout(0, 1, 0, 10));
        contentPanel.setBorder(JBUI.Borders.empty(10, 0, 0, 0));
        
        // Settings
        addGitHubSettingsSection(contentPanel);
        
        // Repository
        addGitHubRepositorySection(contentPanel);
        
        // Wrap in scroll pane
        JBScrollPane scrollPane = new JBScrollPane(contentPanel);
        scrollPane.setBorder(JBUI.Borders.empty());
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Add GitHub settings section to panel.
     * @param panel The panel to add to
     */
    private void addGitHubSettingsSection(JPanel panel) {
        JPanel settingsPanel = new JBPanel<>(new GridLayout(0, 2, 10, 5));
        settingsPanel.setBorder(JBUI.Borders.emptyTop(10));
        
        // GitHub username
        settingsPanel.add(new JBLabel("GitHub Username:"));
        JBTextField usernameTextField = new JBTextField();
        ModForgeSettings settings = ModForgeSettings.getInstance();
        usernameTextField.setText(settings.getGithubUsername());
        usernameTextField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                settings.setGithubUsername(usernameTextField.getText());
            }
            
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                settings.setGithubUsername(usernameTextField.getText());
            }
            
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                settings.setGithubUsername(usernameTextField.getText());
            }
        });
        settingsPanel.add(usernameTextField);
        
        // GitHub token
        settingsPanel.add(new JBLabel("GitHub Token:"));
        JBTextField tokenTextField = new JBTextField();
        tokenTextField.setText(settings.getGithubToken());
        tokenTextField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                settings.setGithubToken(tokenTextField.getText());
            }
            
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                settings.setGithubToken(tokenTextField.getText());
            }
            
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                settings.setGithubToken(tokenTextField.getText());
            }
        });
        settingsPanel.add(tokenTextField);
        
        // Test button
        settingsPanel.add(new JBLabel(""));
        JButton testButton = new JButton("Test GitHub Connection");
        testButton.addActionListener(e -> {
            // TODO: Implement GitHub connection test
        });
        settingsPanel.add(testButton);
        
        // Add title
        JPanel titlePanel = new JBPanel<>(new BorderLayout());
        JBLabel title = new JBLabel("Settings");
        title.setFont(title.getFont().deriveFont(Font.BOLD));
        titlePanel.add(title, BorderLayout.NORTH);
        titlePanel.add(settingsPanel, BorderLayout.CENTER);
        
        panel.add(titlePanel);
    }
    
    /**
     * Add GitHub repository section to panel.
     * @param panel The panel to add to
     */
    private void addGitHubRepositorySection(JPanel panel) {
        JPanel repoPanel = new JBPanel<>(new GridLayout(0, 2, 10, 5));
        repoPanel.setBorder(JBUI.Borders.emptyTop(10));
        
        // Repository owner
        repoPanel.add(new JBLabel("Repository Owner:"));
        JBTextField ownerTextField = new JBTextField();
        repoPanel.add(ownerTextField);
        
        // Repository name
        repoPanel.add(new JBLabel("Repository Name:"));
        JBTextField nameTextField = new JBTextField();
        repoPanel.add(nameTextField);
        
        // Repository branch
        repoPanel.add(new JBLabel("Repository Branch:"));
        JBTextField branchTextField = new JBTextField();
        branchTextField.setText("main");
        repoPanel.add(branchTextField);
        
        // Push button
        repoPanel.add(new JBLabel(""));
        JButton pushButton = new JButton("Push to GitHub");
        pushButton.addActionListener(e -> {
            // TODO: Implement push to GitHub
        });
        repoPanel.add(pushButton);
        
        // Add title
        JPanel titlePanel = new JBPanel<>(new BorderLayout());
        JBLabel title = new JBLabel("Repository");
        title.setFont(title.getFont().deriveFont(Font.BOLD));
        titlePanel.add(title, BorderLayout.NORTH);
        titlePanel.add(repoPanel, BorderLayout.CENTER);
        
        panel.add(titlePanel);
    }
    
    /**
     * Get the content.
     * @return The content panel
     */
    public JPanel getContent() {
        return mainPanel;
    }
}