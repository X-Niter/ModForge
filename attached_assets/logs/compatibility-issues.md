# ModForge IntelliJ IDEA 2025.1.1.1 Compatibility Issues 
 
This report contains potential compatibility issues found in the codebase. 
Generated on Mon 05/12/2025  2:45:20.54. 
 
## Overview 
 
This scan looks for deprecated APIs, problematic patterns, and compatibility issues 
that might affect plugin functionality for IntelliJ IDEA 2025.1.1.1. 
 
## Known Deprecated APIs and Replacements 
 
* **Project.getBaseDir()** - Use CompatibilityUtil.getProjectBaseDir(project) - Removed in 2020.3+ 
* **CacheUpdater** - Use CompatibilityUtil.refreshAll(project) - Removed in 2020.1+ 
* **CacheUpdaterFacade** - Use CompatibilityUtil.refreshAll(project) - Removed in 2020.1+ 
* **ApplicationManager.getApplication().runReadAction()** - Use CompatibilityUtil.runReadAction() 
* **ApplicationManager.getApplication().runWriteAction()** - Use CompatibilityUtil.runWriteAction() 
 
## Summary 
 
* Total Java files scanned: 234 
* Files with potential compatibility issues: 9 
* Total potential issues found: 12 
 
## Detailed Issue List 
 
### Issue in G 
 
* Potential problem: \JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\FixErrorsAction.java 
 
### Issue in G 
 
* Potential problem: \JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\actions\GenerateCodeAction.java 
 
### Issue in G 
 
* Potential problem: \JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\collaboration\CollaborativeEditor.java 
 
### Issue in G 
 
* Potential problem: \JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\services\AutomatedRefactoringService.java 
 
### Issue in G 
 
* Potential problem: \JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\services\AutomatedRefactoringService.java 
 
### Issue in G 
 
* Potential problem: \JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\services\ContinuousDevelopmentService.java 
 
### Issue in G 
 
* Potential problem: \JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\services\IDEIntegrationService.java 
 
### Issue in G 
 
* Potential problem: \JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\services\IDEIntegrationService.java 
 
### Issue in G 
 
* Potential problem: \JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\toolwindow\AIAssistPanel.java 
 
### Issue in G 
 
* Potential problem: \JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\toolwindow\panels\AIAssistPanel.java 
 
### Issue in G 
 
* Potential problem: \JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\ui\toolwindow\panels\AIAssistPanel.java 
 
### Issue in G 
 
* Potential problem: \JavaMinecraftProjects\ModForge\modforge-intellij-plugin\src\main\java\com\modforge\intellij\plugin\utils\CompatibilityUtil.java 
 
