package com.modforge.intellij.plugin.toolwindow;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.JBUI;
import com.modforge.intellij.plugin.ai.AIServiceManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

/**
 * Panel for resolving errors in code.
 * This panel allows users to paste error messages and get AI-powered suggestions to fix them.
 */
public class ErrorResolverPanel extends JPanel {
    private static final Logger LOG = Logger.getInstance(ErrorResolverPanel.class);
    
    private final Project project;
    private final JBTextArea errorTextArea;
    private final JBTextArea codeTextArea;
    private final JBTextArea resultTextArea;
    private final ComboBox<String> languageComboBox;
    private final JButton resolveButton;
    
    /**
     * Creates a new error resolver panel.
     *
     * @param project The project
     */
    public ErrorResolverPanel(@NotNull Project project) {
        super(new BorderLayout());
        this.project = project;
        
        // Create components
        errorTextArea = new JBTextArea();
        errorTextArea.setRows(5);
        errorTextArea.setLineWrap(true);
        errorTextArea.setWrapStyleWord(true);
        
        codeTextArea = new JBTextArea();
        codeTextArea.setRows(10);
        codeTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        
        resultTextArea = new JBTextArea();
        resultTextArea.setRows(10);
        resultTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        resultTextArea.setEditable(false);
        
        languageComboBox = new ComboBox<>(new String[]{"Java", "Kotlin", "Groovy"});
        resolveButton = new JButton("Resolve Error");
        
        // Set up layout
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(JBUI.Borders.empty(5));
        
        JPanel errorPanel = new JPanel(new BorderLayout());
        errorPanel.setBorder(JBUI.Borders.empty(5));
        errorPanel.add(new JBLabel("Error Message:"), BorderLayout.NORTH);
        errorPanel.add(new JBScrollPane(errorTextArea), BorderLayout.CENTER);
        
        JPanel optionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        optionsPanel.add(new JBLabel("Language:"));
        optionsPanel.add(languageComboBox);
        optionsPanel.add(resolveButton);
        
        topPanel.add(errorPanel, BorderLayout.CENTER);
        topPanel.add(optionsPanel, BorderLayout.SOUTH);
        
        JPanel codePanel = new JPanel(new BorderLayout());
        codePanel.setBorder(JBUI.Borders.empty(5));
        codePanel.add(new JBLabel("Code with Error (optional):"), BorderLayout.NORTH);
        codePanel.add(new JBScrollPane(codeTextArea), BorderLayout.CENTER);
        
        JPanel resultPanel = new JPanel(new BorderLayout());
        resultPanel.setBorder(JBUI.Borders.empty(5));
        resultPanel.add(new JBLabel("Suggested Fix:"), BorderLayout.NORTH);
        resultPanel.add(new JBScrollPane(resultTextArea), BorderLayout.CENTER);
        
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, codePanel, resultPanel);
        splitPane.setResizeWeight(0.5);
        
        add(topPanel, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
        
        // Set up event listeners
        resolveButton.addActionListener(e -> resolveError());
    }
    
    /**
     * Resolves the error using AI.
     */
    private void resolveError() {
        String errorMessage = errorTextArea.getText().trim();
        String code = codeTextArea.getText().trim();
        String language = (String) languageComboBox.getSelectedItem();
        
        if (errorMessage.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter an error message.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Show loading state
        resultTextArea.setText("Analyzing error and generating fix...");
        resolveButton.setEnabled(false);
        
        // Run in background
        SwingUtilities.invokeLater(() -> {
            try {
                // Get AI service
                AIServiceManager aiService = AIServiceManager.getInstance();
                
                String result;
                if (code.isEmpty()) {
                    // Only error message
                    result = aiService.explainError(errorMessage);
                } else {
                    // Error message and code
                    result = aiService.fixCode(code, errorMessage, null);
                }
                
                // Show result
                resultTextArea.setText(result);
            } catch (IOException ex) {
                LOG.error("Error resolving error", ex);
                resultTextArea.setText("Error: " + ex.getMessage());
            } finally {
                resolveButton.setEnabled(true);
            }
        });
    }
}