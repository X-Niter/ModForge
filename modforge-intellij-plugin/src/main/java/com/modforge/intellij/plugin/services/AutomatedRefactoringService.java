package com.modforge.intellij.plugin.services;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.extractMethod.ExtractMethodHandler;
import com.intellij.refactoring.rename.RenameProcessor;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Service for automated code refactoring and modification.
 * This service allows ModForge to autonomously:
 * - Rename symbols
 * - Extract methods
 * - Add new code
 * - Format code
 * - Fix common issues
 */
@Service(Service.Level.PROJECT)
public final class AutomatedRefactoringService {
    private static final Logger LOG = Logger.getInstance(AutomatedRefactoringService.class);
    
    private final Project project;
    private final ExecutorService executor = AppExecutorUtil.createBoundedApplicationPoolExecutor(
            "ModForge.AutomatedRefactoring", 2);
    
    /**
     * Creates a new AutomatedRefactoringService.
     * @param project The project
     */
    public AutomatedRefactoringService(@NotNull Project project) {
        this.project = project;
    }
    
    /**
     * Gets the AutomatedRefactoringService instance.
     * @param project The project
     * @return The AutomatedRefactoringService instance
     */
    public static AutomatedRefactoringService getInstance(@NotNull Project project) {
        return project.getService(AutomatedRefactoringService.class);
    }
    
