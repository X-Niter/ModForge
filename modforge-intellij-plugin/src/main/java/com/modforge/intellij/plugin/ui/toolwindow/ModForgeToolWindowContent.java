package com.modforge.intellij.plugin.ui.toolwindow;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
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
import java.text.NumberFormat;

/**
 * Content for ModForge tool window.
 */
public class ModForgeToolWindowContent {
    private static final Logger LOG = Logger.getInstance(ModForgeToolWindowContent.class);
    
    private final JPanel mainPanel;
    private final Project project;
    
    // Status labels
    private final JLabel authStatusLabel = new JLabel("Not authenticated");
    private final JLabel usernameLabel = new JLabel("Not logged in");
    private final JLabel continuousDevelopmentLabel = new JLabel("Disabled");
    private final JLabel patternRecognitionLabel = new JLabel("Disabled");
    
    // Metrics labels
    private final JLabel taskCountLabel = new JLabel("0");
    private final JLabel patternMatchesLabel = new JLabel("0");
    private final JLabel apiFallbacksLabel = new JLabel("0");
    private final JLabel costSavingsLabel = new JLabel("$0.00");
    
    // Refresh timer
    private Timer refreshTimer;
    
    /**
     * Create ModForge tool window content.
     * @param project Project
     * @param toolWindow Tool window
     */
    public ModForgeToolWindowContent(Project project, ToolWindow toolWindow) {
        this.project = project;
        
        // Create main panel
        mainPanel = new SimpleToolWindowPanel(true, true);
        
        // Create and add content
        JBTabbedPane tabbedPane = new JBTabbedPane();
        tabbedPane.addTab("Status", createStatusPanel());
        tabbedPane.addTab("Metrics", createMetricsPanel());
        
        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        
        // Initial UI update
        updateUI();
        
        // Set up refresh timer (5 seconds)
        refreshTimer = new Timer(5000, e -> updateUI());
        refreshTimer.start();
    }
    
