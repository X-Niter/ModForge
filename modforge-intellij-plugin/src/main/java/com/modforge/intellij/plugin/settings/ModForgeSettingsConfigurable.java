package com.modforge.intellij.plugin.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UI;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

/**
 * Configurable component for plugin settings.
 * This class is responsible for creating the settings UI and handling changes.
 */
public final class ModForgeSettingsConfigurable implements Configurable {
    private JPanel mainPanel;
    private JBPasswordField openAiApiKeyField;
    private JBCheckBox continuousDevelopmentCheckBox;
    private ComboBox<String> continuousDevelopmentIntervalComboBox;
    private JBCheckBox patternRecognitionCheckBox;
    private JBCheckBox syncWithWebCheckBox;
    private JBTextField webApiUrlField;
    private JBPasswordField webApiKeyField;
    
    private final ModForgeSettings settings;
    
    /**
     * Creates a new ModForgeSettingsConfigurable.
     */
    public ModForgeSettingsConfigurable() {
        this.settings = ModForgeSettings.getInstance();
    }
    
    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) String getDisplayName() {
        return "ModForge";
    }
    
    @Override
    public @Nullable JComponent createComponent() {
        // Create UI components
        openAiApiKeyField = new JBPasswordField();
        openAiApiKeyField.setColumns(30);
        
        continuousDevelopmentCheckBox = new JBCheckBox("Enable continuous development");
        
        continuousDevelopmentIntervalComboBox = new ComboBox<>(new String[] {
                "30 seconds",
                "1 minute",
                "5 minutes",
                "15 minutes",
                "30 minutes",
                "1 hour"
        });
        
        patternRecognitionCheckBox = new JBCheckBox("Enable pattern recognition");
        
        syncWithWebCheckBox = new JBCheckBox("Enable sync with web platform");
        
        webApiUrlField = new JBTextField();
        webApiUrlField.setColumns(30);
        
        webApiKeyField = new JBPasswordField();
        webApiKeyField.setColumns(30);
        
        // Create sections
        JPanel aiSection = FormBuilder.createFormBuilder()
                .addLabeledComponent("OpenAI API Key:", openAiApiKeyField)
                .addComponent(patternRecognitionCheckBox)
                .addVerticalGap(10)
                .getPanel();
        
        JPanel continuousDevSection = FormBuilder.createFormBuilder()
                .addComponent(continuousDevelopmentCheckBox)
                .addLabeledComponent("Check Interval:", continuousDevelopmentIntervalComboBox)
                .addVerticalGap(10)
                .getPanel();
        
        JPanel syncSection = FormBuilder.createFormBuilder()
                .addComponent(syncWithWebCheckBox)
                .addLabeledComponent("Web API URL:", webApiUrlField)
                .addLabeledComponent("Web API Key:", webApiKeyField)
                .addVerticalGap(10)
                .getPanel();
        
        // Create main panel with sections
        mainPanel = FormBuilder.createFormBuilder()
                .addComponent(new JBLabel("AI Settings", JBLabel.LEFT))
                .addComponentFillVertically(JBUI.Panels.simplePanel().addToCenter(aiSection), 0)
                .addVerticalGap(20)
                .addComponent(new JBLabel("Continuous Development", JBLabel.LEFT))
                .addComponentFillVertically(JBUI.Panels.simplePanel().addToCenter(continuousDevSection), 0)
                .addVerticalGap(20)
                .addComponent(new JBLabel("Web Sync", JBLabel.LEFT))
                .addComponentFillVertically(JBUI.Panels.simplePanel().addToCenter(syncSection), 0)
                .addVerticalGap(10)
                .getPanel();
        
        mainPanel.setBorder(JBUI.Borders.empty(10));
        
        // Add listeners
        continuousDevelopmentCheckBox.addActionListener(e -> {
            continuousDevelopmentIntervalComboBox.setEnabled(continuousDevelopmentCheckBox.isSelected());
        });
        
        syncWithWebCheckBox.addActionListener(e -> {
            webApiUrlField.setEnabled(syncWithWebCheckBox.isSelected());
            webApiKeyField.setEnabled(syncWithWebCheckBox.isSelected());
        });
        
        // Load settings
        reset();
        
        return mainPanel;
    }
    
    @Override
    public boolean isModified() {
        // Compare current settings with UI values
        if (!Objects.equals(String.valueOf(openAiApiKeyField.getPassword()), settings.getOpenAiApiKey())) {
            return true;
        }
        
        if (continuousDevelopmentCheckBox.isSelected() != settings.isContinuousDevelopmentEnabled()) {
            return true;
        }
        
        if (getSelectedIntervalMs() != settings.getContinuousDevelopmentInterval()) {
            return true;
        }
        
        if (patternRecognitionCheckBox.isSelected() != settings.isPatternRecognitionEnabled()) {
            return true;
        }
        
        if (syncWithWebCheckBox.isSelected() != settings.isSyncWithWebEnabled()) {
            return true;
        }
        
        if (!Objects.equals(webApiUrlField.getText(), settings.getWebApiUrl())) {
            return true;
        }
        
        if (!Objects.equals(String.valueOf(webApiKeyField.getPassword()), settings.getWebApiKey())) {
            return true;
        }
        
        return false;
    }
    
    @Override
    public void apply() {
        // Save settings
        settings.setOpenAiApiKey(String.valueOf(openAiApiKeyField.getPassword()));
        settings.setContinuousDevelopmentEnabled(continuousDevelopmentCheckBox.isSelected());
        settings.setContinuousDevelopmentInterval(getSelectedIntervalMs());
        settings.setPatternRecognitionEnabled(patternRecognitionCheckBox.isSelected());
        settings.setSyncWithWebEnabled(syncWithWebCheckBox.isSelected());
        settings.setWebApiUrl(webApiUrlField.getText());
        settings.setWebApiKey(String.valueOf(webApiKeyField.getPassword()));
    }
    
    @Override
    public void reset() {
        // Load settings to UI
        openAiApiKeyField.setText(settings.getOpenAiApiKey());
        continuousDevelopmentCheckBox.setSelected(settings.isContinuousDevelopmentEnabled());
        setSelectedInterval(settings.getContinuousDevelopmentInterval());
        patternRecognitionCheckBox.setSelected(settings.isPatternRecognitionEnabled());
        syncWithWebCheckBox.setSelected(settings.isSyncWithWebEnabled());
        webApiUrlField.setText(settings.getWebApiUrl());
        webApiKeyField.setText(settings.getWebApiKey());
        
        // Update enabled state
        continuousDevelopmentIntervalComboBox.setEnabled(continuousDevelopmentCheckBox.isSelected());
        webApiUrlField.setEnabled(syncWithWebCheckBox.isSelected());
        webApiKeyField.setEnabled(syncWithWebCheckBox.isSelected());
    }
    
    @Override
    public void disposeUIResources() {
        mainPanel = null;
        openAiApiKeyField = null;
        continuousDevelopmentCheckBox = null;
        continuousDevelopmentIntervalComboBox = null;
        patternRecognitionCheckBox = null;
        syncWithWebCheckBox = null;
        webApiUrlField = null;
        webApiKeyField = null;
    }
    
    /**
     * Gets the selected interval in milliseconds.
     * @return The selected interval in milliseconds
     */
    private long getSelectedIntervalMs() {
        String selected = (String) continuousDevelopmentIntervalComboBox.getSelectedItem();
        
        if (selected == null) {
            return 60_000; // 1 minute default
        }
        
        return switch (selected) {
            case "30 seconds" -> 30_000L;
            case "5 minutes" -> 300_000L;
            case "15 minutes" -> 900_000L;
            case "30 minutes" -> 1_800_000L;
            case "1 hour" -> 3_600_000L;
            default -> 60_000L; // 1 minute default
        };
    }
    
    /**
     * Sets the selected interval based on milliseconds.
     * @param intervalMs The interval in milliseconds
     */
    private void setSelectedInterval(long intervalMs) {
        String interval = switch ((int) (intervalMs / 1000)) {
            case 30 -> "30 seconds";
            case 300 -> "5 minutes";
            case 900 -> "15 minutes";
            case 1800 -> "30 minutes";
            case 3600 -> "1 hour";
            default -> "1 minute";
        };
        
        continuousDevelopmentIntervalComboBox.setSelectedItem(interval);
    }
}