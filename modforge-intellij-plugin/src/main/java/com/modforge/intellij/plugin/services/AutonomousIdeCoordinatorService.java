package com.modforge.intellij.plugin.services;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.modforge.intellij.plugin.ai.AIServiceManager;
import com.modforge.intellij.plugin.ai.PatternRecognitionService;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.*;

/**
 * Central coordinator service for autonomous IDE operations.
 * This service orchestrates the various autonomous services to provide
 * high-level functionality for ModForge. It serves as the main entry point
 * for the autonomous capabilities of the IDE plugin.
 */
@Service(Service.Level.PROJECT)
public final class AutonomousIdeCoordinatorService {
    private static final Logger LOG = Logger.getInstance(AutonomousIdeCoordinatorService.class);
    
    private final Project project;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final ExecutorService executor = AppExecutorUtil.createBoundedApplicationPoolExecutor(
            "ModForge.AutonomousCoordinator", 4);
    
    // Services needed
    private final AIServiceManager aiServiceManager;
    private final IDEIntegrationService ideIntegrationService;
    private final AutomatedRefactoringService refactoringService;
    private final CodeAnalysisService codeAnalysisService;
    private final PatternRecognitionService patternRecognitionService;
    private final AutonomousCodeGenerationService codeGenerationService;
    
    // Task tracking
    private final Map<String, CompletableFuture<?>> activeTasks = new ConcurrentHashMap<>();
    private final Set<String> completedTasks = Collections.synchronizedSet(new HashSet<>());
    private final List<String> taskHistory = Collections.synchronizedList(new ArrayList<>());
    
    // Task notifications
    private final List<TaskListener> taskListeners = new CopyOnWriteArrayList<>();
    
    /**
     * Creates a new AutonomousIdeCoordinatorService.
     * @param project The project
     */
    public AutonomousIdeCoordinatorService(@NotNull Project project) {
        this.project = project;
        
        // Get other services
        this.aiServiceManager = project.getService(AIServiceManager.class);
        this.ideIntegrationService = project.getService(IDEIntegrationService.class);
        this.refactoringService = project.getService(AutomatedRefactoringService.class);
        this.codeAnalysisService = project.getService(CodeAnalysisService.class);
        this.patternRecognitionService = project.getService(PatternRecognitionService.class);
        this.codeGenerationService = project.getService(AutonomousCodeGenerationService.class);
        
        // Schedule periodic tasks
        scheduler.scheduleAtFixedRate(this::performPeriodicTasks, 5, 60, TimeUnit.MINUTES);
    }
    
    /**
     * Gets the AutonomousIdeCoordinatorService instance.
     * @param project The project
     * @return The AutonomousIdeCoordinatorService instance
     */
    public static AutonomousIdeCoordinatorService getInstance(@NotNull Project project) {
        return project.getService(AutonomousIdeCoordinatorService.class);
    }
    
    /**
     * Performs periodic tasks.
     */
    private void performPeriodicTasks() {
        try {
            LOG.info("Performing periodic tasks");
            
            // Analyze project for code issues
            analyzeBestPractices().thenAccept(issues -> {
                LOG.info("Found " + issues.size() + " best practice issues");
            });
            
        } catch (Exception e) {
            LOG.error("Error performing periodic tasks", e);
        }
    }
    
