package com.modforge.intellij.plugin.run;

import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessTerminatedListener;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.modforge.intellij.plugin.run.MinecraftRunConfiguration.RunType;
import org.jetbrains.annotations.NotNull;

/**
 * Execution state for Minecraft run configurations.
 * Handles the actual execution of Minecraft runs by setting up the command line.
 */
public class MinecraftRunProfileState extends CommandLineState {
    
    private static final Logger LOG = Logger.getInstance(MinecraftRunProfileState.class);
    
    private final MinecraftRunConfiguration configuration;
    private final Project project;
    
    public MinecraftRunProfileState(ExecutionEnvironment environment, MinecraftRunConfiguration configuration) {
        super(environment);
        this.configuration = configuration;
        this.project = environment.getProject();
    }
    
    @NotNull
    @Override
    protected ProcessHandler startProcess() throws ExecutionException {
        JavaParameters params = createJavaParameters();
        
        // Log the command line for debugging
        GeneralCommandLine commandLine = params.toCommandLine();
        String fullCommand = commandLine.getCommandLineString();
        LOG.info("Executing Minecraft run command: " + fullCommand);
        
        // Create the process handler
        OSProcessHandler processHandler = new OSProcessHandler(commandLine);
        ProcessTerminatedListener.attach(processHandler);
        
        return processHandler;
    }
    
    @NotNull
    private JavaParameters createJavaParameters() throws ExecutionException {
        JavaParameters params = new JavaParameters();
        
        // Detect the main module of the project
        Module mainModule = findMainModule();
        if (mainModule == null) {
            throw new ExecutionException("Cannot find main module in project");
        }
        
        // Set up module-based classpath
        params.configureByModule(mainModule, JavaParameters.JDK_AND_CLASSES);
        
        // Set main class based on run type and mod loader
        String mainClass = determineMainClass();
        params.setMainClass(mainClass);
        
        // Add VM arguments
        if (configuration.getVmArgs() != null && !configuration.getVmArgs().isEmpty()) {
            for (String arg : configuration.getVmArgs().split("\\s+")) {
                if (!arg.trim().isEmpty()) {
                    params.getVMParametersList().add(arg.trim());
                }
            }
        }
        
        // Add default VM arguments
        setupDefaultVMArgs(params);
        
        // Working directory is the project directory
        params.setWorkingDirectory(project.getBasePath());
        
        // Add program arguments
        if (configuration.getProgramArgs() != null && !configuration.getProgramArgs().isEmpty()) {
            for (String arg : configuration.getProgramArgs().split("\\s+")) {
                if (!arg.trim().isEmpty()) {
                    params.getProgramParametersList().add(arg.trim());
                }
            }
        }
        
        // Add run type specific arguments
        addRunTypeSpecificArgs(params);
        
        return params;
    }
    
    private Module findMainModule() {
        ModuleManager moduleManager = ModuleManager.getInstance(project);
        Module[] modules = moduleManager.getModules();
        
        // If there's only one module, use that
        if (modules.length == 1) {
            return modules[0];
        }
        
        // Look for a module that matches typical Minecraft mod patterns
        for (Module module : modules) {
            String moduleName = module.getName().toLowerCase();
            if (moduleName.contains("mod") || moduleName.contains("minecraft") || 
                moduleName.contains("forge") || moduleName.contains("fabric")) {
                return module;
            }
            
            // Also check for build.gradle or build.gradle.kts
            ModuleRootManager rootManager = ModuleRootManager.getInstance(module);
            VirtualFile[] contentRoots = rootManager.getContentRoots();
            for (VirtualFile root : contentRoots) {
                VirtualFile buildGradle = root.findChild("build.gradle");
                VirtualFile buildGradleKts = root.findChild("build.gradle.kts");
                if (buildGradle != null || buildGradleKts != null) {
                    // Check if it's a Minecraft project
                    if ((buildGradle != null && buildGradle.contentsToByteArray().toString().contains("minecraft")) ||
                        (buildGradleKts != null && buildGradleKts.contentsToByteArray().toString().contains("minecraft"))) {
                        return module;
                    }
                }
            }
        }
        
        // Default to the first module if we can't find a better match
        return modules.length > 0 ? modules[0] : null;
    }
    
    private String determineMainClass() {
        RunType runType = configuration.getRunType();
        
        // These would normally be detected from the build system,
        // but we're using some reasonable defaults for common cases
        switch (runType) {
            case CLIENT:
                return "net.minecraft.client.main.Main";
            case SERVER:
                return "net.minecraft.server.Main";
            case DATA_GEN:
                return "net.minecraft.data.Main";
            default:
                return "net.minecraft.client.main.Main";
        }
    }
    
    private void setupDefaultVMArgs(JavaParameters params) {
        // Common VM args for Minecraft
        params.getVMParametersList().add("-Xmx2G");
        params.getVMParametersList().add("-XX:+UseG1GC");
        params.getVMParametersList().add("-XX:+UnlockExperimentalVMOptions");
        params.getVMParametersList().add("-XX:G1NewSizePercent=20");
        params.getVMParametersList().add("-XX:G1ReservePercent=20");
        params.getVMParametersList().add("-XX:MaxGCPauseMillis=50");
        params.getVMParametersList().add("-XX:G1HeapRegionSize=32M");
        
        // Add debug parameters if debugging is enabled
        if (configuration.isEnableDebug()) {
            params.getVMParametersList().add("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005");
        }
    }
    
    private void addRunTypeSpecificArgs(JavaParameters params) {
        RunType runType = configuration.getRunType();
        
        switch (runType) {
            case CLIENT:
                // Client-specific arguments
                params.getProgramParametersList().add("--username");
                params.getProgramParametersList().add("Dev");
                params.getProgramParametersList().add("--version");
                params.getProgramParametersList().add("ModForge");
                params.getProgramParametersList().add("--gameDir");
                params.getProgramParametersList().add("run");
                params.getProgramParametersList().add("--assetsDir");
                params.getProgramParametersList().add(".gradle/caches/forge_gradle/assets");
                params.getProgramParametersList().add("--assetIndex");
                params.getProgramParametersList().add("1.19");  // This should be detected from project
                params.getProgramParametersList().add("--accessToken");
                params.getProgramParametersList().add("0");
                break;
                
            case SERVER:
                // Server-specific arguments
                params.getProgramParametersList().add("--nogui");
                break;
                
            case DATA_GEN:
                // Data generation arguments
                params.getProgramParametersList().add("--all");
                params.getProgramParametersList().add("--output");
                params.getProgramParametersList().add("src/generated/resources");
                break;
        }
    }
    
    @NotNull
    @Override
    public ExecutionResult execute(@NotNull Executor executor, @NotNull ProgramRunner<?> runner) throws ExecutionException {
        ProcessHandler processHandler = startProcess();
        ConsoleView console = createConsole(executor);
        if (console != null) {
            console.attachToProcess(processHandler);
            
            // Add some helpful info to the console
            RunType runType = configuration.getRunType();
            console.print("\n--- ModForge " + runType.getDisplayName() + " ---\n\n", ConsoleViewContentType.SYSTEM_OUTPUT);
            
            if (configuration.isEnableDebug()) {
                console.print("Debug mode enabled. Connect debugger to port 5005.\n\n", 
                        ConsoleViewContentType.SYSTEM_OUTPUT);
            }
        }
        
        return new DefaultExecutionResult(console, processHandler);
    }
}