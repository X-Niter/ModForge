package com.modforge.intellij.plugin.designers.structure.ui;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.ui.ColorPicker;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.modforge.intellij.plugin.designers.structure.StructureManager;
import com.modforge.intellij.plugin.designers.structure.models.BlockState;
import com.modforge.intellij.plugin.designers.structure.models.StructureModel;
import com.modforge.intellij.plugin.designers.structure.models.StructurePart;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.HashMap;
import java.util.Map;

/**
 * UI panel for designing Minecraft structures
 */
public class StructureDesignerPanel extends SimpleToolWindowPanel {
    private static final Logger LOG = Logger.getInstance(StructureDesignerPanel.class);
    
    private final Project project;
    private final StructureManager structureManager;
    
    private JBList<String> structureList;
    private DefaultListModel<String> structureListModel;
    private JBTabbedPane detailsTabs;
    
    private JTextField idField;
    private JTextField nameField;
    private JComboBox<StructureModel.StructureType> typeCombo;
    
    // 3D structure view components
    private JPanel structureViewPanel;
    private JPanel[][][] gridCells;
    private JSpinner xSizeSpinner;
    private JSpinner ySizeSpinner;
    private JSpinner zSizeSpinner;
    private JPanel gridPanel;
    private JComboBox<String> layerSelector;
    
    // Block palette components
    private JList<String> paletteList;
    private DefaultListModel<String> paletteListModel;
    private JTextField blockIdField;
    private Map<String, Color> blockColors = new HashMap<>();
    
    private StructureModel currentStructure;
    private StructurePart currentPart;
    private BlockState currentBlock;
    private int currentLayer = 0;
    
    /**
     * Create a new structure designer panel
     * 
     * @param project The current project
     */
    public StructureDesignerPanel(@NotNull Project project) {
        super(true);
        this.project = project;
        this.structureManager = project.getService(StructureManager.class);
        
        setupUI();
        
        // Load structures if any exist
        refreshStructureList();
    }
    
    /**
     * Set up the UI components
     */
    private void setupUI() {
        // Create the toolbar
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        // Add actions here (load, save, etc.)
        
        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("StructureDesigner", actionGroup, true);
        toolbar.setTargetComponent(this);
        setToolbar(toolbar.getComponent());
        
        // Create the main content panel
        JPanel contentPanel = new JPanel(new BorderLayout());
        
        // Create the split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(200);
        
        // Left panel - Structure list
        JPanel leftPanel = new JPanel(new BorderLayout());
        JPanel structureActionsPanel = new JPanel();
        JButton addButton = new JButton("Add");
        JButton removeButton = new JButton("Remove");
        
        structureActionsPanel.add(addButton);
        structureActionsPanel.add(removeButton);
        
        structureListModel = new DefaultListModel<>();
        structureList = new JBList<>(structureListModel);
        structureList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        leftPanel.add(new JBScrollPane(structureList), BorderLayout.CENTER);
        leftPanel.add(structureActionsPanel, BorderLayout.SOUTH);
        
        // Right panel - Structure details
        JPanel rightPanel = new JPanel(new BorderLayout());
        
        // Basic info panel
        JPanel basicInfoPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        basicInfoPanel.add(new JLabel("Structure ID:"), gbc);
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
        
        basicInfoPanel.add(new JLabel("Type:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        
        typeCombo = new ComboBox<>(StructureModel.StructureType.values());
        basicInfoPanel.add(typeCombo, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        
        basicInfoPanel.add(new JLabel("Size:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        
        JPanel sizePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        xSizeSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 32, 1));
        ySizeSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 32, 1));
        zSizeSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 32, 1));
        
        sizePanel.add(new JLabel("X:"));
        sizePanel.add(xSizeSpinner);
        sizePanel.add(new JLabel("Y:"));
        sizePanel.add(ySizeSpinner);
        sizePanel.add(new JLabel("Z:"));
        sizePanel.add(zSizeSpinner);
        
        basicInfoPanel.add(sizePanel, gbc);
        
        // Create the tabbed pane for details
        detailsTabs = new JBTabbedPane();
        
        // Structure view panel
        JPanel structurePanel = new JPanel(new BorderLayout());
        
        // Layer selector
        JPanel layerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        layerPanel.add(new JLabel("Layer:"));
        
