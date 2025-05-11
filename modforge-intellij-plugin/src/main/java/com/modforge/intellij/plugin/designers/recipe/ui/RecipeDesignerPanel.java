package com.modforge.intellij.plugin.designers.recipe.ui;

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
import com.intellij.ui.components.JBTabbedPane;
import com.modforge.intellij.plugin.designers.recipe.RecipeManager;
import com.modforge.intellij.plugin.designers.recipe.models.RecipeItem;
import com.modforge.intellij.plugin.designers.recipe.models.RecipeModel;
import com.modforge.intellij.plugin.designers.recipe.models.RecipeModel.RecipeType;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;

/**
 * UI panel for designing Minecraft recipes
 */
public class RecipeDesignerPanel extends SimpleToolWindowPanel {
    private static final Logger LOG = Logger.getInstance(RecipeDesignerPanel.class);
    
    private final Project project;
    private final RecipeManager recipeManager;
    
    private JBList<String> recipeList;
    private DefaultListModel<String> recipeListModel;
    private JBTabbedPane detailsTabs;
    
    private JComboBox<RecipeType> recipeTypeCombo;
    private JTextField idField;
    private JTextField groupField;
    
    // Shaped crafting components
    private JPanel shapedPanel;
    private JPanel craftingGridPanel;
    private JButton[][] craftingGridButtons;
    private JComboBox<Character> keySymbolCombo;
    private JTextField keyItemField;
    private JButton addKeyButton;
    private JList<String> keysList;
    private DefaultListModel<String> keysListModel;
    
    // Shapeless crafting components
    private JPanel shapelessPanel;
    private JTextField ingredientItemField;
    private JButton addIngredientButton;
    private JList<String> ingredientsList;
    private DefaultListModel<String> ingredientsListModel;
    
    // Cooking components
    private JPanel cookingPanel;
    private JTextField cookingIngredientField;
    private JTextField experienceField;
    private JTextField cookingTimeField;
    
    // Stonecutting components
    private JPanel stonecuttingPanel;
    private JTextField stonecuttingIngredientField;
    private JTextField stonecuttingCountField;
    
    // Smithing components
    private JPanel smithingPanel;
    private JTextField baseItemField;
    private JTextField additionItemField;
    
    // Common result components
    private JTextField resultItemField;
    private JTextField resultCountField;
    
    private RecipeModel currentRecipe;
    
    /**
     * Create a new recipe designer panel
     * 
     * @param project The current project
     */
    public RecipeDesignerPanel(@NotNull Project project) {
        super(true);
        this.project = project;
        this.recipeManager = project.getService(RecipeManager.class);
        
        setupUI();
        
        // Load recipes if any exist
        refreshRecipeList();
    }
    
