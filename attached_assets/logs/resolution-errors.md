# Potential Resolution Error Report 
 
This report identifies potential "Cannot resolve" errors for IntelliJ IDEA 2025.1.1.1. 
Generated on Mon 05/12/2025  2:06:05.18. 
 
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
--- End Code --- 
 
## Fix Recommendations 
 
1. **Update imports** with latest packages 
2. **Use CompatibilityUtil** on deprecated API calls 
3. **Fix API signatures** correctly 
4. **Add missing implementations** in service classes 
5. **Implement proper getInstance()** functions in services 
 