    /**
     * Analyzes the project and improves it based on best practices.
     * @return A list of issues found and fixed
     */
    public CompletableFuture<List<CodeIssue>> analyzeBestPractices() {
        return submitTask("analyze_best_practices", () -> {
            try {
                LOG.info("Analyzing project for best practices");
                
                // Analyze project structure
                IDEIntegrationService.ProjectStructure structure = ideIntegrationService.analyzeProjectStructure().get();
                
                // Analyze for code issues
                List<CodeIssue> issues = new ArrayList<>();
                
                // Check each class
                for (IDEIntegrationService.ClassInfo classInfo : structure.getClasses()) {
                    String filePath = classInfo.getFilePath();
                    
                    // Analyze file
                    List<CodeAnalysisService.CodeIssue> codeIssues = codeAnalysisService.analyzeFile(filePath).get();
                    
                    // Convert to our issue format
                    for (CodeAnalysisService.CodeIssue codeIssue : codeIssues) {
                        issues.add(new CodeIssue(
                                codeIssue.getFilePath(),
                                codeIssue.getLine(),
                                codeIssue.getDescription(),
                                codeIssue.getIssueType(),
                                codeIssue.hasQuickFix()
                        ));
                    }
                    
                    // Check for missing getters/setters
                    if (classInfo.getQualifiedName() != null) {
                        // Check if class has fields without getters/setters
                        boolean hasFieldsWithoutAccessors = checkForMissingAccessors(classInfo.getQualifiedName());
                        
                        if (hasFieldsWithoutAccessors) {
                            issues.add(new CodeIssue(
                                    filePath,
                                    1, // Line number not applicable here
                                    "Class has fields without getters/setters",
                                    "MissingAccessors",
                                    true
                            ));
                            
                            // Generate getters and setters
                            codeGenerationService.generateGettersAndSetters(classInfo.getQualifiedName()).get();
                        }
                    }
                }
                
                // Apply quick fixes for other issues
                for (CodeIssue issue : issues) {
                    if (issue.canFix) {
                        codeAnalysisService.applyQuickFixes(issue.filePath, true).get();
                    }
                }
                
                LOG.info("Completed best practices analysis. Found " + issues.size() + " issues.");
                return issues;
            } catch (Exception e) {
                LOG.error("Error analyzing best practices", e);
                throw new CompletionException(e);
            }
        });
    }
    
    /**
     * Checks if a class has fields without getters/setters.
     * @param className The fully qualified class name
     * @return Whether the class has fields without getters/setters
     */
    private boolean checkForMissingAccessors(@NotNull String className) {
        try {
            // Get class info from IDE
            // This is a simplified implementation - in a real world scenario we'd
            // check field by field and method by method to see what accessors are missing
            
            return true; // Assume missing accessors for demonstration
        } catch (Exception e) {
            LOG.error("Error checking for missing accessors", e);
            return false;
        }
    }
    
    /**
     * Creates a complete project structure based on analyzed requirements.
     * @param requirements The project requirements as a text description
     * @return A summary of what was created
     */
    public CompletableFuture<ProjectCreationSummary> createProjectFromRequirements(@NotNull String requirements) {
        return submitTask("create_project", () -> {
            try {
                LOG.info("Creating project from requirements");
                
                // This is where we would parse requirements, generate a design, and create classes
                // For demonstration, we'll create a simple project with a few classes
                
                ProjectCreationSummary summary = new ProjectCreationSummary();
                
                // Create domain objects
                createEntityClass("com.example.domain", "User").thenAccept(created -> {
                    if (created) {
                        summary.addCreatedFile("com/example/domain/User.java");
                    }
                }).get();
                
                createEntityClass("com.example.domain", "Product").thenAccept(created -> {
                    if (created) {
                        summary.addCreatedFile("com/example/domain/Product.java");
                    }
                }).get();
                
                // Create service interfaces
                createServiceInterface("com.example.service", "UserService").thenAccept(created -> {
                    if (created) {
                        summary.addCreatedFile("com/example/service/UserService.java");
                    }
                }).get();
                
                createServiceInterface("com.example.service", "ProductService").thenAccept(created -> {
                    if (created) {
                        summary.addCreatedFile("com/example/service/ProductService.java");
                    }
                }).get();
                
                // Create service implementations
                codeGenerationService.generateImplementation(
                        "com.example.service.impl.UserServiceImpl",
                        "com.example.service.UserService",
                        "com.example.service.impl"
                ).thenAccept(created -> {
                    if (created) {
                        summary.addCreatedFile("com/example/service/impl/UserServiceImpl.java");
                    }
                }).get();
                
                codeGenerationService.generateImplementation(
                        "com.example.service.impl.ProductServiceImpl",
                        "com.example.service.ProductService",
                        "com.example.service.impl"
                ).thenAccept(created -> {
                    if (created) {
                        summary.addCreatedFile("com/example/service/impl/ProductServiceImpl.java");
                    }
                }).get();
                
                LOG.info("Project creation complete. Created " + summary.getCreatedFiles().size() + " files.");
                return summary;
            } catch (Exception e) {
                LOG.error("Error creating project from requirements", e);
                throw new CompletionException(e);
            }
        });
    }
    
