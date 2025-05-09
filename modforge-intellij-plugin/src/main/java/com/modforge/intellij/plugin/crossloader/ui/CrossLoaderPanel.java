package com.modforge.intellij.plugin.crossloader.ui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.modforge.intellij.plugin.crossloader.ArchitecturyService;
import com.modforge.intellij.plugin.crossloader.CrossLoaderAPI;
import com.modforge.intellij.plugin.crossloader.ArchitecturyService.ModLoader;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Panel for cross-loader mod development.
 * Provides tools and information for developing mods that work across multiple mod loaders.
 */
public class CrossLoaderPanel extends SimpleToolWindowPanel {
    private static final Logger LOG = Logger.getInstance(CrossLoaderPanel.class);
    
    private final Project project;
    private final ArchitecturyService architecturyService;
    private final CrossLoaderAPI crossLoaderAPI;
    
    // UI components
    private JBTabbedPane tabbedPane;
    private JPanel overviewPanel;
    private JPanel templatesPanel;
    private JPanel projectPanel;
    
    /**
     * Creates a new cross-loader panel.
     * @param project The project
     */
    public CrossLoaderPanel(@NotNull Project project) {
        super(true);
        
        this.project = project;
        this.architecturyService = ArchitecturyService.getInstance(project);
        this.crossLoaderAPI = CrossLoaderAPI.getInstance(project);
        
        createUI();
    }
    
    /**
     * Creates the UI.
     */
    private void createUI() {
        // Create tabbed pane
        tabbedPane = new JBTabbedPane();
        
        // Create panels
        createOverviewPanel();
        createTemplatesPanel();
        createProjectPanel();
        
        // Add panels to tabbed pane
        tabbedPane.addTab("Overview", overviewPanel);
        tabbedPane.addTab("Templates", templatesPanel);
        tabbedPane.addTab("Project", projectPanel);
        
        // Add tabbed pane to this panel
        setContent(tabbedPane);
    }
    
    /**
     * Creates the overview panel.
     */
    private void createOverviewPanel() {
        overviewPanel = new JPanel(new BorderLayout());
        overviewPanel.setBorder(JBUI.Borders.empty(10));
        
        // Add title
        JLabel titleLabel = new JLabel("Cross-Loader Mod Development");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        overviewPanel.add(titleLabel, BorderLayout.NORTH);
        
        // Create main content panel
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBorder(JBUI.Borders.empty(10, 0, 0, 0));
        
        // Add description
        JEditorPane descriptionPane = new JEditorPane();
        descriptionPane.setContentType("text/html");
        descriptionPane.setEditable(false);
        descriptionPane.setText(
                "<html><body style='margin: 5px'>" +
                "<p>Cross-loader development allows your mods to work across multiple mod loaders " +
                "such as Forge, Fabric, and Quilt without having to maintain separate codebases.</p>" +
                
                "<h3>Benefits:</h3>" +
                "<ul>" +
                "<li>Reach a wider audience by supporting multiple platforms</li>" +
                "<li>Maintain a single codebase for easier updates</li>" +
                "<li>Use platform-specific features while keeping common code shared</li>" +
                "</ul>" +
                
                "<h3>Supported Approaches:</h3>" +
                "<ul>" +
                "<li><b>Architectury API:</b> A comprehensive API for cross-platform mod development</li>" +
                "<li><b>Direct Conversion:</b> Convert mods between platforms without an intermediary API</li>" +
                "</ul>" +
                
                "<p>Use the <b>Templates</b> tab to get code snippets for common cross-loader patterns " +
                "and the <b>Project</b> tab to set up or analyze your cross-loader project structure.</p>" +
                "</body></html>"
        );
        descriptionPane.setBackground(UIUtil.getPanelBackground());
        
        JBScrollPane scrollPane = new JBScrollPane(descriptionPane);
        contentPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Add buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.setBorder(JBUI.Borders.empty(10, 0, 0, 0));
        
        JButton createProjectButton = new JButton("Create Cross-Loader Project");
        createProjectButton.addActionListener(e -> showCreateProjectDialog());
        
        JButton architecturyDocsButton = new JButton("Architectury Documentation");
        architecturyDocsButton.addActionListener(e -> openArchitecturyDocs());
        
        buttonPanel.add(createProjectButton);
        buttonPanel.add(architecturyDocsButton);
        
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        // Add content panel to overview panel
        overviewPanel.add(contentPanel, BorderLayout.CENTER);
    }
    
