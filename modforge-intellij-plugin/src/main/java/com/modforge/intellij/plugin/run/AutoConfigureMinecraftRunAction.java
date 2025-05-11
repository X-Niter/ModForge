package com.modforge.intellij.plugin.run;

import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.modforge.intellij.plugin.run.MinecraftRunConfiguration.RunType;
import org.jetbrains.annotations.NotNull;

/**
 * Action to auto-configure Minecraft run configurations.
 * Creates standard client, server, and data generation configurations automatically.
 */
public class AutoConfigureMinecraftRunAction extends AnAction {
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;
        
        // Confirm with user
        int result = Messages.showYesNoDialog(
                project,
                "This will create standard Minecraft run configurations for client, server, and data generation. Continue?",
                "Auto-Configure Minecraft Runs",
                Messages.getQuestionIcon()
        );
        
        if (result != Messages.YES) return;
        
        // Get run manager
        RunManager runManager = RunManager.getInstance(project);
        
        // Create client configuration
        createRunConfiguration(runManager, project, RunType.CLIENT, "Run Minecraft Client");
        
        // Create server configuration
        createRunConfiguration(runManager, project, RunType.SERVER, "Run Minecraft Server");
        
        // Create data gen configuration
        createRunConfiguration(runManager, project, RunType.DATA_GEN, "Run Data Generation");
        
        // Inform user
        Messages.showInfoMessage(
                project,
                "Created 3 Minecraft run configurations. You can now access them from the run configurations dropdown.",
                "Run Configurations Created"
        );
    }
    
    private void createRunConfiguration(RunManager runManager, Project project, RunType runType, String name) {
        RunnerAndConfigurationSettings settings = runManager.createConfiguration(
                name,
                MinecraftRunConfigurationType.class
        );
        
        MinecraftRunConfiguration configuration = (MinecraftRunConfiguration) settings.getConfiguration();
        configuration.setRunType(runType);
        
        // Configure common settings
        configuration.setEnableDebug(true);
        
        // Configure specific settings based on run type
        switch (runType) {
            case CLIENT:
                // Client-specific VM args
                configuration.setVmArgs("-Xmx3G -XX:+UseG1GC -XX:+ParallelRefProcEnabled");
                break;
                
            case SERVER:
                // Server-specific VM args (more memory for server)
                configuration.setVmArgs("-Xmx4G -XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200");
                configuration.setProgramArgs("--nogui");
                break;
                
            case DATA_GEN:
                // Data gen specific args
                configuration.setProgramArgs("--all --output src/generated/resources");
                break;
        }
        
        // Save the configuration
        runManager.addConfiguration(settings);
        
        // If this is a client configuration, set it as default
        if (runType == RunType.CLIENT) {
            runManager.setSelectedConfiguration(settings);
        }
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        e.getPresentation().setEnabledAndVisible(project != null);
    }
}