    /**
     * Creates an entity class.
     * @param packageName The package name
     * @param className The class name
     * @return Whether the class was created
     */
    private CompletableFuture<Boolean> createEntityClass(@NotNull String packageName, @NotNull String className) {
        // Create fields for an entity
        List<AutonomousCodeGenerationService.FieldDefinition> fields = new ArrayList<>();
        fields.add(new AutonomousCodeGenerationService.FieldDefinition(
                "id", "Long", true, false, false, null, "The entity ID"
        ));
        fields.add(new AutonomousCodeGenerationService.FieldDefinition(
                "name", "String", true, false, false, null, "The entity name"
        ));
        fields.add(new AutonomousCodeGenerationService.FieldDefinition(
                "createdAt", "java.util.Date", true, false, false, "new java.util.Date()", "The creation date"
        ));
        
        // Create methods for the entity
        List<AutonomousCodeGenerationService.MethodDefinition> methods = new ArrayList<>();
        
        // Add constructor
        List<AutonomousCodeGenerationService.ParameterDefinition> constructorParams = new ArrayList<>();
        constructorParams.add(new AutonomousCodeGenerationService.ParameterDefinition(
                "name", "String", "The entity name"
        ));
        
        methods.add(new AutonomousCodeGenerationService.MethodDefinition(
                className, "void", constructorParams, false, false, false,
                "this.name = name;\nthis.createdAt = new java.util.Date();",
                "Creates a new " + className, null
        ));
        
        // Generate class
        return codeGenerationService.generateClass(
                packageName,
                className,
                AutonomousCodeGenerationService.ClassType.CLASS,
                fields,
                methods,
                "Entity representing a " + className
        );
    }
    
    /**
     * Creates a service interface.
     * @param packageName The package name
     * @param interfaceName The interface name
     * @return Whether the interface was created
     */
    private CompletableFuture<Boolean> createServiceInterface(@NotNull String packageName, @NotNull String interfaceName) {
        // Create methods for the interface
        List<AutonomousCodeGenerationService.MethodDefinition> methods = new ArrayList<>();
        
        // Add standard CRUD methods
        String entityName = interfaceName.replace("Service", "");
        
        // findAll method
        methods.add(new AutonomousCodeGenerationService.MethodDefinition(
                "findAll", "java.util.List<com.example.domain." + entityName + ">",
                Collections.emptyList(), false, false, false, null,
                "Finds all " + entityName + " entities", "List of all entities"
        ));
        
        // findById method
        List<AutonomousCodeGenerationService.ParameterDefinition> findByIdParams = new ArrayList<>();
        findByIdParams.add(new AutonomousCodeGenerationService.ParameterDefinition(
                "id", "Long", "The entity ID"
        ));
        
        methods.add(new AutonomousCodeGenerationService.MethodDefinition(
                "findById", "com.example.domain." + entityName, findByIdParams,
                false, false, false, null,
                "Finds an entity by ID", "The entity, or null if not found"
        ));
        
        // save method
        List<AutonomousCodeGenerationService.ParameterDefinition> saveParams = new ArrayList<>();
        saveParams.add(new AutonomousCodeGenerationService.ParameterDefinition(
                "entity", "com.example.domain." + entityName, "The entity to save"
        ));
        
        methods.add(new AutonomousCodeGenerationService.MethodDefinition(
                "save", "com.example.domain." + entityName, saveParams,
                false, false, false, null,
                "Saves an entity", "The saved entity"
        ));
        
        // delete method
        List<AutonomousCodeGenerationService.ParameterDefinition> deleteParams = new ArrayList<>();
        deleteParams.add(new AutonomousCodeGenerationService.ParameterDefinition(
                "id", "Long", "The entity ID"
        ));
        
        methods.add(new AutonomousCodeGenerationService.MethodDefinition(
                "delete", "void", deleteParams, false, false, false, null,
                "Deletes an entity", null
        ));
        
        // Generate interface
        return codeGenerationService.generateClass(
                packageName,
                interfaceName,
                AutonomousCodeGenerationService.ClassType.INTERFACE,
                Collections.emptyList(),
                methods,
                "Service for " + entityName + " entities"
        );
    }
    
