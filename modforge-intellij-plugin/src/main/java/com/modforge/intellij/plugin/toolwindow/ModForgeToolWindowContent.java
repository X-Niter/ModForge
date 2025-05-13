package com.modforge.intellij.plugin.toolwindow;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.modforge.intellij.plugin.model.ModLoaderType;
import com.modforge.intellij.plugin.services.ModForgeProjectService;

import javax.swing.*;
import java.awt.*;

/**
 * Main content for the ModForge AI tool window.
 */
public class ModForgeToolWindowContent {
    private static final Logger LOG = Logger.getInstance(ModForgeToolWindowContent.class);
    
    private final Project project;
    private JPanel mainPanel;
    
    public ModForgeToolWindowContent(Project project) {
        this.project = project;
        createUI();
    }
    
    /**
     * Creates the UI.
     */
    private void createUI() {
        mainPanel = new JBPanel<>(new BorderLayout());
        mainPanel.setBorder(JBUI.Borders.empty(10));
        
        // Get project service to check if this is a mod project
        ModForgeProjectService projectService = project.getService(ModForgeProjectService.class);
        
        // Create a panel for the welcome message
        JPanel welcomePanel = new JBPanel<>(new BorderLayout());
        welcomePanel.setBorder(JBUI.Borders.empty(0, 0, 10, 0));
        
        // Add logo and welcome message
        JBLabel titleLabel = new JBLabel("ModForge AI");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 20));
        welcomePanel.add(titleLabel, BorderLayout.NORTH);
        
        // Add subtitle based on project type
        String subtitle;
        if (projectService.isModProject()) {
            ModLoaderType modLoaderType = projectService.getModLoaderType();
            String mcVersion = projectService.getMinecraftVersion() != null ?
                    projectService.getMinecraftVersion() : "Unknown";
            
            subtitle = "Detected " + modLoaderType.getDisplayName() + " project for Minecraft " + mcVersion;
        } else {
            subtitle = "AI-powered Minecraft mod development";
        }
        
        JBLabel subtitleLabel = new JBLabel(subtitle);
        subtitleLabel.setFont(subtitleLabel.getFont().deriveFont(Font.PLAIN, 14));
        subtitleLabel.setBorder(JBUI.Borders.empty(5, 0, 10, 0));
        welcomePanel.add(subtitleLabel, BorderLayout.CENTER);
        
        mainPanel.add(welcomePanel, BorderLayout.NORTH);
        
        // Create and add content panel
        JPanel contentPanel = createContentPanel(projectService.isModProject());
        JBScrollPane scrollPane = new JBScrollPane(contentPanel);
        scrollPane.setBorder(JBUI.Borders.empty());
        mainPanel.add(scrollPane, BorderLayout.CENTER);
    }
    
    /**
     * Creates the content panel.
     * @param isModProject Whether this is a mod project
     * @return The content panel
     */
    private JPanel createContentPanel(boolean isModProject) {
        JPanel contentPanel = new JBPanel<>(new GridLayout(0, 1, 0, 10));
        contentPanel.setBorder(JBUI.Borders.empty());
        
        if (isModProject) {
            // Add quick action cards
            contentPanel.add(createActionCard(
                    "Generate Code",
                    "Create new mod components with AI",
                    "Generate a new block, item, entity, or other mod component",
                    () -> openTab("Code Generator")
            ));
            
            contentPanel.add(createActionCard(
                    "Fix Errors",
                    "Resolve compilation and runtime errors",
                    "Select a file with errors to automatically fix issues",
                    () -> openTab("Error Resolver")
            ));
            
            contentPanel.add(createActionCard(
                    "Add Features",
                    "Enhance your mod with new capabilities",
                    "Describe a feature you want to add to your mod",
                    () -> openTab("Feature Suggester")
            ));
            
            contentPanel.add(createActionCard(
                    "Analyze Mods",
                    "Learn from existing Minecraft mods",
                    "Extract patterns and examples from mod JAR files",
                    () -> openTab("JAR Analyzer")
            ));
        } else {
            // Add "no mod detected" panel
            JPanel noModPanel = new JBPanel<>(new BorderLayout());
            noModPanel.setBorder(JBUI.Borders.empty(20));
            
            JBLabel messageLabel = new JBLabel("No Minecraft mod project detected");
            messageLabel.setFont(messageLabel.getFont().deriveFont(Font.BOLD, 16));
            messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
            noModPanel.add(messageLabel, BorderLayout.NORTH);
            
            JBLabel instructionsLabel = new JBLabel("<html><body style='width: 300px'>" +
                    "Open a Minecraft mod project or create a new one to use ModForge AI's full capabilities." +
                    "<br><br>ModForge AI supports Forge, Fabric, Quilt, and Bukkit/Spigot projects." +
                    "</body></html>");
            instructionsLabel.setHorizontalAlignment(SwingConstants.CENTER);
            instructionsLabel.setBorder(JBUI.Borders.empty(10, 0, 0, 0));
            noModPanel.add(instructionsLabel, BorderLayout.CENTER);
            
            // Add create new mod button
            JButton createModButton = new JButton("Create New Mod Project");
            createModButton.addActionListener(e -> {
                // This would open a dialog to create a new mod project
                LOG.info("Create new mod project button clicked");
            });
            
            JPanel buttonPanel = new JBPanel<>(new FlowLayout(FlowLayout.CENTER));
            buttonPanel.add(createModButton);
            buttonPanel.setBorder(JBUI.Borders.empty(20, 0, 0, 0));
            noModPanel.add(buttonPanel, BorderLayout.SOUTH);
            
            contentPanel.add(noModPanel);
        }
        
        return contentPanel;
    }
    
    /**
     * Creates an action card for the main panel.
     * @param title The card title
     * @param subtitle The card subtitle
     * @param description The card description
     * @param action The action to perform when the card is clicked
     * @return The action card panel
     */
    private JPanel createActionCard(String title, String subtitle, String description, Runnable action) {
        JPanel card = new JBPanel<>(new BorderLayout());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                JBUI.Borders.empty(15)
        ));
        
        // Title and subtitle
        JPanel headerPanel = new JBPanel<>(new BorderLayout());
        
        JBLabel titleLabel = new JBLabel(title);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16));
        headerPanel.add(titleLabel, BorderLayout.NORTH);
        
        JBLabel subtitleLabel = new JBLabel(subtitle);
        subtitleLabel.setFont(subtitleLabel.getFont().deriveFont(Font.PLAIN, 14));
        subtitleLabel.setBorder(JBUI.Borders.empty(5, 0, 0, 0));
        headerPanel.add(subtitleLabel, BorderLayout.CENTER);
        
        card.add(headerPanel, BorderLayout.NORTH);
        
        // Description
        JBLabel descriptionLabel = new JBLabel("<html><body>" + description + "</body></html>");
        descriptionLabel.setBorder(JBUI.Borders.empty(10, 0, 10, 0));
        card.add(descriptionLabel, BorderLayout.CENTER);
        
        // Button
        JButton openButton = new JButton("Open");
        openButton.addActionListener(e -> action.run());
        
        JPanel buttonPanel = new JBPanel<>(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(openButton);
        card.add(buttonPanel, BorderLayout.SOUTH);
        
        // Make the card clickable
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                action.run();
            }
        });
        
        return card;
    }
    
    /**
     * Opens the specified tab in the tool window.
     * @param tabName The name of the tab to open
     */
    private void openTab(String tabName) {
        LOG.info("Opening tab: " + tabName);
        // This would be implemented to switch to the specified tab
        // but requires access to the ToolWindow instance
    }
    
    /**
     * Gets the main content panel.
     * @return The main content panel
     */
    public JPanel getContent() {
        return mainPanel;
    }
}