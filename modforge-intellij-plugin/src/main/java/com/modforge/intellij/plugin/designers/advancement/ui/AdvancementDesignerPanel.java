package com.modforge.intellij.plugin.designers.advancement.ui;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.treeStructure.Tree;
import com.modforge.intellij.plugin.designers.advancement.AdvancementManager;
import com.modforge.intellij.plugin.designers.advancement.models.AdvancementCriterion;
import com.modforge.intellij.plugin.designers.advancement.models.AdvancementModel;
import com.modforge.intellij.plugin.designers.advancement.models.AdvancementModel.AdvancementFrameType;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;

/**
 * UI panel for designing Minecraft advancements
 */
public class AdvancementDesignerPanel extends SimpleToolWindowPanel {
    private static final Logger LOG = Logger.getInstance(AdvancementDesignerPanel.class);
    
    private final Project project;
    private final AdvancementManager advancementManager;
    
    private JBList<String> advancementList;
    private DefaultListModel<String> advancementListModel;
    private JTabbedPane detailsTabs;
    
    private JTextField idField;
    private JTextField nameField;
    private JTextField descriptionField;
    private JComboBox<AdvancementFrameType> frameTypeCombo;
    private JTextField iconItemField;
    private JTextField backgroundField;
    private JCheckBox showToastCheck;
    private JCheckBox announceCheck;
    private JCheckBox hiddenCheck;
    
    private Tree advancementTree;
    private DefaultTreeModel treeModel;
    private DefaultMutableTreeNode rootNode;
    
    private JTextField criterionIdField;
    private JTextField triggerField;
    private JTextArea conditionsArea;
    private JButton addCriterionButton;
    private JList<String> criteriaList;
    private DefaultListModel<String> criteriaListModel;
    
    private AdvancementModel currentAdvancement;
    
    /**
     * Create a new advancement designer panel
     * 
     * @param project The current project
     */
    public AdvancementDesignerPanel(@NotNull Project project) {
        super(true);
        this.project = project;
        this.advancementManager = project.getService(AdvancementManager.class);
        
        setupUI();
        
        // Load advancements if any exist
        refreshAdvancementList();
    }
    