    /**
     * Creates the templates panel.
     */
    private void createTemplatesPanel() {
        templatesPanel = new JPanel(new BorderLayout());
        templatesPanel.setBorder(JBUI.Borders.empty(10));
        
        // Add title
        JLabel titleLabel = new JLabel("Cross-Loader Templates");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        templatesPanel.add(titleLabel, BorderLayout.NORTH);
        
        // Create main content panel
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBorder(JBUI.Borders.empty(10, 0, 0, 0));
        
        // Create template selection panel
        JPanel selectionPanel = new JPanel(new GridLayout(0, 1));
        selectionPanel.setBorder(BorderFactory.createTitledBorder("Template Categories"));
        
        // Add Architectury templates
        for (ArchitecturyService.ArchitecturyFeatureType featureType : ArchitecturyService.ArchitecturyFeatureType.values()) {
            JButton templateButton = new JButton(featureType.name().replace('_', ' '));
            templateButton.addActionListener(e -> showArchitecturyTemplate(featureType));
            selectionPanel.add(templateButton);
        }
        
        // Add direct conversion templates
        JButton forgeToFabricButton = new JButton("Forge to Fabric Conversion");
        forgeToFabricButton.addActionListener(e -> showDirectConversionTemplate(ModLoader.FORGE, ModLoader.FABRIC));
        selectionPanel.add(forgeToFabricButton);
        
        JButton fabricToForgeButton = new JButton("Fabric to Forge Conversion");
        fabricToForgeButton.addActionListener(e -> showDirectConversionTemplate(ModLoader.FABRIC, ModLoader.FORGE));
        selectionPanel.add(fabricToForgeButton);
        
        // Create template display panel
        JPanel displayPanel = new JPanel(new BorderLayout());
        displayPanel.setBorder(BorderFactory.createTitledBorder("Template"));
        
        JTextArea templateTextArea = new JTextArea();
        templateTextArea.setEditable(false);
        templateTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        templateTextArea.setText("Select a template from the left to view it here.");
        
        JBScrollPane templateScrollPane = new JBScrollPane(templateTextArea);
        displayPanel.add(templateScrollPane, BorderLayout.CENTER);
        
        // Create buttons panel
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonsPanel.setBorder(JBUI.Borders.empty(5, 0, 0, 0));
        
        JButton copyButton = new JButton("Copy to Clipboard");
        copyButton.addActionListener(e -> {
            templateTextArea.selectAll();
            templateTextArea.copy();
            templateTextArea.select(0, 0);
        });
        
        JButton insertButton = new JButton("Insert at Cursor");
        // Implementation would insert into editor
        
        buttonsPanel.add(copyButton);
        buttonsPanel.add(insertButton);
        
        displayPanel.add(buttonsPanel, BorderLayout.SOUTH);
        
        // Add selection and display panels to content panel
        contentPanel.add(selectionPanel, BorderLayout.WEST);
        contentPanel.add(displayPanel, BorderLayout.CENTER);
        
        // Add content panel to templates panel
        templatesPanel.add(contentPanel, BorderLayout.CENTER);
    }
    
