# Potential Resolution Error Report 
 
This report identifies potential "Cannot resolve" errors for IntelliJ IDEA 2025.1.1.1. 
Generated on Mon 05/12/2025  2:45:37.95. 
 
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
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\GenerateMinecraftCodeAction.java:21: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ai\generation\MinecraftCodeGenerator.java:14: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ai\generation\MinecraftCodeGenerator.java:51: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ai\generation\MinecraftCodeGenerator.java:92: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ai\generation\MinecraftCodeGenerator.java:204: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ai\generation\MinecraftCodeGenerator.java:258: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ai\generation\MinecraftCodeGenerator.java:278: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ai\generation\MinecraftCodeGenerator.java:351: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ai\generation\MinecraftCodeGenerator.java:420: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ai\generation\MinecraftCodeGenerator.java:537: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ai\generation\MinecraftCodeGenerator.java:541: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ai\generation\MinecraftCodeGenerator.java:566: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\services\AutonomousCodeGenerationService.java:13: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\ExplainCodeAction.java:62: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\FixErrorsAction.java:206: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\FixErrorsAction.java:230: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\FixErrorsAction.java:236: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\FixErrorsAction.java:240: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\FixErrorsAction.java:248: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\FixErrorsAction.java:249: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\GenerateCodeAction.java:51: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\GenerateDocumentationAction.java:64: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\GenerateMinecraftCodeAction.java:81: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\GenerateMinecraftCodeAction.java:93: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\GenerateMinecraftCodeAction.java:130: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\GenerateMinecraftCodeAction.java:131: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\GenerateMinecraftCodeAction.java:132: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\LogoutAction.java:57: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\PushToGitHubAction.java:106: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\PushToGitHubAction.java:136: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\StartCollaborationAction.java:52: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\StartCollaborationAction.java:108: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\StartCollaborationAction.java:112: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\StartCollaborationAction.java:176: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\StartCollaborationAction.java:240: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\collaboration\CollaborationService.java:165: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\collaboration\CollaborationService.java:180: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\collaboration\CollaborationService.java:675: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\collaboration\CollaborativeEditor.java:211: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\TestAuthEndpointsAction.java:30: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\TestAuthEndpointsAction.java:41: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\TestAuthEndpointsAction.java:41: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\TestAuthEndpointsAction.java:42: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\TestCompleteAuthFlowAction.java:54: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\TestCompleteAuthFlowAction.java:93: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\TestCompleteAuthFlowAction.java:96: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\TestCompleteAuthFlowAction.java:97: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\ToggleContinuousDevelopmentAction.java:118: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\services\ContinuousDevelopmentService.java:63: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\services\ContinuousDevelopmentService.java:66: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\services\ContinuousDevelopmentService.java:66: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\services\ContinuousDevelopmentService.java:174: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\services\ContinuousDevelopmentService.java:229: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\services\ContinuousDevelopmentService.java:246: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\services\ContinuousDevelopmentService.java:390: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\TogglePatternRecognitionAction.java:90: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\TogglePatternRecognitionAction.java:103: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\TogglePatternRecognitionAction.java:125: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\VerifyAuthenticationAction.java:32: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ai\AIServiceManager.java:170: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ai\AIServiceManager.java:171: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ai\AIServiceManager.java:172: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ai\AIServiceManager.java:173: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ai\AIServiceManager.java:213: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ai\AIServiceManager.java:216: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ai\AIServiceManager.java:216: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ai\generation\MinecraftCodeGenerator.java:73: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ai\generation\MinecraftCodeGenerator.java:73: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ai\generation\MinecraftCodeGenerator.java:74: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ai\generation\MinecraftCodeGenerator.java:151: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ai\generation\MinecraftCodeGenerator.java:208: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ai\generation\MinecraftCodeGenerator.java:215: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ai\generation\MinecraftCodeGenerator.java:218: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ai\generation\MinecraftCodeGenerator.java:220: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ai\generation\MinecraftCodeGenerator.java:455: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ai\PatternRecognitionService.java:236: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\auth\AuthenticationManager.java:35: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\auth\AuthenticationManager.java:36: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\auth\AuthenticationManager.java:123: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\auth\AuthenticationManager.java:135: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\auth\AuthenticationManager.java:148: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\auth\AuthenticationManager.java:154: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\auth\AuthenticationManager.java:171: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\auth\AuthenticationManager.java:176: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\auth\AuthenticationManager.java:180: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\auth\AuthenticationManager.java:182: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\auth\AuthenticationManager.java:198: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\auth\AuthenticationManager.java:200: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\auth\AuthenticationManager.java:202: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\auth\AuthenticationManager.java:207: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\auth\AuthenticationManager.java:211: error: cannot find symbol
G:\JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\auth\AuthenticationManager.java:213: error: cannot find symbol
--- End Code --- 
 
## Fix Recommendations 
 
1. **Update imports** with latest packages 
2. **Use CompatibilityUtil** on deprecated API calls 
3. **Fix API signatures** correctly 
4. **Add missing implementations** in service classes 
5. **Implement proper getInstance()** functions in services 
 
