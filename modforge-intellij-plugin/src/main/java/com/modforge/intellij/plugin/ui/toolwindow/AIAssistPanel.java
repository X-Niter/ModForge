package com.modforge.intellij.plugin.ui.toolwindow;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.panels.VerticalLayout;
import com.intellij.util.ui.JBUI;
import com.modforge.intellij.plugin.services.AutonomousCodeGenerationService;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Panel for the AI Assist tab in the tool window.
 * This panel provides a chat interface to interact with the AI.
 */
public final class AIAssistPanel implements Disposable {
    private static final Logger LOG = Logger.getInstance(AIAssistPanel.class);
    
    private final Project project;
    private final AutonomousCodeGenerationService codeGenerationService;
    
    private JPanel mainPanel;
    private JPanel chatContainer;
    private EditorTextField inputField;
    private JButton sendButton;
    
    private final List<Editor> editors = new java.util.ArrayList<>();
    
    /**
     * Creates a new AIAssistPanel.
     * @param project The project
     */
    public AIAssistPanel(@NotNull Project project) {
        this.project = project;
        this.codeGenerationService = AutonomousCodeGenerationService.getInstance(project);
        
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
        // Create main panel
        mainPanel = new JPanel(new BorderLayout());
        
        // Create chat container
        chatContainer = new JPanel(new VerticalLayout(10));
        chatContainer.setBorder(JBUI.Borders.empty(10));
        
        // Create scroll pane for chat
        JBScrollPane scrollPane = new JBScrollPane(chatContainer);
        scrollPane.setBorder(JBUI.Borders.empty());
        
        // Create input panel
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBorder(JBUI.Borders.empty(0, 10, 10, 10));
        
        // Create input field
        inputField = new EditorTextField("", project, FileTypeManager.getInstance().getFileTypeByExtension("txt"));
        inputField.setOneLineMode(false);
        inputField.setPreferredSize(new Dimension(-1, 80));
        
        // Add key listener for Enter key
        inputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && e.isControlDown()) {
                    e.consume();
                    sendMessage();
                }
            }
        });
        
        // Create send button
        sendButton = new JButton("Send");
        sendButton.addActionListener(this::onSendButtonClick);
        
        // Add components to input panel
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        
        // Add components to main panel
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(inputPanel, BorderLayout.SOUTH);
        
        // Add welcome message
        addBotMessage("Hi! I'm your ModForge AI assistant. How can I help you with your Minecraft mod development?");
    }
    
    /**
     * Handles send button click.
     * @param e The action event
     */
    private void onSendButtonClick(ActionEvent e) {
        sendMessage();
    }
    
    /**
     * Sends a message from the input field.
     */
    private void sendMessage() {
        String message = inputField.getText();
        
        if (message.isEmpty()) {
            return;
        }
        
        // Add user message to chat
        addUserMessage(message);
        
        // Clear input field
        inputField.setText("");
        
        // Generate response
        sendButton.setEnabled(false);
        inputField.setEnabled(false);
        
        CompletableFuture<String> future = codeGenerationService.generateChatResponse(message, new HashMap<>());
        
        future.thenAccept(response -> {
            SwingUtilities.invokeLater(() -> {
                // Add bot message to chat
                addBotMessage(response);
                
                // Re-enable input
                sendButton.setEnabled(true);
                inputField.setEnabled(true);
                inputField.requestFocus();
            });
        }).exceptionally(ex -> {
            LOG.error("Error generating chat response", ex);
            
            SwingUtilities.invokeLater(() -> {
                // Add error message to chat
                addBotMessage("Sorry, I encountered an error. Please try again.");
                
                // Re-enable input
                sendButton.setEnabled(true);
                inputField.setEnabled(true);
                inputField.requestFocus();
            });
            
            return null;
        });
    }
    
    /**
     * Adds a user message to the chat.
     * @param message The message to add
     */
    private void addUserMessage(@NotNull String message) {
        // Create user message panel
        JPanel messagePanel = new JPanel(new BorderLayout());
        messagePanel.setBorder(JBUI.Borders.empty(5));
        messagePanel.setBackground(JBUI.CurrentTheme.List.Focused.BACKGROUND);
        
        // Create avatar label
        JBLabel avatarLabel = new JBLabel("You");
        avatarLabel.setBorder(JBUI.Borders.empty(0, 0, 0, 10));
        avatarLabel.setForeground(JBUI.CurrentTheme.Label.FOREGROUND);
        avatarLabel.setFont(avatarLabel.getFont().deriveFont(Font.BOLD));
        
        // Create message editor
        Editor editor = createReadOnlyEditor(message);
        ((EditorEx) editor).setBackgroundColor(JBUI.CurrentTheme.List.Focused.BACKGROUND);
        
        // Add components to message panel
        messagePanel.add(avatarLabel, BorderLayout.WEST);
        messagePanel.add(editor.getComponent(), BorderLayout.CENTER);
        
        // Add message panel to chat container
        chatContainer.add(messagePanel);
        chatContainer.revalidate();
        chatContainer.repaint();
        
        // Scroll to bottom
        SwingUtilities.invokeLater(() -> {
            JScrollPane scrollPane = (JScrollPane) chatContainer.getParent().getParent();
            JScrollBar vertical = scrollPane.getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        });
    }
    
    /**
     * Adds a bot message to the chat.
     * @param message The message to add
     */
    private void addBotMessage(@NotNull String message) {
        // Create bot message panel
        JPanel messagePanel = new JPanel(new BorderLayout());
        messagePanel.setBorder(JBUI.Borders.empty(5));
        
        // Create avatar label
        JBLabel avatarLabel = new JBLabel("ModForge");
        avatarLabel.setBorder(JBUI.Borders.empty(0, 0, 0, 10));
        avatarLabel.setForeground(JBUI.CurrentTheme.Label.FOREGROUND);
        avatarLabel.setFont(avatarLabel.getFont().deriveFont(Font.BOLD));
        
        // Create message editor
        Editor editor = createReadOnlyEditor(message);
        
        // Add components to message panel
        messagePanel.add(avatarLabel, BorderLayout.WEST);
        messagePanel.add(editor.getComponent(), BorderLayout.CENTER);
        
        // Add message panel to chat container
        chatContainer.add(messagePanel);
        chatContainer.revalidate();
        chatContainer.repaint();
        
        // Scroll to bottom
        SwingUtilities.invokeLater(() -> {
            JScrollPane scrollPane = (JScrollPane) chatContainer.getParent().getParent();
            JScrollBar vertical = scrollPane.getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        });
    }
    
    /**
     * Creates a read-only editor for displaying messages.
     * @param text The text to display
     * @return The created editor
     */
    @NotNull
    private Editor createReadOnlyEditor(@NotNull String text) {
        EditorFactory editorFactory = EditorFactory.getInstance();
        Document document = editorFactory.createDocument(text);
        
        Editor editor = editorFactory.createEditor(document, project);
        editor.setBorder(null);
        
        EditorSettings settings = editor.getSettings();
        settings.setLineNumbersShown(false);
        settings.setFoldingOutlineShown(false);
        settings.setRightMarginShown(false);
        settings.setLineMarkerAreaShown(false);
        settings.setIndentGuidesShown(false);
        settings.setVirtualSpace(false);
        settings.setWheelFontChangeEnabled(false);
        settings.setAdditionalColumnsCount(0);
        settings.setAdditionalLinesCount(0);
        settings.setCaretRowShown(false);
        
        if (editor instanceof EditorEx) {
            EditorEx editorEx = (EditorEx) editor;
            editorEx.setHorizontalScrollbarVisible(false);
            editorEx.setVerticalScrollbarVisible(false);
            editorEx.setContextMenuGroupId(null);
            
            EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
            editorEx.setColorsScheme(scheme);
        }
        
        editors.add(editor);
        
        return editor;
    }
    
    @Override
    public void dispose() {
        // Dispose editors
        for (Editor editor : editors) {
            EditorFactory.getInstance().releaseEditor(editor);
        }
        
        editors.clear();
    }
}