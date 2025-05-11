package com.modforge.intellij.plugin.run;

import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UI;
import com.modforge.intellij.plugin.run.MinecraftRunConfiguration.RunType;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

/**
 * Editor for Minecraft run configurations.
 * Provides UI for configuring Minecraft run settings.
 */
public class MinecraftRunConfigurationEditor extends SettingsEditor<MinecraftRunConfiguration> {
    
    private final JPanel myPanel;
    private final ComboBox<RunType> runTypeComboBox;
    private final JBCheckBox enableDebugCheckBox;
    private final JBTextField vmArgsField;
    private final JBTextField programArgsField;
    private final ComboBox<String> modLoaderComboBox;
    
    public MinecraftRunConfigurationEditor(Project project) {
        // Run type selection
        runTypeComboBox = new ComboBox<>(RunType.values());
        runTypeComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof RunType) {
                    setText(((RunType) value).getDisplayName());
                }
                return this;
            }
        });
        
        // Debug option
        enableDebugCheckBox = new JBCheckBox("Enable debugging");
        
        // VM arguments
        vmArgsField = new JBTextField();
        vmArgsField.getEmptyText().setText("VM arguments (e.g., -Xmx2G -XX:+UseG1GC)");
        
        // Program arguments
        programArgsField = new JBTextField();
        programArgsField.getEmptyText().setText("Program arguments");
        
        // Mod loader selection
        modLoaderComboBox = new ComboBox<>(new String[]{"FORGE", "FABRIC", "QUILT", "ARCHITECTURY"});
        
        // Build the panel
        myPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("Run Type:"), runTypeComboBox, 1, false)
                .addComponent(enableDebugCheckBox)
                .addVerticalGap(10)
                .addLabeledComponent(new JBLabel("Mod Loader:"), modLoaderComboBox, 1, false)
                .addVerticalGap(10)
                .addLabeledComponent(new JBLabel("VM Arguments:"), vmArgsField, 1, false)
                .addLabeledComponent(new JBLabel("Program Arguments:"), programArgsField, 1, false)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
        
        myPanel.setPreferredSize(new Dimension(600, 400));
        myPanel.setBorder(JBUI.Borders.empty(10));
        
        // Add behavior based on run type selection
        runTypeComboBox.addActionListener(e -> {
            RunType selectedType = (RunType) runTypeComboBox.getSelectedItem();
            updateUIForRunType(selectedType);
        });
    }
    
    private void updateUIForRunType(RunType runType) {
        if (runType == null) return;
        
        switch (runType) {
            case CLIENT:
                // For client runs, offer specific client-related settings
                programArgsField.getEmptyText().setText("Client arguments (e.g., --username Dev)");
                break;
            case SERVER:
                // For server runs, offer server-specific settings
                programArgsField.getEmptyText().setText("Server arguments (e.g., --nogui)");
                break;
            case DATA_GEN:
                // For data generation runs, offer data gen specific settings
                programArgsField.getEmptyText().setText("Data gen arguments (e.g., --all)");
                break;
        }
    }
    
    @Override
    protected void resetEditorFrom(@NotNull MinecraftRunConfiguration configuration) {
        runTypeComboBox.setSelectedItem(configuration.getRunType());
        enableDebugCheckBox.setSelected(configuration.isEnableDebug());
        vmArgsField.setText(configuration.getVmArgs());
        programArgsField.setText(configuration.getProgramArgs());
        
        // Update UI based on selected run type
        updateUIForRunType(configuration.getRunType());
    }
    
    @Override
    protected void applyEditorTo(@NotNull MinecraftRunConfiguration configuration) {
        configuration.setRunType((RunType) runTypeComboBox.getSelectedItem());
        configuration.setEnableDebug(enableDebugCheckBox.isSelected());
        configuration.setVmArgs(vmArgsField.getText().trim());
        configuration.setProgramArgs(programArgsField.getText().trim());
    }
    
    @NotNull
    @Override
    protected JComponent createEditor() {
        return myPanel;
    }
}