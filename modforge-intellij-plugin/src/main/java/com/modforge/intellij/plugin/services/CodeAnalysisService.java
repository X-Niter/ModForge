package com.modforge.intellij.plugin.services;

import com.intellij.analysis.AnalysisScope;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.daemon.impl.HighlightInfoType;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInspection.*;
import com.intellij.codeInspection.ex.GlobalInspectionContextImpl;
import com.intellij.codeInspection.ex.InspectionManagerEx;
import com.intellij.codeInspection.ex.InspectionToolWrapper;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * Service for automated code analysis and issue resolution.
 * This service allows ModForge to autonomously:
 * - Detect code issues
 * - Apply fixes for common problems
 * - Optimize code
 * - Ensure code quality standards
 */
@Service(Service.Level.PROJECT)
public final class CodeAnalysisService {
    private static final Logger LOG = Logger.getInstance(CodeAnalysisService.class);
    
    private final Project project;
    private final ExecutorService executor = AppExecutorUtil.createBoundedApplicationPoolExecutor(
            "ModForge.CodeAnalysis", 2);
    
    /**
     * Creates a new CodeAnalysisService.
     * @param project The project
     */
    public CodeAnalysisService(@NotNull Project project) {
        this.project = project;
    }
    
    /**
     * Gets the CodeAnalysisService instance.
     * @param project The project
     * @return The CodeAnalysisService instance
     */
    public static CodeAnalysisService getInstance(@NotNull Project project) {
        return project.getService(CodeAnalysisService.class);
    }
    
