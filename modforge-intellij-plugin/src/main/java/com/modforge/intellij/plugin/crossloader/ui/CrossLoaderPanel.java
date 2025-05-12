package com.modforge.intellij.plugin.crossloader.ui;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.modforge.intellij.plugin.crossloader.ArchitecturyService;
import com.modforge.intellij.plugin.crossloader.CrossLoaderAPI;
import com.modforge.intellij.plugin.crossloader.actions.ConvertToArchitecturyAction;
import com.modforge.intellij.plugin.crossloader.actions.CreateCrossLoaderProjectAction;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * Panel for managing cross-loader mod development.
 * Provides UI for creating and managing cross-loader projects.
 */
public class CrossLoaderPanel extends JBPanel<CrossLoaderPanel> {
    private static final Logger LOG = Logger.getInstance(CrossLoaderPanel.class);
    
    private final Project project;
    private final ArchitecturyService architecturyService;
    private final CrossLoaderAPI crossLoaderAPI;
    
    // UI components
    private JBSplitter mainSplitter;
    private JBList<String> platformsList;
    private JEditorPane infoPane;
    
    /**
     * Creates a new CrossLoaderPanel.
     * @param project The project
     */
    public CrossLoaderPanel(@NotNull Project project) {
        super(new BorderLayout());
        this.project = project;
        this.architecturyService = ArchitecturyService.getInstance(project);
        this.crossLoaderAPI = CrossLoaderAPI.getInstance(project);
        
        createUI();
    }
    
    /**
     * Creates the UI.
     */
    private void createUI() {
        // Create toolbar
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        actionGroup.add(new CreateCrossLoaderProjectAction());
        actionGroup.add(new ConvertToArchitecturyAction());
        
        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(
                "CrossLoaderPanel", actionGroup, true
        );
        toolbar.setTargetComponent(this);
        add(toolbar.getComponent(), BorderLayout.NORTH);
        
        // Create main content
        mainSplitter = new JBSplitter(false, 0.3f);
        
        // Create platforms list
        createPlatformsList();
        
        // Create info panel
        createInfoPanel();
        
        // Add splitter to panel
        add(mainSplitter, BorderLayout.CENTER);
        
        // Create status panel
        JPanel statusPanel = createStatusPanel();
        add(statusPanel, BorderLayout.SOUTH);
    }
    
