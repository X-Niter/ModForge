package com.modforge.intellij.plugin.run;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * Run configuration type for ModForge-managed Minecraft runs.
 * Supports client, server, and data generation runs with automated setup.
 */
public class MinecraftRunConfigurationType implements ConfigurationType {
    private static final String ID = "MODFORGE_MINECRAFT_RUN_CONFIGURATION";
    private static final String DISPLAY_NAME = "ModForge Minecraft";
    private static final String DESCRIPTION = "Run Minecraft client, server, or data generation with ModForge";
    private static final Icon ICON = IconLoader.getIcon("/icons/modforge.svg", MinecraftRunConfigurationType.class);
    
    private final ConfigurationFactory myFactory;
    
    public MinecraftRunConfigurationType() {
        myFactory = new ConfigurationFactory(this) {
            @Override
            public @NotNull RunConfiguration createTemplateConfiguration(@NotNull Project project) {
                return new MinecraftRunConfiguration(project, this, "ModForge Minecraft");
            }
            
            @Override
            public @NotNull String getId() {
                return "ModForge Minecraft Run Configuration Factory";
            }
        };
    }
    
    @Override
    public @NotNull String getDisplayName() {
        return DISPLAY_NAME;
    }
    
    @Override
    public @NotNull String getConfigurationTypeDescription() {
        return DESCRIPTION;
    }
    
    @Override
    public Icon getIcon() {
        return ICON;
    }
    
    @Override
    public @NotNull String getId() {
        return ID;
    }
    
    @Override
    public ConfigurationFactory[] getConfigurationFactories() {
        return new ConfigurationFactory[] { myFactory };
    }
}