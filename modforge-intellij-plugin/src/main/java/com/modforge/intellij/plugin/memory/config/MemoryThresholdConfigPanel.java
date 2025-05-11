package com.modforge.intellij.plugin.memory.config;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.ConfigurableBase;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration panel for memory thresholds
 */
public class MemoryThresholdConfigPanel extends ConfigurableBase<MemoryThresholdConfigPanel.Form, MemoryThresholdConfig.State> {
    private static final Logger LOG = Logger.getInstance(MemoryThresholdConfigPanel.class);
    
    private JPanel mainPanel;
    private JComboBox<String> environmentComboBox;
    private JButton addEnvironmentButton;
    private JButton removeEnvironmentButton;
    private JBCheckBox autoDetectCheckBox;
    
    // Threshold inputs
    private JSlider warningThresholdSlider;
    private JSlider criticalThresholdSlider;
    private JSlider emergencyThresholdSlider;
    private JLabel warningThresholdLabel;
    private JLabel criticalThresholdLabel;
    private JLabel emergencyThresholdLabel;
    
    private JSpinner availableMemoryWarningSpinner;
    private JSpinner availableMemoryCriticalSpinner;
    private JSpinner availableMemoryEmergencySpinner;
    
    private JSpinner memoryGrowthRateSpinner;
    
    // Current environment config
    private String currentEnvironment;
    private final Map<String, MemoryThresholdConfig.EnvironmentConfig> configCache = new HashMap<>();
    
    public MemoryThresholdConfigPanel() {
        super("com.modforge.intellij.plugin.memory.config.MemoryThresholdConfigPanel", 
              "Memory Thresholds", 
              "ModForge.MemoryThresholds");
    }
    
    @Override
    protected @NotNull MemoryThresholdConfig.State getSettings() {
        MemoryThresholdConfig config = MemoryThresholdConfig.getInstance();
        MemoryThresholdConfig.State state = config.getState();
        if (state == null) {
            // Fallback to default state if null
            LOG.warn("Memory threshold config state is null, creating default state");
            state = new MemoryThresholdConfig.State();
            state.environmentConfigs.put("default", new MemoryThresholdConfig.EnvironmentConfig());
        }
        return state;
    }
    
    @Override
    protected Runnable enableSearch(String option) {
        return null; // No search functionality
    }
    
    @Override
    public String getDisplayName() {
        return "Memory Thresholds";
    }
    
    @Override
    public String getHelpTopic() {
        return "Memory Thresholds Configuration";
    }
    
    @Override
    protected @Nullable Form createPanel() {
        return new Form();
    }
    
    /**
     * The Form class for the configurable
     */
    public class Form {
        private final JPanel panel;
        
        Form() {
            createComponents();
            
            JPanel environmentPanel = createEnvironmentPanel();
            JPanel thresholdsPanel = createThresholdsPanel();
            
            mainPanel = new JPanel(new BorderLayout());
            
            JPanel contentPanel = FormBuilder.createFormBuilder()
                .addComponent(environmentPanel)
                .addVerticalGap(10)
                .addComponent(thresholdsPanel)
                .getPanel();
            
            contentPanel.setBorder(JBUI.Borders.empty(10));
            
            mainPanel.add(new JBScrollPane(contentPanel), BorderLayout.CENTER);
            
            loadSettings();
            setupListeners();
            
            panel = mainPanel;
        }
        
        JComponent getComponent() {
            return panel;
        }
    }
    
    /**
     * Create UI components
     */
    private void createComponents() {
        // Environment selection
        environmentComboBox = new ComboBox<>();
        addEnvironmentButton = new JButton("Add");
        removeEnvironmentButton = new JButton("Remove");
        autoDetectCheckBox = new JBCheckBox("Auto-detect environment");
        
        // Threshold sliders
        warningThresholdSlider = new JSlider(50, 95, 70);
        criticalThresholdSlider = new JSlider(55, 98, 85);
        emergencyThresholdSlider = new JSlider(60, 99, 95);
        
        warningThresholdSlider.setMajorTickSpacing(5);
        warningThresholdSlider.setMinorTickSpacing(1);
        warningThresholdSlider.setPaintTicks(true);
        warningThresholdSlider.setPaintLabels(true);
        
        criticalThresholdSlider.setMajorTickSpacing(5);
        criticalThresholdSlider.setMinorTickSpacing(1);
        criticalThresholdSlider.setPaintTicks(true);
        criticalThresholdSlider.setPaintLabels(true);
        
        emergencyThresholdSlider.setMajorTickSpacing(5);
        emergencyThresholdSlider.setMinorTickSpacing(1);
        emergencyThresholdSlider.setPaintTicks(true);
        emergencyThresholdSlider.setPaintLabels(true);
        
        warningThresholdLabel = new JLabel("Warning threshold: 70%");
        criticalThresholdLabel = new JLabel("Critical threshold: 85%");
        emergencyThresholdLabel = new JLabel("Emergency threshold: 95%");
        
        // Memory spinners
        availableMemoryWarningSpinner = new JSpinner(new SpinnerNumberModel(512, 64, 4096, 64));
        availableMemoryCriticalSpinner = new JSpinner(new SpinnerNumberModel(256, 32, 2048, 32));
        availableMemoryEmergencySpinner = new JSpinner(new SpinnerNumberModel(128, 16, 1024, 16));
        
        memoryGrowthRateSpinner = new JSpinner(new SpinnerNumberModel(1.0, 0.1, 10.0, 0.1));
    }
    
