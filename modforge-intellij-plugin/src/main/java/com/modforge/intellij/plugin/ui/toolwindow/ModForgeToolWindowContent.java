package com.modforge.intellij.plugin.ui.toolwindow;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.modforge.intellij.plugin.actions.LoginAction;
import com.modforge.intellij.plugin.actions.LogoutAction;
import com.modforge.intellij.plugin.actions.ToggleContinuousDevelopmentAction;
import com.modforge.intellij.plugin.actions.TogglePatternRecognitionAction;
import com.modforge.intellij.plugin.auth.ModAuthenticationManager;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import com.modforge.intellij.plugin.utils.ConnectionTestUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * Content for the ModForge tool window.
 */
public class ModForgeToolWindowContent {
    private static final Logger LOG = Logger.getInstance(ModForgeToolWindowContent.class);
    
    private final Project project;
    private final ToolWindow toolWindow;
    private final JPanel contentPanel;
    
    /**
     * Construct the tool window content.
     *
     * @param project    The project
     * @param toolWindow The tool window
     */
    public ModForgeToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        this.project = project;
        this.toolWindow = toolWindow;
        
        // Create content panel
        contentPanel = new JBPanel<>(new BorderLayout());
        
        // Create toolbar
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        actionGroup.add(new LoginAction());
        actionGroup.add(new LogoutAction());
        actionGroup.addSeparator();
        actionGroup.add(new ToggleContinuousDevelopmentAction());
        actionGroup.add(new TogglePatternRecognitionAction());
        
        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("ModForgeToolbar", actionGroup, false);
        toolbar.setTargetComponent(contentPanel);
        
        contentPanel.add(toolbar.getComponent(), BorderLayout.NORTH);
        
        // Create status panel
        JPanel statusPanel = createStatusPanel();
        contentPanel.add(statusPanel, BorderLayout.CENTER);
        
        // Refresh content
        refreshContent();
    }
    
    /**
     * Get the component for the tool window.
     *
     * @return The component
     */
    public JComponent getComponent() {
        return contentPanel;
    }
    
    /**
     * Refresh the content.
     */
    public void refreshContent() {
        SwingUtilities.invokeLater(() -> {
            updateStatusPanel();
        });
    }
    
    /**
     * Create the status panel.
     *
     * @return The status panel
     */
    private JPanel createStatusPanel() {
        JPanel statusPanel = new JBPanel<>(new GridBagLayout());
        statusPanel.setBorder(JBUI.Borders.empty(10));
        
        return statusPanel;
    }
    
    /**
     * Update the status panel.
     */
    private void updateStatusPanel() {
        try {
            JPanel statusPanel = (JPanel) contentPanel.getComponent(1);
            statusPanel.removeAll();
            
            GridBagConstraints c = new GridBagConstraints();
            c.anchor = GridBagConstraints.WEST;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 0;
            c.gridy = 0;
            c.weightx = 1;
            c.insets = JBUI.insets(5);
            
            // Add authentication status
            ModAuthenticationManager authManager = ModAuthenticationManager.getInstance();
            JBLabel authLabel = new JBLabel("Authentication Status: ");
            authLabel.setComponentStyle(UIUtil.ComponentStyle.LARGE);
            statusPanel.add(authLabel, c);
            
            c.gridx = 1;
            JBLabel authStatusLabel = new JBLabel();
            if (authManager.isAuthenticated()) {
                authStatusLabel.setText("Logged in as " + authManager.getUsername());
                authStatusLabel.setIcon(AllIcons.General.InspectionsOK);
                authStatusLabel.setForeground(JBColor.GREEN);
            } else {
                authStatusLabel.setText("Not logged in");
                authStatusLabel.setIcon(AllIcons.General.Warning);
                authStatusLabel.setForeground(JBColor.RED);
            }
            authStatusLabel.setComponentStyle(UIUtil.ComponentStyle.LARGE);
            statusPanel.add(authStatusLabel, c);
            
            // Add server status
            c.gridx = 0;
            c.gridy = 1;
            JBLabel serverLabel = new JBLabel("Server Status: ");
            serverLabel.setComponentStyle(UIUtil.ComponentStyle.LARGE);
            statusPanel.add(serverLabel, c);
            
            c.gridx = 1;
            JBLabel serverStatusLabel = new JBLabel();
            
            ModForgeSettings settings = ModForgeSettings.getInstance();
            boolean connected = ConnectionTestUtil.testConnection(settings.getServerUrl());
            
            if (connected) {
                serverStatusLabel.setText("Connected to " + settings.getServerUrl());
                serverStatusLabel.setIcon(AllIcons.General.InspectionsOK);
                serverStatusLabel.setForeground(JBColor.GREEN);
            } else {
                serverStatusLabel.setText("Not connected to " + settings.getServerUrl());
                serverStatusLabel.setIcon(AllIcons.General.Error);
                serverStatusLabel.setForeground(JBColor.RED);
            }
            serverStatusLabel.setComponentStyle(UIUtil.ComponentStyle.LARGE);
            statusPanel.add(serverStatusLabel, c);
            
            // Add feature status
            c.gridx = 0;
            c.gridy = 2;
            JBLabel featuresLabel = new JBLabel("Features: ");
            featuresLabel.setComponentStyle(UIUtil.ComponentStyle.LARGE);
            statusPanel.add(featuresLabel, c);
            
            c.gridx = 1;
            
            // Add continuous development status
            JBLabel continuousDevelopmentLabel = new JBLabel();
            if (settings.isContinuousDevelopment()) {
                continuousDevelopmentLabel.setText("Continuous Development: Enabled");
                continuousDevelopmentLabel.setIcon(AllIcons.General.InspectionsOK);
                continuousDevelopmentLabel.setForeground(JBColor.GREEN);
            } else {
                continuousDevelopmentLabel.setText("Continuous Development: Disabled");
                continuousDevelopmentLabel.setIcon(AllIcons.General.Information);
                continuousDevelopmentLabel.setForeground(JBColor.GRAY);
            }
            continuousDevelopmentLabel.setComponentStyle(UIUtil.ComponentStyle.LARGE);
            statusPanel.add(continuousDevelopmentLabel, c);
            
            // Add pattern recognition status
            c.gridy = 3;
            JBLabel patternRecognitionLabel = new JBLabel();
            if (settings.isPatternRecognition()) {
                patternRecognitionLabel.setText("Pattern Recognition: Enabled");
                patternRecognitionLabel.setIcon(AllIcons.General.InspectionsOK);
                patternRecognitionLabel.setForeground(JBColor.GREEN);
            } else {
                patternRecognitionLabel.setText("Pattern Recognition: Disabled");
                patternRecognitionLabel.setIcon(AllIcons.General.Information);
                patternRecognitionLabel.setForeground(JBColor.GRAY);
            }
            patternRecognitionLabel.setComponentStyle(UIUtil.ComponentStyle.LARGE);
            statusPanel.add(patternRecognitionLabel, c);
            
            statusPanel.revalidate();
            statusPanel.repaint();
        } catch (Exception e) {
            LOG.error("Error updating status panel", e);
        }
    }
}