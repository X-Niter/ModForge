# Potential Resolution Error Report 
 
This report identifies potential "Cannot resolve" errors for IntelliJ IDEA 2025.1.1.1. 
Generated on Sun 05/11/2025 22:48:14.51. 
 
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
 
## Potentially Problematic Method Calls 
 
Method calls causing resolution issues: 
 
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
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\AddFeaturesAction.java:91: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\AddFeaturesAction.java:106: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\CreateFromTemplateAction.java:494: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\EnhanceCodeAction.java:110: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\EnhanceCodeAction.java:113: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\ExplainCodeAction.java:49: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\ExplainCodeAction.java:63: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\FixErrorsAction.java:202: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\FixErrorsAction.java:204: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\FixErrorsAction.java:223: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\FixErrorsAction.java:268: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\GenerateDocumentationAction.java:52: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\GenerateDocumentationAction.java:64: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\GenerateImplementationAction.java:68: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\GenerateImplementationAction.java:83: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\GenerateImplementationAction.java:96: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ai\generation\MinecraftCodeGenerator.java:462: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ai\generation\MinecraftCodeGenerator.java:463: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\GenerateMinecraftCodeAction.java:83: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\GenerateMinecraftCodeAction.java:95: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ai\PatternRecognitionService.java:236: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\LoginAction.java:71: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\LoginAction.java:80: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\LogoutAction.java:42: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\LogoutAction.java:57: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\OptimizeCodeAction.java:51: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\PushToGitHubAction.java:75: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\PushToGitHubAction.java:107: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\PushToGitHubAction.java:137: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\PushToGitHubAction.java:177: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\PushToGitHubAction.java:179: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\StartCollaborationAction.java:50: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\StartCollaborationAction.java:106: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\StartCollaborationAction.java:110: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\collaboration\CollaborationService.java:163: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\collaboration\CollaborativeEditor.java:210: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\TestAuthEndpointsAction.java:30: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\TestAuthEndpointsAction.java:30: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\TestAuthEndpointsAction.java:41: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\TestAuthEndpointsAction.java:41: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\TestAuthEndpointsAction.java:42: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\TestCompleteAuthFlowAction.java:54: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\TestCompleteAuthFlowAction.java:96: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\TestCompleteAuthFlowAction.java:97: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\TestTokenAuthAction.java:26: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\ToggleContinuousDevelopmentAction.java:118: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\services\ContinuousDevelopmentService.java:61: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\services\ContinuousDevelopmentService.java:64: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\services\ContinuousDevelopmentService.java:64: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\services\ContinuousDevelopmentService.java:172: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\services\ContinuousDevelopmentService.java:227: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\services\ContinuousDevelopmentService.java:244: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\services\ContinuousDevelopmentService.java:252: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\services\ContinuousDevelopmentService.java:388: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\services\ContinuousDevelopmentService.java:730: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\TogglePatternRecognitionAction.java:90: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\TogglePatternRecognitionAction.java:103: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\TogglePatternRecognitionAction.java:125: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\VerifyAuthenticationAction.java:32: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\VerifyAuthenticationAction.java:38: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ai\AIServiceManager.java:172: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ai\AIServiceManager.java:173: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ai\AIServiceManager.java:174: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ai\AIServiceManager.java:175: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ai\AIServiceManager.java:215: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ai\AIServiceManager.java:218: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ai\AIServiceManager.java:218: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\auth\AuthenticationManager.java:36: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\auth\AuthenticationManager.java:122: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\auth\AuthenticationManager.java:123: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\auth\AuthenticationManager.java:135: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\auth\AuthenticationManager.java:148: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\auth\AuthenticationManager.java:149: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\auth\AuthenticationManager.java:154: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\auth\AuthenticationManager.java:155: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\auth\AuthenticationManager.java:171: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\auth\AuthenticationManager.java:171: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\auth\AuthenticationManager.java:176: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\auth\AuthenticationManager.java:180: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\auth\AuthenticationManager.java:181: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\auth\AuthenticationManager.java:182: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\auth\AuthenticationManager.java:198: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\auth\AuthenticationManager.java:198: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\auth\AuthenticationManager.java:200: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\auth\AuthenticationManager.java:201: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\auth\AuthenticationManager.java:202: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\auth\AuthenticationManager.java:207: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\auth\AuthenticationManager.java:211: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\auth\AuthenticationManager.java:212: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\auth\AuthenticationManager.java:213: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\collaboration\CollaborationWebSocketHandler.java:67: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\collaboration\CollaborationWebSocketHandler.java:70: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\collaboration\CollaborationWebSocketHandler.java:73: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\collaboration\CollaborationWebSocketHandler.java:76: error: cannot find symbol
--- End Code --- 
 
## Fix Recommendations 
 
1. **Update imports** with latest packages 
2. **Use CompatibilityUtil** on deprecated methods 
3. **Fix method signatures** correctly 
4. **Add missing methods** in service classes 
5. **Implement proper getInstance()** methods in services 
 
