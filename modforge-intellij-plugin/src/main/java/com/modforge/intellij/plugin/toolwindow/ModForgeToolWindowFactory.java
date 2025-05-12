package com.modforge.intellij.plugin.toolwindow;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.modforge.intellij.plugin.crossloader.ui.CrossLoaderPanel;
import com.modforge.intellij.plugin.services.ModForgeProjectService;
import org.jetbrains.annotations.NotNull;

/**
 * Factory for creating the ModForge AI tool window.
 */
public class ModForgeToolWindowFactory implements ToolWindowFactory {
    private static final Logger LOG = Logger.getInstance(ModForgeToolWindowFactory.class);
    
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        LOG.info("Creating ModForge AI tool window for project: " + project.getName());
        
        // Get the ContentFactory instance
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        
        // Create the tool window content
        ModForgeToolWindowContent toolWindowContent = new ModForgeToolWindowContent(project);
        Content content = contentFactory.createContent(toolWindowContent.getContent(), "ModForge AI", false);
        toolWindow.getContentManager().addContent(content);
        
        // Add tabs for different features
        addFeatureTabs(project, toolWindow, contentFactory);
    }
    
    /**
     * Adds tabs for different features.
     * @param project The project
     * @param toolWindow The tool window
     * @param contentFactory The content factory
     */
    private void addFeatureTabs(@NotNull Project project, @NotNull ToolWindow toolWindow, @NotNull ContentFactory contentFactory) {
        // Get project service to check if this is a mod project
        ModForgeProjectService projectService = project.getService(ModForgeProjectService.class);
        
        // Add Code Generator tab
        CodeGeneratorPanel codeGeneratorPanel = new CodeGeneratorPanel(project);
        Content codeGeneratorContent = contentFactory.createContent(
                codeGeneratorPanel.getContent(),
                "Code Generator",
                false
        );
        toolWindow.getContentManager().addContent(codeGeneratorContent);
        
        // Add Error Resolver tab
        ErrorResolverPanel errorResolverPanel = new ErrorResolverPanel(project);
        Content errorResolverContent = contentFactory.createContent(
                errorResolverPanel.getContent(),
                "Error Resolver",
                false
        );
        toolWindow.getContentManager().addContent(errorResolverContent);
        
        // Add Feature Suggester tab
        FeatureSuggesterPanel featureSuggesterPanel = new FeatureSuggesterPanel(project);
        Content featureSuggesterContent = contentFactory.createContent(
                featureSuggesterPanel.getContent(),
                "Feature Suggester",
                false
        );
        toolWindow.getContentManager().addContent(featureSuggesterContent);
        
        // Add JAR Analyzer tab
        JarAnalyzerPanel jarAnalyzerPanel = new JarAnalyzerPanel(project);
        Content jarAnalyzerContent = contentFactory.createContent(
                jarAnalyzerPanel.getContent(),
                "JAR Analyzer",
                false
        );
        toolWindow.getContentManager().addContent(jarAnalyzerContent);
        
        // Add Cross-Loader tab
        CrossLoaderPanel crossLoaderPanel = new CrossLoaderPanel(project);
        Content crossLoaderContent = contentFactory.createContent(
                crossLoaderPanel,
                "Cross-Loader",
                false
        );
        toolWindow.getContentManager().addContent(crossLoaderContent);
        
        // Add Metrics tab
        MetricsPanel metricsPanel = new MetricsPanel(project);
        Content metricsContent = contentFactory.createContent(
                metricsPanel.getContent(),
                "Metrics",
                false
        );
        toolWindow.getContentManager().addContent(metricsContent);
        
        // Add Settings tab
        SettingsPanel settingsPanel = new SettingsPanel(project);
        Content settingsContent = contentFactory.createContent(
                settingsPanel.getContent(),
                "Settings",
                false
        );
        toolWindow.getContentManager().addContent(settingsContent);
    }
    
    @Override
    public void init(@NotNull ToolWindow toolWindow) {
        // Set tool window properties
        toolWindow.setStripeTitle("ModForge AI");
        toolWindow.setTitle("ModForge AI");
    }
    
    @Override
    public boolean shouldBeAvailable(@NotNull Project project) {
        // Tool window is available for all projects
        return true;
    }
}