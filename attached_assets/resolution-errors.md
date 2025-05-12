# Potential Resolution Error Report 
 
This report identifies potential "Cannot resolve" errors for IntelliJ IDEA 2025.1.1.1. 
Generated on Mon 05/12/2025 11:01:20.24. 
 
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
src\main\java\com\modforge\intellij\plugin\collaboration\CollaborationService.java:        return com.intellij.openapi.vfs.LocalFileSystem.getInstance().findFileByPath(path);
src\main\java\com\modforge\intellij\plugin\designers\advancement\AdvancementManager.java:            VirtualFile baseDir = LocalFileSystem.getInstance().findFileByPath(basePath);
src\main\java\com\modforge\intellij\plugin\designers\recipe\RecipeManager.java:            VirtualFile baseDir = LocalFileSystem.getInstance().findFileByPath(basePath);
src\main\java\com\modforge\intellij\plugin\designers\structure\StructureManager.java:            VirtualFile baseDir = LocalFileSystem.getInstance().findFileByPath(basePath);
src\main\java\com\modforge\intellij\plugin\utils\VirtualFileUtil.java:        return LocalFileSystem.getInstance().findFileByPath(path);
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
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\utils\CompatibilityUtil.java:459: error: cannot find symbol
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
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\listeners\ModForgeCompilationListener.java:63: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\listeners\ModForgeCompilationStatusListener.java:48: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\listeners\ModForgeCompilationStatusListener.java:154: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\listeners\ModForgeCompilationStatusListener.java:155: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\listeners\ModForgeCompilationStatusListener.java:166: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\listeners\ModForgeFileEditorManagerListener.java:107: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\listeners\ModForgeFileListener.java:61: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\listeners\ModForgeFileListener.java:66: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\listeners\ModForgeProjectListener.java:22: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\listeners\ModForgeProjectListener.java:25: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\listeners\ModForgeProjectListener.java:37: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\listeners\ModForgeProjectListener.java:42: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\listeners\ModForgeProjectListener.java:51: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\listeners\ModForgeProjectListener.java:54: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\listeners\ModForgeProjectManagerListener.java:28: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\listeners\ModForgeProjectManagerListener.java:37: error: cannot find symbol
--- End Code --- 
 
## Fix Recommendations 
 
1. **Update imports** with latest packages 
2. **Use CompatibilityUtil** on deprecated API calls 
3. **Fix API signatures** correctly 
4. **Add missing implementations** in service classes 
5. **Implement proper getInstance()** functions in services 
 