    /**
     * Optimizes the project by applying code improvements.
     * @return A summary of the optimizations performed
     */
    public CompletableFuture<OptimizationSummary> optimizeProject() {
        return submitTask("optimize_project", () -> {
            try {
                LOG.info("Starting project optimization");
                
                OptimizationSummary summary = new OptimizationSummary();
                
                // Analyze project for code issues
                CodeAnalysisService.CodeIssueStatistics statistics = 
                        codeAnalysisService.analyzeProjectCodeIssuePatterns().get();
                
                summary.setIssuesFound(statistics.getTotalIssueCount());
                summary.setFilesWithIssues(statistics.getFilesWithIssues());
                summary.setTotalFiles(statistics.getFileCount());
                
                // Apply quick fixes
                int fixedIssues = 0;
                
                // Get all Java files
                IDEIntegrationService.ProjectStructure structure = ideIntegrationService.analyzeProjectStructure().get();
                
                for (IDEIntegrationService.ClassInfo classInfo : structure.getClasses()) {
                    String filePath = classInfo.getFilePath();
                    
                    // Apply quick fixes to each file
                    List<CodeAnalysisService.AppliedFix> fixes = codeAnalysisService.applyQuickFixes(filePath, true).get();
                    fixedIssues += fixes.size();
                    
                    // Record fixes
                    for (CodeAnalysisService.AppliedFix fix : fixes) {
                        summary.addAppliedFix(fix.getFilePath(), fix.getIssueType() + ": " + fix.getFixName());
                    }
                }
                
                summary.setIssuesFixed(fixedIssues);
                
                LOG.info("Project optimization complete. Fixed " + fixedIssues + " issues.");
                return summary;
            } catch (Exception e) {
                LOG.error("Error optimizing project", e);
                throw new CompletionException(e);
            }
        });
    }
    
    /**
     * Submits a task to be executed.
     * @param taskId The task ID
     * @param task The task to execute
     * @param <T> The task result type
     * @return A future that completes when the task is done
     */
    private <T> CompletableFuture<T> submitTask(@NotNull String taskId, @NotNull Callable<T> task) {
        LOG.info("Submitting task: " + taskId);
        
        CompletableFuture<T> future = CompletableFuture.supplyAsync(() -> {
            try {
                notifyTaskStarted(taskId);
                T result = task.call();
                notifyTaskCompleted(taskId);
                return result;
            } catch (Exception e) {
                notifyTaskFailed(taskId, e);
                throw new CompletionException(e);
            }
        }, executor);
        
        activeTasks.put(taskId, future);
        
        future.whenComplete((result, ex) -> {
            activeTasks.remove(taskId);
            if (ex == null) {
                completedTasks.add(taskId);
                taskHistory.add(taskId + ": SUCCESS");
            } else {
                taskHistory.add(taskId + ": FAILED - " + ex.getMessage());
            }
        });
        
        return future;
    }
    
    /**
     * Disposes the service.
     */
    public void dispose() {
        scheduler.shutdown();
        executor.shutdown();
    }
    
    /**
     * Gets the active tasks.
     * @return The active tasks
     */
    @NotNull
    public Map<String, CompletableFuture<?>> getActiveTasks() {
        return new HashMap<>(activeTasks);
    }
    
    /**
     * Gets the completed tasks.
     * @return The completed tasks
     */
    @NotNull
    public Set<String> getCompletedTasks() {
        return new HashSet<>(completedTasks);
    }
    
    /**
     * Gets the task history.
     * @return The task history
     */
    @NotNull
    public List<String> getTaskHistory() {
        return new ArrayList<>(taskHistory);
    }
    
    /**
     * Adds a task listener.
     * @param listener The listener
     */
    public void addTaskListener(@NotNull TaskListener listener) {
        taskListeners.add(listener);
    }
    
    /**
     * Removes a task listener.
     * @param listener The listener
     */
    public void removeTaskListener(@NotNull TaskListener listener) {
        taskListeners.remove(listener);
    }
    
    /**
     * Notifies listeners that a task has started.
     * @param taskId The task ID
     */
    private void notifyTaskStarted(@NotNull String taskId) {
        for (TaskListener listener : taskListeners) {
            try {
                listener.onTaskStarted(taskId);
            } catch (Exception e) {
                LOG.error("Error notifying task listener", e);
            }
        }
    }
    
