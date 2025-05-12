package com.modforge.intellij.plugin.utils;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.impl.status.TogglePopupHintsPanel;
import com.intellij.problems.Problem;
import com.intellij.problems.WolfTheProblemSolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for working with problems (errors and warnings).
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
     * Gets all problem files in the project.
     *
     * @param project The project.
     * @return A collection of problem files.
     */
    @NotNull
    public static Collection<VirtualFile> getProblemFiles(@NotNull Project project) {
        return ReadAction.compute(() -> {
            try {
                WolfTheProblemSolver problemSolver = WolfTheProblemSolver.getInstance(project);
                
                // Use reflection to access the method
                try {
                    java.lang.reflect.Method method = WolfTheProblemSolver.class.getDeclaredMethod("getProblemFiles");
                    method.setAccessible(true);
                    Object result = method.invoke(problemSolver);
                    
                    if (result instanceof Collection) {
                        @SuppressWarnings("unchecked")
                        Collection<VirtualFile> files = (Collection<VirtualFile>) result;
                        return files;
                    }
                } catch (Exception e) {
                    LOG.warn("Failed to get problem files via reflection", e);
                }
                
                // Fallback: Get all open files and check if they have problems
                FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
                List<VirtualFile> problemFiles = new ArrayList<>();
                
                for (VirtualFile file : fileEditorManager.getOpenFiles()) {
                    if (problemSolver.hasProblemFilesBeneath(file::equals)) {
                        problemFiles.add(file);
                    }
                }
                
                return problemFiles;
            } catch (Exception e) {
                LOG.error("Failed to get problem files", e);
                return List.of();
            }
        });
    }

    /**
     * Gets all problems in the project.
     *
     * @param project The project.
     * @return A collection of problems.
     */
    @NotNull
    public static Collection<Problem> getAllProblems(@NotNull Project project) {
        return ReadAction.compute(() -> {
            try {
                WolfTheProblemSolver problemSolver = WolfTheProblemSolver.getInstance(project);
                
                // Use reflection to access the method
                try {
                    java.lang.reflect.Method method = WolfTheProblemSolver.class.getDeclaredMethod("getAllProblems");
                    method.setAccessible(true);
                    Object result = method.invoke(problemSolver);
                    
                    if (result instanceof Collection) {
                        @SuppressWarnings("unchecked")
                        Collection<Problem> problems = (Collection<Problem>) result;
                        return problems;
                    }
                } catch (Exception e) {
                    LOG.warn("Failed to get all problems via reflection", e);
                }
                
                // Fallback: Return empty list
                return List.of();
            } catch (Exception e) {
                LOG.error("Failed to get all problems", e);
                return List.of();
            }
        });
    }

    /**
     * Gets the description of a problem.
     *
     * @param problem The problem.
     * @return The problem description.
     */
    @NotNull
    public static String getDescription(@NotNull Problem problem) {
        try {
            // Use reflection to access the method
            try {
                java.lang.reflect.Method method = Problem.class.getDeclaredMethod("getDescription");
                method.setAccessible(true);
                Object result = method.invoke(problem);
                
                if (result instanceof String) {
                    return (String) result;
                }
            } catch (Exception e) {
                LOG.warn("Failed to get problem description via reflection", e);
            }
            
            // Fallback: Return a default description
            return problem.toString();
        } catch (Exception e) {
            LOG.error("Failed to get problem description", e);
            return "Unknown problem";
        }
    }

    /**
     * Gets the file of a problem.
     *
     * @param problem The problem.
     * @return The problem file.
     */
    @Nullable
    public static VirtualFile getFile(@NotNull Problem problem) {
        try {
            // Use reflection to access the method
            try {
                java.lang.reflect.Method method = Problem.class.getDeclaredMethod("getFile");
                method.setAccessible(true);
                Object result = method.invoke(problem);
                
                if (result instanceof VirtualFile) {
                    return (VirtualFile) result;
                }
            } catch (Exception e) {
                LOG.warn("Failed to get problem file via reflection", e);
            }
            
            // Fallback: Return null
            return null;
        } catch (Exception e) {
            LOG.error("Failed to get problem file", e);
            return null;
        }
    }

    /**
     * Gets problems for a specific file.
     *
     * @param project The project.
     * @param file    The file.
     * @return A list of problems for the file.
     */
    @NotNull
    public static List<Problem> getProblemsForFile(@NotNull Project project, @NotNull VirtualFile file) {
        return getAllProblems(project).stream()
                .filter(problem -> {
                    VirtualFile problemFile = getFile(problem);
                    return problemFile != null && problemFile.equals(file);
                })
                .collect(Collectors.toList());
    }

    /**
     * Gets current editor problems as highlight infos.
     *
     * @param project The project.
     * @param editor  The editor.
     * @return A list of highlight infos.
     */
    @NotNull
    public static List<HighlightInfo> getCurrentEditorProblems(@NotNull Project project, @NotNull Editor editor) {
        return ReadAction.compute(() -> {
            try {
                Document document = editor.getDocument();
                VirtualFile file = FileDocumentManager.getInstance().getFile(document);
                
                if (file == null) {
                    return List.of();
                }
                
                // Use reflection to get the highlights
                try {
                    Class<?> daemonCodeAnalyzerClass = Class.forName("com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl");
                    java.lang.reflect.Method getInstance = daemonCodeAnalyzerClass.getDeclaredMethod("getInstance", Project.class);
                    Object daemonCodeAnalyzer = getInstance.invoke(null, project);
                    
                    java.lang.reflect.Method getHighlights = daemonCodeAnalyzerClass.getDeclaredMethod("getHighlights", Document.class, HighlightInfo.class, Project.class);
                    Object result = getHighlights.invoke(daemonCodeAnalyzer, document, null, project);
                    
                    if (result instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<HighlightInfo> highlights = (List<HighlightInfo>) result;
                        return highlights.stream()
                                .filter(info -> info.getSeverity().myVal >= HighlightInfo.ERROR.getSeverity().myVal)
                                .collect(Collectors.toList());
                    }
                } catch (Exception e) {
                    LOG.warn("Failed to get highlights via reflection", e);
                }
                
                // Fallback: Return empty list
                return List.of();
            } catch (Exception e) {
                LOG.error("Failed to get current editor problems", e);
                return List.of();
            }
        });
    }

    /**
     * Gets error text at a specific offset.
     *
     * @param project The project.
     * @param editor  The editor.
     * @param offset  The offset.
     * @return The error text.
     */
    @Nullable
    public static String getErrorTextAtOffset(@NotNull Project project, @NotNull Editor editor, int offset) {
        return ReadAction.compute(() -> {
            try {
                List<HighlightInfo> highlights = getCurrentEditorProblems(project, editor);
                
                for (HighlightInfo info : highlights) {
                    TextRange range = new TextRange(info.getStartOffset(), info.getEndOffset());
                    if (range.contains(offset)) {
                        return info.getDescription();
                    }
                }
                
                return null;
            } catch (Exception e) {
                LOG.error("Failed to get error text at offset", e);
                return null;
            }
        });
    }
}