    /**
     * Renames a class, field, or method.
     * @param symbolType The type of symbol to rename
     * @param qualifiedName The qualified name of the symbol
     * @param newName The new name
     * @return Whether the rename was successful
     */
    public CompletableFuture<Boolean> renameSymbol(@NotNull SymbolType symbolType, 
                                                  @NotNull String qualifiedName,
                                                  @NotNull String newName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                LOG.info("Renaming " + symbolType + " from " + qualifiedName + " to " + newName);
                
                // Find the PsiElement to rename
                PsiElement element = findSymbol(symbolType, qualifiedName);
                if (element == null) {
                    LOG.warn("Symbol not found: " + qualifiedName);
                    return false;
                }
                
                // Execute rename in a write action
                Boolean result = ApplicationManager.getApplication().runReadAction((Computable<Boolean>) () -> {
                    try {
                        RenameProcessor processor = new RenameProcessor(project, element, newName, false, false);
                        processor.run();
                        return true;
                    } catch (Exception e) {
                        LOG.error("Error renaming symbol", e);
                        return false;
                    }
                });
                
                LOG.info("Renamed " + symbolType + " " + qualifiedName + " to " + newName + ": " + result);
                return result;
            } catch (Exception e) {
                LOG.error("Error renaming symbol", e);
                return false;
            }
        }, executor);
    }
    
    /**
     * Adds a new method to a class.
     * @param className The fully qualified class name
     * @param methodCode The method code, including signature and body
     * @param beforeMethod The name of the method to insert the new method before (null to append)
     * @return Whether the method was added successfully
     */
    public CompletableFuture<Boolean> addMethodToClass(@NotNull String className, 
                                                      @NotNull String methodCode,
                                                      @Nullable String beforeMethod) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                LOG.info("Adding method to class: " + className);
                
                // Find the class
                PsiClass psiClass = findClass(className);
                if (psiClass == null) {
                    LOG.warn("Class not found: " + className);
                    return false;
                }
                
                // Create factory for creating code
                final PsiElementFactory factory = PsiElementFactory.getInstance(project);
                
                // Execute in a write action
                WriteCommandAction.runWriteCommandAction(project, () -> {
                    try {
                        // Create method
                        PsiMethod method = factory.createMethodFromText(methodCode, psiClass);
                        
                        // Find method to insert before
                        PsiMethod beforePsiMethod = null;
                        if (beforeMethod != null) {
                            PsiMethod[] methods = psiClass.getMethods();
                            for (PsiMethod m : methods) {
                                if (beforeMethod.equals(m.getName())) {
                                    beforePsiMethod = m;
                                    break;
                                }
                            }
                        }
                        
                        // Add method to class
                        if (beforePsiMethod != null) {
                            psiClass.addBefore(method, beforePsiMethod);
                        } else {
                            psiClass.add(method);
                        }
                        
                        // Format code
                        CodeStyleManager.getInstance(project).reformat(method);
                    } catch (Exception e) {
                        LOG.error("Error adding method to class", e);
                    }
                });
                
                LOG.info("Added method to class: " + className);
                return true;
            } catch (Exception e) {
                LOG.error("Error adding method to class", e);
                return false;
            }
        }, executor);
    }
    
    /**
     * Adds an import statement to a file.
     * @param filePath The file path
     * @param importStatement The import statement
     * @return Whether the import was added successfully
     */
    public CompletableFuture<Boolean> addImportToFile(@NotNull String filePath, 
                                                     @NotNull String importStatement) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                LOG.info("Adding import to file: " + filePath);
                
                // Find the file
                VirtualFile[] files = FilenameIndex.getVirtualFilesByName(
                        filePath.substring(filePath.lastIndexOf('/') + 1),
                        GlobalSearchScope.projectScope(project)
                );
                
                if (files.length == 0) {
                    LOG.warn("File not found: " + filePath);
                    return false;
                }
                
                VirtualFile targetFile = null;
                for (VirtualFile file : files) {
                    if (file.getPath().equals(filePath)) {
                        targetFile = file;
                        break;
                    }
                }
                
                if (targetFile == null) {
                    LOG.warn("File not found: " + filePath);
                    return false;
                }
                
                // Get the PSI file
                PsiFile psiFile = PsiManager.getInstance(project).findFile(targetFile);
                if (psiFile == null || !(psiFile instanceof PsiJavaFile)) {
                    LOG.warn("Not a Java file: " + filePath);
                    return false;
                }
                
                PsiJavaFile javaFile = (PsiJavaFile) psiFile;
                
                // Create factory for creating code
                final PsiElementFactory factory = PsiElementFactory.getInstance(project);
                
                // Execute in a write action
                WriteCommandAction.runWriteCommandAction(project, () -> {
                    try {
                        // Create import statement
                        PsiImportStatement importStatement1 = factory.createImportStatementOnDemand(importStatement);
                        
                        // Add import statement to file
                        javaFile.getImportList().add(importStatement1);
                    } catch (Exception e) {
                        LOG.error("Error adding import to file", e);
                    }
                });
                
                LOG.info("Added import to file: " + filePath);
                return true;
            } catch (Exception e) {
                LOG.error("Error adding import to file", e);
                return false;
            }
        }, executor);
    }
    
    /**
     * Formats a file.
     * @param filePath The file path
     * @return Whether the formatting was successful
     */
    public CompletableFuture<Boolean> formatFile(@NotNull String filePath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                LOG.info("Formatting file: " + filePath);
                
                // Find the file
                VirtualFile[] files = FilenameIndex.getVirtualFilesByName(
                        filePath.substring(filePath.lastIndexOf('/') + 1),
                        GlobalSearchScope.projectScope(project)
                );
                
                if (files.length == 0) {
                    LOG.warn("File not found: " + filePath);
                    return false;
                }
                
                VirtualFile targetFile = null;
                for (VirtualFile file : files) {
                    if (file.getPath().equals(filePath)) {
                        targetFile = file;
                        break;
                    }
                }
                
                if (targetFile == null) {
                    LOG.warn("File not found: " + filePath);
                    return false;
                }
                
                // Get the PSI file
                PsiFile psiFile = PsiManager.getInstance(project).findFile(targetFile);
                if (psiFile == null) {
                    LOG.warn("PSI file not found: " + filePath);
                    return false;
                }
                
                // Execute in a write action
                WriteCommandAction.runWriteCommandAction(project, () -> {
                    try {
                        // Format the file
                        CodeStyleManager.getInstance(project).reformat(psiFile);
                    } catch (Exception e) {
                        LOG.error("Error formatting file", e);
                    }
                });
                
                LOG.info("Formatted file: " + filePath);
                return true;
            } catch (Exception e) {
                LOG.error("Error formatting file", e);
                return false;
            }
        }, executor);
    }
    
    /**
     * Creates a new file.
     * @param filePath The file path
     * @param fileContent The file content
     * @return Whether the file was created successfully
     */
    public CompletableFuture<Boolean> createFile(@NotNull String filePath, 
                                                @NotNull String fileContent) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                LOG.info("Creating file: " + filePath);
                
                // Get the directory
                String directoryPath = filePath.substring(0, filePath.lastIndexOf('/'));
                String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
                
                // Find or create the directory
                PsiDirectory directory = ApplicationManager.getApplication().runReadAction(
                        (Computable<PsiDirectory>) () -> {
                            try {
                                return PsiManager.getInstance(project).findDirectory(
                                        project.getBaseDir().findFileByRelativePath(directoryPath)
                                );
                            } catch (Exception e) {
                                LOG.error("Error finding directory", e);
                                return null;
                            }
                        }
                );
                
                if (directory == null) {
                    LOG.warn("Directory not found: " + directoryPath);
                    return false;
                }
                
                // Execute in a write action
                WriteCommandAction.runWriteCommandAction(project, () -> {
                    try {
                        // Create the file
                        PsiFile psiFile = PsiFileFactory.getInstance(project).createFileFromText(
                                fileName,
                                PsiJavaFile.class,
                                fileContent
                        );
                        
                        // Add the file to the directory
                        directory.add(psiFile);
                        
                        // Format the file
                        CodeStyleManager.getInstance(project).reformat(psiFile);
                    } catch (Exception e) {
                        LOG.error("Error creating file", e);
                    }
                });
                
                LOG.info("Created file: " + filePath);
                return true;
            } catch (Exception e) {
                LOG.error("Error creating file", e);
                return false;
            }
        }, executor);
    }
    
    /**
     * Creates a new class.
     * @param packageName The package name
     * @param className The class name
     * @param template The class template
     * @return Whether the class was created successfully
     */
    public CompletableFuture<Boolean> createClass(@NotNull String packageName, 
                                                 @NotNull String className,
                                                 @NotNull String template) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                LOG.info("Creating class: " + packageName + "." + className);
                
                // Generate file path and content
                String directoryPath = packageName.replace('.', '/');
                String filePath = directoryPath + "/" + className + ".java";
                String fileContent = "package " + packageName + ";\n\n" + template;
                
                // Create the file
                return createFile(filePath, fileContent).get();
            } catch (Exception e) {
                LOG.error("Error creating class", e);
                return false;
            }
        }, executor);
    }
    
    /**
     * Finds a symbol in the project.
     * @param symbolType The symbol type
     * @param qualifiedName The qualified name
     * @return The PsiElement representing the symbol, or null if not found
     */
    @Nullable
    private PsiElement findSymbol(@NotNull SymbolType symbolType, @NotNull String qualifiedName) {
        switch (symbolType) {
            case CLASS:
                return findClass(qualifiedName);
            case METHOD:
                return findMethod(qualifiedName);
            case FIELD:
                return findField(qualifiedName);
            default:
                LOG.warn("Unknown symbol type: " + symbolType);
                return null;
        }
    }
    
    /**
     * Finds a class in the project.
     * @param qualifiedName The fully qualified class name
     * @return The PsiClass, or null if not found
     */
    @Nullable
    private PsiClass findClass(@NotNull String qualifiedName) {
        return ApplicationManager.getApplication().runReadAction((Computable<PsiClass>) () -> {
            try {
                String className = qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1);
                PsiClass[] classes = PsiShortNamesCache.getInstance(project).getClassesByName(
                        className,
                        GlobalSearchScope.projectScope(project)
                );
                
                for (PsiClass psiClass : classes) {
                    if (qualifiedName.equals(psiClass.getQualifiedName())) {
                        return psiClass;
                    }
                }
                
                return null;
            } catch (Exception e) {
                LOG.error("Error finding class", e);
                return null;
            }
        });
    }
    
    /**
     * Finds a method in the project.
     * @param qualifiedName The qualified method name (class.method)
     * @return The PsiMethod, or null if not found
     */
    @Nullable
    private PsiMethod findMethod(@NotNull String qualifiedName) {
        return ApplicationManager.getApplication().runReadAction((Computable<PsiMethod>) () -> {
            try {
                int lastDot = qualifiedName.lastIndexOf('.');
                if (lastDot == -1) {
                    LOG.warn("Invalid method name: " + qualifiedName);
                    return null;
                }
                
                String className = qualifiedName.substring(0, lastDot);
                String methodName = qualifiedName.substring(lastDot + 1);
                
                PsiClass psiClass = findClass(className);
                if (psiClass == null) {
                    LOG.warn("Class not found: " + className);
                    return null;
                }
                
                PsiMethod[] methods = psiClass.getMethods();
                for (PsiMethod method : methods) {
                    if (methodName.equals(method.getName())) {
                        return method;
                    }
                }
                
                return null;
            } catch (Exception e) {
                LOG.error("Error finding method", e);
                return null;
            }
        });
    }
    
    /**
     * Finds a field in the project.
     * @param qualifiedName The qualified field name (class.field)
     * @return The PsiField, or null if not found
     */
    @Nullable
    private PsiField findField(@NotNull String qualifiedName) {
        return ApplicationManager.getApplication().runReadAction((Computable<PsiField>) () -> {
            try {
                int lastDot = qualifiedName.lastIndexOf('.');
                if (lastDot == -1) {
                    LOG.warn("Invalid field name: " + qualifiedName);
                    return null;
                }
                
                String className = qualifiedName.substring(0, lastDot);
                String fieldName = qualifiedName.substring(lastDot + 1);
                
                PsiClass psiClass = findClass(className);
                if (psiClass == null) {
                    LOG.warn("Class not found: " + className);
                    return null;
                }
                
                PsiField[] fields = psiClass.getFields();
                for (PsiField field : fields) {
                    if (fieldName.equals(field.getName())) {
                        return field;
                    }
                }
                
                return null;
            } catch (Exception e) {
                LOG.error("Error finding field", e);
                return null;
            }
        });
    }
    
    /**
     * Symbol types.
     */
    public enum SymbolType {
        CLASS,
        METHOD,
        FIELD
    }
}