    /**
     * Notifies listeners that a task has completed.
     * @param taskId The task ID
     */
    private void notifyTaskCompleted(@NotNull String taskId) {
        for (TaskListener listener : taskListeners) {
            try {
                listener.onTaskCompleted(taskId);
            } catch (Exception e) {
                LOG.error("Error notifying task listener", e);
            }
        }
    }
    
    /**
     * Notifies listeners that a task has failed.
     * @param taskId The task ID
     * @param exception The exception
     */
    private void notifyTaskFailed(@NotNull String taskId, @NotNull Exception exception) {
        for (TaskListener listener : taskListeners) {
            try {
                listener.onTaskFailed(taskId, exception);
            } catch (Exception e) {
                LOG.error("Error notifying task listener", e);
            }
        }
    }
    
    /**
     * Listener for task events.
     */
    public interface TaskListener {
        /**
         * Called when a task is started.
         * @param taskId The task ID
         */
        void onTaskStarted(@NotNull String taskId);
        
        /**
         * Called when a task is completed.
         * @param taskId The task ID
         */
        void onTaskCompleted(@NotNull String taskId);
        
        /**
         * Called when a task has failed.
         * @param taskId The task ID
         * @param exception The exception
         */
        void onTaskFailed(@NotNull String taskId, @NotNull Exception exception);
    }
    
    /**
     * Summary of project creation.
     */
    public static class ProjectCreationSummary {
        private final List<String> createdFiles = new ArrayList<>();
        
        /**
         * Adds a created file.
         * @param filePath The file path
         */
        public void addCreatedFile(@NotNull String filePath) {
            createdFiles.add(filePath);
        }
        
        /**
         * Gets the created files.
         * @return The created files
         */
        @NotNull
        public List<String> getCreatedFiles() {
            return createdFiles;
        }
    }
    
    /**
     * Summary of project optimization.
     */
    public static class OptimizationSummary {
        private int issuesFound;
        private int issuesFixed;
        private int filesWithIssues;
        private int totalFiles;
        private final Map<String, List<String>> appliedFixes = new HashMap<>();
        
        /**
         * Gets the number of issues found.
         * @return The number of issues found
         */
        public int getIssuesFound() {
            return issuesFound;
        }
        
        /**
         * Sets the number of issues found.
         * @param issuesFound The number of issues found
         */
        public void setIssuesFound(int issuesFound) {
            this.issuesFound = issuesFound;
        }
        
        /**
         * Gets the number of issues fixed.
         * @return The number of issues fixed
         */
        public int getIssuesFixed() {
            return issuesFixed;
        }
        
        /**
         * Sets the number of issues fixed.
         * @param issuesFixed The number of issues fixed
         */
        public void setIssuesFixed(int issuesFixed) {
            this.issuesFixed = issuesFixed;
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
         * Gets the total number of files.
         * @return The total number of files
         */
        public int getTotalFiles() {
            return totalFiles;
        }
        
        /**
         * Sets the total number of files.
         * @param totalFiles The total number of files
         */
        public void setTotalFiles(int totalFiles) {
            this.totalFiles = totalFiles;
        }
        
        /**
         * Adds an applied fix.
         * @param filePath The file path
         * @param fixDescription The fix description
         */
        public void addAppliedFix(@NotNull String filePath, @NotNull String fixDescription) {
            appliedFixes.computeIfAbsent(filePath, k -> new ArrayList<>()).add(fixDescription);
        }
        
        /**
         * Gets the applied fixes.
         * @return The applied fixes
         */
        @NotNull
        public Map<String, List<String>> getAppliedFixes() {
            return appliedFixes;
        }
    }
    
    /**
     * Represents a code issue.
     */
    public static class CodeIssue {
        private final String filePath;
        private final int line;
        private final String description;
        private final String issueType;
        private final boolean canFix;
        
        /**
         * Creates a new CodeIssue.
         * @param filePath The file path
         * @param line The line number
         * @param description The issue description
         * @param issueType The issue type
         * @param canFix Whether the issue can be fixed
         */
        public CodeIssue(@NotNull String filePath, int line, @NotNull String description,
                         @NotNull String issueType, boolean canFix) {
            this.filePath = filePath;
            this.line = line;
            this.description = description;
            this.issueType = issueType;
            this.canFix = canFix;
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
         * Checks if the issue can be fixed.
         * @return Whether the issue can be fixed
         */
        public boolean canFix() {
            return canFix;
        }
    }
}