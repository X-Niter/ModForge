package com.modforge.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.psi.PsiFile;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.JBUI;
import com.modforge.intellij.plugin.services.AutonomousCodeGenerationService;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

/**
 * Action to generate code using AI.
 * This action shows a dialog to input a prompt and generates code based on it.
 */
public final class GenerateCodeAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(GenerateCodeAction.class);
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        LOG.info("Generate code action performed");
        
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        
        if (project == null) {
            return;
        }
        
        // Create dialog components
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(10));
        
        JBTextArea promptArea = new JBTextArea(5, 40);
        promptArea.setLineWrap(true);
        promptArea.setWrapStyleWord(true);
        promptArea.setBorder(JBUI.Borders.empty(5));
        
        JBScrollPane scrollPane = new JBScrollPane(promptArea);
        scrollPane.setBorder(JBUI.Borders.empty());
        
        panel.add(new JLabel("Describe the code you want to generate:"), BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Show dialog
        int result = Messages.showDialog(
                project,
                panel,
                "Generate Code",
                new String[] { "Generate", "Cancel" },
                0,
                Messages.getQuestionIcon()
        );
        
        if (result != 0 || promptArea.getText().trim().isEmpty()) {
            return;
        }
        
        // Get prompt
        String prompt = promptArea.getText().trim();
        
        // Determine language from file type
        String language = "java";
        
        if (psiFile != null) {
            FileType fileType = psiFile.getFileType();
            language = fileType.getDefaultExtension();
        }
        
        // Show progress dialog and generate code
        String finalLanguage = language;
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Generating Code", false) {
            private String generatedCode;
            
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                
                try {
                    // Get code generation service
                    AutonomousCodeGenerationService service = AutonomousCodeGenerationService.getInstance(project);
                    
                    // Generate code
                    HashMap<String, Object> options = new HashMap<>();
                    options.put("language", finalLanguage);
                    
                    CompletableFuture<String> future = service.generateCode(prompt, finalLanguage, options);
                    
                    // Wait for result
                    generatedCode = future.get();
                } catch (Exception ex) {
                    LOG.error("Error generating code", ex);
                    
                    ApplicationManager.getApplication().invokeLater(() -> {
                        Messages.showErrorDialog(
                                project,
                                "An error occurred while generating code: " + ex.getMessage(),
                                "Code Generation Error"
                        );
                    });
                }
            }
            
            @Override
            public void onSuccess() {
                if (generatedCode == null || generatedCode.isEmpty()) {
                    return;
                }
                
                showGeneratedCodePopup(project, editor, generatedCode);
            }
        });
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        // Enable action if project is open
        Project project = e.getProject();
        e.getPresentation().setEnabledAndVisible(project != null);
    }
    
    /**
     * Shows a popup with the generated code.
     * @param project The project
     * @param editor The editor
     * @param code The generated code
     */
    private void showGeneratedCodePopup(@NotNull Project project, Editor editor, @NotNull String code) {
        // Create UI components
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(800, 600));
        
        JBTextArea codeArea = new JBTextArea(code);
        codeArea.setEditable(true);
        codeArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        
        JBScrollPane scrollPane = new JBScrollPane(codeArea);
        
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Create buttons panel
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        JButton insertButton = new JButton("Insert at Cursor");
        insertButton.addActionListener(e -> {
            if (editor != null) {
                insertCodeAtCursor(editor, codeArea.getText());
            }
            
            if (panel.getParent() instanceof JComponent) {
                ((JComponent) panel.getParent()).putClientProperty("JBPopup.lightweightWindow.closing", true);
            }
        });
        
        JButton copyButton = new JButton("Copy to Clipboard");
        copyButton.addActionListener(e -> {
            codeArea.selectAll();
            codeArea.copy();
            codeArea.select(0, 0);
        });
        
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> {
            if (panel.getParent() instanceof JComponent) {
                ((JComponent) panel.getParent()).putClientProperty("JBPopup.lightweightWindow.closing", true);
            }
        });
        
        buttonsPanel.add(insertButton);
        buttonsPanel.add(copyButton);
        buttonsPanel.add(closeButton);
        
        panel.add(buttonsPanel, BorderLayout.SOUTH);
        
        // Show popup
        JBPopupFactory.getInstance()
                .createComponentPopupBuilder(panel, codeArea)
                .setTitle("Generated Code")
                .setMovable(true)
                .setResizable(true)
                .setRequestFocus(true)
                .createPopup()
                .showInFocusCenter();
    }
    
    /**
     * Inserts code at the cursor position.
     * @param editor The editor
     * @param code The code to insert
     */
    private void insertCodeAtCursor(@NotNull Editor editor, @NotNull String code) {
        Document document = editor.getDocument();
        int offset = editor.getCaretModel().getOffset();
        
        ApplicationManager.getApplication().invokeLater(() -> {
            ApplicationManager.getApplication().runWriteAction(() -> {
                document.insertString(offset, code);
            });
        }, ModalityState.NON_MODAL);
    }
}