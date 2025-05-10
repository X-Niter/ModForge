package com.modforge.intellij.plugin.ui.toolwindow;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.modforge.intellij.plugin.ai.PatternRecognitionService;
import com.modforge.intellij.plugin.auth.ModAuthenticationManager;
import com.modforge.intellij.plugin.services.AutonomousCodeGenerationService;
import com.modforge.intellij.plugin.services.ContinuousDevelopmentService;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.json.simple.JSONObject;

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
    private Timer refreshTimer;
    
    // UI components
    private JLabel authStatusLabel;
    private JLabel usernameLabel;
    private JLabel continuousDevelopmentStatusLabel;
    private JLabel patternRecognitionStatusLabel;
    private JLabel taskCountLabel;
    private JLabel patternMatchesLabel;
    private JLabel apiFallbacksLabel;
    private JLabel patternSuccessRateLabel;
    private JLabel uniquePatternsLabel;
    
    public ModForgeToolWindowContent(Project project, ToolWindow toolWindow) {
        this.project = project;
        this.toolWindow = toolWindow;
        
        createUI();
        startRefreshTimer();
    }
    
    /**
     * Create UI components.
     */
    private void createUI() {
        mainPanel = new JPanel(new BorderLayout());
        
        // Create tabs
        JBTabbedPane tabbedPane = new JBTabbedPane();
        
        // Dashboard tab
        JPanel dashboardPanel = createDashboardPanel();
        tabbedPane.addTab("Dashboard", dashboardPanel);
        
        // Development tab
        JPanel developmentPanel = createDevelopmentPanel();
        tabbedPane.addTab("Development", developmentPanel);
        
        // Patterns tab
        JPanel patternsPanel = createPatternsPanel();
        tabbedPane.addTab("Patterns", patternsPanel);
        
        // Add tabs to main panel
        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        
        // Initial update
        updateUI();
    }
    
    /**
     * Create dashboard panel.
     * @return Dashboard panel
     */
    private JPanel createDashboardPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(JBUI.Borders.empty(10));
        
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.insets = JBUI.insets(5);
        
        // Authentication status
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        panel.add(new JBLabel("Authentication Status:"), c);
        
        c.gridx = 1;
        c.gridy = 0;
        authStatusLabel = new JLabel("Unknown");
        panel.add(authStatusLabel, c);
        
        // Username
        c.gridx = 0;
        c.gridy = 1;
        panel.add(new JBLabel("Username:"), c);
        
        c.gridx = 1;
        c.gridy = 1;
        usernameLabel = new JLabel("Not logged in");
        panel.add(usernameLabel, c);
        
        // Continuous development status
        c.gridx = 0;
        c.gridy = 2;
        panel.add(new JBLabel("Continuous Development:"), c);
        
        c.gridx = 1;
        c.gridy = 2;
        continuousDevelopmentStatusLabel = new JLabel("Disabled");
        panel.add(continuousDevelopmentStatusLabel, c);
        
        // Pattern recognition status
        c.gridx = 0;
        c.gridy = 3;
        panel.add(new JBLabel("Pattern Recognition:"), c);
        
        c.gridx = 1;
        c.gridy = 3;
        patternRecognitionStatusLabel = new JLabel("Disabled");
        panel.add(patternRecognitionStatusLabel, c);
        
        // Spacer
        c.gridx = 0;
        c.gridy = 4;
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
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(JBUI.Borders.empty(10));
        
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.insets = JBUI.insets(5);
        
        // Task count
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        panel.add(new JBLabel("Tasks Executed:"), c);
        
        c.gridx = 1;
        c.gridy = 0;
        taskCountLabel = new JLabel("0");
        panel.add(taskCountLabel, c);
        
        // Spacer
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 2;
        c.weighty = 1.0;
        panel.add(Box.createVerticalGlue(), c);
        
        return panel;
    }
    
    /**
     * Create patterns panel.
     * @return Patterns panel
     */
    private JPanel createPatternsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(JBUI.Borders.empty(10));
        
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.insets = JBUI.insets(5);
        
        // Pattern matches
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        panel.add(new JBLabel("Pattern Matches:"), c);
        
        c.gridx = 1;
        c.gridy = 0;
        patternMatchesLabel = new JLabel("0");
        panel.add(patternMatchesLabel, c);
        
        // API fallbacks
        c.gridx = 0;
        c.gridy = 1;
        panel.add(new JBLabel("API Fallbacks:"), c);
        
        c.gridx = 1;
        c.gridy = 1;
        apiFallbacksLabel = new JLabel("0");
        panel.add(apiFallbacksLabel, c);
        
        // Pattern success rate
        c.gridx = 0;
        c.gridy = 2;
        panel.add(new JBLabel("Pattern Success Rate:"), c);
        
        c.gridx = 1;
        c.gridy = 2;
        patternSuccessRateLabel = new JLabel("0%");
        panel.add(patternSuccessRateLabel, c);
        
        // Unique patterns
        c.gridx = 0;
        c.gridy = 3;
        panel.add(new JBLabel("Unique Patterns:"), c);
        
        c.gridx = 1;
        c.gridy = 3;
        uniquePatternsLabel = new JLabel("0");
        panel.add(uniquePatternsLabel, c);
        
        // Spacer
        c.gridx = 0;
        c.gridy = 4;
        c.gridwidth = 2;
        c.weighty = 1.0;
        panel.add(Box.createVerticalGlue(), c);
        
        return panel;
    }
    
    /**
     * Start refresh timer.
     */
    private void startRefreshTimer() {
        // Create timer that refreshes UI every 5 seconds
        refreshTimer = new Timer(5000, e -> updateUI());
        refreshTimer.start();
    }
    
    /**
     * Update UI with current data.
     */
    private void updateUI() {
        // Update authentication status
        ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
        boolean isAuthenticated = authManager.isAuthenticated();
        
        // Authentication status
        if (isAuthenticated) {
            authStatusLabel.setText("Authenticated");
            authStatusLabel.setForeground(UIUtil.getContextHelpForeground());
            
            // Verify authentication
            boolean isValid = authManager.verifyAuthentication();
            
            if (isValid) {
                authStatusLabel.setText("Authenticated (Verified)");
                authStatusLabel.setForeground(new Color(0, 150, 0)); // Green
            } else {
                authStatusLabel.setText("Authenticated (Invalid)");
                authStatusLabel.setForeground(new Color(200, 0, 0)); // Red
            }
            
            // Username
            String username = authManager.getUsername();
            if (username != null) {
                usernameLabel.setText(username);
            } else {
                // Try to get user data
                JSONObject userData = authManager.getUserData();
                
                if (userData != null && userData.containsKey("username")) {
                    usernameLabel.setText((String) userData.get("username"));
                } else {
                    usernameLabel.setText("Unknown");
                }
            }
        } else {
            authStatusLabel.setText("Not authenticated");
            authStatusLabel.setForeground(new Color(200, 0, 0)); // Red
            
            usernameLabel.setText("Not logged in");
        }
        
        // Update continuous development status
        ModForgeSettings settings = ModForgeSettings.getInstance();
        boolean continuousDevelopment = settings.isContinuousDevelopment();
        
        if (continuousDevelopment) {
            // Check if it's actually running
            ContinuousDevelopmentService service = project.getService(ContinuousDevelopmentService.class);
            
            if (service.isRunning()) {
                continuousDevelopmentStatusLabel.setText("Enabled (Running)");
                continuousDevelopmentStatusLabel.setForeground(new Color(0, 150, 0)); // Green
            } else {
                continuousDevelopmentStatusLabel.setText("Enabled (Not Running)");
                continuousDevelopmentStatusLabel.setForeground(new Color(200, 130, 0)); // Orange
            }
        } else {
            continuousDevelopmentStatusLabel.setText("Disabled");
            continuousDevelopmentStatusLabel.setForeground(UIUtil.getContextHelpForeground());
        }
        
        // Update pattern recognition status
        boolean patternRecognition = settings.isPatternRecognition();
        
        if (patternRecognition) {
            patternRecognitionStatusLabel.setText("Enabled");
            patternRecognitionStatusLabel.setForeground(new Color(0, 150, 0)); // Green
        } else {
            patternRecognitionStatusLabel.setText("Disabled");
            patternRecognitionStatusLabel.setForeground(UIUtil.getContextHelpForeground());
        }
        
        // Update development metrics
        ContinuousDevelopmentService devService = project.getService(ContinuousDevelopmentService.class);
        taskCountLabel.setText(String.valueOf(devService.getTaskCount()));
        
        // Update pattern metrics
        AutonomousCodeGenerationService autoService = project.getService(AutonomousCodeGenerationService.class);
        patternMatchesLabel.setText(String.valueOf(autoService.getSuccessfulPatternMatches()));
        apiFallbacksLabel.setText(String.valueOf(autoService.getTotalApiFallbacks()));
        
        double successRate = autoService.getPatternSuccessRate();
        String successRateStr = String.format("%.1f%%", successRate * 100);
        patternSuccessRateLabel.setText(successRateStr);
        
        // Update unique patterns
        PatternRecognitionService patternService = project.getService(PatternRecognitionService.class);
        uniquePatternsLabel.setText(String.valueOf(patternService.getUniquePatternCount()));
    }
    
    /**
     * Get the main panel.
     * @return Main panel
     */
    public JPanel getContent() {
        return mainPanel;
    }
    
    /**
     * Dispose resources.
     */
    public void dispose() {
        if (refreshTimer != null) {
            refreshTimer.stop();
            refreshTimer = null;
        }
    }
}