    /**
     * Create the environment selection panel
     * 
     * @return The environment panel
     */
    private JPanel createEnvironmentPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Environment"));
        
        JPanel comboPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        comboPanel.add(new JLabel("Environment:"));
        comboPanel.add(environmentComboBox);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.add(addEnvironmentButton);
        buttonPanel.add(removeEnvironmentButton);
        
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(comboPanel, BorderLayout.WEST);
        topPanel.add(buttonPanel, BorderLayout.EAST);
        
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(autoDetectCheckBox, BorderLayout.SOUTH);
        
        return panel;
    }
    
    /**
     * Create the thresholds panel
     * 
     * @return The thresholds panel
     */
    private JPanel createThresholdsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Memory Thresholds"));
        
        JPanel slidersPanel = new JPanel(new GridLayout(6, 1, 0, 10));
        
        // Percentage thresholds
        slidersPanel.add(warningThresholdLabel);
        slidersPanel.add(warningThresholdSlider);
        slidersPanel.add(criticalThresholdLabel);
        slidersPanel.add(criticalThresholdSlider);
        slidersPanel.add(emergencyThresholdLabel);
        slidersPanel.add(emergencyThresholdSlider);
        
        // Available memory thresholds
        JPanel availableMemoryPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        availableMemoryPanel.setBorder(BorderFactory.createTitledBorder("Available Memory Thresholds (MB)"));
        
        availableMemoryPanel.add(new JLabel("Warning threshold:"));
        availableMemoryPanel.add(availableMemoryWarningSpinner);
        availableMemoryPanel.add(new JLabel("Critical threshold:"));
        availableMemoryPanel.add(availableMemoryCriticalSpinner);
        availableMemoryPanel.add(new JLabel("Emergency threshold:"));
        availableMemoryPanel.add(availableMemoryEmergencySpinner);
        
        // Memory growth rate threshold
        JPanel growthPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        growthPanel.setBorder(BorderFactory.createTitledBorder("Memory Growth Rate Threshold"));
        growthPanel.add(new JLabel("Memory growth rate threshold (% per minute):"));
        growthPanel.add(memoryGrowthRateSpinner);
        
        JPanel innerPanel = new JPanel(new BorderLayout());
        innerPanel.add(slidersPanel, BorderLayout.NORTH);
        innerPanel.add(availableMemoryPanel, BorderLayout.CENTER);
        innerPanel.add(growthPanel, BorderLayout.SOUTH);
        
        panel.add(innerPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Set up listeners for components
     */
    private void setupListeners() {
        // Environment combo box
        environmentComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                saveCurrentConfig();
                currentEnvironment = (String) environmentComboBox.getSelectedItem();
                loadEnvironmentConfig(currentEnvironment);
            }
        });
        
        // Add environment button
        addEnvironmentButton.addActionListener(e -> {
            String name = JOptionPane.showInputDialog(mainPanel, 
                    "Enter environment name:", 
                    "Add Environment", 
                    JOptionPane.PLAIN_MESSAGE);
            
            if (name != null && !name.isEmpty()) {
                // Check if it already exists
                if (Arrays.asList(MemoryThresholdConfig.getInstance().getAvailableEnvironments()).contains(name)) {
                    JOptionPane.showMessageDialog(mainPanel, 
                            "Environment already exists", 
                            "Error", 
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                // Add to configs
                MemoryThresholdConfig.EnvironmentConfig config = new MemoryThresholdConfig.EnvironmentConfig();
                configCache.put(name, config);
                
                // Update combo box
                environmentComboBox.addItem(name);
                environmentComboBox.setSelectedItem(name);
            }
        });
        
        // Remove environment button
        removeEnvironmentButton.addActionListener(e -> {
            String name = (String) environmentComboBox.getSelectedItem();
            if (name != null && !name.equals("default")) {
                int result = JOptionPane.showConfirmDialog(mainPanel, 
                        "Are you sure you want to remove environment '" + name + "'?", 
                        "Remove Environment", 
                        JOptionPane.YES_NO_OPTION);
                
                if (result == JOptionPane.YES_OPTION) {
                    // Remove from configs
                    configCache.remove(name);
                    
                    // Update combo box
                    environmentComboBox.removeItem(name);
                    environmentComboBox.setSelectedItem("default");
                }
            } else {
                JOptionPane.showMessageDialog(mainPanel, 
                        "Cannot remove default environment", 
                        "Error", 
                        JOptionPane.ERROR_MESSAGE);
            }
        });
        
        // Auto-detect checkbox
        autoDetectCheckBox.addItemListener(e -> {
            MemoryThresholdConfig config = MemoryThresholdConfig.getInstance();
            config.setAutoDetectEnvironment(autoDetectCheckBox.isSelected());
        });
        
        // Update threshold labels when sliders change
        warningThresholdSlider.addChangeListener(e -> 
            warningThresholdLabel.setText("Warning threshold: " + warningThresholdSlider.getValue() + "%"));
        
        criticalThresholdSlider.addChangeListener(e -> 
            criticalThresholdLabel.setText("Critical threshold: " + criticalThresholdSlider.getValue() + "%"));
        
        emergencyThresholdSlider.addChangeListener(e -> 
            emergencyThresholdLabel.setText("Emergency threshold: " + emergencyThresholdSlider.getValue() + "%"));
        
        // Ensure thresholds are in order
        warningThresholdSlider.addChangeListener(e -> {
            if (warningThresholdSlider.getValue() >= criticalThresholdSlider.getValue()) {
                criticalThresholdSlider.setValue(warningThresholdSlider.getValue() + 1);
            }
        });
        
        criticalThresholdSlider.addChangeListener(e -> {
            if (criticalThresholdSlider.getValue() <= warningThresholdSlider.getValue()) {
                warningThresholdSlider.setValue(criticalThresholdSlider.getValue() - 1);
            }
            if (criticalThresholdSlider.getValue() >= emergencyThresholdSlider.getValue()) {
                emergencyThresholdSlider.setValue(criticalThresholdSlider.getValue() + 1);
            }
        });
        
        emergencyThresholdSlider.addChangeListener(e -> {
            if (emergencyThresholdSlider.getValue() <= criticalThresholdSlider.getValue()) {
                criticalThresholdSlider.setValue(emergencyThresholdSlider.getValue() - 1);
            }
        });
        
        // Ensure available memory thresholds are in order
        availableMemoryWarningSpinner.addChangeListener(e -> {
            int warningValue = (Integer) availableMemoryWarningSpinner.getValue();
            int criticalValue = (Integer) availableMemoryCriticalSpinner.getValue();
            
            if (warningValue <= criticalValue) {
                availableMemoryCriticalSpinner.setValue(warningValue - 32);
            }
        });
        
        availableMemoryCriticalSpinner.addChangeListener(e -> {
            int warningValue = (Integer) availableMemoryWarningSpinner.getValue();
            int criticalValue = (Integer) availableMemoryCriticalSpinner.getValue();
            int emergencyValue = (Integer) availableMemoryEmergencySpinner.getValue();
            
            if (criticalValue >= warningValue) {
                availableMemoryWarningSpinner.setValue(criticalValue + 32);
            }
            
            if (criticalValue <= emergencyValue) {
                availableMemoryEmergencySpinner.setValue(criticalValue - 16);
            }
        });
        
        availableMemoryEmergencySpinner.addChangeListener(e -> {
            int criticalValue = (Integer) availableMemoryCriticalSpinner.getValue();
            int emergencyValue = (Integer) availableMemoryEmergencySpinner.getValue();
            
            if (emergencyValue >= criticalValue) {
                availableMemoryCriticalSpinner.setValue(emergencyValue + 16);
            }
        });
    }
    
    /**
     * Load settings from the configuration
     */
    private void loadSettings() {
        MemoryThresholdConfig config = MemoryThresholdConfig.getInstance();
        
        // Load environments
        environmentComboBox.removeAllItems();
        for (String env : config.getAvailableEnvironments()) {
            environmentComboBox.addItem(env);
            configCache.put(env, config.getEnvironmentConfig(env));
        }
        
        // Set selected environment
        currentEnvironment = config.getCurrentEnvironmentName();
        environmentComboBox.setSelectedItem(currentEnvironment);
        
        // Set auto-detect
        autoDetectCheckBox.setSelected(config.getState().autoDetectEnvironment);
        
        // Load environment config
        loadEnvironmentConfig(currentEnvironment);
    }
    
    /**
     * Load environment configuration
     * 
     * @param environmentName The environment name
     */
    private void loadEnvironmentConfig(String environmentName) {
        MemoryThresholdConfig.EnvironmentConfig envConfig = configCache.get(environmentName);
        if (envConfig == null) {
            envConfig = new MemoryThresholdConfig.EnvironmentConfig();
            configCache.put(environmentName, envConfig);
        }
        
        // Set thresholds
        warningThresholdSlider.setValue(envConfig.warningThresholdPercent);
        criticalThresholdSlider.setValue(envConfig.criticalThresholdPercent);
        emergencyThresholdSlider.setValue(envConfig.emergencyThresholdPercent);
        
        warningThresholdLabel.setText("Warning threshold: " + envConfig.warningThresholdPercent + "%");
        criticalThresholdLabel.setText("Critical threshold: " + envConfig.criticalThresholdPercent + "%");
        emergencyThresholdLabel.setText("Emergency threshold: " + envConfig.emergencyThresholdPercent + "%");
        
        // Set available memory thresholds
        availableMemoryWarningSpinner.setValue(envConfig.availableMemoryWarningMb);
        availableMemoryCriticalSpinner.setValue(envConfig.availableMemoryCriticalMb);
        availableMemoryEmergencySpinner.setValue(envConfig.availableMemoryEmergencyMb);
        
        // Set memory growth rate threshold
        memoryGrowthRateSpinner.setValue(envConfig.memoryGrowthRateThresholdPctPerMin);
    }
    
    /**
     * Save the current environment configuration
     */
    private void saveCurrentConfig() {
        if (currentEnvironment == null) {
            return;
        }
        
        MemoryThresholdConfig.EnvironmentConfig envConfig = configCache.get(currentEnvironment);
        if (envConfig == null) {
            envConfig = new MemoryThresholdConfig.EnvironmentConfig();
            configCache.put(currentEnvironment, envConfig);
        }
        
        // Save thresholds
        envConfig.warningThresholdPercent = warningThresholdSlider.getValue();
        envConfig.criticalThresholdPercent = criticalThresholdSlider.getValue();
        envConfig.emergencyThresholdPercent = emergencyThresholdSlider.getValue();
        
        // Save available memory thresholds
        envConfig.availableMemoryWarningMb = (Integer) availableMemoryWarningSpinner.getValue();
        envConfig.availableMemoryCriticalMb = (Integer) availableMemoryCriticalSpinner.getValue();
        envConfig.availableMemoryEmergencyMb = (Integer) availableMemoryEmergencySpinner.getValue();
        
        // Save memory growth rate threshold
        envConfig.memoryGrowthRateThresholdPctPerMin = (Double) memoryGrowthRateSpinner.getValue();
    }
    
    @Override
    public void apply() {
        // Save current config
        saveCurrentConfig();
        
        // Apply all configs
        MemoryThresholdConfig config = MemoryThresholdConfig.getInstance();
        
        for (Map.Entry<String, MemoryThresholdConfig.EnvironmentConfig> entry : configCache.entrySet()) {
            config.saveEnvironmentConfig(entry.getKey(), entry.getValue());
        }
        
        // Set current environment
        if (!autoDetectCheckBox.isSelected()) {
            config.setSelectedEnvironment((String) environmentComboBox.getSelectedItem());
        }
        
        config.setAutoDetectEnvironment(autoDetectCheckBox.isSelected());
    }
    
    @Override
    public boolean isModified() {
        MemoryThresholdConfig config = MemoryThresholdConfig.getInstance();
        
        // Check if environment list has changed
        if (!Arrays.equals(config.getAvailableEnvironments(), configCache.keySet().toArray(new String[0]))) {
            return true;
        }
        
        // Check if current environment has changed
        if (autoDetectCheckBox.isSelected() != config.getState().autoDetectEnvironment) {
            return true;
        }
        
        // Check if current environment config has changed
        MemoryThresholdConfig.EnvironmentConfig currentEnvConfig = config.getEnvironmentConfig(currentEnvironment);
        MemoryThresholdConfig.EnvironmentConfig cachedEnvConfig = configCache.get(currentEnvironment);
        
        if (cachedEnvConfig == null) {
            return true;
        }
        
        if (warningThresholdSlider.getValue() != currentEnvConfig.warningThresholdPercent ||
                criticalThresholdSlider.getValue() != currentEnvConfig.criticalThresholdPercent ||
                emergencyThresholdSlider.getValue() != currentEnvConfig.emergencyThresholdPercent ||
                (Integer) availableMemoryWarningSpinner.getValue() != currentEnvConfig.availableMemoryWarningMb ||
                (Integer) availableMemoryCriticalSpinner.getValue() != currentEnvConfig.availableMemoryCriticalMb ||
                (Integer) availableMemoryEmergencySpinner.getValue() != currentEnvConfig.availableMemoryEmergencyMb ||
                (Double) memoryGrowthRateSpinner.getValue() != currentEnvConfig.memoryGrowthRateThresholdPctPerMin) {
            return true;
        }
        
        return false;
    }
    
    @Override
    public void reset() {
        loadSettings();
    }
}