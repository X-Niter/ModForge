package com.modforge.intellij.plugin.crossloader.ui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.fileChooser.FileChooserDialog;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.*;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.modforge.intellij.plugin.crossloader.ArchitecturyService;
import com.modforge.intellij.plugin.crossloader.ArchitecturyService.ModLoader;
import com.modforge.intellij.plugin.crossloader.generator.ArchitecturyTemplateGenerator;
import com.modforge.intellij.plugin.utils.CompatibilityUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Dialog for setting up a cross-loader project.
 * Allows the user to select project structure, mod loaders, and other settings.
 */
public class CrossLoaderProjectSetupDialog extends DialogWrapper {
    private static final Logger LOG = Logger.getInstance(CrossLoaderProjectSetupDialog.class);
    
    private final Project project;
    private final ArchitecturyService architecturyService;
    
    // UI components
    private JTabbedPane tabbedPane;
    private JPanel projectSetupPanel;
    private JPanel loaderSetupPanel;
    private JPanel advancedSetupPanel;
    
    // Project setup
    private JBTextField modIdField;
    private JBTextField modNameField;
    private JBTextField modDescriptionField;
    private JBTextField modVersionField;
    private JBTextField modAuthorField;
    private JBTextField packageNameField;
    
    // Loader setup
    private JCheckBox forgeCheckBox;
    private JCheckBox fabricCheckBox;
    private JCheckBox quiltCheckBox;
    private JBRadioButton useArchitecturyRadioButton;
    private JBRadioButton useDirectConversionRadioButton;
    
    // Advanced setup
    private JBTextField minecraftVersionField;
    private JCheckBox generateExampleContentCheckBox;
    private JCheckBox useGradleCheckBox;
    private JComboBox<String> buildSystemComboBox;
    
    // Configuration
    private final List<ModLoader> selectedLoaders = new ArrayList<>();
    private boolean useArchitectury = true;
    
    /**
     * Creates a new cross-loader project setup dialog.
     * @param project The project
     */
    public CrossLoaderProjectSetupDialog(@NotNull Project project) {
        super(project);
        this.project = project;
        this.architecturyService = ArchitecturyService.getInstance(project);
        
        setTitle("Cross-Loader Mod Project Setup");
        setOKButtonText("Create Project");
        setOKActionEnabled(false); // Will enable when required fields are filled
        
        init();
    }
    
    /**
     * Creates the UI.
     */
    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel dialogPanel = new JBPanel<>(new BorderLayout());
        dialogPanel.setPreferredSize(new Dimension(650, 500));
        
        // Create tabbed pane
        tabbedPane = new JTabbedPane();
        
        // Create panels
        createProjectSetupPanel();
        createLoaderSetupPanel();
        createAdvancedSetupPanel();
        
        // Add panels to tabbed pane
        tabbedPane.addTab("Project Setup", projectSetupPanel);
        tabbedPane.addTab("Mod Loaders", loaderSetupPanel);
        tabbedPane.addTab("Advanced Settings", advancedSetupPanel);
        
        // Add tabbed pane to dialog
        dialogPanel.add(tabbedPane, BorderLayout.CENTER);
        
        // Add help panel
        JPanel helpPanel = createHelpPanel();
        dialogPanel.add(helpPanel, BorderLayout.EAST);
        