        layerSelector = new JComboBox<>();
        for (int i = 0; i < 10; i++) {
            layerSelector.addItem("Layer " + i);
        }
        
        layerPanel.add(layerSelector);
        
        // Grid panel
        gridPanel = new JPanel(new GridBagLayout());
        gridPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        structureViewPanel = new JPanel(new BorderLayout());
        structureViewPanel.add(layerPanel, BorderLayout.NORTH);
        structureViewPanel.add(new JBScrollPane(gridPanel), BorderLayout.CENTER);
        
        // Block palette panel
        JPanel palettePanel = new JPanel(new BorderLayout());
        
        JPanel blockInputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        blockIdField = new JTextField(20);
        JButton addBlockButton = new JButton("Add Block");
        JButton setColorButton = new JButton("Set Color");
        
        blockInputPanel.add(new JLabel("Block ID:"));
        blockInputPanel.add(blockIdField);
        blockInputPanel.add(addBlockButton);
        blockInputPanel.add(setColorButton);
        
        paletteListModel = new DefaultListModel<>();
        paletteList = new JList<>(paletteListModel);
        paletteList.setCellRenderer(new BlockListCellRenderer());
        
        palettePanel.add(blockInputPanel, BorderLayout.NORTH);
        palettePanel.add(new JBScrollPane(paletteList), BorderLayout.CENTER);
        
        // Add tabs
        detailsTabs.addTab("Structure", structureViewPanel);
        detailsTabs.addTab("Block Palette", palettePanel);
        
        rightPanel.add(basicInfoPanel, BorderLayout.NORTH);
        rightPanel.add(detailsTabs, BorderLayout.CENTER);
        
        // Add a save button
        JButton saveButton = new JButton("Save Structure");
        saveButton.addActionListener(e -> saveCurrentStructure());
        rightPanel.add(saveButton, BorderLayout.SOUTH);
        
        // Add panels to split pane
        splitPane.setLeftComponent(leftPanel);
        splitPane.setRightComponent(rightPanel);
        
        contentPanel.add(splitPane, BorderLayout.CENTER);
        
        setContent(contentPanel);
        
        // Add listeners
        addButton.addActionListener(e -> addNewStructure());
        removeButton.addActionListener(e -> removeSelectedStructure());
        
