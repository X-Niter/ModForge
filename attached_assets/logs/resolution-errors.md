# Potential Resolution Error Report 
 
This report identifies potential "Cannot resolve" errors for IntelliJ IDEA 2025.1.1.1. 
Generated on Mon 05/12/2025 15:25:25.21. 
 
## Overview 
 
This scan identifies problematic packages and classes: 
 
1. Relocated into other packages 
2. Renamed or significantly changed 
3. Removed entirely in IntelliJ IDEA 2025.1.1.1 
 
These references are likely to cause "Cannot resolve symbol" errors during compilation. 
 
## Potentially Problematic Imports 
 
Imports causing resolution issues: 
 
### References to ProjectManagerEx  
 
--- Java Code --- 
--- End Code --- 
 
Consider updating these imports with latest API. 
 
### References to ContentManagerEx  
 
--- Java Code --- 
--- End Code --- 
 
Consider updating these imports with latest API. 
 
### References to ToolWindowManagerEx  
 
--- Java Code --- 
--- End Code --- 
 
Consider updating these imports with latest API. 
 
### References to FileEditorManagerEx  
 
--- Java Code --- 
--- End Code --- 
 
Consider updating these imports with latest API. 
 
### References to NotificationGroup  
 
--- Java Code --- 
--- End Code --- 
 
Consider updating these imports with latest API. 
 
### References to JBPopupFactoryImpl  
 
--- Java Code --- 
--- End Code --- 
 
Consider updating these imports with latest API. 
 
### References to IntentionActionDelegate  
 
--- Java Code --- 
--- End Code --- 
 
Consider updating these imports with latest API. 
 
### References to GotoActionBase  
 
--- Java Code --- 
--- End Code --- 
 
Consider updating these imports with latest API. 
 
### References to RefreshAction  
 
--- Java Code --- 
--- End Code --- 
 
Consider updating these imports with latest API. 
 
### References to ProjectFileIndex  
 
--- Java Code --- 
--- End Code --- 
 
Consider updating these imports with latest API. 
 
### References to PluginId  
 
--- Java Code --- 
--- End Code --- 
 
Consider updating these imports with latest API. 
 
### References to ExtensionsArea  
 
--- Java Code --- 
--- End Code --- 
 
Consider updating these imports with latest API. 
 
### References to JavaPsiFacade  
 
--- Java Code --- 
--- End Code --- 
 
Consider updating these imports with latest API. 
 
### References to ClassUtil  
 
--- Java Code --- 
--- End Code --- 
 
Consider updating these imports with latest API. 
 
### References to VirtualFileManager  
 
--- Java Code --- 
--- End Code --- 
 
Consider updating these imports with latest API. 
 
### References to PsiUtils  
 
--- Java Code --- 
--- End Code --- 
 
Consider updating these imports with latest API. 
 
### References to PsiUtil  
 
--- Java Code --- 
--- End Code --- 
 
Consider updating these imports with latest API. 
 
### References to PsiTreeUtil  
 
--- Java Code --- 
--- End Code --- 
 
Consider updating these imports with latest API. 
 
### References to FileChooserDescriptor  
 
--- Java Code --- 
--- End Code --- 
 
Consider updating these imports with latest API. 
 
### References to LightVirtualFile  
 
--- Java Code --- 
--- End Code --- 
 
Consider updating these imports with latest API. 
 
### References to CoreLocalVirtualFile  
 
--- Java Code --- 
--- End Code --- 
 
Consider updating these imports with latest API. 
 
### References to JavaDirectoryService  
 
--- Java Code --- 
--- End Code --- 
 
Consider updating these imports with latest API. 
 
### References to PsiElementFactory  
 
--- Java Code --- 
--- End Code --- 
 
Consider updating these imports with latest API. 
 
### References to XmlElementFactory  
 
--- Java Code --- 
--- End Code --- 
 
Consider updating these imports with latest API. 
 
## Potentially Problematic API Calls 
 
API calls causing resolution issues: 
 
### Calls to getBaseDir 
 
--- Java Code --- 
src\main\java\com\modforge\intellij\plugin\utils\CompatibilityUtil.java:     * Replacement for deprecated Project.getBaseDir()
--- End Code --- 
 