    /**
     * Set up the UI components
     */
    private void setupUI() {
        // Create the toolbar
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        // Add actions here (load, save, etc.)
        
        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("AdvancementDesigner", actionGroup, true);
        toolbar.setTargetComponent(this);
        setToolbar(toolbar.getComponent());
        
        // Create the main content panel
        JPanel contentPanel = new JPanel(new BorderLayout());
        
        // Create the split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(200);
        
        // Left panel - Advancement list
        JPanel leftPanel = new JPanel(new BorderLayout());
        JPanel advancementActionsPanel = new JPanel();
        JButton addButton = new JButton("Add");
        JButton removeButton = new JButton("Remove");
        
        advancementActionsPanel.add(addButton);
        advancementActionsPanel.add(removeButton);
        
        advancementListModel = new DefaultListModel<>();
        advancementList = new JBList<>(advancementListModel);
        advancementList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        leftPanel.add(new JBScrollPane(advancementList), BorderLayout.CENTER);
        leftPanel.add(advancementActionsPanel, BorderLayout.SOUTH);
        
        // Right panel - Advancement details
        JPanel rightPanel = new JPanel(new BorderLayout());
        
        // Create the tabbed pane for details
        detailsTabs = new JTabbedPane();
        
        // Basic info panel
        JPanel basicInfoPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        basicInfoPanel.add(new JLabel("Advancement ID:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        
        idField = new JTextField();
        basicInfoPanel.add(idField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        
        basicInfoPanel.add(new JLabel("Display Name:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        
        nameField = new JTextField();
        basicInfoPanel.add(nameField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        
        basicInfoPanel.add(new JLabel("Description:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        
        descriptionField = new JTextField();
        basicInfoPanel.add(descriptionField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        
        basicInfoPanel.add(new JLabel("Frame Type:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        
        frameTypeCombo = new ComboBox<>(AdvancementFrameType.values());
        basicInfoPanel.add(frameTypeCombo, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        
        basicInfoPanel.add(new JLabel("Icon Item:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        
        iconItemField = new JTextField();
        basicInfoPanel.add(iconItemField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        
        basicInfoPanel.add(new JLabel("Background:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        
        backgroundField = new JTextField();
        basicInfoPanel.add(backgroundField, gbc);
        
        // Display options
        JPanel displayOptionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        showToastCheck = new JCheckBox("Show Toast", true);
        announceCheck = new JCheckBox("Announce to Chat", true);
        hiddenCheck = new JCheckBox("Hidden", false);
        
        displayOptionsPanel.add(showToastCheck);
        displayOptionsPanel.add(announceCheck);
        displayOptionsPanel.add(hiddenCheck);
        
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        basicInfoPanel.add(displayOptionsPanel, gbc);
        
        // Tree structure panel
        JPanel treePanel = new JPanel(new BorderLayout());
        rootNode = new DefaultMutableTreeNode("Advancements");
        treeModel = new DefaultTreeModel(rootNode);
        advancementTree = new Tree(treeModel);
        advancementTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        
        treePanel.add(new JLabel("Advancement Tree:"), BorderLayout.NORTH);
        treePanel.add(new JBScrollPane(advancementTree), BorderLayout.CENTER);
        
        JPanel treeControlPanel = new JPanel();
        JButton addChildButton = new JButton("Add Child");
        JButton setParentButton = new JButton("Set Parent");
        
        treeControlPanel.add(addChildButton);
        treeControlPanel.add(setParentButton);
        
        treePanel.add(treeControlPanel, BorderLayout.SOUTH);
        
        // Criteria panel
        JPanel criteriaPanel = new JPanel(new BorderLayout());
        JPanel criteriaInputPanel = new JPanel(new GridBagLayout());
        
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        criteriaInputPanel.add(new JLabel("Criterion ID:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        
        criterionIdField = new JTextField(20);
        criteriaInputPanel.add(criterionIdField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        
        criteriaInputPanel.add(new JLabel("Trigger:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        
        triggerField = new JTextField(20);
        criteriaInputPanel.add(triggerField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        
        criteriaInputPanel.add(new JLabel("Conditions:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        
        conditionsArea = new JTextArea(5, 20);
        conditionsArea.setLineWrap(true);
        criteriaInputPanel.add(new JBScrollPane(conditionsArea), gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        
        addCriterionButton = new JButton("Add Criterion");
        criteriaInputPanel.add(addCriterionButton, gbc);
        
        criteriaListModel = new DefaultListModel<>();
        criteriaList = new JList<>(criteriaListModel);
        
        criteriaPanel.add(criteriaInputPanel, BorderLayout.NORTH);
        criteriaPanel.add(new JBScrollPane(criteriaList), BorderLayout.CENTER);
        
        JPanel criteriaControlPanel = new JPanel();
        JButton generateRequirementsButton = new JButton("Generate Requirements");
        criteriaControlPanel.add(generateRequirementsButton);
        
        criteriaPanel.add(criteriaControlPanel, BorderLayout.SOUTH);
        
        // Add tabs
        detailsTabs.addTab("Basic Info", basicInfoPanel);
        detailsTabs.addTab("Tree Structure", treePanel);
        detailsTabs.addTab("Criteria", criteriaPanel);
        
        rightPanel.add(detailsTabs, BorderLayout.CENTER);
        
        // Add a save button
        JButton saveButton = new JButton("Save Advancement");
        saveButton.addActionListener(e -> saveCurrentAdvancement());
        rightPanel.add(saveButton, BorderLayout.SOUTH);
        
        // Add panels to split pane
        splitPane.setLeftComponent(leftPanel);
        splitPane.setRightComponent(rightPanel);
        
        contentPanel.add(splitPane, BorderLayout.CENTER);
        
        setContent(contentPanel);
        
        // Add listeners
        addButton.addActionListener(e -> addNewAdvancement());
        removeButton.addActionListener(e -> removeSelectedAdvancement());
        
        advancementList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadSelectedAdvancement();
            }
        });
        
        addChildButton.addActionListener(e -> addChildAdvancement());
        setParentButton.addActionListener(e -> setAdvancementParent());
        
        addCriterionButton.addActionListener(e -> addCriterion());
        generateRequirementsButton.addActionListener(e -> generateRequirements());
    }
    
    /**
     * Refresh the advancement list
     */
    private void refreshAdvancementList() {
        advancementListModel.clear();
        
        for (AdvancementModel advancement : advancementManager.getAllAdvancements()) {
            advancementListModel.addElement(advancement.getId());
        }
        
        refreshAdvancementTree();
    }
    
    /**
     * Refresh the advancement tree
     */
    private void refreshAdvancementTree() {
        rootNode.removeAllChildren();
        
        // Add root advancements
        for (AdvancementModel rootAdvancement : advancementManager.getRootAdvancements()) {
            DefaultMutableTreeNode advNode = new DefaultMutableTreeNode(rootAdvancement);
            rootNode.add(advNode);
            
            // Add children recursively
            addChildrenToTree(advNode, rootAdvancement);
        }
        
        treeModel.reload();
    }
    
    /**
     * Add children to the tree recursively
     * 
     * @param parentNode The parent tree node
     * @param parentAdvancement The parent advancement
     */
    private void addChildrenToTree(DefaultMutableTreeNode parentNode, AdvancementModel parentAdvancement) {
        for (AdvancementModel child : parentAdvancement.getChildren()) {
            DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(child);
            parentNode.add(childNode);
            
            // Add children recursively
            addChildrenToTree(childNode, child);
        }
    }
    
    /**
     * Add a new advancement
     */
    private void addNewAdvancement() {
        String id = Messages.showInputDialog(
            project,
            "Enter Advancement ID:",
            "New Advancement",
            Messages.getQuestionIcon()
        );
        
        if (id == null || id.trim().isEmpty()) {
            return;
        }
        
        // Add namespace if missing
        if (!id.contains(":")) {
            id = "minecraft:" + id;
        }
        
        // Create a new advancement
        AdvancementModel advancement = advancementManager.createAdvancement(id, id);
        
        // Update the list
        refreshAdvancementList();
        
        // Select the new advancement
        advancementList.setSelectedValue(id, true);
    }
    
    /**
     * Remove the selected advancement
     */
    private void removeSelectedAdvancement() {
        String id = advancementList.getSelectedValue();
        if (id == null) {
            return;
        }
        
        int result = Messages.showYesNoDialog(
            project,
            "Are you sure you want to delete the advancement '" + id + "'?",
            "Delete Advancement",
            Messages.getQuestionIcon()
        );
        
        if (result == Messages.YES) {
            advancementManager.deleteAdvancement(id);
            refreshAdvancementList();
            clearForm();
        }
    }
    
    /**
     * Load the selected advancement
     */
    private void loadSelectedAdvancement() {
        String id = advancementList.getSelectedValue();
        if (id == null) {
            clearForm();
            return;
        }
        
        AdvancementModel advancement = advancementManager.getAdvancement(id);
        if (advancement == null) {
            clearForm();
            return;
        }
        
        currentAdvancement = advancement;
        
        // Set basic info
        idField.setText(advancement.getId());
        nameField.setText(advancement.getName());
        descriptionField.setText(advancement.getDescription());
        frameTypeCombo.setSelectedItem(advancement.getFrameType());
        iconItemField.setText(advancement.getIconItem());
        backgroundField.setText(advancement.getBackground());
        
        // Set display options
        showToastCheck.setSelected(advancement.isShowToast());
        announceCheck.setSelected(advancement.isAnnounceToChat());
        hiddenCheck.setSelected(advancement.isHidden());
        
        // Set criteria
        criteriaListModel.clear();
        for (AdvancementCriterion criterion : advancement.getCriteria()) {
            criteriaListModel.addElement(criterion.getId() + " -> " + criterion.getTrigger());
        }
    }
    
    /**
     * Clear the form
     */
    private void clearForm() {
        currentAdvancement = null;
        
        idField.setText("");
        nameField.setText("");
        descriptionField.setText("");
        frameTypeCombo.setSelectedItem(AdvancementFrameType.TASK);
        iconItemField.setText("");
        backgroundField.setText("");
        
        showToastCheck.setSelected(true);
        announceCheck.setSelected(true);
        hiddenCheck.setSelected(false);
        
        criteriaListModel.clear();
        criterionIdField.setText("");
        triggerField.setText("");
        conditionsArea.setText("");
    }
    
    /**
     * Add a child advancement
     */
    private void addChildAdvancement() {
        if (currentAdvancement == null) {
            Messages.showErrorDialog(
                project,
                "Please select a parent advancement first",
                "No Parent Selected"
            );
            return;
        }
        
        String id = Messages.showInputDialog(
            project,
            "Enter Child Advancement ID:",
            "New Child Advancement",
            Messages.getQuestionIcon()
        );
        
        if (id == null || id.trim().isEmpty()) {
            return;
        }
        
        // Add namespace if missing
        if (!id.contains(":")) {
            id = "minecraft:" + id;
        }
        
        // Create a new advancement
        AdvancementModel childAdvancement = advancementManager.createAdvancement(id, id);
        
        // Set the parent
        currentAdvancement.addChild(childAdvancement);
        
        // Update the lists
        refreshAdvancementList();
        
        // Select the new advancement
        advancementList.setSelectedValue(id, true);
    }
    
    /**
     * Set the advancement parent
     */
    private void setAdvancementParent() {
        if (currentAdvancement == null) {
            Messages.showErrorDialog(
                project,
                "Please select an advancement first",
                "No Advancement Selected"
            );
            return;
        }
        
        // Get all advancements except the current one
        List<AdvancementModel> possibleParents = new ArrayList<>();
        for (AdvancementModel advancement : advancementManager.getAllAdvancements()) {
            if (!advancement.getId().equals(currentAdvancement.getId())) {
                possibleParents.add(advancement);
            }
        }
        
        if (possibleParents.isEmpty()) {
            Messages.showErrorDialog(
                project,
                "No other advancements available to set as parent",
                "No Parent Options"
            );
            return;
        }
        
        // Create a dialog to select the parent
        String[] parentOptions = new String[possibleParents.size() + 1];
        parentOptions[0] = "(None - Make Root Advancement)";
        
        for (int i = 0; i < possibleParents.size(); i++) {
            parentOptions[i + 1] = possibleParents.get(i).getId();
        }
        
        String selectedParent = (String) JOptionPane.showInputDialog(
            null,
            "Select Parent Advancement:",
            "Set Parent",
            JOptionPane.QUESTION_MESSAGE,
            null,
            parentOptions,
            parentOptions[0]
        );
        
        if (selectedParent == null) {
            return;
        }
        
        // Update the parent
        if (selectedParent.equals(parentOptions[0])) {
            // Remove current parent if any
            AdvancementModel parent = currentAdvancement.getParent();
            if (parent != null) {
                parent.removeChild(currentAdvancement);
            }
            currentAdvancement.setParent(null);
        } else {
            // Find the selected parent
            AdvancementModel parent = advancementManager.getAdvancement(selectedParent);
            if (parent != null) {
                parent.addChild(currentAdvancement);
            }
        }
        
        // Update the lists
        refreshAdvancementList();
        
        // Reselect the current advancement
        advancementList.setSelectedValue(currentAdvancement.getId(), true);
    }
    
    /**
     * Add a criterion to the current advancement
     */
    private void addCriterion() {
        if (currentAdvancement == null) {
            Messages.showErrorDialog(
                project,
                "Please select an advancement first",
                "No Advancement Selected"
            );
            return;
        }
        
        String id = criterionIdField.getText().trim();
        String trigger = triggerField.getText().trim();
        String conditions = conditionsArea.getText().trim();
        
        if (id.isEmpty() || trigger.isEmpty()) {
            Messages.showErrorDialog(
                project,
                "Please enter an ID and trigger for the criterion",
                "Missing Information"
            );
            return;
        }
        
        // Create and add the criterion
        AdvancementCriterion criterion = new AdvancementCriterion(
            id,
            trigger,
            conditions.isEmpty() ? null : conditions
        );
        
        currentAdvancement.addCriterion(criterion);
        
        // Update the list
        criteriaListModel.addElement(id + " -> " + trigger);
        
        // Clear the fields
        criterionIdField.setText("");
        triggerField.setText("");
        conditionsArea.setText("");
    }
    
    /**
     * Generate requirements for the current advancement
     */
    private void generateRequirements() {
        if (currentAdvancement == null) {
            Messages.showErrorDialog(
                project,
                "Please select an advancement first",
                "No Advancement Selected"
            );
            return;
        }
        
        currentAdvancement.generateRequirements();
        
        Messages.showInfoMessage(
            project,
            "Requirements generated successfully",
            "Requirements Generated"
        );
    }
    
    /**
     * Save the current advancement
     */
    private void saveCurrentAdvancement() {
        if (currentAdvancement == null) {
            return;
        }
        
        // Update the advancement from form fields
        currentAdvancement.setId(idField.getText().trim());
        currentAdvancement.setName(nameField.getText().trim());
        currentAdvancement.setDescription(descriptionField.getText().trim());
        currentAdvancement.setFrameType((AdvancementFrameType) frameTypeCombo.getSelectedItem());
        currentAdvancement.setIconItem(iconItemField.getText().trim());
        currentAdvancement.setBackground(backgroundField.getText().trim());
        
        currentAdvancement.setShowToast(showToastCheck.isSelected());
        currentAdvancement.setAnnounceToChat(announceCheck.isSelected());
        currentAdvancement.setHidden(hiddenCheck.isSelected());
        
        // Update the advancement in the manager
        refreshAdvancementList();
        
        // Select the current advancement
        advancementList.setSelectedValue(currentAdvancement.getId(), true);
        
        Messages.showInfoMessage(
            project,
            "Advancement saved successfully",
            "Advancement Saved"
        );
    }
}