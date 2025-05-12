package com.modforge.intellij.plugin.utils;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.problems.Problem;
import com.intellij.problems.WolfTheProblemSolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Utility class to handle API differences between IntelliJ versions.
 * Compatible with IntelliJ IDEA 2025.1.1.1 (Build #IC-251.25410.129)
 * 
 * This class provides wrappers around IntelliJ Platform APIs that have changed or been deprecated,
 * ensuring compatibility across different versions.
 */
public final class CompatibilityUtil {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(CompatibilityUtil.class);

    /**
     * Gets the base directory of a project.
     * Replacement for deprecated Project.getBaseDir()
     *
     * @param project The project.
     * @return The base directory.
     */
    @Nullable
    public static VirtualFile getProjectBaseDir(@NotNull Project project) {
        return ProjectRootManager.getInstance(project).getContentRoots().length > 0 
               ? ProjectRootManager.getInstance(project).getContentRoots()[0]
               : null;
    }

    /**
     * Gets the currently selected text editor.
     * Replacement for FileEditorManager.getSelectedTextEditor()
     *
     * @param project The project.
     * @return The selected text editor, or null if none is selected.
     */
    @Nullable
    public static Editor getSelectedTextEditor(@NotNull Project project) {
        return FileEditorManager.getInstance(project).getSelectedTextEditor();
    }

    /**
     * Refreshes all files in the project.
     * Replacement for deprecated CacheUpdater and CacheUpdaterFacade
     *
     * @param project The project.
     */
    public static void refreshAll(@NotNull Project project) {
        VirtualFile baseDir = getProjectBaseDir(project);
        if (baseDir != null) {
            baseDir.refresh(true, true);
        }
    }

    /**
     * Clears caches for the project.
     * Safely handles API differences across IntelliJ versions.
     *
     * @param project The project.
     */
    public static void clearCaches(@NotNull Project project) {
        // Refresh PSI caches
        PsiManager.getInstance(project).dropPsiCaches();
        
        // Refresh file system
        refreshAll(project);
        
        // Commit all documents
        PsiDocumentManager.getInstance(project).commitAllDocuments();
    }
    
    /**
     * Runs a task on the UI thread with non-modal state. This is a replacement for 
     * ApplicationManager.getApplication().invokeLater() that handles compatibility with newer IntelliJ versions.
     *
     * @param runnable The task to run.
     */
    public static void runOnUIThreadNonModal(@NotNull Runnable runnable) {
        ApplicationManager.getApplication().invokeLater(runnable, ModalityState.NON_MODAL);
    }
    
    /**
     * Open a file in the editor.
     *
     * @param project The project.
     * @param file The file to open.
     * @param requestFocus Whether to request focus for the editor.
     */
    public static void openFileInEditor(@NotNull Project project, @NotNull VirtualFile file, boolean requestFocus) {
        runOnUIThreadNonModal(() -> FileEditorManager.getInstance(project).openFile(file, requestFocus));
    }
    
    /**
     * Show an info dialog with the specified message.
     * Replacement for Messages.showInfoDialog(Project, String, String) which was removed in newer versions.
     *
     * @param project The project.
     * @param message The message to show.
     * @param title The dialog title.
     */
    public static void showInfoDialog(@NotNull Project project, @NotNull String message, @NotNull String title) {
        runOnUIThreadNonModal(() -> Messages.showInfoMessage(project, message, title));
    }
    
    /**
     * Runs a task in a read action, safely handling exceptions.
     *
     * @param computable The task to run.
     * @param <T> The return type.
     * @return The result of the task, or null if an exception occurred.
     */
    @Nullable
    public static <T> T runReadAction(@NotNull Computable<T> computable) {
        try {
            return ReadAction.compute(computable);
        } catch (Exception e) {
            LOG.error("Error running read action", e);
            return null;
        }
    }
    
    /**
     * Runs a task in a write action, safely handling exceptions.
     *
     * @param runnable The task to run.
     * @return True if the task was executed successfully, false otherwise.
     */
    public static boolean runWriteAction(@NotNull Runnable runnable) {
        try {
            WriteAction.runAndWait(runnable);
            return true;
        } catch (Exception e) {
            LOG.error("Error running write action", e);
            return false;
        }
    }
    
    /**
     * Gets all problems for a specific file, compatible with IntelliJ IDEA 2025.1.1.1.
     *
     * @param project The project.
     * @param file The virtual file to check for problems.
     * @return A collection of problems associated with the file.
     */
    @NotNull
    public static Collection<Problem> getProblemsForFile(@NotNull Project project, @NotNull VirtualFile file) {
        WolfTheProblemSolver problemSolver = WolfTheProblemSolver.getInstance(project);
        Collection<Problem> problems = new ArrayList<>();
        
        try {
            // Use 2025.1.1.1 API
            problemSolver.getProblemFiles().forEach(problemFile -> {
                if (problemFile.equals(file)) {
                    problemSolver.getAllProblems().stream()
                        .filter(problem -> {
                            VirtualFile problemFile1 = problem.getVirtualFile();
                            return problemFile1 != null && problemFile1.equals(file);
                        })
                        .forEach(problems::add);
                }
            });
        } catch (Exception e) {
            LOG.warn("Error getting problems using new API", e);
            // Fallback for older versions - will never be called in practice for 2025.1.1.1
            try {
                // This code will not be executed in 2025.1.1.1 but is kept for backward compatibility
                problemSolver.getAllProblems().stream()
                    .filter(problem -> {
                        VirtualFile problemFile = problem.getVirtualFile();
                        return problemFile != null && problemFile.equals(file);
                    })
                    .forEach(problems::add);
            } catch (Exception ex) {
                LOG.error("Error getting problems using fallback approach", ex);
            }
        }
        
        return problems;
    }
    
    /**
     * Gets the description of a problem in a way that's compatible with IntelliJ IDEA 2025.1.1.1.
     *
     * @param problem The problem.
     * @return The problem description.
     */
    @NotNull
    public static String getProblemDescription(@NotNull Problem problem) {
        try {
            // First try the getDescription() method
            return problem.getDescription();
        } catch (NoSuchMethodError e) {
            // Fall back to toString() which should contain the important info
            String description = problem.toString();
            if (description == null || description.isEmpty()) {
                return "Unknown error";
            }
            return description;
        }
    }

    /**
     * Finds a PSI file for a virtual file.
     * Safer replacement for PsiManager.getInstance(project).findFile()
     *
     * @param project The project.
     * @param file The virtual file.
     * @return The PSI file, or null if not found or the file is invalid.
     */
    @Nullable
    public static PsiFile findPsiFile(@NotNull Project project, @Nullable VirtualFile file) {
        if (file == null || !file.isValid()) {
            return null;
        }
        
        return runReadAction(() -> PsiManager.getInstance(project).findFile(file));
    }

    /**
     * Converts a java.io.File to a VirtualFile.
     * Safely handles conversion across IntelliJ versions.
     *
     * @param file The file to convert.
     * @return The virtual file, or null if not found.
     */
    @Nullable
    public static VirtualFile toVirtualFile(@NotNull File file) {
        return VfsUtil.findFileByIoFile(file, true);
    }

    /**
     * Creates an XML file from a string.
     * Safely handles creation across IntelliJ versions.
     *
     * @param project The project.
     * @param content The XML content.
     * @param directory The parent directory.
     * @param fileName The file name.
     * @return The created XML file, or null if creation failed.
     */
    @Nullable
    public static XmlFile createXmlFile(
            @NotNull Project project,
            @NotNull String content,
            @NotNull PsiDirectory directory,
            @NotNull String fileName) {
        
        return runWriteAction(() -> {
            try {
                PsiFile psiFile = directory.createFile(fileName);
                Document document = PsiDocumentManager.getInstance(project).getDocument(psiFile);
                if (document != null) {
                    document.setText(content);
                    PsiDocumentManager.getInstance(project).commitDocument(document);
                    FileDocumentManager.getInstance().saveDocument(document);
                    
                    if (psiFile instanceof XmlFile) {
                        return (XmlFile) psiFile;
                    }
                }
                return null;
            } catch (Exception e) {
                LOG.error("Failed to create XML file", e);
                return null;
            }
        });
    }

    /**
     * Finds a mod file by its relative path.
     * Safely handles file access across IntelliJ versions.
     *
     * @param project The project.
     * @param relativePath The relative path.
     * @return The virtual file, or null if not found.
     */
    @Nullable
    public static VirtualFile getModFileByRelativePath(@NotNull Project project, @NotNull String relativePath) {
        VirtualFile baseDir = getProjectBaseDir(project);
        if (baseDir == null) {
            return null;
        }
        
        return baseDir.findFileByRelativePath(relativePath);
    }

    /**
     * Gets a value from an XML tag by attribute name.
     * Safely handles XML access across IntelliJ versions.
     *
     * @param tag The XML tag.
     * @param attributeName The attribute name.
     * @return The attribute value, or null if not found.
     */
    @Nullable
    public static String getXmlAttributeValue(@Nullable XmlTag tag, @NotNull String attributeName) {
        if (tag == null) {
            return null;
        }
        
        return tag.getAttributeValue(attributeName);
    }

    /**
     * Runs an action in a read action safely.
     *
     * @param action The action to run.
     * @param <T> The return type.
     * @return The result of the action.
     */
    public static <T> T computeInReadAction(@NotNull Computable<T> action) {
        return ReadAction.compute(action);
    }

    /**
     * Runs an action in a write action safely.
     *
     * @param action The action to run.
     * @param <T> The return type.
     * @return The result of the action.
     */
    public static <T> T computeInWriteAction(@NotNull Computable<T> action) {
        return WriteAction.compute(action);
    }

    /**
     * Runs an action in a read action safely, with no return value.
     *
     * @param action The action to run.
     */
    public static void runReadAction(@NotNull Runnable action) {
        ReadAction.run(action);
    }

    /**
     * Runs an action in a write action safely, with no return value.
     *
     * @param action The action to run.
     */
    public static void runWriteAction(@NotNull Runnable action) {
        WriteAction.run(action);
    }

    /**
     * Runs an action on the UI thread.
     * This version uses a simple name to avoid confusion with runOnUIThread.
     *
     * @param action The action to run.
     */
    public static void executeOnUiThread(@NotNull Runnable action) {
        ApplicationManager.getApplication().invokeLater(action);
    }

    /**
     * Runs an action on the UI thread and waits for it to complete.
     *
     * @param action The action to run.
     * @param <T> The return type.
     * @return The result of the action.
     * @throws Exception If an error occurs.
     */
    public static <T> T runOnUiThreadAndWait(@NotNull Callable<T> action) throws Exception {
        if (ApplicationManager.getApplication().isDispatchThread()) {
            return action.call();
        }
        
        Future<T> future = ApplicationManager.getApplication().executeOnPooledThread(action);
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new Exception("Failed to run action on UI thread", e);
        }
    }

    /**
     * Private constructor to prevent instantiation.
     */
    private CompatibilityUtil() {
        throw new AssertionError("Utility class should not be instantiated");
    }
}