    /**
     * Set up the UI components
     */
    private void setupUI() {
        // Create the toolbar
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        // Add actions here (load, save, etc.)
        
        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("RecipeDesigner", actionGroup, true);
        toolbar.setTargetComponent(this);
        setToolbar(toolbar.getComponent());
        
        // Create the main content panel
        JPanel contentPanel = new JPanel(new BorderLayout());
        
        // Create the split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(200);
        
        // Left panel - Recipe list
        JPanel leftPanel = new JPanel(new BorderLayout());
        JPanel recipeActionsPanel = new JPanel();
        JButton addButton = new JButton("Add");
        JButton removeButton = new JButton("Remove");
        
        recipeActionsPanel.add(addButton);
        recipeActionsPanel.add(removeButton);
        
        recipeListModel = new DefaultListModel<>();
        recipeList = new JBList<>(recipeListModel);
        recipeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        leftPanel.add(new JBScrollPane(recipeList), BorderLayout.CENTER);
        leftPanel.add(recipeActionsPanel, BorderLayout.SOUTH);
        
        // Right panel - Recipe details
        JPanel rightPanel = new JPanel(new BorderLayout());
        
        // Basic info panel
        JPanel basicInfoPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        basicInfoPanel.add(new JLabel("Recipe Type:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        
        recipeTypeCombo = new ComboBox<>(RecipeType.values());
        basicInfoPanel.add(recipeTypeCombo, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        
        basicInfoPanel.add(new JLabel("Recipe ID:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        
        idField = new JTextField();
        basicInfoPanel.add(idField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        
        basicInfoPanel.add(new JLabel("Group:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        
        groupField = new JTextField();
        basicInfoPanel.add(groupField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        
        basicInfoPanel.add(new JLabel("Result Item:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        
        resultItemField = new JTextField();
        basicInfoPanel.add(resultItemField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        
        basicInfoPanel.add(new JLabel("Result Count:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        
        resultCountField = new JTextField("1");
        basicInfoPanel.add(resultCountField, gbc);
        
        // Create the tabbed pane for recipe type-specific details
        detailsTabs = new JBTabbedPane();
        
        // Create panels for each recipe type
        createShapedCraftingPanel();
        createShapelessCraftingPanel();
        createCookingPanel();
        createStonecuttingPanel();
        createSmithingPanel();
        
        // Add panels to tabs
        detailsTabs.addTab("Shaped Crafting", shapedPanel);
        detailsTabs.addTab("Shapeless Crafting", shapelessPanel);
        detailsTabs.addTab("Cooking", cookingPanel);
        detailsTabs.addTab("Stonecutting", stonecuttingPanel);
        detailsTabs.addTab("Smithing", smithingPanel);
        
        rightPanel.add(basicInfoPanel, BorderLayout.NORTH);
        rightPanel.add(detailsTabs, BorderLayout.CENTER);
        
        // Add a save button
        JButton saveButton = new JButton("Save Recipe");
        saveButton.addActionListener(e -> saveCurrentRecipe());
        rightPanel.add(saveButton, BorderLayout.SOUTH);
        
        // Add panels to split pane
        splitPane.setLeftComponent(leftPanel);
        splitPane.setRightComponent(rightPanel);
        
        contentPanel.add(splitPane, BorderLayout.CENTER);
        
        setContent(contentPanel);
        
        // Add listeners
        addButton.addActionListener(e -> addNewRecipe());
        removeButton.addActionListener(e -> removeSelectedRecipe());
        
        recipeList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadSelectedRecipe();
            }
        });
        
        recipeTypeCombo.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                updateSelectedRecipeType((RecipeType) e.getItem());
            }
        });
    }
    
    /**
     * Create the shaped crafting panel
     */
    private void createShapedCraftingPanel() {
        shapedPanel = new JPanel(new BorderLayout());
        
        // Crafting grid
        JPanel gridPanel = new JPanel(new BorderLayout());
        craftingGridPanel = new JPanel(new GridLayout(3, 3, 5, 5));
        craftingGridButtons = new JButton[3][3];
        
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                final int r = row;
                final int c = col;
                craftingGridButtons[row][col] = new JButton(" ");
                craftingGridButtons[row][col].setPreferredSize(new Dimension(50, 50));
                craftingGridButtons[row][col].addActionListener(e -> onCraftingGridButtonClick(r, c));
                craftingGridPanel.add(craftingGridButtons[row][col]);
            }
        }
        
        gridPanel.add(new JLabel("Crafting Grid:"), BorderLayout.NORTH);
        gridPanel.add(craftingGridPanel, BorderLayout.CENTER);
        
        // Key mapping
        JPanel keyPanel = new JPanel(new BorderLayout());
        JPanel keyInputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        keySymbolCombo = new JComboBox<>(new Character[] {
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
            'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
            '#', '$', '%', '&', '*', '+', '-', '/', '=', '@', '~', ' '
        });
        
        keyItemField = new JTextField(20);
        addKeyButton = new JButton("Add Key");
        
        keyInputPanel.add(new JLabel("Symbol:"));
        keyInputPanel.add(keySymbolCombo);
        keyInputPanel.add(new JLabel("Item:"));
        keyInputPanel.add(keyItemField);
        keyInputPanel.add(addKeyButton);
        
        keysListModel = new DefaultListModel<>();
        keysList = new JList<>(keysListModel);
        
        keyPanel.add(new JLabel("Key Mappings:"), BorderLayout.NORTH);
        keyPanel.add(keyInputPanel, BorderLayout.CENTER);
        keyPanel.add(new JBScrollPane(keysList), BorderLayout.SOUTH);
        
        // Add components to panel
        JPanel topPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        topPanel.add(gridPanel);
        topPanel.add(keyPanel);
        
        shapedPanel.add(topPanel, BorderLayout.CENTER);
        
        // Add listeners
        addKeyButton.addActionListener(e -> addKeyMapping());
    }
    
    /**
     * Create the shapeless crafting panel
     */
    private void createShapelessCraftingPanel() {
        shapelessPanel = new JPanel(new BorderLayout());
        
        // Ingredients
        JPanel ingredientsPanel = new JPanel(new BorderLayout());
        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        ingredientItemField = new JTextField(20);
        addIngredientButton = new JButton("Add Ingredient");
        
        inputPanel.add(new JLabel("Item:"));
        inputPanel.add(ingredientItemField);
        inputPanel.add(addIngredientButton);
        
        ingredientsListModel = new DefaultListModel<>();
        ingredientsList = new JList<>(ingredientsListModel);
        
        ingredientsPanel.add(new JLabel("Ingredients:"), BorderLayout.NORTH);
        ingredientsPanel.add(inputPanel, BorderLayout.CENTER);
        ingredientsPanel.add(new JBScrollPane(ingredientsList), BorderLayout.SOUTH);
        
        shapelessPanel.add(ingredientsPanel, BorderLayout.CENTER);
        
        // Add listeners
        addIngredientButton.addActionListener(e -> addShapelessIngredient());
    }
    
    /**
     * Create the cooking panel
     */
    private void createCookingPanel() {
        cookingPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        cookingPanel.add(new JLabel("Ingredient:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        
        cookingIngredientField = new JTextField();
        cookingPanel.add(cookingIngredientField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        
        cookingPanel.add(new JLabel("Experience:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        
        experienceField = new JTextField("0.0");
        cookingPanel.add(experienceField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        
        cookingPanel.add(new JLabel("Cooking Time:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        
        cookingTimeField = new JTextField("200");
        cookingPanel.add(cookingTimeField, gbc);
    }
    
    /**
     * Create the stonecutting panel
     */
    private void createStonecuttingPanel() {
        stonecuttingPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        stonecuttingPanel.add(new JLabel("Input Material:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        
        stonecuttingIngredientField = new JTextField();
        stonecuttingPanel.add(stonecuttingIngredientField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        
        stonecuttingPanel.add(new JLabel("Output Count:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        
        stonecuttingCountField = new JTextField("1");
        stonecuttingPanel.add(stonecuttingCountField, gbc);
    }
    
    /**
     * Create the smithing panel
     */
    private void createSmithingPanel() {
        smithingPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        smithingPanel.add(new JLabel("Base Item:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        
        baseItemField = new JTextField();
        smithingPanel.add(baseItemField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        
        smithingPanel.add(new JLabel("Addition:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        
        additionItemField = new JTextField();
        smithingPanel.add(additionItemField, gbc);
    }
    
    /**
     * Refresh the recipe list
     */
    private void refreshRecipeList() {
        recipeListModel.clear();
        
        for (RecipeModel recipe : recipeManager.getAllRecipes()) {
            recipeListModel.addElement(recipe.getId());
        }
    }
    
    /**
     * Add a new recipe
     */
    private void addNewRecipe() {
        String id = Messages.showInputDialog(
            project,
            "Enter Recipe ID:",
            "New Recipe",
            Messages.getQuestionIcon()
        );
        
        if (id == null || id.trim().isEmpty()) {
            return;
        }
        
        // Add namespace if missing
        if (!id.contains(":")) {
            id = "minecraft:" + id;
        }
        
        // Create a new recipe
        RecipeModel recipe = recipeManager.createRecipe(id, RecipeType.CRAFTING_SHAPED);
        
        // Update the list
        refreshRecipeList();
        
        // Select the new recipe
        recipeList.setSelectedValue(id, true);
    }
    
    /**
     * Remove the selected recipe
     */
    private void removeSelectedRecipe() {
        String id = recipeList.getSelectedValue();
        if (id == null) {
            return;
        }
        
        int result = Messages.showYesNoDialog(
            project,
            "Are you sure you want to delete the recipe '" + id + "'?",
            "Delete Recipe",
            Messages.getQuestionIcon()
        );
        
        if (result == Messages.YES) {
            recipeManager.deleteRecipe(id);
            refreshRecipeList();
            clearForm();
        }
    }
    
    /**
     * Load the selected recipe
     */
    private void loadSelectedRecipe() {
        String id = recipeList.getSelectedValue();
        if (id == null) {
            clearForm();
            return;
        }
        
        RecipeModel recipe = recipeManager.getRecipe(id);
        if (recipe == null) {
            clearForm();
            return;
        }
        
        currentRecipe = recipe;
        
        // Set basic info
        idField.setText(recipe.getId());
        groupField.setText(recipe.getGroup());
        recipeTypeCombo.setSelectedItem(recipe.getType());
        
        // Set result
        RecipeItem result = recipe.getResult();
        resultItemField.setText(result.getItem());
        resultCountField.setText(String.valueOf(result.getCount()));
        
        // Update UI based on recipe type
        updateUIForRecipeType(recipe.getType());
        
        // Set type-specific fields
        switch (recipe.getType()) {
            case CRAFTING_SHAPED:
                loadShapedRecipe(recipe);
                break;
                
            case CRAFTING_SHAPELESS:
                loadShapelessRecipe(recipe);
                break;
                
            case SMELTING:
            case BLASTING:
            case SMOKING:
            case CAMPFIRE_COOKING:
                loadCookingRecipe(recipe);
                break;
                
            case STONECUTTING:
                loadStonecuttingRecipe(recipe);
                break;
                
            case SMITHING:
                loadSmithingRecipe(recipe);
                break;
        }
    }
    
    /**
     * Load a shaped crafting recipe
     * 
     * @param recipe The recipe to load
     */
    private void loadShapedRecipe(RecipeModel recipe) {
        // Clear existing data
        keysListModel.clear();
        
        // Set pattern in grid
        List<String> pattern = recipe.getPattern();
        for (int row = 0; row < Math.min(pattern.size(), 3); row++) {
            String patternRow = pattern.get(row);
            for (int col = 0; col < Math.min(patternRow.length(), 3); col++) {
                char symbol = patternRow.charAt(col);
                craftingGridButtons[row][col].setText(String.valueOf(symbol));
            }
        }
        
        // Add keys to list
        for (Map.Entry<Character, RecipeItem> entry : recipe.getKey().entrySet()) {
            char symbol = entry.getKey();
            RecipeItem item = entry.getValue();
            keysListModel.addElement(symbol + " -> " + item.getItem());
        }
    }
    
    /**
     * Load a shapeless crafting recipe
     * 
     * @param recipe The recipe to load
     */
    private void loadShapelessRecipe(RecipeModel recipe) {
        // Clear existing data
        ingredientsListModel.clear();
        
        // Add ingredients to list
        for (RecipeItem ingredient : recipe.getIngredients()) {
            ingredientsListModel.addElement(ingredient.getItem());
        }
    }
    
    /**
     * Load a cooking recipe
     * 
     * @param recipe The recipe to load
     */
    private void loadCookingRecipe(RecipeModel recipe) {
        List<RecipeItem> ingredients = recipe.getIngredients();
        if (!ingredients.isEmpty()) {
            cookingIngredientField.setText(ingredients.get(0).getItem());
        } else {
            cookingIngredientField.setText("");
        }
        
        experienceField.setText(String.valueOf(recipe.getExperience()));
        cookingTimeField.setText(String.valueOf(recipe.getCookingTime()));
    }
    
    /**
     * Load a stonecutting recipe
     * 
     * @param recipe The recipe to load
     */
    private void loadStonecuttingRecipe(RecipeModel recipe) {
        List<RecipeItem> ingredients = recipe.getIngredients();
        if (!ingredients.isEmpty()) {
            stonecuttingIngredientField.setText(ingredients.get(0).getItem());
        } else {
            stonecuttingIngredientField.setText("");
        }
        
        stonecuttingCountField.setText(String.valueOf(recipe.getResult().getCount()));
    }
    
    /**
     * Load a smithing recipe
     * 
     * @param recipe The recipe to load
     */
    private void loadSmithingRecipe(RecipeModel recipe) {
        List<RecipeItem> ingredients = recipe.getIngredients();
        if (ingredients.size() > 0) {
            baseItemField.setText(ingredients.get(0).getItem());
        } else {
            baseItemField.setText("");
        }
        
        if (ingredients.size() > 1) {
            additionItemField.setText(ingredients.get(1).getItem());
        } else {
            additionItemField.setText("");
        }
    }
    
    /**
     * Clear the form
     */
    private void clearForm() {
        currentRecipe = null;
        
        idField.setText("");
        groupField.setText("");
        resultItemField.setText("");
        resultCountField.setText("1");
        
        // Clear shaped crafting fields
        keysListModel.clear();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                craftingGridButtons[row][col].setText(" ");
            }
        }
        
        // Clear shapeless crafting fields
        ingredientsListModel.clear();
        ingredientItemField.setText("");
        
        // Clear cooking fields
        cookingIngredientField.setText("");
        experienceField.setText("0.0");
        cookingTimeField.setText("200");
        
        // Clear stonecutting fields
        stonecuttingIngredientField.setText("");
        stonecuttingCountField.setText("1");
        
        // Clear smithing fields
        baseItemField.setText("");
        additionItemField.setText("");
    }
    
    /**
     * Update the UI for the selected recipe type
     * 
     * @param type The recipe type
     */
    private void updateUIForRecipeType(RecipeType type) {
        switch (type) {
            case CRAFTING_SHAPED:
                detailsTabs.setSelectedIndex(0);
                break;
                
            case CRAFTING_SHAPELESS:
                detailsTabs.setSelectedIndex(1);
                break;
                
            case SMELTING:
            case BLASTING:
            case SMOKING:
            case CAMPFIRE_COOKING:
                detailsTabs.setSelectedIndex(2);
                break;
                
            case STONECUTTING:
                detailsTabs.setSelectedIndex(3);
                break;
                
            case SMITHING:
                detailsTabs.setSelectedIndex(4);
                break;
        }
    }
    
    /**
     * Update the selected recipe type
     * 
     * @param type The new recipe type
     */
    private void updateSelectedRecipeType(RecipeType type) {
        if (currentRecipe != null) {
            currentRecipe.setType(type);
        }
        
        updateUIForRecipeType(type);
    }
    
    /**
     * Handle a click on a crafting grid button
     * 
     * @param row The row index
     * @param col The column index
     */
    private void onCraftingGridButtonClick(int row, int col) {
        if (currentRecipe == null || currentRecipe.getType() != RecipeType.CRAFTING_SHAPED) {
            return;
        }
        
        Character selected = (Character) keySymbolCombo.getSelectedItem();
        if (selected != null) {
            craftingGridButtons[row][col].setText(selected.toString());
            
            // Update the pattern in the recipe
            List<String> pattern = currentRecipe.getPattern();
            
            // Ensure we have enough rows
            while (pattern.size() <= row) {
                pattern.add("   ");
            }
            
            // Update the row
            String rowStr = pattern.get(row);
            StringBuilder newRow = new StringBuilder(rowStr);
            
            // Ensure the row is long enough
            while (newRow.length() <= col) {
                newRow.append(' ');
            }
            
            // Set the character
            newRow.setCharAt(col, selected);
            
            // Update the pattern
            pattern.set(row, newRow.toString());
            currentRecipe.setPattern(pattern);
        }
    }
    
    /**
     * Add a key mapping
     */
    private void addKeyMapping() {
        if (currentRecipe == null || currentRecipe.getType() != RecipeType.CRAFTING_SHAPED) {
            return;
        }
        
        Character symbol = (Character) keySymbolCombo.getSelectedItem();
        String item = keyItemField.getText().trim();
        
        if (symbol == null || item.isEmpty()) {
            return;
        }
        
        // Add to the key mapping
        currentRecipe.addKey(symbol, new RecipeItem(item));
        
        // Update the list
        keysListModel.addElement(symbol + " -> " + item);
        
        // Clear the item field
        keyItemField.setText("");
    }
    
    /**
     * Add a shapeless ingredient
     */
    private void addShapelessIngredient() {
        if (currentRecipe == null || currentRecipe.getType() != RecipeType.CRAFTING_SHAPELESS) {
            return;
        }
        
        String item = ingredientItemField.getText().trim();
        
        if (item.isEmpty()) {
            return;
        }
        
        // Add to the ingredients
        currentRecipe.addIngredient(new RecipeItem(item));
        
        // Update the list
        ingredientsListModel.addElement(item);
        
        // Clear the field
        ingredientItemField.setText("");
    }
    
    /**
     * Save the current recipe
     */
    private void saveCurrentRecipe() {
        if (currentRecipe == null) {
            return;
        }
        
        // Update the recipe from form fields
        currentRecipe.setId(idField.getText().trim());
        currentRecipe.setGroup(groupField.getText().trim());
        
        // Update the result
        String resultItem = resultItemField.getText().trim();
        int resultCount;
        try {
            resultCount = Integer.parseInt(resultCountField.getText().trim());
        } catch (NumberFormatException e) {
            resultCount = 1;
        }
        
        currentRecipe.setResult(new RecipeItem(resultItem, resultCount));
        
        // Update type-specific fields
        switch (currentRecipe.getType()) {
            case CRAFTING_SHAPED:
                // Pattern is updated when clicking the grid buttons
                // Keys are updated when adding key mappings
                break;
                
            case CRAFTING_SHAPELESS:
                // Ingredients are updated when adding ingredients
                break;
                
            case SMELTING:
            case BLASTING:
            case SMOKING:
            case CAMPFIRE_COOKING:
                saveCookingRecipe();
                break;
                
            case STONECUTTING:
                saveStonecuttingRecipe();
                break;
                
            case SMITHING:
                saveSmithingRecipe();
                break;
        }
        
        // Update the recipe in the manager
        refreshRecipeList();
        
        // Select the current recipe
        recipeList.setSelectedValue(currentRecipe.getId(), true);
        
        Messages.showInfoMessage(
            project,
            "Recipe saved successfully",
            "Recipe Saved"
        );
    }
    
    /**
     * Save a cooking recipe
     */
    private void saveCookingRecipe() {
        String ingredient = cookingIngredientField.getText().trim();
        
        // Clear existing ingredients
        List<RecipeItem> ingredients = currentRecipe.getIngredients();
        ingredients.clear();
        
        // Add the ingredient
        if (!ingredient.isEmpty()) {
            currentRecipe.addIngredient(new RecipeItem(ingredient));
        }
        
        // Update experience
        try {
            float experience = Float.parseFloat(experienceField.getText().trim());
            currentRecipe.setExperience(experience);
        } catch (NumberFormatException e) {
            currentRecipe.setExperience(0.0f);
        }
        
        // Update cooking time
        try {
            int cookingTime = Integer.parseInt(cookingTimeField.getText().trim());
            currentRecipe.setCookingTime(cookingTime);
        } catch (NumberFormatException e) {
            currentRecipe.setCookingTime(200);
        }
    }
    
    /**
     * Save a stonecutting recipe
     */
    private void saveStonecuttingRecipe() {
        String ingredient = stonecuttingIngredientField.getText().trim();
        
        // Clear existing ingredients
        List<RecipeItem> ingredients = currentRecipe.getIngredients();
        ingredients.clear();
        
        // Add the ingredient
        if (!ingredient.isEmpty()) {
            currentRecipe.addIngredient(new RecipeItem(ingredient));
        }
        
        // Result count is updated with the result
    }
    
    /**
     * Save a smithing recipe
     */
    private void saveSmithingRecipe() {
        String baseItem = baseItemField.getText().trim();
        String additionItem = additionItemField.getText().trim();
        
        // Clear existing ingredients
        List<RecipeItem> ingredients = currentRecipe.getIngredients();
        ingredients.clear();
        
        // Add the base and addition
        if (!baseItem.isEmpty()) {
            currentRecipe.addIngredient(new RecipeItem(baseItem));
        }
        
        if (!additionItem.isEmpty()) {
            currentRecipe.addIngredient(new RecipeItem(additionItem));
        }
    }
}