        return dialogPanel;
    }
    
    /**
     * Creates the project setup panel.
     */
    private void createProjectSetupPanel() {
        projectSetupPanel = new JBPanel<>(new GridBagLayout());
        projectSetupPanel.setBorder(JBUI.Borders.empty(10));
        
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = JBUI.insets(5);
        
        // Mod ID
        c.gridx = 0;
        c.gridy = 0;
        projectSetupPanel.add(new JBLabel("Mod ID:"), c);
        
        c.gridx = 1;
        c.weightx = 1.0;
        modIdField = new JBTextField();
        modIdField.setToolTipText("The unique identifier for your mod (e.g., 'my_awesome_mod')");
        modIdField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                updateOkButton();
                updatePackageName();
            }
            
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                updateOkButton();
                updatePackageName();
            }
            
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                updateOkButton();
                updatePackageName();
            }
        });
        projectSetupPanel.add(modIdField, c);
        
        // Mod Name
        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 0.0;
        projectSetupPanel.add(new JBLabel("Mod Name:"), c);
        
        c.gridx = 1;
        c.weightx = 1.0;
        modNameField = new JBTextField();
        modNameField.setToolTipText("The display name of your mod");
        projectSetupPanel.add(modNameField, c);
        
        // Mod Description
        c.gridx = 0;
        c.gridy = 2;
        c.weightx = 0.0;
        projectSetupPanel.add(new JBLabel("Description:"), c);
        
        c.gridx = 1;
        c.weightx = 1.0;
        modDescriptionField = new JBTextField();
        modDescriptionField.setToolTipText("A brief description of your mod");
        projectSetupPanel.add(modDescriptionField, c);
        
        // Mod Version
        c.gridx = 0;
        c.gridy = 3;
        c.weightx = 0.0;
        projectSetupPanel.add(new JBLabel("Version:"), c);
        
        c.gridx = 1;
        c.weightx = 1.0;
        modVersionField = new JBTextField("1.0.0");
        modVersionField.setToolTipText("The version of your mod (e.g., '1.0.0')");
        projectSetupPanel.add(modVersionField, c);
        
        // Mod Author
        c.gridx = 0;
        c.gridy = 4;
        c.weightx = 0.0;
        projectSetupPanel.add(new JBLabel("Author:"), c);
        
        c.gridx = 1;
        c.weightx = 1.0;
        modAuthorField = new JBTextField();
        modAuthorField.setToolTipText("The author(s) of the mod");
        projectSetupPanel.add(modAuthorField, c);
        
        // Package Name
        c.gridx = 0;
        c.gridy = 5;
        c.weightx = 0.0;
        projectSetupPanel.add(new JBLabel("Package:"), c);
        
        c.gridx = 1;
        c.weightx = 1.0;
        packageNameField = new JBTextField();
        packageNameField.setToolTipText("The Java package for your mod code");
        projectSetupPanel.add(packageNameField, c);
    }
    
    /**
     * Creates the loader setup panel.
     */
    private void createLoaderSetupPanel() {
        loaderSetupPanel = new JBPanel<>(new GridBagLayout());
        loaderSetupPanel.setBorder(JBUI.Borders.empty(10));
        
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = JBUI.insets(5);
        
        // Mod Loaders
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        JPanel loadersPanel = new JBPanel<>(new GridLayout(3, 1));
        loadersPanel.setBorder(BorderFactory.createTitledBorder("Select Mod Loaders"));
        
        forgeCheckBox = new JCheckBox("Forge", true);
        forgeCheckBox.addActionListener(e -> {
            if (forgeCheckBox.isSelected()) {
                selectedLoaders.add(ModLoader.FORGE);
            } else {
                selectedLoaders.remove(ModLoader.FORGE);
            }
            updateOkButton();
        });
        selectedLoaders.add(ModLoader.FORGE);
        
        fabricCheckBox = new JCheckBox("Fabric", true);
        fabricCheckBox.addActionListener(e -> {
            if (fabricCheckBox.isSelected()) {
                selectedLoaders.add(ModLoader.FABRIC);
            } else {
                selectedLoaders.remove(ModLoader.FABRIC);
            }
            updateOkButton();
        });
        selectedLoaders.add(ModLoader.FABRIC);
        
        quiltCheckBox = new JCheckBox("Quilt", false);
        quiltCheckBox.addActionListener(e -> {
            if (quiltCheckBox.isSelected()) {
                selectedLoaders.add(ModLoader.QUILT);
            } else {
                selectedLoaders.remove(ModLoader.QUILT);
            }
            updateOkButton();
        });
        
        loadersPanel.add(forgeCheckBox);
        loadersPanel.add(fabricCheckBox);
        loadersPanel.add(quiltCheckBox);
        loaderSetupPanel.add(loadersPanel, c);
        
        // Cross-Loader Framework
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 2;
        JPanel frameworkPanel = new JBPanel<>(new GridLayout(2, 1));
        frameworkPanel.setBorder(BorderFactory.createTitledBorder("Cross-Loader Framework"));
        
        useArchitecturyRadioButton = new JBRadioButton("Use Architectury API (Recommended)", true);
        useArchitecturyRadioButton.addActionListener(e -> useArchitectury = true);
        
        useDirectConversionRadioButton = new JBRadioButton("Use Direct Conversion (Advanced)", false);
        useDirectConversionRadioButton.addActionListener(e -> useArchitectury = false);
        
        ButtonGroup frameworkGroup = new ButtonGroup();
        frameworkGroup.add(useArchitecturyRadioButton);
        frameworkGroup.add(useDirectConversionRadioButton);
        
        frameworkPanel.add(useArchitecturyRadioButton);
        frameworkPanel.add(useDirectConversionRadioButton);
        loaderSetupPanel.add(frameworkPanel, c);
        
        // Framework Info
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 2;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.fill = GridBagConstraints.BOTH;
        
        JEditorPane frameworkInfoPane = new JEditorPane();
        frameworkInfoPane.setContentType("text/html");
        frameworkInfoPane.setEditable(false);
        frameworkInfoPane.setText(
                "<html><body style='margin: 5px'>" +
                "<h3>About Architectury</h3>" +
                "<p>Architectury is a development API that provides abstraction layers for developing cross-platform mods.<p>" +
                "<p>Benefits:</p>" +
                "<ul>" +
                "<li>Write code once, deploy on multiple platforms</li>" +
                "<li>Automatic handling of platform differences</li>" +
                "<li>Well-maintained and widely used in the modding community</li>" +
                "</ul>" +
                "</body></html>"
        );
        frameworkInfoPane.setBackground(UIUtil.getPanelBackground());
        
        JScrollPane scrollPane = new JBScrollPane(frameworkInfoPane);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Information"));
        loaderSetupPanel.add(scrollPane, c);
    }
    
    /**
     * Creates the advanced setup panel.
     */
    private void createAdvancedSetupPanel() {
        advancedSetupPanel = new JBPanel<>(new GridBagLayout());
        advancedSetupPanel.setBorder(JBUI.Borders.empty(10));
        
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = JBUI.insets(5);
        
        // Minecraft Version
        c.gridx = 0;
        c.gridy = 0;
        advancedSetupPanel.add(new JBLabel("Minecraft Version:"), c);
        
        c.gridx = 1;
        c.weightx = 1.0;
        minecraftVersionField = new JBTextField("1.19.2");
        minecraftVersionField.setToolTipText("The Minecraft version to target");
        advancedSetupPanel.add(minecraftVersionField, c);
        
        // Example Content
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 2;
        c.weightx = 0.0;
        generateExampleContentCheckBox = new JCheckBox("Generate Example Content", true);
        generateExampleContentCheckBox.setToolTipText("Generate example mod content such as items, blocks, and events");
        advancedSetupPanel.add(generateExampleContentCheckBox, c);
        
        // Build System
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 1;
        advancedSetupPanel.add(new JBLabel("Build System:"), c);
        
        c.gridx = 1;
        c.weightx = 1.0;
        buildSystemComboBox = new JComboBox<>(new String[]{"Gradle", "Maven"});
        buildSystemComboBox.setToolTipText("The build system to use for the project");
        advancedSetupPanel.add(buildSystemComboBox, c);
        
        // Use Gradle
        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 2;
        c.weightx = 0.0;
        useGradleCheckBox = new JCheckBox("Use Gradle", true);
        useGradleCheckBox.setToolTipText("Use Gradle as the build system");
        useGradleCheckBox.setEnabled(false); // Gradle is always used with Architectury
        advancedSetupPanel.add(useGradleCheckBox, c);
        
        // Project Structure Preview
        c.gridx = 0;
        c.gridy = 4;
        c.gridwidth = 2;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.fill = GridBagConstraints.BOTH;
        
        JEditorPane projectStructurePane = new JEditorPane();
        projectStructurePane.setContentType("text/html");
        projectStructurePane.setEditable(false);
        projectStructurePane.setText(
                "<html><body style='margin: 5px'>" +
                "<h3>Project Structure Preview</h3>" +
                "<pre>" +
                "my-mod/\n" +
                "├── common/              # Shared code\n" +
                "├── fabric/              # Fabric platform code\n" +
                "├── forge/               # Forge platform code\n" +
                "├── quilt/               # Quilt platform code (optional)\n" +
                "├── gradle/              # Gradle wrapper and scripts\n" +
                "├── build.gradle         # Main build script\n" +
                "├── settings.gradle      # Project settings\n" +
                "└── gradle.properties    # Project properties\n" +
                "</pre>" +
                "<p>This is the recommended structure for a cross-loader mod using Architectury.</p>" +
                "</body></html>"
        );
        projectStructurePane.setBackground(UIUtil.getPanelBackground());
        
        JScrollPane scrollPane = new JBScrollPane(projectStructurePane);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Project Structure Preview"));
        advancedSetupPanel.add(scrollPane, c);
    }
    
    /**
     * Creates the help panel.
     * @return The help panel
     */
    @NotNull
    private JPanel createHelpPanel() {
        JPanel helpPanel = new JBPanel<>(new BorderLayout());
        helpPanel.setPreferredSize(new Dimension(200, 500));
        helpPanel.setBorder(BorderFactory.createTitledBorder("Tips & Information"));
        
        JEditorPane helpPane = new JEditorPane();
        helpPane.setContentType("text/html");
        helpPane.setEditable(false);
        helpPane.setText(
                "<html><body style='margin: 5px'>" +
                "<h3>About Cross-Loader Mods</h3>" +
                "<p>Cross-loader mods can run on multiple modding platforms like Forge, Fabric, and Quilt.</p>" +
                
                "<h4>Architectury API</h4>" +
                "<p>Architectury provides a unified API that abstracts platform-specific code.</p>" +
                
                "<h4>Project Structure</h4>" +
                "<p>Each platform has its own module, with shared code in a common module.</p>" +
                
                "<h4>Platform-Specific Code</h4>" +
                "<p>Use <code>ExpectPlatform</code> for platform-specific implementations.</p>" +
                
                "<h4>Testing</h4>" +
                "<p>Always test your mod on all supported platforms.</p>" +
                
                "<h4>Documentation</h4>" +
                "<p>For more information, visit <a href='https://docs.architectury.dev/'>Architectury Documentation</a>.</p>" +
                "</body></html>"
        );
        helpPane.setBackground(UIUtil.getPanelBackground());
        
        JScrollPane scrollPane = new JBScrollPane(helpPane);
        helpPanel.add(scrollPane, BorderLayout.CENTER);
        
        return helpPanel;
    }
    
    /**
     * Updates the OK button state based on input validation.
     */
    private void updateOkButton() {
        boolean valid = isModIdValid() && !selectedLoaders.isEmpty();
        setOKActionEnabled(valid);
    }
    
    /**
     * Updates the package name based on the mod ID.
     */
    private void updatePackageName() {
        String modId = modIdField.getText();
        if (modId != null && !modId.isEmpty()) {
            modId = modId.toLowerCase().replaceAll("[^a-z0-9_]", "");
            packageNameField.setText("com.example." + modId);
        }
    }
    
    /**
     * Validates the mod ID.
     * @return Whether the mod ID is valid
     */
    private boolean isModIdValid() {
        String modId = modIdField.getText();
        return modId != null && !modId.isEmpty() && modId.matches("[a-z0-9_]+");
    }
    
    /**
     * Validates the input.
     * @return The validation info, or null if input is valid
     */
    @Nullable
    @Override
    protected ValidationInfo doValidate() {
        // Validate mod ID
        if (modIdField.getText().isEmpty()) {
            return new ValidationInfo("Mod ID is required", modIdField);
        }
        if (!modIdField.getText().matches("[a-z0-9_]+")) {
            return new ValidationInfo("Mod ID must contain only lowercase letters, numbers, and underscores", modIdField);
        }
        
        // Validate mod name
        if (modNameField.getText().isEmpty()) {
            return new ValidationInfo("Mod name is required", modNameField);
        }
        
        // Validate version
        if (modVersionField.getText().isEmpty()) {
            return new ValidationInfo("Version is required", modVersionField);
        }
        
        // Validate selected loaders
        if (selectedLoaders.isEmpty()) {
            return new ValidationInfo("At least one mod loader must be selected", tabbedPane);
        }
        
        return null;
    }
    
    /**
     * Gets the configuration.
     * @return The configuration
     */
    @NotNull
    public CrossLoaderProjectConfig getConfiguration() {
        CrossLoaderProjectConfig config = new CrossLoaderProjectConfig();
        
        config.setModId(modIdField.getText());
        config.setModName(modNameField.getText());
        config.setModDescription(modDescriptionField.getText());
        config.setModVersion(modVersionField.getText());
        config.setModAuthor(modAuthorField.getText());
        config.setPackageName(packageNameField.getText());
        
        config.setSelectedLoaders(new ArrayList<>(selectedLoaders));
        config.setUseArchitectury(useArchitectury);
        
        config.setMinecraftVersion(minecraftVersionField.getText());
        config.setGenerateExampleContent(generateExampleContentCheckBox.isSelected());
        config.setUseGradle(useGradleCheckBox.isSelected());
        config.setBuildSystem(buildSystemComboBox.getSelectedItem().toString());
        
        return config;
    }
    
    /**
     * Configuration for a cross-loader project.
     */
    public static class CrossLoaderProjectConfig {
        private String modId;
        private String modName;
        private String modDescription;
        private String modVersion;
        private String modAuthor;
        private String packageName;
        
        private List<ModLoader> selectedLoaders;
        private boolean useArchitectury;
        
        private String minecraftVersion;
        private boolean generateExampleContent;
        private boolean useGradle;
        private String buildSystem;
        
        /**
         * Gets the mod ID.
         * @return The mod ID
         */
        @NotNull
        public String getModId() {
            return modId;
        }
        
        /**
         * Sets the mod ID.
         * @param modId The mod ID
         */
        public void setModId(@NotNull String modId) {
            this.modId = modId;
        }
        
        /**
         * Gets the mod name.
         * @return The mod name
         */
        @NotNull
        public String getModName() {
            return modName;
        }
        
        /**
         * Sets the mod name.
         * @param modName The mod name
         */
        public void setModName(@NotNull String modName) {
            this.modName = modName;
        }
        
        /**
         * Gets the mod description.
         * @return The mod description
         */
        @NotNull
        public String getModDescription() {
            return modDescription;
        }
        
        /**
         * Sets the mod description.
         * @param modDescription The mod description
         */
        public void setModDescription(@NotNull String modDescription) {
            this.modDescription = modDescription;
        }
        
        /**
         * Gets the mod version.
         * @return The mod version
         */
        @NotNull
        public String getModVersion() {
            return modVersion;
        }
        
        /**
         * Sets the mod version.
         * @param modVersion The mod version
         */
        public void setModVersion(@NotNull String modVersion) {
            this.modVersion = modVersion;
        }
        
        /**
         * Gets the mod author.
         * @return The mod author
         */
        @NotNull
        public String getModAuthor() {
            return modAuthor;
        }
        
        /**
         * Sets the mod author.
         * @param modAuthor The mod author
         */
        public void setModAuthor(@NotNull String modAuthor) {
            this.modAuthor = modAuthor;
        }
        
        /**
         * Gets the package name.
         * @return The package name
         */
        @NotNull
        public String getPackageName() {
            return packageName;
        }
        
        /**
         * Sets the package name.
         * @param packageName The package name
         */
        public void setPackageName(@NotNull String packageName) {
            this.packageName = packageName;
        }
        
        /**
         * Gets the selected loaders.
         * @return The selected loaders
         */
        @NotNull
        public List<ModLoader> getSelectedLoaders() {
            return selectedLoaders;
        }
        
        /**
         * Sets the selected loaders.
         * @param selectedLoaders The selected loaders
         */
        public void setSelectedLoaders(@NotNull List<ModLoader> selectedLoaders) {
            this.selectedLoaders = selectedLoaders;
        }
        
        /**
         * Checks if Architectury is used.
         * @return Whether Architectury is used
         */
        public boolean isUseArchitectury() {
            return useArchitectury;
        }
        
        /**
         * Sets whether Architectury is used.
         * @param useArchitectury Whether Architectury is used
         */
        public void setUseArchitectury(boolean useArchitectury) {
            this.useArchitectury = useArchitectury;
        }
        
        /**
         * Gets the Minecraft version.
         * @return The Minecraft version
         */
        @NotNull
        public String getMinecraftVersion() {
            return minecraftVersion;
        }
        
        /**
         * Sets the Minecraft version.
         * @param minecraftVersion The Minecraft version
         */
        public void setMinecraftVersion(@NotNull String minecraftVersion) {
            this.minecraftVersion = minecraftVersion;
        }
        
        /**
         * Checks if example content is generated.
         * @return Whether example content is generated
         */
        public boolean isGenerateExampleContent() {
            return generateExampleContent;
        }
        
        /**
         * Sets whether example content is generated.
         * @param generateExampleContent Whether example content is generated
         */
        public void setGenerateExampleContent(boolean generateExampleContent) {
            this.generateExampleContent = generateExampleContent;
        }
        
        /**
         * Checks if Gradle is used.
         * @return Whether Gradle is used
         */
        public boolean isUseGradle() {
            return useGradle;
        }
        
        /**
         * Sets whether Gradle is used.
         * @param useGradle Whether Gradle is used
         */
        public void setUseGradle(boolean useGradle) {
            this.useGradle = useGradle;
        }
        
        /**
         * Gets the build system.
         * @return The build system
         */
        @NotNull
        public String getBuildSystem() {
            return buildSystem;
        }
        
        /**
         * Sets the build system.
         * @param buildSystem The build system
         */
        public void setBuildSystem(@NotNull String buildSystem) {
            this.buildSystem = buildSystem;
        }
    }
    
    /**
     * Called when the user clicks the "Create Project" button.
     * Creates a new cross-loader mod project.
     */
    @Override
    protected void doOKAction() {
        if (useArchitectury) {
            createArchitecturyProject();
        } else {
            createDirectConversionProject();
        }
        
        super.doOKAction();
    }
    
    /**
     * Creates an Architectury project.
     */
    private void createArchitecturyProject() {
        // Get project data
        String modId = modIdField.getText().trim();
        String modName = modNameField.getText().trim();
        String modDescription = modDescriptionField.getText().trim();
        String author = modAuthorField.getText().trim();
        String packageName = packageNameField.getText().trim();
        
        // Show directory chooser
        FileChooserDialog fileChooser = FileChooserFactory.getInstance().createFileChooser(
                FileChooserDescriptorFactory.createSingleFolderDescriptor(),
                project,
                null
        );
        
        VirtualFile[] files = fileChooser.choose(project);
        if (files.length == 0) {
            return;
        }
        
        VirtualFile baseDir = files[0];
        
        // Generate Architectury project
        ArchitecturyTemplateGenerator generator = new ArchitecturyTemplateGenerator(project);
        boolean success = generator.generateArchitecturyMod(
                modId, modName, modDescription, packageName, author, baseDir
        );
        
        if (success) {
            LOG.info("Successfully generated Architectury mod project: " + modId);
            CompatibilityUtil.showInfoDialog(
                    myProject,
                    "Successfully created cross-loader mod project at:\n" + baseDir.getPath(),
                    "Project Created"
            );
        } else {
            LOG.error("Failed to generate Architectury mod project: " + modId);
            Messages.showErrorDialog(
                    "Failed to create cross-loader mod project. See the IDE log for details.",
                    "Project Creation Failed"
            );
        }
    }
    
    /**
     * Creates a direct conversion project.
     */
    private void createDirectConversionProject() {
        // TODO: Implement direct conversion project creation
        CompatibilityUtil.showInfoDialog(
                myProject,
                "Direct conversion project creation is not yet implemented.\n" +
                "Please use Architectury for now.",
                "Not Implemented"
        );
    }
}