    /**
     * Analyzes a file for issues.
     * @param filePath The file path
     * @return The list of issues found
     */
    public CompletableFuture<List<CodeIssue>> analyzeFile(@NotNull String filePath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                LOG.info("Analyzing file: " + filePath);
                
                // Get the file
                PsiFile psiFile = getPsiFile(filePath);
                if (psiFile == null) {
                    LOG.warn("File not found: " + filePath);
                    return Collections.emptyList();
                }
                
                List<CodeIssue> issues = new ArrayList<>();
                
                // Run code inspections
                ApplicationManager.getApplication().runReadAction(() -> {
                    try {
                        // Get document
                        Document document = PsiDocumentManager.getInstance(project).getDocument(psiFile);
                        if (document == null) {
                            LOG.warn("Document not found for file: " + filePath);
                            return;
                        }
                        
                        // Create editor
                        Editor editor = EditorFactory.getInstance().createEditor(document, project);
                        
                        try {
                            // Get inspection tools
                            List<LocalInspectionTool> inspectionTools = getInspectionTools();
                            
                            // Run inspections
                            for (LocalInspectionTool tool : inspectionTools) {
                                ProblemDescriptor[] problems = InspectionManager.getInstance(project)
                                        .createInspectionContext()
                                        .getManager()
                                        .createProblemDescriptors(psiFile, tool);
                                
                                for (ProblemDescriptor problem : problems) {
                                    PsiElement element = problem.getPsiElement();
                                    if (element != null) {
                                        TextRange range = element.getTextRange();
                                        int line = document.getLineNumber(range.getStartOffset());
                                        
                                        issues.add(new CodeIssue(
                                                filePath,
                                                line + 1,
                                                range.getStartOffset(),
                                                range.getEndOffset(),
                                                problem.getDescriptionTemplate(),
                                                getToolName(tool),
                                                hasQuickFix(problem)
                                        ));
                                    }
                                }
                            }
                        } finally {
                            // Release editor
                            EditorFactory.getInstance().releaseEditor(editor);
                        }
                    } catch (Exception e) {
                        LOG.error("Error analyzing file", e);
                    }
                });
                
                LOG.info("Found " + issues.size() + " issues in file: " + filePath);
                return issues;
            } catch (Exception e) {
                LOG.error("Error analyzing file", e);
                return Collections.emptyList();
            }
        }, executor);
    }
    
    /**
     * Applies quick fixes to resolve issues in a file.
     * @param filePath The file path
     * @param autoFix Whether to automatically apply fixes without confirmation
     * @return The list of applied fixes
     */
    public CompletableFuture<List<AppliedFix>> applyQuickFixes(@NotNull String filePath, boolean autoFix) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                LOG.info("Applying quick fixes to file: " + filePath);
                
                // Get the file
                PsiFile psiFile = getPsiFile(filePath);
                if (psiFile == null) {
                    LOG.warn("File not found: " + filePath);
                    return Collections.emptyList();
                }
                
                // First analyze the file
                List<CodeIssue> issues = analyzeFile(filePath).get();
                
                // Filter issues with quick fixes
                List<CodeIssue> fixableIssues = issues.stream()
                        .filter(CodeIssue::hasQuickFix)
                        .collect(Collectors.toList());
                
                LOG.info("Found " + fixableIssues.size() + " fixable issues in file: " + filePath);
                
                if (fixableIssues.isEmpty()) {
                    return Collections.emptyList();
                }
                
                List<AppliedFix> appliedFixes = new ArrayList<>();
                
                // Run code inspections
                ApplicationManager.getApplication().runReadAction(() -> {
                    try {
                        // Get document
                        Document document = PsiDocumentManager.getInstance(project).getDocument(psiFile);
                        if (document == null) {
                            LOG.warn("Document not found for file: " + filePath);
                            return;
                        }
                        
                        // Create editor
                        Editor editor = EditorFactory.getInstance().createEditor(document, project);
                        
                        try {
                            // Get inspection tools
                            List<LocalInspectionTool> inspectionTools = getInspectionTools();
                            
                            // Run inspections and apply fixes
                            for (LocalInspectionTool tool : inspectionTools) {
                                ProblemDescriptor[] problems = InspectionManager.getInstance(project)
                                        .createInspectionContext()
                                        .getManager()
                                        .createProblemDescriptors(psiFile, tool);
                                
                                for (ProblemDescriptor problem : problems) {
                                    if (!hasQuickFix(problem)) {
                                        continue;
                                    }
                                    
                                    PsiElement element = problem.getPsiElement();
                                    if (element != null) {
                                        TextRange range = element.getTextRange();
                                        int line = document.getLineNumber(range.getStartOffset());
                                        
                                        // Apply the fix
                                        QuickFix[] fixes = problem.getFixes();
                                        if (fixes != null && fixes.length > 0) {
                                            QuickFix<?> fix = fixes[0]; // Take the first fix
                                            
                                            if (fix instanceof LocalQuickFix) {
                                                LocalQuickFix localFix = (LocalQuickFix) fix;
                                                
                                                // Apply fix in write action
                                                WriteCommandAction.runWriteCommandAction(project, () -> {
                                                    try {
                                                        // Apply the fix
                                                        localFix.applyFix(project, problem);
                                                        
                                                        // Record applied fix
                                                        appliedFixes.add(new AppliedFix(
                                                                filePath,
                                                                line + 1,
                                                                range.getStartOffset(),
                                                                range.getEndOffset(),
                                                                problem.getDescriptionTemplate(),
                                                                getToolName(tool),
                                                                localFix.getName()
                                                        ));
                                                        
                                                        LOG.info("Applied fix: " + localFix.getName() + " to line " + (line + 1));
                                                    } catch (Exception e) {
                                                        LOG.error("Error applying fix", e);
                                                    }
                                                });
                                            }
                                        }
                                    }
                                }
                            }
                        } finally {
                            // Release editor
                            EditorFactory.getInstance().releaseEditor(editor);
                        }
                    } catch (Exception e) {
                        LOG.error("Error applying quick fixes", e);
                    }
                });
                
                LOG.info("Applied " + appliedFixes.size() + " fixes to file: " + filePath);
                return appliedFixes;
            } catch (Exception e) {
                LOG.error("Error applying quick fixes", e);
                return Collections.emptyList();
            }
        }, executor);
    }
    
    /**
     * Analyzes patterns of code issues across the project.
     * @return Statistics on the most common code issues
     */
    public CompletableFuture<CodeIssueStatistics> analyzeProjectCodeIssuePatterns() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                LOG.info("Analyzing project code issue patterns");
                
                // Get all Java files in the project
                // This is a simplified approach - in a real implementation we'd use AnalysisScope
                PsiDirectory baseDir = PsiManager.getInstance(project).findDirectory(project.getBaseDir());
                if (baseDir == null) {
                    LOG.warn("Project base directory not found");
                    return new CodeIssueStatistics();
                }
                
                List<PsiFile> javaFiles = new ArrayList<>();
                collectJavaFiles(baseDir, javaFiles);
                
                LOG.info("Analyzing " + javaFiles.size() + " Java files for issue patterns");
                
                Map<String, Integer> issueTypeCounts = new HashMap<>();
                Map<String, Integer> issueByFile = new HashMap<>();
                Map<String, List<Integer>> issueLinesByType = new HashMap<>();
                
                // Analyze each file
                for (PsiFile psiFile : javaFiles) {
                    String filePath = psiFile.getVirtualFile().getPath();
                    
                    // Analyze file
                    List<CodeIssue> issues = analyzeFile(filePath).get();
                    
                    // Record file count
                    issueByFile.put(filePath, issues.size());
                    
                    // Record issue types
                    for (CodeIssue issue : issues) {
                        // Update issue type count
                        issueTypeCounts.merge(issue.getIssueType(), 1, Integer::sum);
                        
                        // Update line numbers
                        issueLinesByType.computeIfAbsent(issue.getIssueType(), k -> new ArrayList<>())
                                .add(issue.getLine());
                    }
                }
                
                // Create statistics
                CodeIssueStatistics statistics = new CodeIssueStatistics();
                statistics.setTotalIssueCount(issueTypeCounts.values().stream().mapToInt(Integer::intValue).sum());
                statistics.setFileCount(javaFiles.size());
                statistics.setFilesWithIssues((int) issueByFile.values().stream().filter(count -> count > 0).count());
                
                // Sort issue types by count
                List<Map.Entry<String, Integer>> sortedIssueTypes = issueTypeCounts.entrySet().stream()
                        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                        .collect(Collectors.toList());
                
                // Add top issue types
                for (Map.Entry<String, Integer> entry : sortedIssueTypes) {
                    statistics.addIssueTypeCount(entry.getKey(), entry.getValue());
                }
                
                // Compute hot spots
                Map<String, Double> hotspots = new HashMap<>();
                for (Map.Entry<String, List<Integer>> entry : issueLinesByType.entrySet()) {
                    // Calculate density of issues
                    List<Integer> lines = entry.getValue();
                    if (lines.size() >= 3) { // Only consider types with at least 3 occurrences
                        // Group by file
                        Map<String, List<Integer>> linesByFile = new HashMap<>();
                        for (int i = 0; i < lines.size(); i++) {
                            // Get file for this issue
                            String file = getFileForIssueIndex(issues, i, entry.getKey());
                            if (file != null) {
                                linesByFile.computeIfAbsent(file, k -> new ArrayList<>()).add(lines.get(i));
                            }
                        }
                        
                        // Identify files with high density
                        for (Map.Entry<String, List<Integer>> fileEntry : linesByFile.entrySet()) {
                            if (fileEntry.getValue().size() >= 3) { // At least 3 issues of same type in file
                                double density = (double) fileEntry.getValue().size() / 
                                        (issueByFile.getOrDefault(fileEntry.getKey(), 1));
                                hotspots.put(fileEntry.getKey() + " (" + entry.getKey() + ")", density);
                            }
                        }
                    }
                }
                
                // Add hotspots to statistics
                hotspots.entrySet().stream()
                        .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                        .limit(5)
                        .forEach(entry -> statistics.addHotspot(entry.getKey(), entry.getValue()));
                
                LOG.info("Completed project code issue pattern analysis");
                return statistics;
            } catch (Exception e) {
                LOG.error("Error analyzing project code issue patterns", e);
                return new CodeIssueStatistics();
            }
        }, executor);
    }
    
    /**
     * Gets the file for an issue with the given index and type.
     * This is a helper method for the pattern analysis.
     */
    @Nullable
    private String getFileForIssueIndex(List<CodeIssue> issues, int index, String issueType) {
        // This is a simplified approach - in a real implementation we'd maintain better indexing
        if (index < issues.size()) {
            CodeIssue issue = issues.get(index);
            if (issueType.equals(issue.getIssueType())) {
                return issue.getFilePath();
            }
        }
        return null;
    }
    
    /**
     * Recursively collects Java files from a directory.
     * @param directory The directory to scan
     * @param result The list to add Java files to
     */
    private void collectJavaFiles(@NotNull PsiDirectory directory, @NotNull List<PsiFile> result) {
        // Add Java files in this directory
        PsiFile[] files = directory.getFiles();
        for (PsiFile file : files) {
            if (file instanceof PsiJavaFile) {
                result.add(file);
            }
        }
        
        // Recursively process subdirectories
        PsiDirectory[] subdirectories = directory.getSubdirectories();
        for (PsiDirectory subdirectory : subdirectories) {
            collectJavaFiles(subdirectory, result);
        }
    }
    
    /**
     * Gets the name of an inspection tool.
     * @param tool The inspection tool
     * @return The tool name
     */
    @NotNull
    private String getToolName(@NotNull LocalInspectionTool tool) {
        return tool.getShortName();
    }
    
    /**
     * Checks if a problem has a quick fix.
     * @param problem The problem descriptor
     * @return Whether the problem has a quick fix
     */
    private boolean hasQuickFix(@NotNull ProblemDescriptor problem) {
        QuickFix<?>[] fixes = problem.getFixes();
        return fixes != null && fixes.length > 0;
    }
    
    /**
     * Gets a PsiFile for a file path.
     * @param filePath The file path
     * @return The PsiFile, or null if not found
     */
    @Nullable
    private PsiFile getPsiFile(@NotNull String filePath) {
        return ApplicationManager.getApplication().runReadAction((Computable<PsiFile>) () -> {
            try {
                VirtualFile virtualFile = project.getBaseDir().findFileByRelativePath(filePath);
                if (virtualFile == null) {
                    LOG.warn("Virtual file not found: " + filePath);
                    return null;
                }
                
                return PsiManager.getInstance(project).findFile(virtualFile);
            } catch (Exception e) {
                LOG.error("Error getting PSI file", e);
                return null;
            }
        });
    }
    
    /**
     * Gets the list of inspection tools to use for analysis.
     * @return The list of inspection tools
     */
    @NotNull
    private List<LocalInspectionTool> getInspectionTools() {
        // In a real implementation, we would get this from InspectionProfileManager
        return Arrays.asList(
                new UnusedDeclarationInspection(),
                new UnusedImportInspection(),
                new RedundantImportInspection()
                // Add more inspections as needed
        );
    }
    
    /**
     * Represents a code issue found in analysis.
     */
    public static class CodeIssue {
        private final String filePath;
        private final int line;
        private final int startOffset;
        private final int endOffset;
        private final String description;
        private final String issueType;
        private final boolean hasQuickFix;
        
        /**
         * Creates a new CodeIssue.
         * @param filePath The file path
         * @param line The line number
         * @param startOffset The start offset
         * @param endOffset The end offset
         * @param description The issue description
         * @param issueType The issue type
         * @param hasQuickFix Whether the issue has a quick fix
         */
        public CodeIssue(@NotNull String filePath, int line, int startOffset, int endOffset,
                          @NotNull String description, @NotNull String issueType, boolean hasQuickFix) {
            this.filePath = filePath;
            this.line = line;
            this.startOffset = startOffset;
            this.endOffset = endOffset;
            this.description = description;
            this.issueType = issueType;
            this.hasQuickFix = hasQuickFix;
        }
        
        /**
         * Gets the file path.
         * @return The file path
         */
        @NotNull
        public String getFilePath() {
            return filePath;
        }
        
        /**
         * Gets the line number.
         * @return The line number
         */
        public int getLine() {
            return line;
        }
        
        /**
         * Gets the start offset.
         * @return The start offset
         */
        public int getStartOffset() {
            return startOffset;
        }
        
        /**
         * Gets the end offset.
         * @return The end offset
         */
        public int getEndOffset() {
            return endOffset;
        }
        
        /**
         * Gets the issue description.
         * @return The issue description
         */
        @NotNull
        public String getDescription() {
            return description;
        }
        
        /**
         * Gets the issue type.
         * @return The issue type
         */
        @NotNull
        public String getIssueType() {
            return issueType;
        }
        
        /**
         * Checks if the issue has a quick fix.
         * @return Whether the issue has a quick fix
         */
        public boolean hasQuickFix() {
            return hasQuickFix;
        }
        
        @Override
        public String toString() {
            return "CodeIssue{" +
                    "file='" + filePath + '\'' +
                    ", line=" + line +
                    ", description='" + description + '\'' +
                    ", type='" + issueType + '\'' +
                    ", hasQuickFix=" + hasQuickFix +
                    '}';
        }
    }
    
    /**
     * Represents a fix applied to a code issue.
     */
    public static class AppliedFix {
        private final String filePath;
        private final int line;
        private final int startOffset;
        private final int endOffset;
        private final String issueDescription;
        private final String issueType;
        private final String fixName;
        
        /**
         * Creates a new AppliedFix.
         * @param filePath The file path
         * @param line The line number
         * @param startOffset The start offset
         * @param endOffset The end offset
         * @param issueDescription The issue description
         * @param issueType The issue type
         * @param fixName The name of the fix applied
         */
        public AppliedFix(@NotNull String filePath, int line, int startOffset, int endOffset,
                           @NotNull String issueDescription, @NotNull String issueType, @NotNull String fixName) {
            this.filePath = filePath;
            this.line = line;
            this.startOffset = startOffset;
            this.endOffset = endOffset;
            this.issueDescription = issueDescription;
            this.issueType = issueType;
            this.fixName = fixName;
        }
        
        /**
         * Gets the file path.
         * @return The file path
         */
        @NotNull
        public String getFilePath() {
            return filePath;
        }
        
        /**
         * Gets the line number.
         * @return The line number
         */
        public int getLine() {
            return line;
        }
        
        /**
         * Gets the start offset.
         * @return The start offset
         */
        public int getStartOffset() {
            return startOffset;
        }
        
        /**
         * Gets the end offset.
         * @return The end offset
         */
        public int getEndOffset() {
            return endOffset;
        }
        
        /**
         * Gets the issue description.
         * @return The issue description
         */
        @NotNull
        public String getIssueDescription() {
            return issueDescription;
        }
        
        /**
         * Gets the issue type.
         * @return The issue type
         */
        @NotNull
        public String getIssueType() {
            return issueType;
        }
        
        /**
         * Gets the name of the fix applied.
         * @return The fix name
         */
        @NotNull
        public String getFixName() {
            return fixName;
        }
        
        @Override
        public String toString() {
            return "AppliedFix{" +
                    "file='" + filePath + '\'' +
                    ", line=" + line +
                    ", issueType='" + issueType + '\'' +
                    ", fixName='" + fixName + '\'' +
                    '}';
        }
    }
    
    /**
     * Statistics on code issues in a project.
     */
    public static class CodeIssueStatistics {
        private int totalIssueCount;
        private int fileCount;
        private int filesWithIssues;
        private final Map<String, Integer> issueTypeCounts = new LinkedHashMap<>();
        private final Map<String, Double> hotspots = new LinkedHashMap<>();
        
        /**
         * Gets the total issue count.
         * @return The total issue count
         */
        public int getTotalIssueCount() {
            return totalIssueCount;
        }
        
        /**
         * Sets the total issue count.
         * @param totalIssueCount The total issue count
         */
        public void setTotalIssueCount(int totalIssueCount) {
            this.totalIssueCount = totalIssueCount;
        }
        
        /**
         * Gets the file count.
         * @return The file count
         */
        public int getFileCount() {
            return fileCount;
        }
        
        /**
         * Sets the file count.
         * @param fileCount The file count
         */
        public void setFileCount(int fileCount) {
            this.fileCount = fileCount;
        }
        
        /**
         * Gets the number of files with issues.
         * @return The number of files with issues
         */
        public int getFilesWithIssues() {
            return filesWithIssues;
        }
        
        /**
         * Sets the number of files with issues.
         * @param filesWithIssues The number of files with issues
         */
        public void setFilesWithIssues(int filesWithIssues) {
            this.filesWithIssues = filesWithIssues;
        }
        
        /**
         * Gets the issue type counts.
         * @return The issue type counts
         */
        @NotNull
        public Map<String, Integer> getIssueTypeCounts() {
            return issueTypeCounts;
        }
        
        /**
         * Adds an issue type count.
         * @param issueType The issue type
         * @param count The count
         */
        public void addIssueTypeCount(@NotNull String issueType, int count) {
            issueTypeCounts.put(issueType, count);
        }
        
        /**
         * Gets the hotspots.
         * @return The hotspots
         */
        @NotNull
        public Map<String, Double> getHotspots() {
            return hotspots;
        }
        
        /**
         * Adds a hotspot.
         * @param hotspot The hotspot description
         * @param density The issue density
         */
        public void addHotspot(@NotNull String hotspot, double density) {
            hotspots.put(hotspot, density);
        }
        
        @Override
        public String toString() {
            return "CodeIssueStatistics{" +
                    "totalIssueCount=" + totalIssueCount +
                    ", fileCount=" + fileCount +
                    ", filesWithIssues=" + filesWithIssues +
                    ", issueTypeCounts=" + issueTypeCounts +
                    ", hotspots=" + hotspots +
                    '}';
        }
    }
}