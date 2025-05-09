package com.modforge.intellij.plugin.ui.toolwindow;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.modforge.intellij.plugin.actions.GenerateCodeAction;
import com.modforge.intellij.plugin.actions.GenerateDocumentationAction;
import com.modforge.intellij.plugin.actions.ExplainCodeAction;
import com.modforge.intellij.plugin.actions.FixErrorsAction;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * Panel for the AI Assist tab in the tool window.
 * This panel provides access to various AI-assisted development actions.
 */
public final class AIAssistPanel {
    private static final Logger LOG = Logger.getInstance(AIAssistPanel.class);
    
    private final Project project;
    
    private JPanel mainPanel;
    private JPanel quickActionsPanel;
    private JPanel helpPanel;
    
    /**
     * Creates a new AIAssistPanel.
     * @param project The project
     */
    public AIAssistPanel(@NotNull Project project) {
        this.project = project;
        
        createUI();
    }
    
    /**
     * Gets the panel content.
     * @return The panel content
     */
    @NotNull
    public JComponent getContent() {
        return mainPanel;
    }
    
    /**
     * Creates the UI for the panel.
     */
    private void createUI() {
        // Create simple tool window panel
        SimpleToolWindowPanel panel = new SimpleToolWindowPanel(true, true);
        
        // Create toolbar
        ActionToolbar toolbar = createToolbar();
        panel.setToolbar(toolbar.getComponent());
        
        // Create content
        JBTabbedPane tabbedPane = createTabbedPane();
        panel.setContent(tabbedPane);
        
        // Create main panel
        mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(panel, BorderLayout.CENTER);
    }
    
    /**
     * Creates the toolbar.
     * @return The toolbar
     */
    @NotNull
    private ActionToolbar createToolbar() {
        DefaultActionGroup group = new DefaultActionGroup();
        
        // Add actions
        group.add(ActionManager.getInstance().getAction("ModForge.GenerateCode"));
        group.add(ActionManager.getInstance().getAction("ModForge.FixErrors"));
        group.add(ActionManager.getInstance().getAction("ModForge.GenerateDocumentation"));
        group.add(ActionManager.getInstance().getAction("ModForge.ExplainCode"));
        
        // Create toolbar
        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(
                ActionPlaces.TOOLBAR,
                group,
                true
        );
        
        return toolbar;
    }
    
    /**
     * Creates the tabbed pane.
     * @return The tabbed pane
     */
    @NotNull
    private JBTabbedPane createTabbedPane() {
        JBTabbedPane tabbedPane = new JBTabbedPane();
        
        // Create quick actions panel
        quickActionsPanel = createQuickActionsPanel();
        
        // Create help panel
        helpPanel = createHelpPanel();
        
        // Add tabs
        tabbedPane.add("Quick Actions", quickActionsPanel);
        tabbedPane.add("Help", helpPanel);
        
        return tabbedPane;
    }
    
    /**
     * Creates the quick actions panel.
     * @return The quick actions panel
     */
    @NotNull
    private JPanel createQuickActionsPanel() {
        // Create buttons
        JButton generateCodeButton = createActionButton(
                "Generate Code",
                AllIcons.Actions.Execute,
                "Generate code using AI.",
                new GenerateCodeAction()
        );
        
        JButton fixErrorsButton = createActionButton(
                "Fix Errors",
                AllIcons.Actions.QuickfixBulb,
                "Fix errors using AI.",
                new FixErrorsAction()
        );
        
        JButton generateDocumentationButton = createActionButton(
                "Generate Documentation",
                AllIcons.Actions.Documentation,
                "Generate documentation using AI.",
                new GenerateDocumentationAction()
        );
        
        JButton explainCodeButton = createActionButton(
                "Explain Code",
                AllIcons.Actions.Help,
                "Explain code using AI.",
                new ExplainCodeAction()
        );
        
        // Create code actions panel
        JPanel codeActionsPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        codeActionsPanel.setBorder(JBUI.Borders.empty(10));
        codeActionsPanel.add(generateCodeButton);
        codeActionsPanel.add(fixErrorsButton);
        codeActionsPanel.add(generateDocumentationButton);
        codeActionsPanel.add(explainCodeButton);
        
        // Create code actions titled panel
        JPanel codeTitledPanel = new JPanel(new BorderLayout());
        codeTitledPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UIUtil.getBorderColor()),
                "Code Actions",
                TitledBorder.LEFT,
                TitledBorder.TOP
        ));
        codeTitledPanel.add(codeActionsPanel, BorderLayout.CENTER);
        
        // Create panel
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(10));
        panel.add(codeTitledPanel, BorderLayout.NORTH);
        
        // Create status panel
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(JBUI.Borders.empty(10, 0, 0, 0));
        
        JBLabel statusLabel = new JBLabel("Ready to help with your Minecraft mod development.");
        statusLabel.setForeground(JBColor.GRAY);
        statusPanel.add(statusLabel, BorderLayout.CENTER);
        
        panel.add(statusPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    /**
     * Creates the help panel.
     * @return The help panel
     */
    @NotNull
    private JPanel createHelpPanel() {
        // Create panel
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(10));
        
        // Create content
        JPanel contentPanel = FormBuilder.createFormBuilder()
                .addComponent(new JBLabel("ModForge AI Assistant", UIUtil.ComponentStyle.LARGE))
                .addVerticalGap(10)
                .addComponent(new JBLabel("Use the AI assistant to help with your Minecraft mod development:"))
                .addVerticalGap(5)
                .addComponent(createHelpItem("Generate Code", "Generate code based on your description."))
                .addComponent(createHelpItem("Fix Errors", "Fix compilation errors in your code."))
                .addComponent(createHelpItem("Generate Documentation", "Add documentation to your code."))
                .addComponent(createHelpItem("Explain Code", "Get an explanation of your code."))
                .addVerticalGap(10)
                .addComponent(new JBLabel("Tips:", UIUtil.ComponentStyle.BOLD))
                .addVerticalGap(5)
                .addComponent(new JBLabel("- You can select specific code or work with entire files."))
                .addComponent(new JBLabel("- For best results, provide clear descriptions when generating code."))
                .addComponent(new JBLabel("- Use pattern recognition to reduce API usage and improve response time."))
                .addComponent(new JBLabel("- Enable continuous development to automatically fix errors."))
                .addVerticalGap(10)
                .getPanel();
        
        // Create scroll pane
        JBScrollPane scrollPane = new JBScrollPane(contentPanel);
        scrollPane.setBorder(JBUI.Borders.empty());
        
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Creates a help item.
     * @param title The title
     * @param description The description
     * @return The help item
     */
    @NotNull
    private JPanel createHelpItem(@NotNull String title, @NotNull String description) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(5, 0));
        
        JBLabel titleLabel = new JBLabel(title, UIUtil.ComponentStyle.BOLD);
        JBLabel descriptionLabel = new JBLabel(description);
        descriptionLabel.setForeground(JBColor.GRAY);
        
        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(descriptionLabel, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Creates an action button.
     * @param text The button text
     * @param icon The button icon
     * @param toolTipText The button tool tip text
     * @param action The action to perform
     * @return The button
     */
    @NotNull
    private JButton createActionButton(@NotNull String text, Icon icon, @NotNull String toolTipText, @NotNull Action action) {
        JButton button = new JButton(text, icon);
        button.setToolTipText(toolTipText);
        button.addActionListener(e -> action.actionPerformed(null));
        
        return button;
    }
}