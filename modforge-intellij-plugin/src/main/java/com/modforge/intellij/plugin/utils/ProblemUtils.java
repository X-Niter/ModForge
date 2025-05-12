package com.modforge.intellij.plugin.utils;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Utility class for working with problems and errors in code.
 * Compatible with IntelliJ IDEA 2025.1.1.1
 */
public final class ProblemUtils {
    private static final Logger LOG = Logger.getInstance(ProblemUtils.class);
    
    /**
     * Private constructor to prevent instantiation.
     */
    private ProblemUtils() {
        // Utility class
    }

    /**
     * Gets problems from a file.
     *
     * @param project The project.
     * @param file    The file.
     * @return The list of problem descriptions.
     */
    @NotNull
    public static List<String> getProblemsFromFile(@NotNull Project project, @NotNull VirtualFile file) {
        List<String> problems = new ArrayList<>();
        
        ReadAction.run(() -> {
            try {
                Document document = FileDocumentManager.getInstance().getDocument(file);
                if (document == null) {
                    return;
                }
                
                PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document);
                if (psiFile == null) {
                    return;
                }
                
                // TODO: This is a mock implementation. In the real code, we would get actual problems.
                // For now, return an empty list.
            } catch (Exception e) {
                LOG.error("Failed to get problems from file: " + file.getPath(), e);
            }
        });
        
        return problems;
    }

    /**
     * Gets problems from a document.
     *
     * @param project  The project.
     * @param document The document.
     * @return The list of problem descriptions.
     */
    @NotNull
    public static List<String> getProblemsFromDocument(@NotNull Project project, @NotNull Document document) {
        List<String> problems = new ArrayList<>();
        
        ReadAction.run(() -> {
            try {
                PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document);
                if (psiFile == null) {
                    return;
                }
                
                VirtualFile file = psiFile.getVirtualFile();
                if (file == null) {
                    return;
                }
                
                problems.addAll(getProblemsFromFile(project, file));
            } catch (Exception e) {
                LOG.error("Failed to get problems from document", e);
            }
        });
        
        return problems;
    }

    /**
     * Gets compilation errors from a file.
     *
     * @param project The project.
     * @param file    The file.
     * @return The list of compilation error descriptions.
     */
    @NotNull
    public static List<String> getCompilationErrorsFromFile(@NotNull Project project, @NotNull VirtualFile file) {
        return getProblemsFromFile(project, file).stream()
                .filter(problem -> problem.toLowerCase().contains("error"))
                .toList();
    }

    /**
     * Navigates to a problem in the editor.
     *
     * @param project     The project.
     * @param file        The file.
     * @param lineNumber  The line number (1-based).
     * @param columnNumber The column number (1-based).
     * @return Whether navigation was successful.
     */
    public static boolean navigateToProblem(
            @NotNull Project project, 
            @NotNull VirtualFile file, 
            int lineNumber, 
            int columnNumber) {
        
        FileEditor[] editors = FileEditorManager.getInstance(project).openFile(file, true);
        
        for (FileEditor editor : editors) {
            if (editor instanceof TextEditor) {
                Editor textEditor = ((TextEditor) editor).getEditor();
                
                lineNumber = Math.max(0, lineNumber - 1); // Convert to 0-based
                columnNumber = Math.max(0, columnNumber - 1); // Convert to 0-based
                
                LogicalPosition position = new LogicalPosition(lineNumber, columnNumber);
                
                DialogUtils.executeOnUiThread(() -> {
                    textEditor.getCaretModel().moveToLogicalPosition(position);
                    textEditor.getScrollingModel().scrollToCaret(ScrollType.CENTER);
                });
                
                return true;
            }
        }
        
        return false;
    }

    /**
     * Gets problem description from line and message.
     *
     * @param lineNumber The line number (1-based).
     * @param message    The error message.
     * @return The problem description.
     */
    @NotNull
    public static String getDescriptionFromLineAndMessage(int lineNumber, @NotNull String message) {
        return "Line " + lineNumber + ": " + message;
    }

    /**
     * Asynchronously gets problems from all open files.
     *
     * @param project The project.
     * @return A future that completes with the list of problems.
     */
    @NotNull
    public static CompletableFuture<List<String>> getProblemsFromOpenFilesAsync(@NotNull Project project) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> problems = new ArrayList<>();
            
            ReadAction.run(() -> {
                try {
                    FileEditor[] editors = FileEditorManager.getInstance(project).getAllEditors();
                    
                    for (FileEditor editor : editors) {
                        if (editor instanceof TextEditor) {
                            Editor textEditor = ((TextEditor) editor).getEditor();
                            Document document = textEditor.getDocument();
                            
                            problems.addAll(getProblemsFromDocument(project, document));
                        }
                    }
                } catch (Exception e) {
                    LOG.error("Failed to get problems from open files", e);
                }
            });
            
            return problems;
        });
    }

    /**
     * Asynchronously gets problems from a file.
     *
     * @param project The project.
     * @param file    The file.
     * @return A future that completes with the list of problems.
     */
    @NotNull
    public static CompletableFuture<List<String>> getProblemsFromFileAsync(
            @NotNull Project project, 
            @NotNull VirtualFile file) {
        
        return CompletableFuture.supplyAsync(() -> getProblemsFromFile(project, file));
    }
}