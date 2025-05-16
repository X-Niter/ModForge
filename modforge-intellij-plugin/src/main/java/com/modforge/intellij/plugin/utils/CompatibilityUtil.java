package com.modforge.intellij.plugin.utils;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Utility for compatibility operations.
 */
public final class CompatibilityUtil {
    // Dialog result constants
    public static final int DIALOG_YES = Messages.YES;
    public static final int DIALOG_NO = Messages.NO;
    public static final int DIALOG_CANCEL = Messages.CANCEL;
    public static final int DIALOG_OK = Messages.OK;

    /**
     * Get problems from a file.
     *
     * @param project The project
     * @param file    The file to check
     * @return Collection of problems
     */
    @NotNull
    public static Collection<Problem> getProblemsForFile(@NotNull Project project, @NotNull VirtualFile file) {
        Collection<Problem> problems = new ArrayList<>();

        // Get the PsiFile for analysis
        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        if (psiFile == null) {
            return problems;
        }

        Document document = FileDocumentManager.getInstance().getDocument(file);
        if (document == null) {
            return problems;
        }

        // TODO: Add actual problem detection logic here

        return problems;
    }

    /**
     * Finds all files with problems in the project.
     *
     * @param project The project to scan
     * @return Collection of files that have problems
     */
    @NotNull
    public static Collection<VirtualFile> findFilesWithProblems(@NotNull Project project) {
        Collection<VirtualFile> problemFiles = new ArrayList<>();
        VirtualFile baseDir = getProjectBaseDir(project);

        // Recursively scan project files
        VfsUtilCore.iterateChildrenRecursively(baseDir, null, fileOrDir -> {
            if (!fileOrDir.isDirectory() && hasProblemsIn(project, fileOrDir)) {
                problemFiles.add(fileOrDir);
            }
            return true;
        });

        return problemFiles;
    }

    /**
     * Gets all problems in a specific file.
     *
     * @param project The project
     * @param file    The file to check
     * @return Collection of problems
     */
    @NotNull
    public static Collection<Problem> getProblems(@NotNull Project project, @NotNull VirtualFile file) {
        return getProblemsForFile(project, file);
    }

    /**
     * Shows an input dialog with project context.
     */
    @Nullable
    public static String showInputDialogWithProject(
            @NotNull Project project,
            @NotNull String message,
            @NotNull String title,
            @Nullable String initialValue) {
        return Messages.showInputDialog(project, message, title,
                Messages.getQuestionIcon(), initialValue, null);
    }

    /**
     * Shows a yes/no dialog with custom button text.
     */
    public static int showYesNoDialog(
            @NotNull Project project,
            @NotNull String message,
            @NotNull String title,
            @NotNull String yesButtonText,
            @NotNull String noButtonText,
            @Nullable Icon icon) {
        return Messages.showYesNoDialog(project, message, title,
                yesButtonText, noButtonText, icon);
    }

    public static int showYesNoDialog(
            @NotNull Project project,
            @NotNull String message,
            @NotNull String title) {
        return Messages.showYesNoDialog(project, message, title,
                Messages.getQuestionIcon());
    }

    /**
     * Checks if a file has problems.
     */
    public static boolean hasProblemsIn(@NotNull Project project, @NotNull VirtualFile file) {
        Collection<Problem> problems = getProblemsForFile(project, file);
        return !problems.isEmpty();
    }

    /**
     * Gets a description of a problem.
     */
    @NotNull
    public static String getProblemDescription(@NotNull Object problem) {
        if (problem instanceof Problem) {
            return ((Problem) problem).getDescription();
        }
        return problem.toString();
    }

    /**
     * Runs a compute operation in a write action.
     */
    public static <T> T computeInWriteAction(@NotNull Computable<T> computable) {
        return ApplicationManager.getApplication().runWriteAction(computable);
    }

    /**
     * Runs a runnable in a write action.
     */
    public static void runWriteAction(@NotNull Runnable runnable) {
        ApplicationManager.getApplication().runWriteAction(runnable);
    }

