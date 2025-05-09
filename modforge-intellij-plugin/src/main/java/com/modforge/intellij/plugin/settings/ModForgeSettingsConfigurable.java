package com.modforge.intellij.plugin.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.JBIntSpinner;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.util.Objects;

/**
 * Configurable for ModForge settings.
 * This class provides UI for editing ModForge settings.
 */
public final class ModForgeSettingsConfigurable implements Configurable {
    private JPanel mainPanel;
    private JBPasswordField apiKeyField;
    private ComboBox<String> modelComboBox;
    private JBIntSpinner maxTokensSpinner;
    private JBIntSpinner temperatureSpinner;
    private JBCheckBox usePatternRecognitionCheckBox;
    private JBCheckBox syncWithWebEnabledCheckBox;
    private JBTextField webSyncUrlField;
    private JBPasswordField webSyncApiKeyField;
    
    private boolean isModified = false;
    
    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "ModForge";
    }
    
    @Nullable
    @Override
    public JComponent createComponent() {
        // Create API key field
        apiKeyField = new JBPasswordField();
        apiKeyField.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent e) {
                isModified = true;
            }
        });
        
        // Create model combo box
        modelComboBox = new ComboBox<>(new String[] {
                "gpt-4",
                "gpt-4-32k",
                "gpt-3.5-turbo",
                "gpt-3.5-turbo-16k"
        });
        modelComboBox.addActionListener(e -> isModified = true);
        
        // Create max tokens spinner
        maxTokensSpinner = new JBIntSpinner(2048, 100, 32000, 100);
        maxTokensSpinner.addChangeListener(e -> isModified = true);
        
        // Create temperature spinner (0.0 to 1.0 in 0.1 increments)
        SpinnerNumberModel temperatureModel = new SpinnerNumberModel(0.7, 0.0, 1.0, 0.1);
        temperatureSpinner = new JBIntSpinner(temperatureModel);
        temperatureSpinner.addChangeListener(e -> isModified = true);
        
        // Create pattern recognition checkbox
        usePatternRecognitionCheckBox = new JBCheckBox("Use pattern recognition to reduce API usage");
        usePatternRecognitionCheckBox.addActionListener(e -> isModified = true);
        
        // Create sync with web enabled checkbox
        syncWithWebEnabledCheckBox = new JBCheckBox("Enable synchronization with web platform");
        syncWithWebEnabledCheckBox.addActionListener(e -> {
            isModified = true;
            updateWebSyncFields();
        });
        
        // Create web sync URL field
        webSyncUrlField = new JBTextField();
        webSyncUrlField.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent e) {
                isModified = true;
            }
        });
        
        // Create web sync API key field
        webSyncApiKeyField = new JBPasswordField();
        webSyncApiKeyField.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent e) {
                isModified = true;
            }
        });
        
        // Create main panel
        mainPanel = FormBuilder.createFormBuilder()
                .addComponent(new JBLabel("OpenAI Settings"))
                .addVerticalGap(5)
                .addLabeledComponent("API Key:", apiKeyField)
                .addLabeledComponent("Model:", modelComboBox)
                .addLabeledComponent("Max Tokens:", maxTokensSpinner)
                .addLabeledComponent("Temperature:", temperatureSpinner)
                .addComponent(usePatternRecognitionCheckBox)
                .addVerticalGap(20)
                .addComponent(new JBLabel("Web Platform Integration"))
                .addVerticalGap(5)
                .addComponent(syncWithWebEnabledCheckBox)
                .addLabeledComponent("URL:", webSyncUrlField)
                .addLabeledComponent("API Key:", webSyncApiKeyField)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
        
        mainPanel.setBorder(JBUI.Borders.empty(10));
        
        // Load settings
        reset();
        
        return mainPanel;
    }
    
    @Override
    public boolean isModified() {
        return isModified;
    }
    
    @Override
    public void apply() throws ConfigurationException {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        // Validate and save settings
        if (apiKeyField.getPassword().length == 0) {
            throw new ConfigurationException("OpenAI API Key is required. Get one at https://platform.openai.com/api-keys", "Missing API Key");
        }
        
        // Get values
        String apiKey = new String(apiKeyField.getPassword());
        String model = (String) modelComboBox.getSelectedItem();
        int maxTokens = maxTokensSpinner.getNumber();
        double temperature = ((Number) temperatureSpinner.getValue()).doubleValue();
        boolean usePatternRecognition = usePatternRecognitionCheckBox.isSelected();
        boolean syncWithWebEnabled = syncWithWebEnabledCheckBox.isSelected();
        String webSyncUrl = webSyncUrlField.getText();
        String webSyncApiKey = new String(webSyncApiKeyField.getPassword());
        
        // Validate web sync settings if enabled
        if (syncWithWebEnabled) {
            if (webSyncUrl.isEmpty()) {
                throw new ConfigurationException("Web Sync URL is required when synchronization is enabled", "Missing Web Sync URL");
            }
            
            if (webSyncApiKey.isEmpty()) {
                throw new ConfigurationException("Web Sync API Key is required when synchronization is enabled", "Missing Web Sync API Key");
            }
        }
        
        // Update settings
        settings.setOpenAiApiKey(apiKey);
        settings.setOpenAiModel(model);
        settings.setMaxTokens(maxTokens);
        settings.setTemperature(temperature);
        settings.setUsePatternRecognition(usePatternRecognition);
        settings.setSyncWithWebEnabled(syncWithWebEnabled);
        settings.setWebSyncUrl(webSyncUrl);
        settings.setWebSyncApiKey(webSyncApiKey);
        
        isModified = false;
    }
    
    @Override
    public void reset() {
        ModForgeSettings settings = ModForgeSettings.getInstance();
        
        // Load settings
        apiKeyField.setText(settings.getOpenAiApiKey());
        modelComboBox.setSelectedItem(settings.getOpenAiModel());
        maxTokensSpinner.setNumber(settings.getMaxTokens());
        temperatureSpinner.setValue(settings.getTemperature());
        usePatternRecognitionCheckBox.setSelected(settings.isUsePatternRecognition());
        syncWithWebEnabledCheckBox.setSelected(settings.isSyncWithWebEnabled());
        webSyncUrlField.setText(settings.getWebSyncUrl());
        webSyncApiKeyField.setText(settings.getWebSyncApiKey());
        
        // Update UI state
        updateWebSyncFields();
        
        isModified = false;
    }
    
    @Override
    public void disposeUIResources() {
        mainPanel = null;
        apiKeyField = null;
        modelComboBox = null;
        maxTokensSpinner = null;
        temperatureSpinner = null;
        usePatternRecognitionCheckBox = null;
        syncWithWebEnabledCheckBox = null;
        webSyncUrlField = null;
        webSyncApiKeyField = null;
    }
    
    /**
     * Updates the web sync fields based on the enabled state.
     */
    private void updateWebSyncFields() {
        boolean enabled = syncWithWebEnabledCheckBox.isSelected();
        
        webSyncUrlField.setEnabled(enabled);
        webSyncApiKeyField.setEnabled(enabled);
    }
}