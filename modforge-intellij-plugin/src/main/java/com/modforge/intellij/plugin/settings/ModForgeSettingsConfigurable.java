package com.modforge.intellij.plugin.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.NlsContexts;
import com.modforge.intellij.plugin.ui.ModForgeConfigurable;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Configurable for ModForge settings.
 * This class manages the settings UI for the ModForge plugin.
 */
public class ModForgeSettingsConfigurable implements Configurable {
    private ModForgeConfigurable panel;
    
    /**
     * Gets the display name of the configurable.
     *
     * @return The display name
     */
    @Override
    public @NlsContexts.ConfigurableName String getDisplayName() {
        return "ModForge";
    }
    
    /**
     * Gets the help topic ID.
     *
     * @return The help topic ID
     */
    @Override
    public @Nullable String getHelpTopic() {
        return "preferences.ModForge";
    }
    
    /**
     * Creates the component for the settings UI.
     *
     * @return The component
     */
    @Override
    public @Nullable JComponent createComponent() {
        panel = new ModForgeConfigurable();
        return panel.getPanel();
    }
    
    /**
     * Checks if the settings have been modified.
     *
     * @return True if the settings have been modified, false otherwise
     */
    @Override
    public boolean isModified() {
        return panel != null && panel.isModified();
    }
    
    /**
     * Applies the settings.
     *
     * @throws ConfigurationException If there's an error applying the settings
     */
    @Override
    public void apply() throws ConfigurationException {
        if (panel != null) {
            panel.apply();
        }
    }
    
    /**
     * Resets the settings.
     */
    @Override
    public void reset() {
        if (panel != null) {
            panel.reset();
        }
    }
    
    /**
     * Disposes the UI resources.
     */
    @Override
    public void disposeUIResources() {
        panel = null;
    }
}