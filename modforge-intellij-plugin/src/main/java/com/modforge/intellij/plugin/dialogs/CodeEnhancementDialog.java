package com.modforge.intellij.plugin.dialogs;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * Dialog for displaying code enhancements before applying them.
 */
public class CodeEnhancementDialog extends DialogWrapper {
    private static final Logger LOG = Logger.getInstance(CodeEnhancementDialog.class);
    
    private final Project project;
    private final String originalCode;
    private final String enhancedCode;
    private final String explanation;
    private final String enhancementType;
    
    private JPanel mainPanel;
    private JTextArea originalCodeTextArea;
    private JTextArea enhancedCodeTextArea;
    private JTextArea explanationTextArea;
    
    /**
     * Constructor for the code enhancement dialog.
     *
     * @param project The current project
     * @param originalCode The original code
     * @param enhancedCode The enhanced code
     * @param explanation Explanation of the enhancements
     * @param enhancementType The type of enhancement that was performed
     */
    public CodeEnhancementDialog(
            @NotNull Project project,
            @NotNull String originalCode,
            @NotNull String enhancedCode,
            @NotNull String explanation,
            @NotNull String enhancementType) {
        super(project, true);
        this.project = project;
        this.originalCode = originalCode;
        this.enhancedCode = enhancedCode;
        this.explanation = explanation;
        this.enhancementType = enhancementType;
        
        setTitle("Code Enhancement: " + enhancementType);
        init();
    }
    
    @Override
    protected @Nullable JComponent createCenterPanel() {
        mainPanel = new JPanel(new BorderLayout());
        
        // Create tabbed pane for different views
        JBTabbedPane tabbedPane = new JBTabbedPane();
        
        // Tab 1: Side-by-side comparison
        JPanel comparisonPanel = createComparisonPanel();
        tabbedPane.addTab("Comparison", comparisonPanel);
        
        // Tab 2: Only enhanced code
        JPanel enhancedPanel = createEnhancedPanel();
        tabbedPane.addTab("Enhanced Code", enhancedPanel);
        
        // Tab 3: Explanation
        JPanel explanationPanel = createExplanationPanel();
        tabbedPane.addTab("Explanation", explanationPanel);
        
        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        mainPanel.setPreferredSize(new Dimension(900, 600));
        
        return mainPanel;
    }
    
    /**
     * Creates a panel with a side-by-side comparison of the original and enhanced code.
     *
     * @return The comparison panel
     */
    private JPanel createComparisonPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 10, 0));
        panel.setBorder(JBUI.Borders.empty(10));
        
        // Original code on the left
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(BorderFactory.createTitledBorder("Original Code"));
        
        originalCodeTextArea = new JTextArea(originalCode);
        originalCodeTextArea.setEditable(false);
        originalCodeTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        
        leftPanel.add(new JBScrollPane(originalCodeTextArea), BorderLayout.CENTER);
        
        // Enhanced code on the right
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createTitledBorder("Enhanced Code"));
        
        enhancedCodeTextArea = new JTextArea(enhancedCode);
        enhancedCodeTextArea.setEditable(false);
        enhancedCodeTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        
        rightPanel.add(new JBScrollPane(enhancedCodeTextArea), BorderLayout.CENTER);
        
        panel.add(leftPanel);
        panel.add(rightPanel);
        
        return panel;
    }
    
    /**
     * Creates a panel with just the enhanced code.
     *
     * @return The enhanced code panel
     */
    private JPanel createEnhancedPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(10));
        
        JTextArea enhancedCodeArea = new JTextArea(enhancedCode);
        enhancedCodeArea.setEditable(false);
        enhancedCodeArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        
        panel.add(new JBScrollPane(enhancedCodeArea), BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Creates a panel with the explanation of the enhancements.
     *
     * @return The explanation panel
     */
    private JPanel createExplanationPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(10));
        
        // Enhancement type label
        JLabel enhancementTypeLabel = new JLabel("Enhancement Type: " + enhancementType);
        enhancementTypeLabel.setFont(enhancementTypeLabel.getFont().deriveFont(Font.BOLD));
        enhancementTypeLabel.setBorder(JBUI.Borders.empty(0, 0, 10, 0));
        
        // Explanation text area
        explanationTextArea = new JTextArea(explanation);
        explanationTextArea.setEditable(false);
        explanationTextArea.setLineWrap(true);
        explanationTextArea.setWrapStyleWord(true);
        
        panel.add(enhancementTypeLabel, BorderLayout.NORTH);
        panel.add(new JBScrollPane(explanationTextArea), BorderLayout.CENTER);
        
        return panel;
    }
    
    @Override
    protected String getDimensionServiceKey() {
        return "ModForge.CodeEnhancementDialog";
    }
    
    @Override
    protected Action[] createActions() {
        return new Action[] {getOKAction(), getCancelAction()};
    }
    
    @Override
    protected void doOKAction() {
        // OK means accept the enhancement and apply it
        super.doOKAction();
    }
    
    @Override
    public void doCancelAction() {
        // Cancel means reject the enhancement and don't apply it
        super.doCancelAction();
    }
}