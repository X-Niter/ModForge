package com.modforge.intellij.plugin.run;

import com.intellij.execution.Executor;
import com.intellij.execution.configurations.*;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Run configuration for Minecraft in ModForge projects.
 * Handles the configuration of different Minecraft run types:
 * - Client (game client with mod loaded)
 * - Server (dedicated server with mod loaded)
 * - Data Generation (for resource pack and data generation)
 */
public class MinecraftRunConfiguration extends RunConfigurationBase<MinecraftRunConfigurationOptions> {
    
    // Run types for Minecraft
    public enum RunType {
        CLIENT("Run Client"),
        SERVER("Run Server"),
        DATA_GEN("Run Data Generation");
        
        private final String displayName;
        
        RunType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    private RunType runType = RunType.CLIENT;  // Default to client
    private boolean enableDebug = true;        // Default to debugging enabled
    private String vmArgs = "";               // Additional VM arguments
    private String programArgs = "";          // Additional program arguments
    
    protected MinecraftRunConfiguration(@NotNull Project project, @NotNull ConfigurationFactory factory, String name) {
        super(project, factory, name);
    }
    
    @NotNull
    @Override
    protected MinecraftRunConfigurationOptions getOptions() {
        return (MinecraftRunConfigurationOptions) super.getOptions();
    }
    
    @Override
    public void readExternal(@NotNull org.jdom.Element element) {
        super.readExternal(element);
        try {
            String runTypeStr = element.getAttributeValue("runType");
            if (runTypeStr != null) {
                runType = RunType.valueOf(runTypeStr);
            }
            String enableDebugStr = element.getAttributeValue("enableDebug");
            if (enableDebugStr != null) {
                enableDebug = Boolean.parseBoolean(enableDebugStr);
            }
            vmArgs = element.getAttributeValue("vmArgs", "");
            programArgs = element.getAttributeValue("programArgs", "");
        } catch (Exception e) {
            // Default to client on error
            runType = RunType.CLIENT;
        }
    }
    
    @Override
    public void writeExternal(@NotNull org.jdom.Element element) {
        super.writeExternal(element);
        element.setAttribute("runType", runType.name());
        element.setAttribute("enableDebug", String.valueOf(enableDebug));
        element.setAttribute("vmArgs", vmArgs);
        element.setAttribute("programArgs", programArgs);
    }
    
    @NotNull
    @Override
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new MinecraftRunConfigurationEditor(getProject());
    }
    
    @Nullable
    @Override
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment) {
        return new MinecraftRunProfileState(environment, this);
    }
    
    @Override
    public @NotNull ConfigurationFactory getFactory() {
        return super.getFactory();
    }
    
    public RunType getRunType() {
        return runType;
    }
    
    public void setRunType(RunType runType) {
        this.runType = runType;
    }
    
    public boolean isEnableDebug() {
        return enableDebug;
    }
    
    public void setEnableDebug(boolean enableDebug) {
        this.enableDebug = enableDebug;
    }
    
    public String getVmArgs() {
        return vmArgs;
    }
    
    public void setVmArgs(String vmArgs) {
        this.vmArgs = vmArgs;
    }
    
    public String getProgramArgs() {
        return programArgs;
    }
    
    public void setProgramArgs(String programArgs) {
        this.programArgs = programArgs;
    }
    
    @Override
    public String suggestedName() {
        return runType.getDisplayName();
    }
}