    /**
     * Gets a mod file by its relative path.
     */
    @Nullable
    public static VirtualFile getModFileByRelativePath(@NotNull Project project, String path) {
        VirtualFile baseDir = project.getProjectFile();
        if (baseDir == null) {
            return null;
        }

        Path resolvedPath = Paths.get(baseDir.getPath()).resolve(path);
        File file = resolvedPath.toFile();

        return LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);
    }

    /**
     * Opens a file in the editor.
     */
    public static void openFileInEditor(@NotNull Project project, @NotNull VirtualFile file, boolean requestFocus) {
        ApplicationManager.getApplication().invokeLater(() -> {
            FileEditorManager.getInstance(project).openFile(file, requestFocus);
        });
    }

    /**
     * Runs code on the UI thread.
     */
    public static void runOnUiThread(@NotNull Runnable runnable) {
        ApplicationManager.getApplication().invokeLater(runnable);
    }

    /**
     * Shows an information dialog.
     */
    public static void showInfoDialog(
            @NotNull Project project,
            @NotNull String message,
            @NotNull String title) {
        Messages.showInfoMessage(project, message, title);
    }

    /**
     * Shows an error dialog with project context.
     *
     * @param project The project
     * @param message The error message
     * @param title   The dialog title
     */
    public static void showErrorDialog(
            @NotNull Project project,
            @NotNull String message,
            @NotNull String title) {
        Messages.showErrorDialog(project, message, title);
    }

    /**
     * Shows a warning dialog with project context.
     *
     * @param project The project
     * @param message The warning message
     * @param title   The dialog title
     */
    public static void showWarningDialog(
            @NotNull Project project,
            @NotNull String message,
            @NotNull String title) {
        Messages.showWarningDialog(project, message, title);
    }

    /**
     * Shows a choose dialog with options.
     *
     * @param message      The dialog message
     * @param title        The dialog title
     * @param options      The available options
     * @param initialValue The initially selected option
     * @return The index of the selected option
     */
    public static int showChooseDialog(
            @NotNull String message,
            @NotNull String title,
            @NotNull String[] options,
            @Nullable String initialValue) {
        return Messages.showDialog(message, title,
                options, 0, Messages.getQuestionIcon());
    }

    /**
     * Gets the base directory of a project.
     *
     * @param project The project
     * @return The project base directory as a VirtualFile
     */
    @NotNull
    public static VirtualFile getProjectBaseDir(@NotNull Project project) {
        return project.getProjectFile().getParent();
    }

    /**
     * Finds a file by its path.
     *
     * @param path The file path
     * @return The VirtualFile, or null if not found
     */
    @Nullable
    public static VirtualFile findFileByPath(@NotNull String path) {
        return LocalFileSystem.getInstance().findFileByPath(path);
    }

    /**
     * Provides a ScheduledExecutorService from the IntelliJ platform.
     */
    public static ScheduledExecutorService getCompatibleAppScheduledExecutorService() {
        return AppExecutorUtil.getAppScheduledExecutorService();
    }

    /**
     * Provides an executor service compatible with the IntelliJ platform.
     */
    public static ExecutorService getCompatibleAppExecutorService() {
        return AppExecutorUtil.getAppExecutorService();
    }

    /**
     * Provides access to the NodeManager from DebugProcessImpl via reflection.
     * Returns null if the method is unavailable.
     */
    @Nullable
    public static Object getNodeManager(@NotNull com.intellij.debugger.engine.DebugProcessImpl process) {
        try {
            java.lang.reflect.Method m = process.getClass().getMethod("getNodeManager");
            return m.invoke(process);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Problem class for compatibility with different IntelliJ versions.
     */
    public static class Problem {
        private final String description;
        private final String fix;

        public Problem(String description, String fix) {
            this.description = description;
            this.fix = fix;
        }

        @NotNull
        public String getDescription() {
            return description;
        }

        @Nullable
        public String getFix() {
            return fix;
        }
    }

    /**
     * Runs a read action using the ApplicationManager.
     *
     * @param action The action to run
     * @param <T>    The return type of the action
     * @return The result of the action
     */
    public static <T> T runReadAction(Computable<T> action) {
        return ApplicationManager.getApplication().runReadAction(action);
    }

    /**
     * Runs a write action using the ApplicationManager.
     *
     * @param action The action to run
     * @param <T>    The return type of the action
     * @return The result of the action
     */
    public static <T> T runWriteAction(Computable<T> action) {
        return ApplicationManager.getApplication().runWriteAction(action);
    }

    /**
     * Stream compatibility for arrays
     */
    public static <T> java.util.stream.Stream<T> toStream(T[] array) {
        return java.util.Arrays.stream(array);
    }

    /**
     * Add breakpoint listener to XDebugSession for compatibility
     */
    public static <L extends com.intellij.xdebugger.breakpoints.XBreakpoint<?>> void addBreakpointListener(
            com.intellij.xdebugger.XDebugSession session,
            com.intellij.xdebugger.XBreakpointListener<L> listener) {
        try {
            session.addBreakpointListener(listener);
        } catch (NoSuchMethodError e) {
            // ignore on older/newer APIs
        }
    }

    private CompatibilityUtil() {
        // Utility class
    }
}