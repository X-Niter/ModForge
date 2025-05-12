package com.modforge.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDialog;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.modforge.intellij.plugin.services.ModForgeNotificationService;
import com.modforge.intellij.plugin.templates.ModTemplate;
import com.modforge.intellij.plugin.templates.ModTemplateService;
import com.modforge.intellij.plugin.templates.ModTemplateType;
import com.modforge.intellij.plugin.utils.CompatibilityUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Action for creating a new project from a template.
 */
public class CreateFromTemplateAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(CreateFromTemplateAction.class);
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        
        // Get the template service
        ModTemplateService templateService = project.getService(ModTemplateService.class);
        if (templateService == null) {
            ModForgeNotificationService notificationService = ModForgeNotificationService.getInstance();
            if (notificationService != null) {
                notificationService.showErrorDialog(
                        "Service Unavailable",
                        "Template service is not available."
                );
            } else {
                Messages.showErrorDialog(
                        project,
                        "Template service is not available.",
                        "Service Unavailable"
                );
            }
            return;
        }
        
        // Load templates
        AtomicReference<Throwable> error = new AtomicReference<>();
        templateService.loadTemplates()
                .exceptionally(ex -> {
                    error.set(ex);
                    return null;
                })
                .join();
        
        if (error.get() != null) {
            ModForgeNotificationService notificationService = ModForgeNotificationService.getInstance();
            if (notificationService != null) {
                notificationService.showErrorDialog(
                        "Error Loading Templates",
                        "Failed to load templates: " + error.get().getMessage()
                );
            } else {
                Messages.showErrorDialog(
                        project,
                        "Failed to load templates: " + error.get().getMessage(),
                        "Error Loading Templates"
                );
            }
            return;
        }
        
        // Get templates
        List<ModTemplate> templates = templateService.getTemplates();
        if (templates.isEmpty()) {
            ModForgeNotificationService notificationService = ModForgeNotificationService.getInstance();
            if (notificationService != null) {
                notificationService.showErrorDialog(
                        "No Templates",
                        "No templates available."
                );
            } else {
                Messages.showErrorDialog(
                        project,
                        "No templates available.",
                        "No Templates"
                );
            }
            return;
        }
        
        // Show template selection dialog
        TemplateSelectionDialog dialog = new TemplateSelectionDialog(project, templates);
        if (dialog.showAndGet()) {
            ModTemplate selectedTemplate = dialog.getSelectedTemplate();
            if (selectedTemplate == null) {
                return;
            }
            
            // Show template configuration dialog
            TemplateConfigurationDialog configDialog = new TemplateConfigurationDialog(project, selectedTemplate);
            if (configDialog.showAndGet()) {
                Map<String, String> variables = configDialog.getVariables();
                File outputDir = configDialog.getOutputDirectory();
                
                // Generate project
                templateService.generateProject(selectedTemplate, outputDir, variables)
                        .thenAccept(v -> {
                            ApplicationManager.getApplication().invokeLater(() -> {
                                // Get notification service
                                ModForgeNotificationService notificationService =
                                        ModForgeNotificationService.getInstance();
                                
                                if (notificationService != null) {
                                    notificationService.showInfoNotification(
                                            project,
                                            "Project Created",
                                            "Project created successfully at " + outputDir.getAbsolutePath()
                                    );
                                } else {
                                    // Fallback to standard Messages API since service is not available
                                    Messages.showInfoMessage(
                                            project,
                                            "Project created successfully at " + outputDir.getAbsolutePath(),
                                            "Project Created"
                                    );
                                }
                            });
                        })
                        .exceptionally(ex -> {
                            ApplicationManager.getApplication().invokeLater(() -> {
                                // Get notification service
                                ModForgeNotificationService notificationService =
                                        ModForgeNotificationService.getInstance();
                                
                                if (notificationService != null) {
                                    notificationService.showErrorNotification(
                                            "Error Creating Project",
                                            "Failed to create project: " + ex.getMessage()
                                    );
                                } else {
                                    // Fallback to standard Messages API since service is not available
                                    Messages.showErrorDialog(
                                            project,
                                            "Failed to create project: " + ex.getMessage(),
                                            "Error Creating Project"
                                    );
                                }
                            });
                            
                            return null;
                        });
            }
        }
    }
    
    /**
     * Dialog for selecting a template.
     */
    private static class TemplateSelectionDialog extends DialogWrapper {
        private final JBList<ModTemplate> templateList;
        private final DefaultListModel<ModTemplate> listModel;
        private final JBTextField searchField;
        private final JComboBox<String> categoryComboBox;
        private final JComboBox<ModTemplateType> typeComboBox;
        private final List<ModTemplate> allTemplates;
        private final Project project; // Store project reference for consistency
        
        public TemplateSelectionDialog(@Nullable Project project, @NotNull List<ModTemplate> templates) {
            super(project);
            
            this.project = project; // Save project for use in methods
            this.allTemplates = templates;
            
            setTitle("Select Template");
            setOKButtonText("Next");
            setCancelButtonText("Cancel");
            
            // Create list model and list
            listModel = new DefaultListModel<>();
            for (ModTemplate template : templates) {
                listModel.addElement(template);
            }
            
            templateList = new JBList<>(listModel);
            templateList.setCellRenderer(new ColoredListCellRenderer<>() {
                @Override
                protected void customizeCellRenderer(@NotNull JList<? extends ModTemplate> list, ModTemplate value,
                                                   int index, boolean selected, boolean hasFocus) {
                    if (value == null) {
                        return;
                    }
                    
                    append(value.getName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
                    append(" (" + value.getType().getDisplayName() + ")", SimpleTextAttributes.GRAYED_ATTRIBUTES);
                    append(" - " + value.getDescription(), SimpleTextAttributes.GRAYED_ATTRIBUTES);
                }
            });
            
            // Create search field
            searchField = new JBTextField();
            searchField.getEmptyText().setText("Search templates...");
            searchField.addCaretListener(e -> updateFilter());
            
            // Create category combo box
            Set<String> categories = new HashSet<>();
            categories.add("All Categories");
            for (ModTemplate template : templates) {
                categories.add(template.getCategory());
            }
            
            categoryComboBox = new JComboBox<>(categories.toArray(new String[0]));
            categoryComboBox.setSelectedItem("All Categories");
            categoryComboBox.addActionListener(e -> updateFilter());
            
            // Create type combo box
            typeComboBox = new JComboBox<>(ModTemplateType.values());
            typeComboBox.insertItemAt(null, 0);
            typeComboBox.setSelectedItem(null);
            typeComboBox.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                             boolean isSelected, boolean cellHasFocus) {
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    
                    if (value == null) {
                        setText("All Types");
                    } else {
                        ModTemplateType type = (ModTemplateType) value;
                        setText(type.getDisplayName());
                    }
                    
                    return this;
                }
            });
            typeComboBox.addActionListener(e -> updateFilter());
            
            init();
        }
        
        @Override
        protected @Nullable JComponent createCenterPanel() {
            // Create filter panel
            JPanel filterPanel = new JPanel(new BorderLayout());
            filterPanel.add(new JLabel("Search:"), BorderLayout.WEST);
            filterPanel.add(searchField, BorderLayout.CENTER);
            
            // Create combo box panel
            JPanel comboBoxPanel = new JPanel(new GridLayout(1, 2, 10, 0));
            comboBoxPanel.add(categoryComboBox);
            comboBoxPanel.add(typeComboBox);
            
            // Create list panel
            JBScrollPane scrollPane = new JBScrollPane(templateList);
            scrollPane.setPreferredSize(new Dimension(500, 300));
            
            // Create main panel
            FormBuilder formBuilder = FormBuilder.createFormBuilder()
                    .addComponent(filterPanel)
                    .addComponent(comboBoxPanel)
                    .addComponentFillVertically(scrollPane, 0);
            
            return formBuilder.getPanel();
        }
        
        @Override
        public @Nullable JComponent getPreferredFocusedComponent() {
            return searchField;
        }
        
        /**
         * Update the template list based on the filter.
         */
        private void updateFilter() {
            String search = searchField.getText().toLowerCase();
            String category = (String) categoryComboBox.getSelectedItem();
            ModTemplateType type = (ModTemplateType) typeComboBox.getSelectedItem();
            
            listModel.clear();
            
            for (ModTemplate template : allTemplates) {
                // Check search
                if (!search.isEmpty() && !template.getName().toLowerCase().contains(search) &&
                        !template.getDescription().toLowerCase().contains(search)) {
                    continue;
                }
                
                // Check category
                if (category != null && !category.equals("All Categories") && !template.getCategory().equals(category)) {
                    continue;
                }
                
                // Check type
                if (type != null && template.getType() != type) {
                    continue;
                }
                
                listModel.addElement(template);
            }
        }
        
        /**
         * Get the selected template.
         *
         * @return The selected template, or null if none is selected
         */
        @Nullable
        public ModTemplate getSelectedTemplate() {
            return templateList.getSelectedValue();
        }
        
        @Override
        protected void doOKAction() {
            if (templateList.getSelectedValue() == null) {
                if (project != null) {
                    ModForgeNotificationService notificationService = ModForgeNotificationService.getInstance();
                    if (notificationService != null) {
                        notificationService.showErrorDialog(
                                "No Template Selected",
                                "Please select a template"
                        );
                    } else {
                        Messages.showErrorDialog(
                                getContentPanel(),
                                "Please select a template",
                                "No Template Selected"
                        );
                    }
                } else {
                    Messages.showErrorDialog(
                            getContentPanel(),
                            "Please select a template",
                            "No Template Selected"
                    );
                }
                return;
            }
            
            super.doOKAction();
        }
    }
    
    /**
     * Dialog for configuring a template.
     */
    private static class TemplateConfigurationDialog extends DialogWrapper {
        private final Project project; // Project reference
        private final ModTemplate template;
        private final Map<String, JTextField> variableFields = new HashMap<>();
        private final JBTextField outputDirField;
        
        public TemplateConfigurationDialog(@Nullable Project project, @NotNull ModTemplate template) {
            super(project);
            
            this.project = project; // Save project for use in methods
            this.template = template;
            
            setTitle("Configure Template");
            setOKButtonText("Create");
            setCancelButtonText("Cancel");
            
            // Output directory field
            outputDirField = new JBTextField();
            outputDirField.setEditable(false);
            
            // Set default output directory
            if (project != null) {
                VirtualFile baseDir = CompatibilityUtil.getProjectBaseDir(project);
                if (baseDir != null) {
                    File baseDirFile = new File(baseDir.getPath());
                    File defaultOutputDir = new File(baseDirFile, template.getVariable("modid"));
                    outputDirField.setText(defaultOutputDir.getAbsolutePath());
                }
            }
            
            init();
        }
        
        @Override
        protected @Nullable JComponent createCenterPanel() {
            // Create form builder
            FormBuilder formBuilder = FormBuilder.createFormBuilder();
            
            // Add template info
            JBLabel titleLabel = new JBLabel(template.getName());
            titleLabel.setFont(JBUI.Fonts.label().biggerOn(1));
            formBuilder.addComponent(titleLabel);
            
            JBLabel descriptionLabel = new JBLabel(template.getDescription());
            descriptionLabel.setFont(JBUI.Fonts.label());
            formBuilder.addComponent(descriptionLabel);
            
            formBuilder.addSeparator();
            
            // Add variable fields
            for (Map.Entry<String, String> entry : template.getVariables().entrySet()) {
                String name = entry.getKey();
                String defaultValue = entry.getValue();
                
                JTextField field = new JTextField(defaultValue);
                variableFields.put(name, field);
                
                formBuilder.addLabeledComponent(new JLabel(formatVariableName(name) + ":"), field);
            }
            
            formBuilder.addSeparator();
            
            // Add output directory field
            JPanel outputDirPanel = new JPanel(new BorderLayout());
            outputDirPanel.add(outputDirField, BorderLayout.CENTER);
            
            JButton browseButton = new JButton("Browse...");
            browseButton.addActionListener(e -> browseOutputDir());
            outputDirPanel.add(browseButton, BorderLayout.EAST);
            
            formBuilder.addLabeledComponent(new JLabel("Output Directory:"), outputDirPanel);
            
            JPanel panel = formBuilder.getPanel();
            panel.setPreferredSize(new Dimension(500, panel.getPreferredSize().height));
            
            return panel;
        }
        
        /**
         * Format a variable name for display.
         *
         * @param name The variable name
         * @return The formatted name
         */
        @NotNull
        private String formatVariableName(@NotNull String name) {
            StringBuilder sb = new StringBuilder();
            
            boolean capitalizeNext = true;
            for (char c : name.toCharArray()) {
                if (c == '_') {
                    sb.append(' ');
                    capitalizeNext = true;
                } else {
                    if (capitalizeNext) {
                        sb.append(Character.toUpperCase(c));
                        capitalizeNext = false;
                    } else {
                        sb.append(c);
                    }
                }
            }
            
            return sb.toString();
        }
        
        /**
         * Browse for an output directory.
         */
        private void browseOutputDir() {
            FileChooserDescriptor descriptor = new FileChooserDescriptor(false, true, false, false, false, false);
            descriptor.setTitle("Select Output Directory");
            descriptor.setDescription("Select the directory where the project will be created.");
            
            FileChooserDialog chooser = FileChooserFactory.getInstance().createFileChooser(descriptor, null, null);
            VirtualFile[] files = chooser.choose(null, VirtualFile.EMPTY_ARRAY);
            
            if (files.length > 0) {
                VirtualFile file = files[0];
                outputDirField.setText(file.getPath() + File.separator + template.getVariable("modid"));
            }
        }
        
        /**
         * Get the configured variables.
         *
         * @return The variables
         */
        @NotNull
        public Map<String, String> getVariables() {
            Map<String, String> variables = new HashMap<>();
            
            for (Map.Entry<String, JTextField> entry : variableFields.entrySet()) {
                variables.put(entry.getKey(), entry.getValue().getText());
            }
            
            return variables;
        }
        
        /**
         * Get the selected output directory.
         *
         * @return The output directory
         */
        @NotNull
        public File getOutputDirectory() {
            return new File(outputDirField.getText());
        }
        
        @Override
        protected void doOKAction() {
            // Check if output directory is specified
            if (outputDirField.getText().isEmpty()) {
                if (project != null) {
                    ModForgeNotificationService notificationService = ModForgeNotificationService.getInstance();
                    if (notificationService != null) {
                        notificationService.showErrorDialog(
                                "No Output Directory",
                                "Please specify an output directory"
                        );
                    } else {
                        Messages.showErrorDialog(
                                getContentPanel(),
                                "Please specify an output directory",
                                "No Output Directory"
                        );
                    }
                } else {
                    Messages.showErrorDialog(
                            getContentPanel(),
                            "Please specify an output directory",
                            "No Output Directory"
                    );
                }
                return;
            }
            
            // Check if output directory exists or can be created
            File outputDir = getOutputDirectory();
            if (outputDir.exists() && outputDir.isFile()) {
                if (project != null) {
                    ModForgeNotificationService notificationService = ModForgeNotificationService.getInstance();
                    if (notificationService != null) {
                        notificationService.showErrorDialog(
                                "Invalid Output Directory",
                                "Output directory is a file"
                        );
                    } else {
                        Messages.showErrorDialog(
                                getContentPanel(),
                                "Output directory is a file",
                                "Invalid Output Directory"
                        );
                    }
                } else {
                    Messages.showErrorDialog(
                            getContentPanel(),
                            "Output directory is a file",
                            "Invalid Output Directory"
                    );
                }
                return;
            }
            
            // Check if output directory is empty
            if (outputDir.exists() && outputDir.listFiles() != null && outputDir.listFiles().length > 0) {
                if (project != null) {
                    ModForgeNotificationService notificationService = ModForgeNotificationService.getInstance();
                    if (notificationService != null) {
                        int result = notificationService.showYesNoDialog(
                                "Output directory is not empty. Continue?",
                                "Output Directory Not Empty",
                                "Continue",
                                "Cancel",
                                null
                        );
                        
                        if (result != Messages.YES) {
                            return;
                        }
                    } else {
                        // Use the compatible version with Project parameter in IntelliJ IDEA 2025.1.1.1
                        int result = Messages.showYesNoDialog(
                                project,
                                "Output directory is not empty. Continue?",
                                "Output Directory Not Empty",
                                "Continue",
                                "Cancel",
                                null
                        );
                        
                        if (result != Messages.YES) {
                            return;
                        }
                    }
                } else {
                    // Use the compatible version with Project parameter in IntelliJ IDEA 2025.1.1.1
                    int result = Messages.showYesNoDialog(
                            project,
                            "Output directory is not empty. Continue?",
                            "Output Directory Not Empty",
                            "Continue",
                            "Cancel",
                            null
                    );
                    
                    if (result != Messages.YES) {
                        return;
                    }
                }
            }
            
            super.doOKAction();
        }
    }
}