    /**
     * Update UI with current data.
     */
    private void updateUI() {
        try {
            // Update authentication status
            ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
            boolean isAuthenticated = false;
            
            if (authManager != null) {
                isAuthenticated = authManager.isAuthenticated();
            }
            
            // Authentication status
            if (isAuthenticated) {
                authStatusLabel.setText("Authenticated");
                authStatusLabel.setForeground(UIUtil.getContextHelpForeground());
                
                // Verify authentication
                boolean isValid = false;
                
                try {
                    isValid = authManager.verifyAuthentication();
                } catch (Exception e) {
                    LOG.error("Error verifying authentication", e);
                }
                
                if (isValid) {
                    authStatusLabel.setText("Authenticated (Verified)");
                    authStatusLabel.setForeground(new Color(0, 150, 0)); // Green
                } else {
                    authStatusLabel.setText("Authenticated (Invalid)");
                    authStatusLabel.setForeground(new Color(200, 0, 0)); // Red
                }
                
                // Username
                String username = null;
                try {
                    username = authManager.getUsername();
                } catch (Exception e) {
                    LOG.error("Error getting username", e);
                }
                
                if (username != null) {
                    usernameLabel.setText(username);
                } else {
                    // Try to get user data
                    JSONObject userData = null;
                    try {
                        userData = authManager.getUserData();
                    } catch (Exception e) {
                        LOG.error("Error getting user data", e);
                    }
                    
                    if (userData != null && userData.containsKey("username")) {
                        Object usernameObj = userData.get("username");
                        if (usernameObj instanceof String) {
                            usernameLabel.setText((String) usernameObj);
                        } else {
                            usernameLabel.setText("Unknown");
                        }
                    } else {
                        usernameLabel.setText("Unknown");
                    }
                }
            } else {
                authStatusLabel.setText("Not authenticated");
                authStatusLabel.setForeground(new Color(200, 0, 0)); // Red
                
                usernameLabel.setText("Not logged in");
            }
        } catch (Exception e) {
            LOG.error("Error updating authentication status", e);
            
            // Set safe defaults
            authStatusLabel.setText("Error checking authentication");
            authStatusLabel.setForeground(new Color(200, 0, 0)); // Red
            usernameLabel.setText("Unknown");
        }
        
        // Update continuous development status
        ModForgeSettings settings = ModForgeSettings.getInstance();
        boolean continuousDevelopment = settings.isContinuousDevelopment();
        
        if (continuousDevelopment) {
            continuousDevelopmentLabel.setText("Enabled");
            continuousDevelopmentLabel.setForeground(new Color(0, 150, 0)); // Green
        } else {
            continuousDevelopmentLabel.setText("Disabled");
            continuousDevelopmentLabel.setForeground(new Color(200, 0, 0)); // Red
        }
        
        // Update pattern recognition status
        boolean patternRecognition = settings.isPatternRecognition();
        
        if (patternRecognition) {
            patternRecognitionLabel.setText("Enabled");
            patternRecognitionLabel.setForeground(new Color(0, 150, 0)); // Green
        } else {
            patternRecognitionLabel.setText("Disabled");
            patternRecognitionLabel.setForeground(new Color(200, 0, 0)); // Red
        }
        
        // Update metrics (if services are available)
        try {
            // Update continuous development metrics
            ContinuousDevelopmentService continuousService = project.getService(ContinuousDevelopmentService.class);
            if (continuousService != null) {
                taskCountLabel.setText(String.valueOf(continuousService.getTaskCount()));
            }
            
            // Update pattern recognition metrics
            AutonomousCodeGenerationService autonomousService = project.getService(AutonomousCodeGenerationService.class);
            if (autonomousService != null) {
                int patternMatches = autonomousService.getSuccessfulPatternMatches();
                int apiFallbacks = autonomousService.getApiFallbacks();
                
                patternMatchesLabel.setText(String.valueOf(patternMatches));
                apiFallbacksLabel.setText(String.valueOf(apiFallbacks));
                
                // Calculate estimated cost savings (approximate)
                double estimatedCostPerRequest = 0.01; // $0.01 per API request saved
                double costSavings = patternMatches * estimatedCostPerRequest;
                
                NumberFormat currencyFormat = NumberFormat.getCurrencyInstance();
                costSavingsLabel.setText(currencyFormat.format(costSavings));
            }
        } catch (Exception e) {
            LOG.error("Error updating metrics", e);
        }
    }
    
    /**
     * Create status panel.
     * @return Status panel
     */
    private JPanel createStatusPanel() {
        JPanel panel = new JBPanel<>(new GridBagLayout());
        panel.setBorder(JBUI.Borders.empty(10));
        
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = JBUI.insets(5);
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0.3;
        
        // Authentication section
        JLabel authSectionLabel = new JBLabel("Authentication");
        authSectionLabel.setFont(authSectionLabel.getFont().deriveFont(Font.BOLD));
        panel.add(authSectionLabel, c);
        
        c.gridx = 1;
        c.weightx = 0.7;
        panel.add(authStatusLabel, c);
        
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0.3;
        panel.add(new JBLabel("Username:"), c);
        
        c.gridx = 1;
        c.weightx = 0.7;
        panel.add(usernameLabel, c);
        
        // Features section
        c.gridx = 0;
        c.gridy++;
        c.weightx = 1.0;
        c.gridwidth = 2;
        c.insets = JBUI.insets(15, 5, 5, 5);
        
        JLabel featuresSectionLabel = new JBLabel("Features");
        featuresSectionLabel.setFont(featuresSectionLabel.getFont().deriveFont(Font.BOLD));
        panel.add(featuresSectionLabel, c);
        
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0.3;
        c.gridwidth = 1;
        c.insets = JBUI.insets(5);
        panel.add(new JBLabel("Continuous Development:"), c);
        
        c.gridx = 1;
        c.weightx = 0.7;
        panel.add(continuousDevelopmentLabel, c);
        
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0.3;
        panel.add(new JBLabel("Pattern Recognition:"), c);
        
        c.gridx = 1;
        c.weightx = 0.7;
        panel.add(patternRecognitionLabel, c);
        
        // Add filler to push content to top
        c.gridx = 0;
        c.gridy++;
        c.weighty = 1.0;
        c.gridwidth = 2;
        panel.add(new JPanel(), c);
        
        return panel;
    }
    
