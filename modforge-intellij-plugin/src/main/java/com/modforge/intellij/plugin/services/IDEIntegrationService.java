package com.modforge.intellij.plugin.services;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Service for integrating with IDE-specific functionality.
 * This service provides methods for interacting with the IDE's project structure,
 * file system, and editor functionality.
 */
@Service(Service.Level.PROJECT)
public final class IDEIntegrationService {
    private static final Logger LOG = Logger.getInstance(IDEIntegrationService.class);
    
    private final Project project;
    private final MessageBusConnection messageBusConnection;
    
    /**
     * Creates a new IDEIntegrationService.
     * @param project The project
     */
    public IDEIntegrationService(@NotNull Project project) {
        this.project = project;
        this.messageBusConnection = project.getMessageBus().connect();
        
        LOG.info("IDEIntegrationService initialized for project: " + project.getName());
        
        // Register for file editor events
        registerFileEditorListeners();
    }
    
    /**
     * Gets the IDEIntegrationService instance.
     * @param project The project
     * @return The IDEIntegrationService instance
     */
    public static IDEIntegrationService getInstance(@NotNull Project project) {
        return project.getService(IDEIntegrationService.class);
    }
    
    /**
     * Registers for file editor events.
     */
    private void registerFileEditorListeners() {
        messageBusConnection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, 
                new FileEditorManagerListener() {
                    @Override
                    public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
                        // Handle file opened event
                    }
                    
                    @Override
                    public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
                        // Handle file closed event
                    }
                });
    }
    
    /**
     * Gets the current editor.
     * @return The current editor, or null if no editor is open
     */
    @Nullable
    public Editor getCurrentEditor() {
        return FileEditorManager.getInstance(project).getSelectedTextEditor();
    }
    
    /**
     * Gets the current file.
     * @return The current file, or null if no file is open
     */
    @Nullable
    public VirtualFile getCurrentFile() {
        VirtualFile[] files = FileEditorManager.getInstance(project).getSelectedFiles();
        return files.length > 0 ? files[0] : null;
    }
    
    /**
     * Gets the document for a file.
     * @param file The file
     * @return The document, or null if the file doesn't have a document
     */
    @Nullable
    public Document getDocument(@NotNull VirtualFile file) {
        return PsiDocumentManager.getInstance(project).getDocument(
                PsiManager.getInstance(project).findFile(file));
    }
    
    /**
     * Gets the PSI file for a file.
     * @param file The file
     * @return The PSI file, or null if the file doesn't have a PSI file
     */
    @Nullable
    public PsiFile getPsiFile(@NotNull VirtualFile file) {
        return PsiManager.getInstance(project).findFile(file);
    }
    
    /**
     * Creates a new file.
     * @param directory The directory to create the file in
     * @param fileName The file name
     * @param content The file content
     * @return The created file, or null if the file couldn't be created
     */
    @Nullable
    public VirtualFile createFile(@NotNull VirtualFile directory, @NotNull String fileName, 
                                 @NotNull String content) {
        try {
            VirtualFile file = directory.findChild(fileName);
            if (file == null) {
                file = directory.createChildData(this, fileName);
            }
            
            ApplicationManager.getApplication().runWriteAction(() -> {
                try {
                    file.setBinaryContent(content.getBytes(StandardCharsets.UTF_8));
                } catch (IOException e) {
                    LOG.error("Error writing to file: " + file.getPath(), e);
                }
            });
            
            return file;
        } catch (IOException e) {
            LOG.error("Error creating file: " + fileName, e);
            return null;
        }
    }
    
    /**
     * Opens a file in the editor.
     * @param file The file to open
     * @return Whether the file was opened successfully
     */
    public boolean openFile(@NotNull VirtualFile file) {
        OpenFileDescriptor descriptor = new OpenFileDescriptor(project, file);
        return !descriptor.canNavigate() || descriptor.navigate(true);
    }
    
    /**
     * Gets all source files in the project.
     * @return The source files
     */
    @NotNull
    public List<VirtualFile> getSourceFiles() {
        List<VirtualFile> result = new ArrayList<>();
        
        // Get Java source files from all modules
        Module[] modules = ModuleManager.getInstance(project).getModules();
        for (Module module : modules) {
            ModuleRootManager rootManager = ModuleRootManager.getInstance(module);
            VirtualFile[] sourceRoots = rootManager.getSourceRoots(false);
            
            for (VirtualFile sourceRoot : sourceRoots) {
                collectFilesRecursively(sourceRoot, file -> {
                    if (!file.isDirectory() && "java".equals(file.getExtension())) {
                        result.add(file);
                    }
                });
            }
        }
        
        return result;
    }
    
    /**
     * Gets all Java files in the project.
     * @return The Java files
     */
    @NotNull
    public List<VirtualFile> getJavaFiles() {
        Collection<VirtualFile> files = FileTypeIndex.getFiles(
                JavaFileType.INSTANCE, GlobalSearchScope.projectScope(project));
        return new ArrayList<>(files);
    }
    
    /**
     * Gets all classes in the project.
     * @return The classes
     */
    @NotNull
    public List<PsiClass> getClasses() {
        List<PsiClass> result = new ArrayList<>();
        
        // Get Java source files
        List<VirtualFile> javaFiles = getJavaFiles();
        
        // Get classes from Java files
        for (VirtualFile file : javaFiles) {
            PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
            if (psiFile instanceof PsiJavaFile) {
                PsiJavaFile javaFile = (PsiJavaFile) psiFile;
                
                for (PsiClass psiClass : javaFile.getClasses()) {
                    result.add(psiClass);
                }
            }
        }
        
        return result;
    }
    
    /**
     * Gets all methods in a class.
     * @param psiClass The class
     * @return The methods
     */
    @NotNull
    public List<PsiMethod> getMethods(@NotNull PsiClass psiClass) {
        PsiMethod[] methods = psiClass.getMethods();
        List<PsiMethod> result = new ArrayList<>(methods.length);
        
        for (PsiMethod method : methods) {
            result.add(method);
        }
        
        return result;
    }
    
    /**
     * Gets all fields in a class.
     * @param psiClass The class
     * @return The fields
     */
    @NotNull
    public List<PsiField> getFields(@NotNull PsiClass psiClass) {
        PsiField[] fields = psiClass.getFields();
        List<PsiField> result = new ArrayList<>(fields.length);
        
        for (PsiField field : fields) {
            result.add(field);
        }
        
        return result;
    }
    
    /**
     * Gets the source root for a file.
     * @param file The file
     * @return The source root, or null if the file doesn't have a source root
     */
    @Nullable
    public VirtualFile getSourceRoot(@NotNull VirtualFile file) {
        return ProjectRootManager.getInstance(project).getFileIndex().getSourceRootForFile(file);
    }
    
    /**
     * Gets the module for a file.
     * @param file The file
     * @return The module, or null if the file doesn't have a module
     */
    @Nullable
    public Module getModule(@NotNull VirtualFile file) {
        return ProjectRootManager.getInstance(project).getFileIndex().getModuleForFile(file);
    }
    
    /**
     * Gets the package for a file.
     * @param file The file
     * @return The package, or an empty string if the file doesn't have a package
     */
    @NotNull
    public String getPackage(@NotNull VirtualFile file) {
        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        if (psiFile instanceof PsiJavaFile) {
            PsiJavaFile javaFile = (PsiJavaFile) psiFile;
            return javaFile.getPackageName();
        }
        
        return "";
    }
    
    /**
     * Collects files recursively.
     * @param directory The directory to collect files from
     * @param consumer The consumer to process each file
     */
    private void collectFilesRecursively(@NotNull VirtualFile directory, @NotNull Consumer<VirtualFile> consumer) {
        // Process the directory itself
        consumer.accept(directory);
        
        // Process children
        VirtualFile[] children = directory.getChildren();
        for (VirtualFile child : children) {
            if (child.isDirectory()) {
                collectFilesRecursively(child, consumer);
            } else {
                consumer.accept(child);
            }
        }
    }
    
    /**
     * Gets the project base directory.
     * @return The project base directory
     */
    @Nullable
    public VirtualFile getProjectBaseDir() {
        return ProjectUtil.guessProjectDir(project);
    }
    
    /**
     * Detects the mod loader for a project.
     * @return The detected mod loader, or null if no mod loader was detected
     */
    @Nullable
    public String detectModLoader() {
        // Check for mod loader specific files
        VirtualFile baseDir = getProjectBaseDir();
        if (baseDir == null) {
            return null;
        }
        
        // Check for build.gradle
        VirtualFile buildGradle = baseDir.findChild("build.gradle");
        if (buildGradle != null) {
            try {
                String content = new String(buildGradle.contentsToByteArray(), StandardCharsets.UTF_8);
                
                if (content.contains("net.minecraftforge") || content.contains("fg.deobf")) {
                    return "Forge";
                } else if (content.contains("fabric-loom") || content.contains("net.fabricmc")) {
                    return "Fabric";
                } else if (content.contains("architectury") || content.contains("dev.architectury")) {
                    return "Architectury";
                } else if (content.contains("quilt-loom") || content.contains("org.quiltmc")) {
                    return "Quilt";
                }
            } catch (IOException e) {
                LOG.error("Error reading build.gradle", e);
            }
        }
        
        // Check for mod loader specific dependencies in Java files
        List<PsiClass> classes = getClasses();
        for (PsiClass psiClass : classes) {
            PsiFile containingFile = psiClass.getContainingFile();
            if (containingFile instanceof PsiJavaFile) {
                PsiJavaFile javaFile = (PsiJavaFile) containingFile;
                
                // Check imports
                for (PsiImportStatement importStatement : javaFile.getImportList().getImportStatements()) {
                    String importText = importStatement.getQualifiedName();
                    
                    if (importText != null) {
                        if (importText.startsWith("net.minecraftforge")) {
                            return "Forge";
                        } else if (importText.startsWith("net.fabricmc")) {
                            return "Fabric";
                        } else if (importText.startsWith("dev.architectury")) {
                            return "Architectury";
                        } else if (importText.startsWith("org.quiltmc")) {
                            return "Quilt";
                        }
                    }
                }
                
                // Check annotations
                PsiModifierList modifierList = psiClass.getModifierList();
                if (modifierList != null) {
                    for (PsiAnnotation annotation : modifierList.getAnnotations()) {
                        String qualifiedName = annotation.getQualifiedName();
                        
                        if (qualifiedName != null) {
                            if (qualifiedName.startsWith("net.minecraftforge")) {
                                return "Forge";
                            } else if (qualifiedName.startsWith("net.fabricmc")) {
                                return "Fabric";
                            } else if (qualifiedName.startsWith("dev.architectury")) {
                                return "Architectury";
                            } else if (qualifiedName.startsWith("org.quiltmc")) {
                                return "Quilt";
                            }
                        }
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * Finds Minecraft mod classes in the project.
     * @return The mod classes
     */
    @NotNull
    public List<PsiClass> findModClasses() {
        List<PsiClass> allClasses = getClasses();
        List<PsiClass> modClasses = new ArrayList<>();
        
        for (PsiClass psiClass : allClasses) {
            if (isMinecraftModClass(psiClass)) {
                modClasses.add(psiClass);
            }
        }
        
        return modClasses;
    }
    
    /**
     * Checks if a class is a Minecraft mod class.
     * @param psiClass The class to check
     * @return Whether the class is a Minecraft mod class
     */
    private boolean isMinecraftModClass(@NotNull PsiClass psiClass) {
        // Check for mod annotations
        PsiModifierList modifierList = psiClass.getModifierList();
        if (modifierList != null) {
            for (PsiAnnotation annotation : modifierList.getAnnotations()) {
                String qualifiedName = annotation.getQualifiedName();
                
                if (qualifiedName != null) {
                    if (qualifiedName.contains("Mod") || 
                            qualifiedName.contains("Plugin") || 
                            qualifiedName.contains("ModInitializer")) {
                        return true;
                    }
                }
            }
        }
        
        // Check for mod interfaces
        for (PsiClassType implementsType : psiClass.getImplementsListTypes()) {
            String implementsName = implementsType.getClassName();
            
            if (implementsName != null) {
                if (implementsName.contains("ModInitializer") || 
                        implementsName.contains("ClientModInitializer") || 
                        implementsName.contains("DedicatedServerModInitializer")) {
                    return true;
                }
            }
        }
        
        // Check for mod base classes
        PsiClassType extendsType = psiClass.getExtendsListType();
        if (extendsType != null) {
            String extendsName = extendsType.getClassName();
            
            if (extendsName != null) {
                if (extendsName.contains("Mod") || extendsName.contains("Plugin")) {
                    return true;
                }
            }
        }
        
        return false;
    }
}