    /**
     * Creates the project panel.
     */
    private void createProjectPanel() {
        projectPanel = new JPanel(new BorderLayout());
        projectPanel.setBorder(JBUI.Borders.empty(10));
        
        // Add title
        JLabel titleLabel = new JLabel("Cross-Loader Project Management");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        projectPanel.add(titleLabel, BorderLayout.NORTH);
        
        // Create main content panel
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBorder(JBUI.Borders.empty(10, 0, 0, 0));
        
        // Create project info panel
        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.setBorder(BorderFactory.createTitledBorder("Project Information"));
        
        // Check if project is using Architectury
        boolean isArchitecturyProject = architecturyService.isArchitecturyProject();
        boolean isMultiModuleSetup = architecturyService.isMultiModuleSetup();
        List<ArchitecturyService.ModLoaderInfo> detectedLoaders = architecturyService.detectModLoaders();
        
        JEditorPane infoPane = new JEditorPane();
        infoPane.setContentType("text/html");
        infoPane.setEditable(false);
        
        StringBuilder infoText = new StringBuilder();
        infoText.append("<html><body style='margin: 5px'>");
        
        if (isArchitecturyProject) {
            infoText.append("<h3>Architectury Project Detected</h3>");
            infoText.append("<p>This project is using Architectury for cross-loader mod development.</p>");
        } else {
            infoText.append("<h3>Architectury Not Detected</h3>");
            infoText.append("<p>This project does not appear to be using Architectury. ");
            infoText.append("Consider setting up an Architectury project for cross-loader development.</p>");
        }
        
        if (isMultiModuleSetup) {
            infoText.append("<h3>Multi-Module Setup Detected</h3>");
            infoText.append("<p>This project is using a multi-module setup, which is recommended for cross-loader development.</p>");
        } else {
            infoText.append("<h3>Single Module Setup Detected</h3>");
            infoText.append("<p>This project is using a single module setup. Consider using a multi-module setup for better organization.</p>");
        }
        
        infoText.append("<h3>Detected Mod Loaders:</h3>");
        infoText.append("<ul>");
        if (detectedLoaders.isEmpty()) {
            infoText.append("<li>No mod loaders detected. This may not be a Minecraft mod project.</li>");
        } else {
            for (ArchitecturyService.ModLoaderInfo loaderInfo : detectedLoaders) {
                infoText.append("<li><b>").append(loaderInfo.getLoader().getDisplayName()).append("</b>");
                infoText.append(" (Module: ").append(loaderInfo.getModule().getName()).append(")</li>");
            }
        }
        infoText.append("</ul>");
        
        infoText.append("</body></html>");
        infoPane.setText(infoText.toString());
        infoPane.setBackground(UIUtil.getPanelBackground());
        
        JBScrollPane infoScrollPane = new JBScrollPane(infoPane);
        infoPanel.add(infoScrollPane, BorderLayout.CENTER);
        
        // Create project structure panel
        JPanel structurePanel = new JPanel(new BorderLayout());
        structurePanel.setBorder(BorderFactory.createTitledBorder("Recommended Project Structure"));
        
        JEditorPane structurePane = new JEditorPane();
        structurePane.setContentType("text/html");
        structurePane.setEditable(false);
        structurePane.setText(architecturyService.getProjectStructureRecommendation());
        structurePane.setBackground(UIUtil.getPanelBackground());
        
        JBScrollPane structureScrollPane = new JBScrollPane(structurePane);
        structurePanel.add(structureScrollPane, BorderLayout.CENTER);
        
        // Create buttons panel
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonsPanel.setBorder(JBUI.Borders.empty(10, 0, 0, 0));
        
        JButton createProjectButton = new JButton("Create Cross-Loader Project");
        createProjectButton.addActionListener(e -> showCreateProjectDialog());
        
        JButton convertProjectButton = new JButton("Convert Existing Project");
        // Implementation would convert existing project
        
        JButton analyzeProjectButton = new JButton("Analyze Project");
        analyzeProjectButton.addActionListener(e -> analyzeProject());
        
        buttonsPanel.add(createProjectButton);
        buttonsPanel.add(convertProjectButton);
        buttonsPanel.add(analyzeProjectButton);
        
        // Add panels to content panel
        JPanel topPanel = new JPanel(new GridLayout(1, 2));
        topPanel.add(infoPanel);
        topPanel.add(structurePanel);
        
        contentPanel.add(topPanel, BorderLayout.CENTER);
        contentPanel.add(buttonsPanel, BorderLayout.SOUTH);
        
        // Add content panel to project panel
        projectPanel.add(contentPanel, BorderLayout.CENTER);
    }
    
    /**
     * Shows the create project dialog.
     */
    private void showCreateProjectDialog() {
        CrossLoaderProjectSetupDialog dialog = new CrossLoaderProjectSetupDialog(project);
        dialog.show();
    }
    
