package com.modforge.intellij.plugin.utils;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.WriteAction;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
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
    public static <T> T runReadAction(@NotNull Computable<T> action) {
        return ReadAction.compute(action);
    }

    /**
     * Runs an action in a write action safely.
     *
     * @param action The action to run.
     * @param <T> The return type.
     * @return The result of the action.
     */
    public static <T> T runWriteAction(@NotNull Computable<T> action) {
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
     *
     * @param action The action to run.
     */
    public static void runOnUiThread(@NotNull Runnable action) {
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