package com.modforge.intellij.plugin.ui.toolwindow;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * Factory for creating the ModForge tool window.
 */
public class ModForgeToolWindowFactory implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        ModForgeToolWindowContent toolWindowContent = new ModForgeToolWindowContent(project);
        Content content = ContentFactory.getInstance().createContent(
                toolWindowContent.getContent(), "", false);
        toolWindow.getContentManager().addContent(content);
    }
    
    /**
     * Content of the ModForge tool window.
     */
    private static class ModForgeToolWindowContent {
        private final JPanel contentPanel;
        
        public ModForgeToolWindowContent(Project project) {
            contentPanel = new JPanel(new BorderLayout());
            
            // Create a welcome panel
            JPanel welcomePanel = new JPanel(new BorderLayout());
            welcomePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            
            // Title
            JLabel titleLabel = new JBLabel("ModForge");
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 18));
            welcomePanel.add(titleLabel, BorderLayout.NORTH);
            
            // Authentication status
            ModForgeSettings settings = ModForgeSettings.getInstance();
            String status = settings.isAuthenticated() ? 
                    "Authenticated as " + settings.getUsername() : 
                    "Not authenticated. Please log in.";
            
            JLabel statusLabel = new JBLabel("Status: " + status);
            welcomePanel.add(statusLabel, BorderLayout.CENTER);
            
            // Add to content panel
            contentPanel.add(welcomePanel, BorderLayout.NORTH);
            
            // Create placeholder for main content
            JPanel mainContentPanel = new JPanel(new BorderLayout());
            mainContentPanel.add(new JBLabel("Welcome to ModForge!"), BorderLayout.CENTER);
            contentPanel.add(new JBScrollPane(mainContentPanel), BorderLayout.CENTER);
        }
        
        public JComponent getContent() {
            return contentPanel;
        }
    }
}