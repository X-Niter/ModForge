package com.modforge.intellij.plugin.services;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectCloseListener;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.util.messages.MessageBusConnection;
import com.modforge.intellij.plugin.ai.PatternRecognitionService;
import com.modforge.intellij.plugin.listeners.ModForgeCompilationListener;
import com.modforge.intellij.plugin.settings.ModForgeSettings;
import org.jetbrains.annotations.NotNull;

/**
 * Main service for the ModForge plugin.
 */
@Service
public final class ModForgeProjectService {
    private static final Logger LOG = Logger.getInstance(ModForgeProjectService.class);
    
    private final Project project;
    private final MessageBusConnection messageBusConnection;
    
    /**
     * Gets the instance of this service for the specified project.
     * @param project The project
     * @return The service instance
     */
    public static ModForgeProjectService getInstance(@NotNull Project project) {
        return project.getService(ModForgeProjectService.class);
    }
    
    /**
     * Creates a new instance of this service.
     * @param project The project
     */
    public ModForgeProjectService(Project project) {
        this.project = project;
        this.messageBusConnection = project.getMessageBus().connect();
        
        // Register project close listener
        messageBusConnection.subscribe(ProjectCloseListener.TOPIC, new ProjectCloseListener() {
            @Override
            public void projectClosing(@NotNull Project project) {
                shutdown();
            }
        });
        
        // Initialize
        initialize();
        
        LOG.info("ModForge project service initialized");
    }
    
    /**
     * Initializes the service.
     */
    private void initialize() {
        LOG.info("Initializing ModForge services");
        
        // Ensure compilation listener is initialized
        ModForgeCompilationListener compilationListener = ModForgeCompilationListener.getInstance(project);
        
        // Ensure AutonomousCodeGenerationService is initialized
        AutonomousCodeGenerationService codeGenService = AutonomousCodeGenerationService.getInstance(project);
        
        // Ensure ContinuousDevelopmentService is initialized
        ContinuousDevelopmentService continuousService = ContinuousDevelopmentService.getInstance(project);
        
        // Start continuous development if enabled
        ModForgeSettings settings = ModForgeSettings.getInstance();
        if (settings.isContinuousDevelopmentEnabled()) {
            continuousService.start();
        }
    }
    
    /**
     * Shuts down the service.
     */
    private void shutdown() {
        LOG.info("Shutting down ModForge services");
        
        // Stop continuous development
        ContinuousDevelopmentService continuousService = ContinuousDevelopmentService.getInstance(project);
        continuousService.stop();
        
        // Disconnect message bus
        messageBusConnection.disconnect();
    }
    
    /**
     * Checks if the OpenAI API key is configured.
     * @return True if the API key is configured, false otherwise
     */
    public boolean isApiKeyConfigured() {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        String apiKey = settings.getOpenAiApiKey();
        
        return apiKey != null && !apiKey.isEmpty();
    }
    
    /**
     * Gets the current usage statistics.
     * @return The usage statistics
     */
    public String getUsageStatistics() {
        PatternRecognitionService patternService = PatternRecognitionService.getInstance();
        
        if (patternService == null) {
            return "Pattern recognition service not available";
        }
        
        // Get statistics
        var statistics = patternService.getStatistics();
        
        int totalRequests = (int) statistics.getOrDefault("totalRequests", 0);
        int patternMatches = (int) statistics.getOrDefault("patternMatches", 0);
        int apiCalls = (int) statistics.getOrDefault("apiCalls", 0);
        int tokensSaved = (int) statistics.getOrDefault("estimatedTokensSaved", 0);
        double costSaved = (double) statistics.getOrDefault("estimatedCostSaved", 0.0);
        
        // Format statistics
        return String.format(
                "Total Requests: %d\n" +
                "Pattern Matches: %d\n" +
                "API Calls: %d\n" +
                "Tokens Saved: %d\n" +
                "Cost Saved: $%.2f",
                totalRequests,
                patternMatches,
                apiCalls,
                tokensSaved,
                costSaved
        );
    }
    
    /**
     * Startup activity to initialize the ModForge services.
     */
    public static class ProjectStartupActivity implements StartupActivity.DumbAware {
        @Override
        public void runActivity(@NotNull Project project) {
            // Ensure ModForgeProjectService is initialized
            ModForgeProjectService.getInstance(project);
        }
    }
}