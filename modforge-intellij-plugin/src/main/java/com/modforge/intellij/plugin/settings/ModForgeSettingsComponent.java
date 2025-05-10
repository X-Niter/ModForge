package com.modforge.intellij.plugin.settings;

import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UI;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;

/**
 * Component for ModForge settings UI.
 */
public class ModForgeSettingsComponent {
    private final JPanel mainPanel;
    private final JBTextField serverUrlField = new JBTextField();
    private final JBCheckBox continuousDevelopmentCheckbox = new JBCheckBox("Enable continuous development");
    private final JBCheckBox patternRecognitionCheckbox = new JBCheckBox("Enable AI pattern recognition");
    private final JSpinner pollingIntervalSpinner;
    
    public ModForgeSettingsComponent() {
        // Create polling interval spinner with model
        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(5, 1, 60, 1);
        pollingIntervalSpinner = new JSpinner(spinnerModel);
        
        // Create time unit label
        JLabel timeUnitLabel = new JBLabel("minutes");
        
        // Create polling interval panel
        JPanel pollingPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pollingPanel.add(new JBLabel("Polling interval:"));
        pollingPanel.add(pollingIntervalSpinner);
        pollingPanel.add(timeUnitLabel);
        
        // Add change listener to continuous development checkbox
        continuousDevelopmentCheckbox.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                // Enable/disable polling interval based on continuous development
                pollingIntervalSpinner.setEnabled(continuousDevelopmentCheckbox.isSelected());
                timeUnitLabel.setEnabled(continuousDevelopmentCheckbox.isSelected());
            }
        });
        
        // Build form
        mainPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("Server URL:"), serverUrlField, 1, false)
                .addComponent(continuousDevelopmentCheckbox)
                .addComponent(pollingPanel)
                .addComponent(patternRecognitionCheckbox)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
                
        // Set panel size
        mainPanel.setPreferredSize(new Dimension(450, 300));
    }
    
    public JPanel getPanel() {
        return mainPanel;
    }
    
    public JComponent getPreferredFocusedComponent() {
        return serverUrlField;
    }
    
    public String getServerUrl() {
        return serverUrlField.getText().trim();
    }
    
    public void setServerUrl(String url) {
        serverUrlField.setText(url);
    }
    
    public boolean isContinuousDevelopment() {
        return continuousDevelopmentCheckbox.isSelected();
    }
    
    public void setContinuousDevelopment(boolean selected) {
        continuousDevelopmentCheckbox.setSelected(selected);
        
        // Update dependent controls
        pollingIntervalSpinner.setEnabled(selected);
    }
    
    public boolean isPatternRecognition() {
        return patternRecognitionCheckbox.isSelected();
    }
    
    public void setPatternRecognition(boolean selected) {
        patternRecognitionCheckbox.setSelected(selected);
    }
    
    public int getPollingInterval() {
        // Convert minutes to milliseconds
        int minutes = (Integer) pollingIntervalSpinner.getValue();
        return minutes * 60 * 1000;
    }
    
    public void setPollingInterval(int milliseconds) {
        // Convert milliseconds to minutes
        int minutes = milliseconds / (60 * 1000);
        pollingIntervalSpinner.setValue(minutes);
    }
}