    /**
     * Opens the Architectury documentation.
     */
    private void openArchitecturyDocs() {
        try {
            Desktop.getDesktop().browse(new java.net.URI("https://docs.architectury.dev/"));
        } catch (Exception e) {
            LOG.error("Error opening Architectury documentation", e);
        }
    }
    
    /**
     * Shows an Architectury template.
     * @param featureType The feature type
     */
    private void showArchitecturyTemplate(@NotNull ArchitecturyService.ArchitecturyFeatureType featureType) {
        String template = architecturyService.getArchitecturySnippet(featureType);
        
        Map<String, String> params = new HashMap<>();
        params.put("MOD_ID", "example_mod");
        
        // Show template in a dialog
        showTemplateDialog(featureType.name() + " Template", template);
    }
    
    /**
     * Shows a direct conversion template.
     * @param sourceLoader The source mod loader
     * @param targetLoader The target mod loader
     */
    private void showDirectConversionTemplate(@NotNull ModLoader sourceLoader, @NotNull ModLoader targetLoader) {
        Map<String, String> params = new HashMap<>();
        params.put("MOD_ID", "example_mod");
        params.put("CLASS_NAME", "ExampleMod");
        
        String templateKey = "direct." + sourceLoader.name().toLowerCase() + "_to_" + targetLoader.name().toLowerCase();
        String template = crossLoaderAPI.getTemplate(templateKey, params);
        
        if (template == null) {
            template = "// No template available for conversion from " + sourceLoader + " to " + targetLoader;
        }
        
        // Show template in a dialog
        showTemplateDialog(sourceLoader + " to " + targetLoader + " Template", template);
    }
    
    /**
     * Shows a template in a dialog.
     * @param title The dialog title
     * @param template The template
     */
    private void showTemplateDialog(@NotNull String title, @NotNull String template) {
        // Create dialog
        JDialog dialog = new JDialog((JFrame) null, title, true);
        dialog.setSize(800, 600);
        dialog.setLocationRelativeTo(null);
        
        // Create content panel
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBorder(JBUI.Borders.empty(10));
        
        // Create text area
        JTextArea textArea = new JTextArea(template);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        textArea.setEditable(false);
        
        JBScrollPane scrollPane = new JBScrollPane(textArea);
        contentPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Create buttons panel
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        JButton copyButton = new JButton("Copy to Clipboard");
        copyButton.addActionListener(e -> {
            textArea.selectAll();
            textArea.copy();
            textArea.select(0, 0);
        });
        
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dialog.dispose());
        
        buttonsPanel.add(copyButton);
        buttonsPanel.add(closeButton);
        
        contentPanel.add(buttonsPanel, BorderLayout.SOUTH);
        
        // Set dialog content and show
        dialog.setContentPane(contentPanel);
        dialog.setVisible(true);
    }
    
    /**
     * Analyzes the project.
     */
    private void analyzeProject() {
        // Re-detect project information
        boolean isArchitecturyProject = architecturyService.isArchitecturyProject();
        boolean isMultiModuleSetup = architecturyService.isMultiModuleSetup();
        List<ArchitecturyService.ModLoaderInfo> detectedLoaders = architecturyService.detectModLoaders();
        
        // Show results in a dialog
        StringBuilder message = new StringBuilder();
        message.append("Project Analysis Results:\n\n");
        
        message.append("Architectury: ").append(isArchitecturyProject ? "Detected" : "Not Detected").append("\n");
        message.append("Multi-Module Setup: ").append(isMultiModuleSetup ? "Yes" : "No").append("\n\n");
        
        message.append("Detected Mod Loaders:\n");
        if (detectedLoaders.isEmpty()) {
            message.append("No mod loaders detected. This may not be a Minecraft mod project.\n");
        } else {
            for (ArchitecturyService.ModLoaderInfo loaderInfo : detectedLoaders) {
                message.append("- ").append(loaderInfo.getLoader().getDisplayName());
                message.append(" (Module: ").append(loaderInfo.getModule().getName()).append(")\n");
            }
        }
        
        JOptionPane.showMessageDialog(
                null,
                message.toString(),
                "Project Analysis",
                JOptionPane.INFORMATION_MESSAGE
        );
    }
}