    /**
     * Creates the platforms list.
     */
    private void createPlatformsList() {
        JPanel platformsPanel = new JBPanel<>(new BorderLayout());
        platformsPanel.setBorder(BorderFactory.createTitledBorder("Platforms"));
        
        DefaultListModel<String> platformsModel = new DefaultListModel<>();
        platformsModel.addElement("Forge");
        platformsModel.addElement("Fabric");
        platformsModel.addElement("Quilt");
        
        platformsList = new JBList<>(platformsModel);
        platformsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        platformsList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateInfoPanel(platformsList.getSelectedValue());
            }
        });
        
        platformsPanel.add(new JBScrollPane(platformsList), BorderLayout.CENTER);
        
        mainSplitter.setFirstComponent(platformsPanel);
    }
    
    /**
     * Creates the info panel.
     */
    private void createInfoPanel() {
        JPanel infoPanel = new JBPanel<>(new BorderLayout());
        infoPanel.setBorder(BorderFactory.createTitledBorder("Platform Information"));
        
        infoPane = new JEditorPane();
        infoPane.setContentType("text/html");
        infoPane.setEditable(false);
        infoPane.setText(getDefaultInfoText());
        infoPane.setBackground(UIUtil.getPanelBackground());
        
        infoPanel.add(new JBScrollPane(infoPane), BorderLayout.CENTER);
        
        mainSplitter.setSecondComponent(infoPanel);
    }
    
    /**
     * Creates the status panel.
     * @return The status panel
     */
    private JPanel createStatusPanel() {
        JPanel statusPanel = new JBPanel<>(new BorderLayout());
        statusPanel.setBorder(JBUI.Borders.empty(5));
        
        StringBuilder statusText = new StringBuilder("Cross-Loader Status: ");
        if (architecturyService.isArchitecturyAvailable()) {
            statusText.append("Architectury API available");
        } else {
            statusText.append("Architectury API not detected");
        }
        
        JBLabel statusLabel = new JBLabel(statusText.toString());
        statusPanel.add(statusLabel, BorderLayout.CENTER);
        
        return statusPanel;
    }
    
    /**
     * Updates the info panel with platform-specific information.
     * @param platform The platform
     */
    private void updateInfoPanel(String platform) {
        if (platform == null) {
            infoPane.setText(getDefaultInfoText());
            return;
        }
        
        switch (platform) {
            case "Forge":
                infoPane.setText(getForgeInfoText());
                break;
            case "Fabric":
                infoPane.setText(getFabricInfoText());
                break;
            case "Quilt":
                infoPane.setText(getQuiltInfoText());
                break;
            default:
                infoPane.setText(getDefaultInfoText());
        }
    }
    
    /**
     * Gets the default info text.
     * @return The default info text
     */
    private String getDefaultInfoText() {
        return "<html><body style='margin: 5px'>" +
                "<h2>Cross-Loader Mod Development</h2>" +
                "<p>Select a platform from the list to see information about developing for that platform.</p>" +
                "<p>Use the toolbar actions to create a new cross-loader project or convert an existing one.</p>" +
                "<h3>Architectury API</h3>" +
                "<p>Architectury API allows you to develop mods that work across multiple mod loaders with a single codebase.</p>" +
                "<p>Key benefits:</p>" +
                "<ul>" +
                "<li>Write code once, deploy everywhere</li>" +
                "<li>Automatic handling of platform differences</li>" +
                "<li>Access to a rich API for common mod tasks</li>" +
                "</ul>" +
                "</body></html>";
    }
    
    /**
     * Gets the Forge info text.
     * @return The Forge info text
     */
    private String getForgeInfoText() {
        return "<html><body style='margin: 5px'>" +
                "<h2>Forge Platform</h2>" +
                "<p>Forge is one of the oldest and most established Minecraft mod loaders, known for its stability and extensive API.</p>" +
                "<h3>Key Features</h3>" +
                "<ul>" +
                "<li>Comprehensive event system</li>" +
                "<li>Extensive modding capabilities</li>" +
                "<li>Large community and documentation</li>" +
                "</ul>" +
                "<h3>Cross-Loader Development</h3>" +
                "<p>When developing for Forge in a cross-loader environment:</p>" +
                "<ul>" +
                "<li>Use <code>@ExpectPlatform</code> for Forge-specific implementations</li>" +
                "<li>Place Forge-specific code in the 'forge' module</li>" +
                "<li>Use <code>ForgePlatform.isForge()</code> to check the platform at runtime</li>" +
                "</ul>" +
                "</body></html>";
    }
    
    /**
     * Gets the Fabric info text.
     * @return The Fabric info text
     */
    private String getFabricInfoText() {
        return "<html><body style='margin: 5px'>" +
                "<h2>Fabric Platform</h2>" +
                "<p>Fabric is a lightweight, modular mod loader designed to make modding more accessible and flexible.</p>" +
                "<h3>Key Features</h3>" +
                "<ul>" +
                "<li>Lightweight and modular design</li>" +
                "<li>Fast updates to new Minecraft versions</li>" +
                "<li>Mixin-based mod development</li>" +
                "</ul>" +
                "<h3>Cross-Loader Development</h3>" +
                "<p>When developing for Fabric in a cross-loader environment:</p>" +
                "<ul>" +
                "<li>Use <code>@ExpectPlatform</code> for Fabric-specific implementations</li>" +
                "<li>Place Fabric-specific code in the 'fabric' module</li>" +
                "<li>Use <code>FabricPlatform.isFabric()</code> to check the platform at runtime</li>" +
                "</ul>" +
                "</body></html>";
    }
    
    /**
     * Gets the Quilt info text.
     * @return The Quilt info text
     */
    private String getQuiltInfoText() {
        return "<html><body style='margin: 5px'>" +
                "<h2>Quilt Platform</h2>" +
                "<p>Quilt is a fork of Fabric that aims to provide more features and improved mod development experience.</p>" +
                "<h3>Key Features</h3>" +
                "<ul>" +
                "<li>Enhanced mod loading capabilities</li>" +
                "<li>Improved APIs for common tasks</li>" +
                "<li>Compatibility with many Fabric mods</li>" +
                "</ul>" +
                "<h3>Cross-Loader Development</h3>" +
                "<p>When developing for Quilt in a cross-loader environment:</p>" +
                "<ul>" +
                "<li>Use <code>@ExpectPlatform</code> for Quilt-specific implementations</li>" +
                "<li>Place Quilt-specific code in the 'quilt' module</li>" +
                "<li>Use <code>QuiltPlatform.isQuilt()</code> to check the platform at runtime</li>" +
                "</ul>" +
                "</body></html>";
    }
}