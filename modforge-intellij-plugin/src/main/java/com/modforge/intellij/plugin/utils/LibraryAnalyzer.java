package com.modforge.intellij.plugin.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.modforge.intellij.plugin.models.LibraryData;
import com.modforge.intellij.plugin.models.LibraryMethodData;
import com.modforge.intellij.plugin.services.AIServiceManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Analyzes project libraries to enhance the knowledge base.
 * This is a key component for self-improving capability.
 */
public class LibraryAnalyzer {
    private static final Logger LOG = Logger.getInstance(LibraryAnalyzer.class);
    
    private final Project project;
    private final AIServiceManager aiServiceManager;
    private final Map<String, LibraryData> libraryDataCache = new ConcurrentHashMap<>();
    private final Set<String> analyzedLibraries = ConcurrentHashMap.newKeySet();
    private final ExecutorService executorService;
    private final Gson gson;
    
    // Stats tracking
    private final AtomicInteger classesAnalyzed = new AtomicInteger(0);
    private final AtomicInteger methodsAnalyzed = new AtomicInteger(0);
    private final AtomicInteger librariesAnalyzed = new AtomicInteger(0);
    
    public LibraryAnalyzer(Project project) {
        this.project = project;
        this.aiServiceManager = project.getService(AIServiceManager.class);
        this.executorService = AppExecutorUtil.getAppExecutorService();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }
    
    /**
     * Analyzes all libraries in the project.
     * This is a long-running operation and should be run in a background thread.
     * @param listener A listener to receive updates on the analysis progress
     */
    public void analyzeAllLibraries(AnalysisProgressListener listener) {
        LOG.info("Starting analysis of all project libraries");
        
        // Reset stats
        classesAnalyzed.set(0);
        methodsAnalyzed.set(0);
        librariesAnalyzed.set(0);
        
        // Get all modules in the project
        Module[] modules = ModuleRootManager.getInstance(project.getModules()[0]).getDependencies();
        Set<Library> libraries = new HashSet<>();
        
        // Collect all libraries from all modules
        for (Module module : modules) {
            for (OrderEntry entry : ModuleRootManager.getInstance(module).getOrderEntries()) {
                if (entry instanceof LibraryOrderEntry) {
                    Library library = ((LibraryOrderEntry) entry).getLibrary();
                    if (library != null) {
                        libraries.add(library);
                    }
                }
            }
        }
        
        // Start analysis
        int totalLibraries = libraries.size();
        listener.onAnalysisStarted(totalLibraries);
        
        for (Library library : libraries) {
            String libraryName = library.getName();
            if (libraryName == null) continue;
            
            // Skip if already analyzed
            if (analyzedLibraries.contains(libraryName)) {
                listener.onLibrarySkipped(libraryName, "Already analyzed");
                continue;
            }
            
            executorService.submit(() -> {
                try {
                    LibraryData libraryData = analyzeLibrary(library);
                    if (libraryData != null) {
                        // Cache the result
                        libraryDataCache.put(libraryName, libraryData);
                        analyzedLibraries.add(libraryName);
                        
                        // Update stats
                        librariesAnalyzed.incrementAndGet();
                        
                        // Upload to web platform if sync is enabled
                        if (project.getService(com.modforge.intellij.plugin.services.ModForgeSettingsService.class).isSyncEnabled()) {
                            uploadLibraryDataToWebPlatform(libraryData);
                        }
                        
                        listener.onLibraryAnalyzed(libraryName, libraryData);
                    }
                } catch (Exception e) {
                    LOG.error("Error analyzing library " + libraryName, e);
                    listener.onLibraryError(libraryName, e.getMessage());
                } finally {
                    // Check if all libraries have been analyzed
                    if (librariesAnalyzed.get() == totalLibraries) {
                        listener.onAnalysisCompleted(librariesAnalyzed.get(), classesAnalyzed.get(), methodsAnalyzed.get());
                    }
                }
            });
        }
    }
    
    /**
     * Analyzes a specific library.
     * @param library The library to analyze
     * @return Data about the library, or null if analysis failed
     */
    private LibraryData analyzeLibrary(Library library) {
        String libraryName = library.getName();
        if (libraryName == null) return null;
        
        LOG.info("Analyzing library: " + libraryName);
        
        LibraryData libraryData = new LibraryData();
        libraryData.setName(libraryName);
        libraryData.setClasses(new ArrayList<>());
        
        // Get all classes in the library
        VirtualFile[] classFiles = library.getFiles(com.intellij.openapi.roots.OrderRootType.CLASSES);
        for (VirtualFile file : classFiles) {
            if (file.isDirectory()) {
                analyzeClassesInDirectory(file, libraryData);
            }
        }
        
        return libraryData;
    }
    
