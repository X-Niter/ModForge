@echo off
setlocal enabledelayedexpansion

REM ===================================
REM ModForge Compatibility Scanner
REM ===================================
REM
REM This script proactively scans the entire codebase for IntelliJ API compatibility issues
REM with IntelliJ IDEA 2025.1.1.1 (Build #IC-251.25410.129).
REM
REM Usage: scan-compatibility-issues.bat [output_file]
REM   - If no output_file is provided, it will use "compatibility-issues.md"

REM Default values
set "OUTPUT_FILE=compatibility-issues.md"
set "SOURCE_DIR=src\main\java"
set "TEMP_MATCHES=%TEMP%\compatibility_matches.txt"
set "TEMP_FILES=%TEMP%\compatibility_files.txt"

REM Colors for Windows cmd
set "GREEN=[92m"
set "YELLOW=[93m"
set "RED=[91m"
set "BLUE=[94m"
set "NC=[0m"

REM Check if output file is provided
if not "%~1"=="" (
    set "OUTPUT_FILE=%~1"
    echo %BLUE%Will write output to: %OUTPUT_FILE%%NC%
)

REM Check if the source directory exists
if not exist "%SOURCE_DIR%" (
    echo %RED%Source directory does not exist: %SOURCE_DIR%%NC%
    exit /b 1
)

echo %BLUE%==== Scanning codebase for compatibility issues ====%NC%

REM Create output file with header
> "%OUTPUT_FILE%" (
    echo # ModForge IntelliJ IDEA 2025.1.1.1 Compatibility Issues
    echo.
    echo This report contains potential compatibility issues found in the codebase.
    echo Generated on %DATE% %TIME%.
    echo.
    echo ## Overview
    echo.
    echo This scan looks for deprecated APIs, problematic patterns, and compatibility issues
    echo that might affect the plugin's functionality with IntelliJ IDEA 2025.1.1.1.
    echo.
    echo ## Known Deprecated APIs and Replacements
    echo.
    echo ^| Deprecated API ^| Replacement ^| Compatibility Note ^|
    echo ^|----------------|-------------|-------------------|
    echo ^| `Project.getBaseDir()` ^| `CompatibilityUtil.getProjectBaseDir(project)` ^| Removed in 2020.3+ ^|
    echo ^| `CacheUpdater` ^| `CompatibilityUtil.refreshAll(project)` ^| Removed in 2020.1+ ^|
    echo ^| `CacheUpdaterFacade` ^| `CompatibilityUtil.refreshAll(project)` ^| Removed in 2020.1+ ^|
    echo ^| `ApplicationManager.getApplication().runReadAction()` ^| `CompatibilityUtil.runReadAction()` ^| Better API in 2020.3+ ^|
    echo ^| `ApplicationManager.getApplication().runWriteAction()` ^| `CompatibilityUtil.runWriteAction()` ^| Better API in 2020.3+ ^|
    echo ^| `PsiElementFactory.getInstance(project).createXmlTag()` ^| `XmlElementFactory.getInstance(project).createTagFromText()` ^| Changed in 2020.2+ ^|
    echo ^| `FileEditorManager.getSelectedTextEditor()` ^| `CompatibilityUtil.getSelectedTextEditor(project)` ^| Better null handling ^|
    echo ^| `VirtualFileManager.getInstance().getFileSystem()` ^| `VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL)` ^| Changed in 2020.1+ ^|
    echo ^| `ActionPlaces.*` constants ^| `com.intellij.openapi.actionSystem.ActionPlaces` ^| Relocated in 2020.3+ ^|
    echo.
)

REM Known API patterns to search for
set "API_PATTERNS=getBaseDir CacheUpdater CacheUpdaterFacade ApplicationManager.getApplication().runReadAction ApplicationManager.getApplication().runWriteAction createXmlTag getSelectedTextEditor VirtualFileManager.getInstance().getFileSystem ActionPlaces PlatformDataKeys DataConstants DataKey.create TransactionGuard ProjectLevelVcsManager VcsException VcsRoot ShowSettingsUtil CreateElementActionBase RefactoringActionHandler BasicEditorPopupActionGroup TreeUtil IdeEventQueue FileTypeManagerEx Messages.showMessageDialog Messages.showErrorDialog Messages.showInfoMessage FileChooserFactory ProjectView.getInstance StandardDartboardPlacement createNotification NotificationGroup .addAction(new Action executeAndGetResult executeOnPooledThread JBPopupFactory JBList JBTable ModalityState ProgressIndicator ProgressManager ReadonlyStatusHandler ShowFilePathAction StatusBar ToolWindowAnchor ToolWindowManager ProjectManager.getInstance().getOpenProjects import com.intellij.util.ui.UIUtil import com.intellij.openapi.ui.DialogWrapper extends AnAction extends LightweightAction createFromText resolveScope PsiManager.getInstance @Override public void update public void update(AnAction @Override public void actionPerformed com.intellij.ide.actions.OpenFileAction com.intellij.openapi.editor.ex com.intellij.openapi.vfs.LocalFileSystem com.intellij.openapi.vcs com.intellij.openapi.roots.ProjectRootManager getInstanceMethod getContentRoots ProjectSettingsService getProjectFilePath getBasePath isReadAccessAllowed isDispatchThread getComponents( getComponent( registerComponent ProjectComponent ApplicationComponent ModuleComponent BaseComponent getProjectDir registerProjectComponent registerApplicationComponent getProjectIdeDependencies ShowUsagesAction VcsConfiguration com.intellij.codeInsight.generation com.intellij.codeInsight.intention com.intellij.codeInsight.completion public void registerExternalResourceProvider SchemaProvider extends RenamePsiElementProcessor extends RenamePsiElementProcessorBase ProjectTaskManager EditorColors"

REM Find all Java files
dir /s /b "%SOURCE_DIR%\*.java" > "%TEMP_FILES%"

echo Searching for potentially problematic API usage...

REM Count total files
set /a TOTAL_FILES=0
for /f %%f in (%TEMP_FILES%) do set /a TOTAL_FILES+=1

REM Initialize counters
set /a TOTAL_ISSUES=0
set /a AFFECTED_FILES=0

REM Process each file
set PROCESSED=0

REM Create empty match file
type nul > "%TEMP_MATCHES%"

echo Scanning %TOTAL_FILES% Java files...

for /f "delims=" %%f in (%TEMP_FILES%) do (
    set /a PROCESSED+=1
    
    REM Progress indicator
    set /a MOD=!PROCESSED! %% 10
    if !MOD! equ 0 (
        echo Processed !PROCESSED! of %TOTAL_FILES% files...
    )
    
    REM Check for patterns
    set "FILE_HAS_ISSUE=false"
    
    for %%p in (%API_PATTERNS%) do (
        findstr /c:"%%p" "%%f" >nul 2>&1
        if !ERRORLEVEL! equ 0 (
            REM Found a match
            echo %%f:%%p >> "%TEMP_MATCHES%"
            set "FILE_HAS_ISSUE=true"
            set /a TOTAL_ISSUES+=1
        )
    )
    
    if "!FILE_HAS_ISSUE!"=="true" (
        set /a AFFECTED_FILES+=1
    )
)

echo %GREEN%Scan complete. Found %TOTAL_ISSUES% potential issues in %AFFECTED_FILES% files.%NC%

REM Add summary to output file
>> "%OUTPUT_FILE%" (
    echo ## Summary
    echo.
    echo * Total Java files scanned: %TOTAL_FILES%
    echo * Files with potential compatibility issues: %AFFECTED_FILES%
    echo * Total potential issues found: %TOTAL_ISSUES%
    echo.
    echo ## Detailed Issue List
    echo.
)

REM Process and add issues to output file
echo Processing issues and generating report...

REM Simple categorization by filename patterns
for /f "tokens=1,2 delims=:" %%a in (%TEMP_MATCHES%) do (
    set "FILE=%%a"
    set "PATTERN=%%b"
    
    >> "%OUTPUT_FILE%" (
        echo ### Issue in !FILE!
        echo.
        echo * Potential problem: `!PATTERN!`
        
        REM Add suggested fix based on pattern
        if "!PATTERN!"=="getBaseDir" (
            echo * **Suggested fix:** Replace `Project.getBaseDir()` with `CompatibilityUtil.getProjectBaseDir(project)`
        ) else if "!PATTERN!"=="CacheUpdater" (
            echo * **Suggested fix:** Replace with `CompatibilityUtil.refreshAll(project)`
        ) else if "!PATTERN!"=="ApplicationManager.getApplication().runReadAction" (
            echo * **Suggested fix:** Replace with `CompatibilityUtil.runReadAction(() -> { ... })`
        ) else if "!PATTERN!"=="ApplicationManager.getApplication().runWriteAction" (
            echo * **Suggested fix:** Replace with `CompatibilityUtil.runWriteAction(() -> { ... })`
        ) else if "!PATTERN!"=="getSelectedTextEditor" (
            echo * **Suggested fix:** Replace with `CompatibilityUtil.getSelectedTextEditor(project)`
        ) else if "!PATTERN!"=="createXmlTag" (
            echo * **Suggested fix:** Replace with `XmlElementFactory.getInstance(project).createTagFromText('<tag></tag>')`
        ) else if "!PATTERN!"=="update(" (
            echo * **Suggested fix:** Replace `update(AnActionEvent)` with `@Override public void update(@NotNull AnActionEvent e) {`
        ) else (
            echo * Check compatibility with IntelliJ IDEA 2025.1.1.1 and use `CompatibilityUtil` methods where appropriate
        )
        
        echo.
    )
)

REM Add compatibility guide
>> "%OUTPUT_FILE%" (
    echo ## Compatibility Guide
    echo.
    echo ### Key IntelliJ IDEA 2025.1.1.1 Compatibility Requirements:
    echo.
    echo 1. **Replace deprecated Project methods**
    echo    - Use `CompatibilityUtil.getProjectBaseDir(project)` instead of `project.getBaseDir()`
    echo    - Use `ProjectRootManager.getInstance(project).getContentRoots()` for content roots
    echo.
    echo 2. **Update Actions**
    echo    - Override `getActionUpdateThread()` in AnAction implementations
    echo    - Make sure action updates happen on correct threads
    echo.
    echo 3. **Update Threading**
    echo    - Use `CompatibilityUtil.runReadAction()` instead of direct ApplicationManager calls
    echo    - Use `CompatibilityUtil.runWriteAction()` for write operations
    echo    - Use virtual threads with `ThreadUtils.createVirtualThreadExecutor()` for better performance
    echo.
    echo 4. **Update Service Management**
    echo    - Use `@Service` annotation instead of component registration
    echo    - Register services in plugin.xml with appropriate level (application/project)
    echo    - Remove usage of ProjectComponent, ApplicationComponent, etc.
    echo.
    echo 5. **Plugin Configuration**
    echo    - Use full element tags in plugin.xml (no shorthand)
    echo    - Specify proper since/until build numbers
    echo    - Use only necessary plugin dependencies
    echo.
    echo 6. **UI Updates**
    echo    - Update to newer notification API
    echo    - Use new dialog API methods
    echo.
    echo 7. **File System Access**
    echo    - Use VfsUtil methods consistently
    echo    - Handle invalid files properly
    echo.
    echo 8. **PSI Operations**
    echo    - Always run in read actions
    echo    - Handle null results properly
)

REM Clean up
del "%TEMP_MATCHES%" 2>nul
del "%TEMP_FILES%" 2>nul

echo %GREEN%Compatibility analysis complete! Report written to: %OUTPUT_FILE%%NC%
echo.
echo Next steps:
echo   1. Review the detailed compatibility report
echo   2. Prioritize fixing the most common issues first
echo   3. Use CompatibilityUtil for consistent API access across IntelliJ versions
echo   4. Run this scan again after making changes to verify improvements

endlocal