**Suggested fix:** Use CompatibilityUtil.getProjectBaseDir(project
 
### Calls to findFileByPath 
 
--- Java Code --- 
src\main\java\com\modforge\intellij\plugin\collaboration\CollaborationService.java:        return com.modforge.intellij.plugin.utils.CompatibilityUtil.findFileByPath(path);
src\main\java\com\modforge\intellij\plugin\designers\advancement\AdvancementManager.java:            VirtualFile baseDir = CompatibilityUtil.findFileByPath(basePath);
src\main\java\com\modforge\intellij\plugin\designers\advancement\AdvancementManager.java:            CompatibilityUtil.findFileByPath(outputPath);
src\main\java\com\modforge\intellij\plugin\designers\recipe\RecipeManager.java:            VirtualFile baseDir = CompatibilityUtil.findFileByPath(basePath);
src\main\java\com\modforge\intellij\plugin\designers\structure\StructureManager.java:            VirtualFile baseDir = CompatibilityUtil.findFileByPath(basePath);
src\main\java\com\modforge\intellij\plugin\utils\CompatibilityUtil.java:     * Use this method instead of directly calling LocalFileSystem.getInstance().findFileByPath().
src\main\java\com\modforge\intellij\plugin\utils\CompatibilityUtil.java:                return LocalFileSystem.getInstance().findFileByPath(path);
src\main\java\com\modforge\intellij\plugin\utils\VirtualFileUtil.java:        return CompatibilityUtil.findFileByPath(path);
--- End Code --- 
 
 
### Calls to getInstanceEx 
 
--- Java Code --- 
--- End Code --- 
 
 
### Calls to getFileSystem 
 
--- Java Code --- 
--- End Code --- 
 
 
### Calls to resolveFile 
 
--- Java Code --- 
--- End Code --- 
 
 
Static analysis found potential resolution issues. 
## Resolution Errors from Build Log 
 
Resolution errors found during compilation: 
 
--- Java Code --- 
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\StartCollaborationAction.java:41: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\TogglePatternRecognitionAction.java:164: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\auth\AuthenticationManager.java:123: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\auth\AuthenticationManager.java:135: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\auth\AuthenticationManager.java:148: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\auth\AuthenticationManager.java:154: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\auth\AuthenticationManager.java:176: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\auth\AuthenticationManager.java:180: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\auth\AuthenticationManager.java:182: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\auth\AuthenticationManager.java:200: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\auth\AuthenticationManager.java:202: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\auth\AuthenticationManager.java:207: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\auth\AuthenticationManager.java:211: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\auth\AuthenticationManager.java:213: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\collaboration\CollaborationService.java:666: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\collaboration\CollaborativeEditor.java:103: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\collaboration\CollaborativeEditor.java:109: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\collaboration\CollaborativeEditor.java:130: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\collaboration\CollaborativeEditor.java:131: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\collaboration\CollaborativeEditor.java:132: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\collaboration\CollaborativeEditor.java:132: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\collaboration\CollaborativeEditor.java:135: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\collaboration\CollaborativeEditor.java:136: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\collaboration\CollaborativeEditor.java:136: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\collaboration\CollaborativeEditor.java:136: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\collaboration\CollaborativeEditor.java:139: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\collaboration\CollaborativeEditor.java:142: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\collaboration\CollaborativeEditor.java:142: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\collaboration\CollaborativeEditor.java:141: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\collaboration\CollaborativeEditor.java:143: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\collaboration\CollaborativeEditor.java:148: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\collaboration\CollaborativeEditor.java:212: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\collaboration\CollaborativeEditor.java:247: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\collaboration\WebSocketMessage.java:196: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\collaboration\CollaborationWebSocketHandler.java:169: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\collaboration\CollaborationWebSocketHandler.java:172: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\collaboration\CollaborationWebSocketHandler.java:175: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\collaboration\ui\CollaborationPanel.java:63: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\collaboration\ui\CollaborationPanel.java:214: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\collaboration\ui\CollaborationPanel.java:231: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\collaboration\ui\CollaborationPanel.java:277: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\collaboration\ui\CollaborationPanel.java:322: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\collaboration\ui\CollaborationPanel.java:349: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\collaboration\ui\CollaborationPanel.java:426: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\crossloader\actions\CreateCrossLoaderModAction.java:68: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\crossloader\ArchitecturyService.java:169: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\crossloader\ArchitecturyService.java:197: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\crossloader\ui\CrossLoaderProjectSetupDialog.java:811: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\crossloader\ui\CrossLoaderProjectSetupDialog.java:830: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\crossloader\intentions\ConvertToPlatformSpecificIntention.java:55: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\crossloader\ui\CrossLoaderPanel.java:137: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\debug\MinecraftDebuggerExtension.java:100: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\debug\MinecraftDebuggerExtension.java:145: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\debug\MinecraftDebugService.java:58: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\debug\MinecraftPerformanceMonitor.java:98: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\designers\advancement\ui\AdvancementDesignerPanel.java:580: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\github\GitHubIntegrationService.java:720: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\github\GitHubIntegrationService.java:958: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\listeners\ModForgeCompilationListener.java:77: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\listeners\ModForgeCompilationStatusListener.java:154: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\listeners\ModForgeCompilationStatusListener.java:155: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\listeners\ModForgeCompilationStatusListener.java:166: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\listeners\ModForgeFileEditorManagerListener.java:107: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\listeners\ModForgeFileListener.java:68: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\listeners\ModForgeFileListener.java:101: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\listeners\ModForgeProjectListener.java:23: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\listeners\ModForgeProjectListener.java:26: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\listeners\ModForgeProjectListener.java:40: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\listeners\ModForgeProjectListener.java:43: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\listeners\ModForgeProjectListener.java:52: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\listeners\ModForgeProjectListener.java:55: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\listeners\ModForgeProjectManagerListener.java:28: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\listeners\ModForgeProjectManagerListener.java:37: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\integration\MemoryAwareServiceIntegration.java:103: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\integration\MemoryAwareServiceIntegration.java:104: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\integration\MemoryAwareServiceIntegration.java:199: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\integration\MemoryAwareServiceIntegration.java:200: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\monitoring\MemoryHealthMonitor.java:156: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\monitoring\MemoryHealthMonitor.java:200: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\monitoring\MemoryHealthMonitor.java:225: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\monitoring\MemoryHealthMonitor.java:270: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\recovery\MemoryRecoveryManager.java:185: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\recovery\MemoryRecoveryManager.java:374: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\recovery\MemoryRecoveryManager.java:469: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\recovery\MemoryRecoveryManager.java:538: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\MemoryManagementStartupActivity.java:44: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\MemoryManagementStartupActivity.java:111: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\MemoryManagementStartupActivity.java:112: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\MemoryOptimizer.java:218: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\MemoryOptimizer.java:358: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\monitoring\ui\MemoryMonitorPanel.java:336: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\monitoring\ui\MemoryMonitorPanel.java:337: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\monitoring\ui\MemoryMonitorPanel.java:338: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\monitoring\ui\MemoryMonitorPanel.java:339: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\monitoring\ui\MemoryMonitorPanel.java:340: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\monitoring\ui\MemoryMonitorPanel.java:341: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\monitoring\ui\MemoryMonitorPanel.java:342: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\monitoring\ui\MemoryMonitorPanel.java:365: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\monitoring\ui\MemoryTrendChart.java:213: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\monitoring\ui\MemoryTrendChart.java:323: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\monitoring\ui\MemoryTrendChart.java:351: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\monitoring\ui\MemoryTrendChart.java:355: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\monitoring\ui\MemoryTrendChart.java:357: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\monitoring\ui\MemoryTrendChart.java:359: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\monitoring\ui\MemoryTrendChart.java:466: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\monitoring\ui\MemoryTrendChart.java:468: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\monitoring\ui\MemoryTrendChart.java:468: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\monitoring\ui\MemoryTrendChart.java:490: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\monitoring\ui\MemoryTrendChart.java:491: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\monitoring\ui\MemoryTrendChart.java:491: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\monitoring\ui\MemoryTrendChart.java:533: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\monitoring\ui\MemoryTrendChart.java:575: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\monitoring\ui\MemoryTrendChart.java:585: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\ui\MemoryDetailsDialog.java:61: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\ui\MemoryDetailsDialog.java:388: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\ui\MemoryDetailsDialog.java:400: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\ui\MemoryDetailsDialog.java:404: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\ui\MemoryInfoPanel.java:62: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\ui\MemoryInfoPanel.java:136: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\ui\MemoryInfoPanel.java:174: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\ui\MemoryInfoPanel.java:210: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\ui\MemoryStatusBarWidget.java:80: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\ui\MemoryStatusBarWidget.java:85: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\ui\MemoryStatusBarWidget.java:94: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\ui\MemoryStatusBarWidget.java:104: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\ui\MemoryStatusBarWidget.java:156: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\ui\MemoryStatusWidget.java:44: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\ui\MemoryStatusWidget.java:221: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\visualization\MemoryVisualizationPanel.java:245: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\visualization\MemoryVisualizationPanel.java:304: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\visualization\MemoryVisualizationPanel.java:305: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\visualization\MemoryVisualizationPanel.java:306: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\visualization\MemoryVisualizationPanel.java:307: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\visualization\MemoryVisualizationPanel.java:321: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\visualization\MemoryVisualizationPanel.java:656: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\visualization\MemoryVisualizationPanel.java:657: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\visualization\MemoryVisualizationPanel.java:658: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\visualization\MemoryVisualizationPanel.java:659: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\visualization\MemoryVisualizationPanel.java:714: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\visualization\MemoryVisualizationPanel.java:720: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\visualization\MemoryVisualizationPanel.java:726: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\visualization\MemoryVisualizationPanel.java:732: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\visualization\MemoryVisualizationPanel.java:738: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\visualization\MemoryVisualizationPanel.java:744: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\visualization\MemoryVisualizationPanel.java:786: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\visualization\MemoryVisualizationPanel.java:1328: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\visualization\MemoryVisualizationPanel.java:1333: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\visualization\MemoryVisualizationPanel.java:1338: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\visualization\MemoryVisualizationPanel.java:1343: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\visualization\MemoryVisualizationPanel.java:1350: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\visualization\MemoryVisualizationToolWindowFactory.java:61: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\memory\visualization\MemoryVisualizationToolWindowFactory.java:61: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ml\ErrorPatternDatabase.java:172: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ml\ErrorPatternDatabase.java:189: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ml\ErrorPatternDatabase.java:248: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ml\ErrorPatternDatabase.java:322: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ModForgePluginActivator.java:39: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\recommendation\PremiumFeatureInjector.java:97: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\run\MinecraftRunConfigurationService.java:70: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\services\AIServiceManager.java:312: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\services\AIServiceManager.java:312: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\services\AIServiceManager.java:320: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\services\AutomatedRefactoringService.java:191: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\services\AutomatedRefactoringService.java:602: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\services\AutomatedRefactoringService.java:620: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\services\AutonomousCodeGenerationService.java:449: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\services\AutonomousCodeGenerationService.java:450: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\services\AutonomousCodeGenerationService.java:521: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\services\AutonomousCodeGenerationService.java:522: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\services\AutonomousCodeGenerationService.java:1090: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\services\AutonomousCodeGenerationService.java:1091: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\settings\ModForgeSettings.java:415: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\settings\ModForgeSettings.java:417: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\settings\ModForgeSettings.java:418: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\settings\ModForgeSettings.java:419: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\settings\ModForgeSettings.java:421: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\settings\ModForgeSettings.java:422: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\services\AutonomousIdeCoordinatorService.java:100: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\services\AutonomousIdeCoordinatorService.java:100: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\services\AutonomousIdeCoordinatorService.java:106: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\services\AutonomousIdeCoordinatorService.java:110: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\services\AutonomousIdeCoordinatorService.java:113: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\services\AutonomousIdeCoordinatorService.java:146: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\services\AutonomousIdeCoordinatorService.java:256: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\services\AutonomousIdeCoordinatorService.java:257: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\services\AutonomousIdeCoordinatorService.java:260: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\services\AutonomousIdeCoordinatorService.java:263: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\services\AutonomousIdeCoordinatorService.java:268: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\services\AutonomousIdeCoordinatorService.java:271: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\services\AutonomousIdeCoordinatorService.java:272: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\services\AutonomousIdeCoordinatorService.java:276: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\services\AutonomousIdeCoordinatorService.java:286: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\services\AutonomousIdeCoordinatorService.java:301: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\services\AutonomousIdeCoordinatorService.java:307: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\services\AutonomousIdeCoordinatorService.java:314: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\services\AutonomousIdeCoordinatorService.java:315: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\services\AutonomousIdeCoordinatorService.java:319: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\services\AutonomousIdeCoordinatorService.java:326: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\services\AutonomousIdeCoordinatorService.java:327: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\services\AutonomousIdeCoordinatorService.java:331: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\services\AutonomousIdeCoordinatorService.java:338: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\services\AutonomousIdeCoordinatorService.java:339: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\services\AutonomousIdeCoordinatorService.java:343: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\services\AutonomousIdeCoordinatorService.java:352: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\services\AutonomousIdeCoordinatorService.java:371: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\services\AutonomousIdeCoordinatorService.java:372: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\services\AutonomousIdeCoordinatorService.java:382: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\services\AutonomousIdeCoordinatorService.java:382: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\services\AutonomousIdeCoordinatorService.java:384: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\services\AutonomousIdeCoordinatorService.java:388: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\services\AutonomousIdeCoordinatorService.java:388: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\services\AutonomousIdeCoordinatorService.java:392: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\services\IDEIntegrationService.java:464: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\services\ErrorResolutionService.java:327: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\services\GitIntegrationService.java:63: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\services\MemoryAwareContinuousService.java:153: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\services\ModForgeProjectService.java:46: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\settings\ModForgeSettingsComponent.java:531: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\settings\ModForgeSettingsComponent.java:533: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\settings\ModForgeSettingsComponent.java:534: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\settings\ModForgeSettingsComponent.java:535: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\settings\ModForgeSettingsComponent.java:537: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\settings\ModForgeSettingsComponent.java:538: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\settings\ModForgeSettingsComponent.java:539: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\settings\ModForgeSettingsComponent.java:540: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\settings\ModForgeSettingsConfigurable.java:43: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\settings\ModForgeSettingsConfigurable.java:45: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\settings\ModForgeSettingsConfigurable.java:47: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\settings\ModForgeSettingsConfigurable.java:48: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\settings\ModForgeSettingsConfigurable.java:50: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\settings\ModForgeSettingsConfigurable.java:51: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\settings\ModForgeSettingsConfigurable.java:52: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\settings\ModForgeSettingsConfigurable.java:53: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\settings\ModForgeSettingsConfigurable.java:91: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\settings\ModForgeSettingsConfigurable.java:95: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\settings\ModForgeSettingsConfigurable.java:96: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\settings\ModForgeSettingsConfigurable.java:98: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\settings\ModForgeSettingsConfigurable.java:99: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\settings\ModForgeSettingsConfigurable.java:100: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\settings\ModForgeSettingsConfigurable.java:101: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\settings\ModForgeSettingsConfigurable.java:112: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\settings\ModForgeSettingsConfigurable.java:114: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\settings\ModForgeSettingsConfigurable.java:116: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\settings\ModForgeSettingsConfigurable.java:117: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\settings\ModForgeSettingsConfigurable.java:119: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\settings\ModForgeSettingsConfigurable.java:120: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\settings\ModForgeSettingsConfigurable.java:121: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\settings\ModForgeSettingsConfigurable.java:122: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\startup\AuthenticationStartupActivity.java:34: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\startup\AuthenticationStartupActivity.java:41: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\startup\AuthenticationStartupActivity.java:45: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\templates\ModTemplateService.java:350: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\templates\ModTemplateService.java:351: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\templates\ModTemplateService.java:355: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\toolwindow\CodeGeneratorPanel.java:182: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\toolwindow\CodeGeneratorPanel.java:183: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\toolwindow\CodeGeneratorPanel.java:190: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\toolwindow\ModForgeToolWindowContent.java:50: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\toolwindow\ModForgeToolWindowContent.java:51: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\toolwindow\ModForgeToolWindowContent.java:52: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\toolwindow\ModForgeToolWindowContent.java:53: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\toolwindow\ModForgeToolWindowContent.java:68: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\toolwindow\ModForgeToolWindowFactory.java:56: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\toolwindow\ModForgeToolWindowFactory.java:56: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\toolwindow\ModForgeToolWindowFactory.java:65: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\toolwindow\ModForgeToolWindowFactory.java:65: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\toolwindow\ModForgeToolWindowFactory.java:74: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\toolwindow\ModForgeToolWindowFactory.java:74: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\toolwindow\ModForgeToolWindowFactory.java:101: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\toolwindow\ModForgeToolWindowFactory.java:101: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\dialog\LoginDialog.java:80: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\dialog\LoginDialog.java:109: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\dialogs\GenerateImplementationDialog.java:207: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\login\LoginDialog.java:53: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\login\LoginDialog.java:57: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\login\LoginDialog.java:175: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\ModForgeToolWindowFactory.java:68: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\ModForgeToolWindowFactory.java:68: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\ModForgeToolWindowFactory.java:274: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\ModForgeToolWindowFactory.java:414: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\ModForgeToolWindowFactory.java:529: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\ModForgeToolWindowFactory.java:538: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\PremiumFeatureNotificationPanel.java:74: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\toolwindow\AIAssistPanel.java:197: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\toolwindow\AIAssistPanel.java:227: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\toolwindow\MetricsPanel.java:49: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\toolwindow\MetricsPanel.java:87: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\toolwindow\MetricsPanel.java:115: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\toolwindow\MetricsPanel.java:139: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\toolwindow\MetricsPanel.java:190: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\toolwindow\MetricsPanel.java:254: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\toolwindow\ModForgeToolWindowContent.java:173: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\toolwindow\ModForgeToolWindowPanel.java:274: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\toolwindow\ModForgeToolWindowPanel.java:297: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\toolwindow\ModForgeToolWindowPanel.java:373: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\toolwindow\ModForgeToolWindowPanel.java:375: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\toolwindow\ModForgeToolWindowPanel.java:386: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\toolwindow\ModForgeToolWindowPanel.java:388: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\toolwindow\ModForgeToolWindowPanel.java:398: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\toolwindow\ModForgeToolWindowPanel.java:403: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\toolwindow\ModForgeToolWindowPanel.java:407: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\toolwindow\ModForgeToolWindowPanel.java:424: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\toolwindow\ModForgeToolWindowPanel.java:425: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\toolwindow\ModForgeToolWindowPanel.java:426: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\toolwindow\ModForgeToolWindowPanel.java:436: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\toolwindow\ModForgeToolWindowPanel.java:437: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\toolwindow\ModForgeToolWindowPanel.java:443: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\toolwindow\ModForgeToolWindowPanel.java:446: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\toolwindow\panels\AIAssistPanel.java:119: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\toolwindow\panels\AIAssistPanel.java:350: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\toolwindow\panels\AIAssistPanel.java:443: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\toolwindow\panels\MetricsPanel.java:181: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\toolwindow\panels\MetricsPanel.java:182: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\toolwindow\panels\MetricsPanel.java:183: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\toolwindow\panels\MetricsPanel.java:184: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\toolwindow\panels\MetricsPanel.java:185: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\toolwindow\panels\ModGenerationPanel.java:57: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\toolwindow\panels\ModGenerationPanel.java:60: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\toolwindow\panels\SettingsPanel.java:180: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\toolwindow\panels\SettingsPanel.java:181: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\toolwindow\panels\SettingsPanel.java:193: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\toolwindow\panels\SettingsPanel.java:194: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\toolwindow\SettingsPanel.java:50: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\toolwindow\SettingsPanel.java:110: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\toolwindow\SettingsPanel.java:111: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\toolwindow\SettingsPanel.java:155: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\toolwindow\SettingsPanel.java:156: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\toolwindow\SettingsPanel.java:157: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\toolwindow\SettingsPanel.java:180: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\toolwindow\SettingsPanel.java:181: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\toolwindow\SettingsPanel.java:182: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\toolwindow\SettingsPanel.java:196: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\toolwindow\SettingsPanel.java:197: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\toolwindow\SettingsPanel.java:200: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\toolwindow\SettingsPanel.java:218: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\toolwindow\SettingsPanel.java:219: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\toolwindow\SettingsPanel.java:220: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\toolwindow\SettingsPanel.java:223: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\toolwindow\SettingsPanel.java:224: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\toolwindow\SettingsPanel.java:225: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\toolwindow\SettingsPanel.java:233: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\toolwindow\SettingsPanel.java:234: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\toolwindow\SettingsPanel.java:258: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\toolwindow\SettingsPanel.java:259: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\toolwindow\SettingsPanel.java:261: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\toolwindow\SettingsPanel.java:262: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\toolwindow\SettingsPanel.java:263: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\toolwindow\SettingsPanel.java:266: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\toolwindow\SettingsPanel.java:267: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\toolwindow\SettingsPanel.java:268: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\toolwindow\SettingsPanel.java:271: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\toolwindow\ToolWindowCleanupListener.java:36: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\utils\AuthTestUtil.java:129: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\utils\AuthTestUtil.java:170: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\utils\CompatibilityUtil.java:192: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\utils\CompatibilityUtil.java:211: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\utils\CompatibilityUtil.java:377: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\utils\CompatibilityUtil.java:656: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\utils\CompatibilityUtil.java:657: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\utils\CompatibilityUtil.java:879: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\utils\CompatibilityUtil.java:907: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\utils\DialogUtils.java:50: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\utils\LibraryAnalyzer.java:67: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\utils\LibraryAnalyzer.java:223: error: cannot find symbol
--- End Code --- 
 
## Fix Recommendations 
 
1. **Update imports** with latest packages 
2. **Use CompatibilityUtil** on deprecated API calls 
3. **Fix API signatures** correctly 
4. **Add missing implementations** in service classes 
5. **Implement proper getInstance()** functions in services 
 