    /**
     * Recursively analyzes all classes in a directory.
     * @param directory The directory to analyze
     * @param libraryData The library data to update
     */
    private void analyzeClassesInDirectory(VirtualFile directory, LibraryData libraryData) {
        for (VirtualFile file : directory.getChildren()) {
            if (file.isDirectory()) {
                analyzeClassesInDirectory(file, libraryData);
            } else if (file.getExtension() != null && file.getExtension().equals("class")) {
                String className = getClassName(file, directory);
                analyzeClass(className, libraryData);
            }
        }
    }
    
    /**
     * Gets the fully qualified class name from a class file.
     * @param classFile The class file
     * @param rootDir The root directory
     * @return The fully qualified class name
     */
    private String getClassName(VirtualFile classFile, VirtualFile rootDir) {
        String relativePath = classFile.getPath().substring(rootDir.getPath().length() + 1);
        return relativePath.replace("/", ".")
                .replace("\\", ".")
                .replace(".class", "");
    }
    
    /**
     * Analyzes a specific class.
     * @param className The class name
     * @param libraryData The library data to update
     */
    private void analyzeClass(String className, LibraryData libraryData) {
        try {
            PsiClass psiClass = JavaPsiFacade.getInstance(project)
                    .findClass(className, GlobalSearchScope.allScope(project));
            
            if (psiClass == null) return;
            
            // Create class data
            LibraryData.ClassData classData = new LibraryData.ClassData();
            classData.setName(className);
            classData.setMethods(new ArrayList<>());
            
            // Analyze methods
            for (PsiMethod method : psiClass.getMethods()) {
                LibraryMethodData methodData = analyzeMethod(method);
                classData.getMethods().add(methodData);
                methodsAnalyzed.incrementAndGet();
            }
            
            libraryData.getClasses().add(classData);
            classesAnalyzed.incrementAndGet();
        } catch (Exception e) {
            LOG.warn("Error analyzing class " + className, e);
        }
    }
    
    /**
     * Analyzes a specific method.
     * @param method The method to analyze
     * @return Data about the method
     */
    private LibraryMethodData analyzeMethod(PsiMethod method) {
        LibraryMethodData methodData = new LibraryMethodData();
        methodData.setName(method.getName());
        methodData.setReturnType(method.getReturnType() != null ? method.getReturnType().getPresentableText() : "void");
        methodData.setParameters(Arrays.stream(method.getParameters())
                .map(p -> p.getType().getPresentableText() + " " + p.getName())
                .toArray(String[]::new));
        methodData.setDocComment(method.getDocComment() != null ? method.getDocComment().getText() : "");
        methodData.setCode(method.getText());
        return methodData;
    }
    
    /**
     * Uploads library data to the web platform.
     * @param libraryData The library data to upload
     */
    private void uploadLibraryDataToWebPlatform(LibraryData libraryData) {
        try {
            LOG.info("Uploading library data to web platform: " + libraryData.getName());
            
            String json = gson.toJson(libraryData);
            String url = project.getService(com.modforge.intellij.plugin.services.ModForgeSettingsService.class).getApiUrl() + "/libraries";
            
            String response = ApiRequestUtil.post(url, json);
            if (response != null) {
                LOG.info("Successfully uploaded library data for " + libraryData.getName());
            } else {
                LOG.warn("Failed to upload library data for " + libraryData.getName());
            }
        } catch (Exception e) {
            LOG.error("Error uploading library data", e);
        }
    }
    
    /**
     * Gets statistics about the library analysis.
     * @return Statistics about the library analysis
     */
    public Map<String, Integer> getAnalysisStats() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("librariesAnalyzed", librariesAnalyzed.get());
        stats.put("classesAnalyzed", classesAnalyzed.get());
        stats.put("methodsAnalyzed", methodsAnalyzed.get());
        return stats;
    }
    
    /**
     * Interface for receiving updates on the analysis progress.
     */
    public interface AnalysisProgressListener {
        void onAnalysisStarted(int totalLibraries);
        void onLibraryAnalyzed(String libraryName, LibraryData libraryData);
        void onLibrarySkipped(String libraryName, String reason);
        void onLibraryError(String libraryName, String errorMessage);
        void onAnalysisCompleted(int librariesAnalyzed, int classesAnalyzed, int methodsAnalyzed);
    }
}