        structureList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadSelectedStructure();
            }
        });
        
        layerSelector.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                currentLayer = layerSelector.getSelectedIndex();
                updateGridView();
            }
        });
        
        addBlockButton.addActionListener(e -> addBlockToPalette());
        setColorButton.addActionListener(e -> setBlockColor());
        
        paletteList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selected = paletteList.getSelectedValue();
                if (selected != null) {
                    currentBlock = new BlockState(selected);
                    blockIdField.setText(selected);
                }
            }
        });
        
        xSizeSpinner.addChangeListener(e -> updateStructureSize());
        ySizeSpinner.addChangeListener(e -> updateStructureSize());
        zSizeSpinner.addChangeListener(e -> updateStructureSize());
    }
    
    /**
     * Refresh the structure list
     */
    private void refreshStructureList() {
        structureListModel.clear();
        
        for (StructureModel structure : structureManager.getAllStructures()) {
            structureListModel.addElement(structure.getId());
        }
    }
    
    /**
     * Add a new structure
     */
    private void addNewStructure() {
        String id = Messages.showInputDialog(
            project,
            "Enter Structure ID:",
            "New Structure",
            Messages.getQuestionIcon()
        );
        
        if (id == null || id.trim().isEmpty()) {
            return;
        }
        
        // Add namespace if missing
        if (!id.contains(":")) {
            id = "minecraft:" + id;
        }
        
        // Create a new structure
        StructureModel structure = structureManager.createStructure(id, StructureModel.StructureType.CUSTOM);
        
        // Add a default part
        int xSize = (Integer) xSizeSpinner.getValue();
        int ySize = (Integer) ySizeSpinner.getValue();
        int zSize = (Integer) zSizeSpinner.getValue();
        
        StructurePart part = new StructurePart(id + "_main", 0, 0, 0, xSize, ySize, zSize);
        structure.addPart(part);
        
        // Update the list
        refreshStructureList();
        
        // Select the new structure
        structureList.setSelectedValue(id, true);
    }
    
    /**
     * Remove the selected structure
     */
    private void removeSelectedStructure() {
        String id = structureList.getSelectedValue();
        if (id == null) {
            return;
        }
        
        int result = Messages.showYesNoDialog(
            project,
            "Are you sure you want to delete the structure '" + id + "'?",
            "Delete Structure",
            Messages.getQuestionIcon()
        );
        
        if (result == Messages.YES) {
            structureManager.deleteStructure(id);
            refreshStructureList();
            clearForm();
        }
    }
    
    /**
     * Load the selected structure
     */
    private void loadSelectedStructure() {
        String id = structureList.getSelectedValue();
        if (id == null) {
            clearForm();
            return;
        }
        
        StructureModel structure = structureManager.getStructure(id);
        if (structure == null) {
            clearForm();
            return;
        }
        
        currentStructure = structure;
        
        // Set basic info
        idField.setText(structure.getId());
        nameField.setText(structure.getName());
        typeCombo.setSelectedItem(structure.getType());
        
        // Load the first part
        if (!structure.getParts().isEmpty()) {
            currentPart = structure.getParts().get(0);
            
            // Update size spinners
            xSizeSpinner.setValue(currentPart.getWidth());
            ySizeSpinner.setValue(currentPart.getHeight());
            zSizeSpinner.setValue(currentPart.getDepth());
            
            // Update layer selector
            layerSelector.removeAllItems();
            for (int i = 0; i < currentPart.getHeight(); i++) {
                layerSelector.addItem("Layer " + i);
            }
            currentLayer = 0;
            layerSelector.setSelectedIndex(currentLayer);
            
            // Create or update grid
            createGridPanel();
            updateGridView();
        } else {
            // Create a new part if none exists
            int xSize = (Integer) xSizeSpinner.getValue();
            int ySize = (Integer) ySizeSpinner.getValue();
            int zSize = (Integer) zSizeSpinner.getValue();
            
            currentPart = new StructurePart(id + "_main", 0, 0, 0, xSize, ySize, zSize);
            structure.addPart(currentPart);
            
            createGridPanel();
            updateGridView();
        }
        
        // Update block palette
        updateBlockPalette();
    }
    
    /**
     * Clear the form
     */
    private void clearForm() {
        currentStructure = null;
        currentPart = null;
        currentBlock = new BlockState("minecraft:air");
        
        idField.setText("");
        nameField.setText("");
        typeCombo.setSelectedItem(StructureModel.StructureType.CUSTOM);
        
        paletteListModel.clear();
        
        // Clear grid
        gridPanel.removeAll();
        gridPanel.revalidate();
        gridPanel.repaint();
    }
    
    /**
     * Create the grid panel
     */
    private void createGridPanel() {
        gridPanel.removeAll();
        
        int width = currentPart.getWidth();
        int depth = currentPart.getDepth();
        
        gridCells = new JPanel[currentPart.getHeight()][depth][width];
        
        // Create grid for the current layer
        GridBagConstraints gbc = new GridBagConstraints();
        
        for (int z = 0; z < depth; z++) {
            for (int x = 0; x < width; x++) {
                gbc.gridx = x;
                gbc.gridy = z;
                gbc.weightx = 1.0;
                gbc.weighty = 1.0;
                gbc.fill = GridBagConstraints.BOTH;
                gbc.insets = new Insets(1, 1, 1, 1);
                
                for (int y = 0; y < currentPart.getHeight(); y++) {
                    JPanel cell = new JPanel();
                    cell.setBorder(new LineBorder(JBColor.GRAY));
                    cell.setPreferredSize(new Dimension(20, 20));
                    
                    final int finalX = x;
                    final int finalY = y;
                    final int finalZ = z;
                    
                    cell.addMouseListener(new java.awt.event.MouseAdapter() {
                        @Override
                        public void mouseClicked(java.awt.event.MouseEvent evt) {
                            if (currentBlock != null) {
                                placeBlock(finalX, finalY, finalZ, currentBlock);
                            }
                        }
                    });
                    
                    gridCells[y][z][x] = cell;
                }
                
                // Only add the current layer to the panel
                gridPanel.add(gridCells[currentLayer][z][x], gbc);
            }
        }
        
        gridPanel.revalidate();
        gridPanel.repaint();
    }
    
    /**
     * Update the grid view to display the current layer
     */
    private void updateGridView() {
        if (currentPart == null || gridCells == null) {
            return;
        }
        
        // Remove all cells
        gridPanel.removeAll();
        
        int width = currentPart.getWidth();
        int depth = currentPart.getDepth();
        
        // Add cells for the current layer
        GridBagConstraints gbc = new GridBagConstraints();
        
        for (int z = 0; z < depth; z++) {
            for (int x = 0; x < width; x++) {
                gbc.gridx = x;
                gbc.gridy = z;
                gbc.weightx = 1.0;
                gbc.weighty = 1.0;
                gbc.fill = GridBagConstraints.BOTH;
                gbc.insets = new Insets(1, 1, 1, 1);
                
                JPanel cell = gridCells[currentLayer][z][x];
                
                // Update cell color based on block
                BlockState block = currentPart.getBlock(x, currentLayer, z);
                if (block != null) {
                    String blockId = block.getBlock();
                    Color color = blockColors.getOrDefault(blockId, JBColor.WHITE);
                    cell.setBackground(color);
                    
                    // Add tooltip
                    cell.setToolTipText(blockId);
                }
                
                gridPanel.add(cell, gbc);
            }
        }
        
        gridPanel.revalidate();
        gridPanel.repaint();
    }
    
    /**
     * Update the structure size
     */
    private void updateStructureSize() {
        if (currentStructure == null || currentPart == null) {
            return;
        }
        
        int xSize = (Integer) xSizeSpinner.getValue();
        int ySize = (Integer) ySizeSpinner.getValue();
        int zSize = (Integer) zSizeSpinner.getValue();
        
        // Create a new part with the new size
        StructurePart newPart = new StructurePart(currentPart.getId(), 0, 0, 0, xSize, ySize, zSize);
        
        // Copy blocks from the old part
        for (int y = 0; y < Math.min(currentPart.getHeight(), ySize); y++) {
            for (int z = 0; z < Math.min(currentPart.getDepth(), zSize); z++) {
                for (int x = 0; x < Math.min(currentPart.getWidth(), xSize); x++) {
                    BlockState block = currentPart.getBlock(x, y, z);
                    if (block != null) {
                        newPart.setBlock(x, y, z, block);
                    }
                }
            }
        }
        
        // Replace the old part
        currentStructure.getParts().remove(currentPart);
        currentStructure.addPart(newPart);
        currentPart = newPart;
        
        // Update layer selector
        layerSelector.removeAllItems();
        for (int i = 0; i < currentPart.getHeight(); i++) {
            layerSelector.addItem("Layer " + i);
        }
        currentLayer = Math.min(currentLayer, currentPart.getHeight() - 1);
        layerSelector.setSelectedIndex(currentLayer);
        
        // Update grid
        createGridPanel();
        updateGridView();
    }
    
    /**
     * Place a block in the structure
     * 
     * @param x The X position
     * @param y The Y position
     * @param z The Z position
     * @param block The block state
     */
    private void placeBlock(int x, int y, int z, BlockState block) {
        if (currentPart == null) {
            return;
        }
        
        try {
            currentPart.setBlock(x, y, z, block);
            
            // Update grid cell
            JPanel cell = gridCells[y][z][x];
            if (cell != null) {
                String blockId = block.getBlock();
                Color color = blockColors.getOrDefault(blockId, JBColor.WHITE);
                cell.setBackground(color);
                cell.setToolTipText(blockId);
            }
        } catch (IllegalArgumentException e) {
            LOG.warn("Error placing block: " + e.getMessage());
        }
    }
    
    /**
     * Add a block to the palette
     */
    private void addBlockToPalette() {
        String blockId = blockIdField.getText().trim();
        
        if (blockId.isEmpty()) {
            return;
        }
        
        // Add namespace if missing
        if (!blockId.contains(":")) {
            blockId = "minecraft:" + blockId;
        }
        
        // Add to palette if not already present
        if (!paletteListModel.contains(blockId)) {
            paletteListModel.addElement(blockId);
            
            // Set a default color if none exists
            if (!blockColors.containsKey(blockId)) {
                blockColors.put(blockId, generateBlockColor(blockId));
            }
        }
        
        // Select the new block
        paletteList.setSelectedValue(blockId, true);
        currentBlock = new BlockState(blockId);
    }
    
    /**
     * Set the color for the selected block
     */
    private void setBlockColor() {
        String blockId = paletteList.getSelectedValue();
        if (blockId == null) {
            return;
        }
        
        Color currentColor = blockColors.getOrDefault(blockId, JBColor.WHITE);
        Color newColor = ColorPicker.showDialog(this, "Choose Block Color", currentColor, true, null, true);
        
        if (newColor != null) {
            blockColors.put(blockId, newColor);
            paletteList.repaint();
            updateGridView();
        }
    }
    
    /**
     * Generate a color for a block based on its ID
     * 
     * @param blockId The block ID
     * @return A color for the block
     */
    private Color generateBlockColor(String blockId) {
        // Simple hash-based color generation
        int hash = blockId.hashCode();
        
        // Use the hash to generate a hue from 0 to 1
        float hue = Math.abs(hash) % 360 / 360.0f;
        
        // Different saturation and brightness based on the block type
        float saturation = 0.8f;
        float brightness = 0.8f;
        
        if (blockId.contains("stone") || blockId.contains("rock") || blockId.contains("ore")) {
            saturation = 0.3f;
            brightness = 0.7f;
        } else if (blockId.contains("wood") || blockId.contains("log") || blockId.contains("planks")) {
            saturation = 0.6f;
            brightness = 0.7f;
        } else if (blockId.contains("leaf") || blockId.contains("leaves") || blockId.contains("grass") || blockId.contains("plant")) {
            saturation = 0.8f;
            brightness = 0.7f;
        }
        
        return Color.getHSBColor(hue, saturation, brightness);
    }
    
    /**
     * Update the block palette
     */
    private void updateBlockPalette() {
        paletteListModel.clear();
        
        // Add common blocks
        paletteListModel.addElement("minecraft:stone");
        paletteListModel.addElement("minecraft:dirt");
        paletteListModel.addElement("minecraft:grass_block");
        paletteListModel.addElement("minecraft:cobblestone");
        paletteListModel.addElement("minecraft:oak_planks");
        paletteListModel.addElement("minecraft:oak_log");
        
        // Add custom colors
        if (blockColors.isEmpty()) {
            blockColors.put("minecraft:stone", new Color(128, 128, 128));
            blockColors.put("minecraft:dirt", new Color(134, 96, 67));
            blockColors.put("minecraft:grass_block", new Color(95, 159, 53));
            blockColors.put("minecraft:cobblestone", new Color(110, 110, 110));
            blockColors.put("minecraft:oak_planks", new Color(169, 132, 79));
            blockColors.put("minecraft:oak_log", new Color(102, 81, 51));
        }
        
        // Select the first block
        if (paletteListModel.getSize() > 0) {
            paletteList.setSelectedIndex(0);
            String blockId = paletteList.getSelectedValue();
            if (blockId != null) {
                currentBlock = new BlockState(blockId);
            }
        }
    }
    
    /**
     * Save the current structure
     */
    private void saveCurrentStructure() {
        if (currentStructure == null) {
            return;
        }
        
        // Update the structure from form fields
        currentStructure.setId(idField.getText().trim());
        currentStructure.setName(nameField.getText().trim());
        currentStructure.setType((StructureModel.StructureType) typeCombo.getSelectedItem());
        
        // Update the structure in the manager
        refreshStructureList();
        
        // Select the current structure
        structureList.setSelectedValue(currentStructure.getId(), true);
        
        Messages.showInfoMessage(
            project,
            "Structure saved successfully",
            "Structure Saved"
        );
    }
    
    /**
     * Custom cell renderer for blocks in the palette list
     */
    private class BlockListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            
            if (value instanceof String) {
                String blockId = (String) value;
                
                if (!isSelected) {
                    Color color = blockColors.getOrDefault(blockId, JBColor.WHITE);
                    c.setBackground(color);
                    
                    // Use contrasting text color
                    float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
                    if (hsb[2] > 0.7) {
                        c.setForeground(JBColor.BLACK);
                    } else {
                        c.setForeground(JBColor.WHITE);
                    }
                }
            }
            
            return c;
        }
    }
}