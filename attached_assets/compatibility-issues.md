# ModForge IntelliJ IDEA 2025.1.1.1 Compatibility Issues 
 
This report contains potential compatibility issues found in the codebase. 
Generated on Sun 05/11/2025 17:26:30.58. 
 
## Overview 
 
This scan looks for deprecated APIs, problematic patterns, and compatibility issues 
that might affect the plugin's functionality with IntelliJ IDEA 2025.1.1.1. 
 
## Known Deprecated APIs and Replacements 
 
* **Project.getBaseDir()** - Replace with CompatibilityUtil.getProjectBaseDir(project) - Removed in 2020.3+ 
* **CacheUpdater** - Replace with CompatibilityUtil.refreshAll(project) - Removed in 2020.1+ 
* **CacheUpdaterFacade** - Replace with CompatibilityUtil.refreshAll(project) - Removed in 2020.1+ 
* **ApplicationManager.getApplication().runReadAction()** - Replace with CompatibilityUtil.runReadAction() 
* **ApplicationManager.getApplication().runWriteAction()** - Replace with CompatibilityUtil.runWriteAction() 
 
## Summary 
 
* Total Java files scanned: 225 
* Files with potential compatibility issues: 10 
* Total potential issues found: 18 
 
## Detailed Issue List 
 