    /**
     * Create metrics panel.
     * @return Metrics panel
     */
    private JPanel createMetricsPanel() {
        JPanel panel = new JBPanel<>(new GridBagLayout());
        panel.setBorder(JBUI.Borders.empty(10));
        
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = JBUI.insets(5);
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0.3;
        
        // Usage metrics section
        JLabel metricsSectionLabel = new JBLabel("Usage Metrics");
        metricsSectionLabel.setFont(metricsSectionLabel.getFont().deriveFont(Font.BOLD));
        panel.add(metricsSectionLabel, c);
        
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0.3;
        panel.add(new JBLabel("Total Tasks Run:"), c);
        
        c.gridx = 1;
        c.weightx = 0.7;
        taskCountLabel.setFont(taskCountLabel.getFont().deriveFont(Font.BOLD));
        panel.add(taskCountLabel, c);
        
        // Pattern recognition metrics section
        c.gridx = 0;
        c.gridy++;
        c.weightx = 1.0;
        c.gridwidth = 2;
        c.insets = JBUI.insets(15, 5, 5, 5);
        
        JLabel patternSectionLabel = new JBLabel("Pattern Recognition Metrics");
        patternSectionLabel.setFont(patternSectionLabel.getFont().deriveFont(Font.BOLD));
        panel.add(patternSectionLabel, c);
        
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0.3;
        c.gridwidth = 1;
        c.insets = JBUI.insets(5);
        panel.add(new JBLabel("Pattern Matches:"), c);
        
        c.gridx = 1;
        c.weightx = 0.7;
        patternMatchesLabel.setFont(patternMatchesLabel.getFont().deriveFont(Font.BOLD));
        patternMatchesLabel.setForeground(new Color(0, 150, 0)); // Green
        panel.add(patternMatchesLabel, c);
        
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0.3;
        panel.add(new JBLabel("API Fallbacks:"), c);
        
        c.gridx = 1;
        c.weightx = 0.7;
        apiFallbacksLabel.setFont(apiFallbacksLabel.getFont().deriveFont(Font.BOLD));
        panel.add(apiFallbacksLabel, c);
        
        // Cost savings section
        c.gridx = 0;
        c.gridy++;
        c.weightx = 1.0;
        c.gridwidth = 2;
        c.insets = JBUI.insets(15, 5, 5, 5);
        
        JLabel savingsSectionLabel = new JBLabel("Estimated Cost Savings");
        savingsSectionLabel.setFont(savingsSectionLabel.getFont().deriveFont(Font.BOLD));
        panel.add(savingsSectionLabel, c);
        
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0.3;
        c.gridwidth = 1;
        c.insets = JBUI.insets(5);
        panel.add(new JBLabel("API Cost Savings:"), c);
        
        c.gridx = 1;
        c.weightx = 0.7;
        costSavingsLabel.setFont(costSavingsLabel.getFont().deriveFont(Font.BOLD));
        costSavingsLabel.setForeground(new Color(0, 100, 0)); // Dark green
        panel.add(costSavingsLabel, c);
        
        // Add filler to push content to top
        c.gridx = 0;
        c.gridy++;
        c.weighty = 1.0;
        c.gridwidth = 2;
        panel.add(new JPanel(), c);
        
        return panel;
    }
    
    /**
     * Get component for display in tool window.
     * @return Component
     */
    public JComponent getComponent() {
        return mainPanel;
    }
    
    /**
     * Dispose of resources.
     */
    public void dispose() {
        if (refreshTimer != null) {
            refreshTimer.stop();
            refreshTimer = null;
        }
    }
}